package com.vinson.addev.services;

import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.ResultReceiver;

import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.socks.library.KLog;
import com.vinson.addev.App;
import com.vinson.addev.model.ws.WSEvent;
import com.vinson.addev.tools.Config;
import com.vinson.addev.utils.Constants;
import com.xdandroid.hellodaemon.AbsWorkService;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import im.zego.zegoexpress.callback.IZegoEventHandler;
import im.zego.zegoexpress.constants.ZegoEngineState;
import im.zego.zegoexpress.constants.ZegoPublisherState;
import im.zego.zegoexpress.constants.ZegoRoomState;
import im.zego.zegoexpress.constants.ZegoUpdateType;
import im.zego.zegoexpress.entity.ZegoStream;
import im.zego.zegoexpress.entity.ZegoUser;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class WSService extends AbsWorkService {
    public static final int RR_STOP_RECORD_SUCCESS = 1001;
    public static final int RR_ALREADY_RECORDING = 1010;
    public static final int RR_RESTART_CAMERA_SUCCESS = 1020;

    private static final int REQUEST_RETRY_DELAY = 30 * 1000;
    private static final int MSG_RETRY_RECORD = 1;
    private static final int MSG_LOAD_AUTHORITY = 3;
    private static final int MSG_LOAD_CONFIG = 4;
    private static final int MSG_OPEN_WEBSOCKET = 5;
    private static final int MSG_UPLOAD_DUMP_FILES = 6;
    private static final int MSG_UPLOAD_LOGS = 7;
    private static final int MSG_SCHEDULE_TASKS = 8;
    private static final int MSG_START_PUSH_STREAM = 101;
    private static final int MSG_STOP_PUSH_STREAM = 110;
    private static final int MSG_PLAY_SPECIAL_VIDEO = 120;
    private static final int MSG_RESTART_CAMERA_REQUEST = 210;
    private static final int MSG_STOP_RECORD_REQUEST = 201;
    private static final int WS_CLOSE_CODE = 1000;
    ZegoUser mZegoUser;
    String roomId;
    String localStreamId;
    private ExecutorService mThreadPool;
    private ConnectivityManager mConnectivityManager;
    private boolean mNetworkAvailable;
    private boolean mStopped = true;
    private WebSocket mWebSocket;
    private Gson mGson;
    private String pushStreamRequester;
    IZegoEventHandler mEventHandler = new IZegoEventHandler() {
        @Override
        public void onEngineStateUpdate(ZegoEngineState state) {
            super.onEngineStateUpdate(state);
        }

        @Override
        public void onRoomStateUpdate(String roomID, ZegoRoomState state, int errorCode,
                                      JSONObject extendedData) {
            super.onRoomStateUpdate(roomID, state, errorCode, extendedData);
        }

        @Override
        public void onRoomUserUpdate(String roomID, ZegoUpdateType updateType,
                                     ArrayList<ZegoUser> userList) {
            super.onRoomUserUpdate(roomID, updateType, userList);
        }

        @Override
        public void onRoomStreamUpdate(String roomID, ZegoUpdateType updateType,
                                       ArrayList<ZegoStream> streamList) {
            super.onRoomStreamUpdate(roomID, updateType, streamList);
        }

        @Override
        public void onPublisherStateUpdate(String streamID, ZegoPublisherState state,
                                           int errorCode, JSONObject extendedData) {
            super.onPublisherStateUpdate(streamID, state, errorCode, extendedData);
            KLog.d(streamID + "," + state + ", " + errorCode);
            if (streamID.equals(Config.getDeviceSerial()) && state.equals(ZegoPublisherState.PUBLISHING)) {
                WSEvent event = new WSEvent();
                event.target = new String[]{pushStreamRequester};
                event.what = Constants.WS_REMOTE_PUSH_STREAM_SUCCESS;
                event.data = streamID;
                mWebSocket.send(mGson.toJson(event));
            }

        }
    };
    private HandlerThread mHandlerThread;
    private Handler mResultHandler;
    MyResultReceiver resultReceiver = new MyResultReceiver(mResultHandler);
    private Handler mHandler = new Handler(msg -> {
        if (mStopped) {
            KLog.w("DataLoader stopped, ignore msg:" + msg.what);
            return true;
        }
        switch (msg.what) {
            case MSG_RETRY_RECORD:
//                retryRecord();
                break;
            case MSG_LOAD_AUTHORITY:
//                loadAuthority();
                break;
            case MSG_LOAD_CONFIG:
//                loadConfig();
                break;
            case MSG_OPEN_WEBSOCKET:
                if (mThreadPool != null && !mThreadPool.isShutdown())
                    mThreadPool.execute(this::openWebSocket);
                break;
            case MSG_UPLOAD_DUMP_FILES:
                if (mThreadPool != null && !mThreadPool.isShutdown())
//                    mThreadPool.execute(this::uploadDumpFiles);
                    break;
            case MSG_UPLOAD_LOGS:
                if (mThreadPool != null && !mThreadPool.isShutdown())
//                    mThreadPool.execute(this::uploadLogs);
                    break;
            case MSG_SCHEDULE_TASKS:
//                timerTask();
                break;
            case MSG_START_PUSH_STREAM:
                startPushStream();
                break;
            case MSG_STOP_RECORD_REQUEST:
                // release camera before start to push stream
                RecorderService.stopRecording(this, resultReceiver);
                break;
            case MSG_STOP_PUSH_STREAM:
                stopPushStream();
                break;
            case MSG_RESTART_CAMERA_REQUEST:
                RecorderService.restartCamera(this, resultReceiver);
                break;
        }
        return true;
    });
    private ConnectivityManager.NetworkCallback mNetworkCallback =
            new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(Network network) {
                    KLog.w("network available");
                    mNetworkAvailable = true;
                    App.getInstance().showToast("网络已连接");
                    start();
                }

                @Override
                public void onLost(Network network) {
                    KLog.w("network lost");
                    mNetworkAvailable = false;
                    App.getInstance().showToast("网络已断开");
                    stop();
                }
            };

    @Override
    public void onCreate() {
        super.onCreate();
        mThreadPool = Executors.newFixedThreadPool(5);
        mHandlerThread = new HandlerThread("resultReceiver");
        mHandlerThread.start();
        mResultHandler = new Handler(mHandlerThread.getLooper());

        mConnectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        assert mConnectivityManager != null;
        NetworkInfo networkInfo = mConnectivityManager.getActiveNetworkInfo();
        mNetworkAvailable = (networkInfo != null && networkInfo.isConnected());
        mConnectivityManager.registerNetworkCallback(new NetworkRequest.Builder().build(),
                mNetworkCallback);
        mGson = new Gson();
//        ZegoExpressEngine.destroyEngine(null);

        RecorderService.startRecording(this, resultReceiver);
//        startService(new Intent(this, SerialPortService.class));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        KLog.d("onStartCommand: [flags]" + flags + ", [startId]" + startId);
        if (intent != null) KLog.d(intent.toString());
        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        mThreadPool.shutdown();
        mThreadPool = null;
        mHandlerThread.quitSafely();
        try {
            mHandlerThread.join();
            mHandlerThread = null;
            mHandler = null;
        } catch (final InterruptedException e) {
            KLog.d("Exception!");
        }
        mConnectivityManager.unregisterNetworkCallback(mNetworkCallback);

        //TODO start service before destroy
        this.sendBroadcast(new Intent("com.vinson.addev.wsservice.restart"));
        super.onDestroy();
    }

    private void start() {
        mStopped = false;
        // 1. load device config
        // 2. open websocket
        mHandler.sendEmptyMessage(MSG_OPEN_WEBSOCKET);
        // 3. upload records that record when disconnect or upload faield
        // 4. schedule tasks

//        App.getEngine().setEventHandler(eventHandler);
    }

    private void stop() {
        mStopped = true;
        mHandler.removeCallbacksAndMessages(null);
        // report device offline
        // close websocket
    }

    private void closeWebSocket() {
        KLog.d("close websocket");
        if (mWebSocket != null) {
            mWebSocket.close(WS_CLOSE_CODE, null);
            mWebSocket = null;
            KLog.d("websocket closed successfully");
        } else {
            KLog.d("websocket already closed");
        }
    }

    private void openWebSocket() {
        KLog.d("openWebSocket");
        if (!mNetworkAvailable) {
            KLog.w("network unavailable when open websocket");
            return;
        }

        if (mWebSocket != null) {
            KLog.w("websocket already opened, close it first");
            closeWebSocket();
        }

        OkHttpClient client = new OkHttpClient.Builder().readTimeout(0, TimeUnit.SECONDS).build();
        Request request =
                new Request.Builder().url(Constants.WS_BASE_URL + Constants.WS_PATH).build();
        client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, okhttp3.Response response) {
                KLog.d("websocket onOpen");
                mWebSocket = webSocket;
                KLog.d("report device info through websocket");
                WSEvent event = new WSEvent();
                event.target = new String[]{"server"};
                event.what = "report";
                event.data = Config.getDeviceSerial();
                mWebSocket.send(mGson.toJson(event));
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                KLog.d("onMessage(text):" + text);
                WSEvent event = mGson.fromJson(text, WSEvent.class);
                switch (event.what) {
                    case "dev.upload.dump": {
                        mHandler.sendEmptyMessage(MSG_UPLOAD_DUMP_FILES);
                        break;
                    }
                    case "dev.upload.log": {
                        mHandler.removeMessages(MSG_UPLOAD_LOGS);
                        mHandler.sendEmptyMessage(MSG_UPLOAD_LOGS);
                        break;
                    }
                    case "upgrade": {
                        mHandler.removeCallbacksAndMessages(null);
//                        mListener.onUpgrade(event.data);
                        break;
                    }
                    case Constants.WS_REQUEST_PUSH_STREAM:
                        pushStreamRequester = event.data;
                        mHandler.removeMessages(MSG_STOP_RECORD_REQUEST);
                        mHandler.sendEmptyMessage(MSG_STOP_RECORD_REQUEST);
                        break;
                    case Constants.WS_REQUEST_STOP_PUSH_STREAM:
                        pushStreamRequester = event.data;
                        mHandler.removeMessages(MSG_STOP_PUSH_STREAM);
                        mHandler.sendEmptyMessage(MSG_STOP_PUSH_STREAM);
                        break;
                    default:
                        KLog.w("unsupported event");
                        break;
                }
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                KLog.d("websocket onClosed:" + code + "," + reason);
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, okhttp3.Response response) {
                KLog.w("websocket onFailure:" + t.getMessage());
                if (mStopped) {
                    KLog.w("DataLoader stopped, ignore websocket failure.");
                    return;
                }
                KLog.w("retry open websocket 10s later");
                mHandler.removeMessages(MSG_OPEN_WEBSOCKET);
                mHandler.sendEmptyMessageDelayed(MSG_OPEN_WEBSOCKET, 10 * 1000);
            }
        });
        client.dispatcher().executorService().shutdown();
    }

    void startPushStream() {
        KLog.d();
        App.getEngine().setEventHandler(null);
        App.getEngine().setEventHandler(mEventHandler);

        roomId = Config.getDeviceSerial();
        Config.setStreamId(roomId);
        localStreamId = roomId;
        // create user
        mZegoUser = new ZegoUser(roomId);
        // login room
        App.getEngine().loginRoom(roomId, mZegoUser);
        // start push stream
        App.getEngine().startPublishingStream(localStreamId);
    }

    void stopPushStream() {
        App.getEngine().stopPublishingStream();
        if (mZegoUser != null) {
            App.getEngine().logoutRoom(roomId);
        }
        App.getEngine().setEventHandler(null);
        mHandler.sendEmptyMessage(MSG_RESTART_CAMERA_REQUEST);
    }

    @Override
    public Boolean shouldStopService(Intent intent, int flags, int startId) {
        return null;
    }

    @Override
    public void startWork(Intent intent, int flags, int startId) {

    }

    @Override
    public void stopWork(Intent intent, int flags, int startId) {

    }

    @Override
    public Boolean isWorkRunning(Intent intent, int flags, int startId) {
        return null;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent, Void alwaysNull) {
        return null;
    }

    @Override
    public void onServiceKilled(Intent rootIntent) {

    }

    class MyResultReceiver extends ResultReceiver {

        /**
         * Create a new ResultReceive to receive results.  Your
         * {@link #onReceiveResult} method will be called from the thread running
         * <var>handler</var> if given, or from an arbitrary thread if null.
         *
         * @param handler
         */
        public MyResultReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            super.onReceiveResult(resultCode, resultData);
            switch (resultCode) {
                case RR_STOP_RECORD_SUCCESS:
                    mHandler.removeMessages(MSG_START_PUSH_STREAM);
                    mHandler.sendEmptyMessage(MSG_START_PUSH_STREAM);
                    break;
                case RR_ALREADY_RECORDING:
                    KLog.w("Already in recording states, don't need to re-recording operations");
                    break;
                case RR_RESTART_CAMERA_SUCCESS:
                    KLog.i("Restart camera success, begin loop recording");
                    break;
                default:
                    KLog.w("known result code :" + resultCode);
            }
        }
    }
}

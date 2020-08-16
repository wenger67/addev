package com.vinson.addev.services;

import android.app.Service;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.vinson.addev.App;
import com.vinson.addev.model.ws.WSEvent;
import com.vinson.addev.tools.Config;
import com.vinson.addev.utils.Constants;

import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import im.zego.zegoexpress.callback.IZegoEventHandler;
import im.zego.zegoexpress.constants.ZegoPublisherState;
import im.zego.zegoexpress.entity.ZegoUser;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class WSService extends Service {
    public static final String TAG = WSService.class.getSimpleName();

    private static final int REQUEST_RETRY_DELAY = 30 * 1000;

    private static final int MSG_RETRY_RECORD = 1;
    private static final int MSG_LOAD_AUTHORITY = 3;
    private static final int MSG_LOAD_CONFIG = 4;
    private static final int MSG_OPEN_WEBSOCKET = 5;
    private static final int MSG_UPLOAD_DUMP_FILES = 6;
    private static final int MSG_UPLOAD_LOGS = 7;
    private static final int MSG_SCHEDULE_TASKS = 8;
    private static final int MSG_START_PUSH_STREAM = 101;

    private static final int WS_CLOSE_CODE = 1000;

    private Binder mBinder = new Binder();
    private ExecutorService mThreadPool;
    private ConnectivityManager mConnectivityManager;
    private boolean mNetworkAvailable;
    private Callback mCallback = new Callback();
    private boolean mStopped = true;
    private WebSocket mWebSocket;
    private Gson mGson;
    private String pushStreamRequester;

    @Override
    public void onCreate() {
        super.onCreate();
        mThreadPool = Executors.newFixedThreadPool(5);
        mConnectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        assert mConnectivityManager != null;
        NetworkInfo networkInfo = mConnectivityManager.getActiveNetworkInfo();
        mNetworkAvailable = (networkInfo != null && networkInfo.isConnected());
        mConnectivityManager.registerNetworkCallback(new NetworkRequest.Builder().build(), mNetworkCallback);

        mGson = new Gson();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mThreadPool.shutdown();
        mThreadPool = null;
        mConnectivityManager.unregisterNetworkCallback(mNetworkCallback);
    }

    private ConnectivityManager.NetworkCallback mNetworkCallback = new ConnectivityManager.NetworkCallback() {
        @Override
        public void onAvailable(Network network) {
            Log.w(TAG, "network available");
            mNetworkAvailable = true;
            App.getInstance().showToast("网络已连接");
            start();
            mCallback.onNetworkChanged(true);
        }

        @Override
        public void onLost(Network network) {
            Log.w(TAG, "network lost");
            mNetworkAvailable = false;
            App.getInstance().showToast("网络已断开");
            stop();
            mCallback.onNetworkChanged(false);
        }
    };

    private void start() {
        mStopped = false;
        // 1. load device config
        // 2. open websocket
        mHandler.sendEmptyMessage(MSG_OPEN_WEBSOCKET);
        // 3. upload records that record when disconnect or upload faield
        // 4. schedule tasks

        App.getEngine().setEventHandler(eventHandler);
    }

    private void stop() {
        mStopped = true;
        mHandler.removeCallbacksAndMessages(null);
        // report device offline
        // close websocket
    }

    public class Binder extends android.os.Binder {

        public void setCallback(WSService.Callback callback) {
            mCallback = callback;
        }

        public boolean networkAvailable() {
            return mNetworkAvailable;
        }
    }

    public static class Callback {
        public void onInfo(String tips, boolean loading, int delay) {
        }


        public void onStaffRemoved(int[] staffIdArray) {
        }

        public void onAuthorityUpdate() {
        }

        public void onDeviceRemoved() {
        }

        public void onUpgrade(String bootlegs) {
        }

        public void onNetworkChanged(boolean connected) {
        }
    }

    private void closeWebSocket() {
        Log.d(TAG, "close websocket");
        if (mWebSocket != null) {
            mWebSocket.close(WS_CLOSE_CODE, null);
            mWebSocket = null;
            Log.d(TAG, "websocket closed successfully");
        } else {
            Log.d(TAG, "websocket already closed");
        }
    }

    private void openWebSocket() {
        Log.d(TAG, "openWebSocket");
        if (!mNetworkAvailable) {
            Log.w(TAG, "network unavailable when open websocket");
            return;
        }

        if (mWebSocket != null) {
            Log.w(TAG, "websocket already opened, close it first");
            closeWebSocket();
        }

        OkHttpClient client = new OkHttpClient.Builder().readTimeout(0, TimeUnit.SECONDS).build();
        Request request = new Request.Builder().url(Constants.WS_BASE_URL + Constants.WS_PATH).build();
        client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, okhttp3.Response response) {
                Log.d(TAG, "websocket onOpen");
                mWebSocket = webSocket;
                Log.d(TAG, "report device info through websocket");
                WSEvent event = new WSEvent();
                event.target = new String[]{"server"};
                event.what = "report";
                event.data = Config.getDeviceSerial();
                mWebSocket.send(mGson.toJson(event));
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                Log.d(TAG, "onMessage(text):" + text);
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
                    case "push.stream":
                        pushStreamRequester = event.data;
                        mHandler.removeMessages(MSG_START_PUSH_STREAM);
                        mHandler.sendEmptyMessage(MSG_START_PUSH_STREAM);
                        break;
                    default:
                        Log.w(TAG, "unsupported event");
                        break;
                }
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                Log.d(TAG, "websocket onClosed:" + code + "," + reason);
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, okhttp3.Response response) {
                Log.w(TAG, "websocket onFailure:" + t.getMessage());
                if (mStopped) {
                    Log.w(TAG, "DataLoader stopped, ignore websocket failure.");
                    return;
                }
                Log.w(TAG, "retry open websocket 10s later");
                mHandler.removeMessages(MSG_OPEN_WEBSOCKET);
                mHandler.sendEmptyMessageDelayed(MSG_OPEN_WEBSOCKET, 10 * 1000);
            }
        });
        client.dispatcher().executorService().shutdown();
    }

    private Handler mHandler = new Handler(msg -> {
        if (mStopped) {
            Log.w(TAG, "DataLoader stopped, ignore msg:" + msg.what);
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
                // TODO push stream
                startPushStream();
        }
        return true;
    });

    void startPushStream() {
        String mStreamId = Config.getDeviceSerial();
        Config.setStreamId(Config.getDeviceSerial());
        ZegoUser user = new ZegoUser(Config.getDeviceSerial());
        App.getEngine().loginRoom(Config.getDeviceSerial(), user);

        WSEvent event = new WSEvent();
        event.target = new String[]{pushStreamRequester};
        event.what = "push.stream.start";
        event.data = "roomid:" + Config.getDeviceSerial();
        mWebSocket.send(mGson.toJson(event));
        App.getEngine().startPublishingStream(mStreamId);
    }

    IZegoEventHandler eventHandler = new IZegoEventHandler() {
        @Override
        public void onPublisherStateUpdate(String streamID, ZegoPublisherState state, int errorCode, JSONObject extendedData) {
            super.onPublisherStateUpdate(streamID, state, errorCode, extendedData);
            Log.d(TAG, state + "");
            if (state.equals(ZegoPublisherState.PUBLISHING)) {
                WSEvent event = new WSEvent();
                event.target = new String[]{pushStreamRequester};
                event.what = "push.stream.success";
                event.data = "streamid:" + streamID;
                mWebSocket.send(mGson.toJson(event));
            }
        }
    };
}

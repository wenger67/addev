package com.vinson.addev;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.ImageView;

import com.mikepenz.community_material_typeface_library.CommunityMaterial;
import com.mikepenz.iconics.IconicsDrawable;
import com.vinson.addev.initial.ConfigActivity;
import com.vinson.addev.services.WSService;
import com.vinson.addev.tools.NetworkObserver;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = MainActivity.class.getSimpleName();

    private NetworkObserver mNetwork;
    ImageView mNetworkStateView;
    private IconicsDrawable mNetworkStateDrawable;
    private Handler mHandler = new Handler(this::handleMessage);
    private static final int MSG_NETWORK_CHANGE = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mNetworkStateView = findViewById(R.id.iv_network_state);
        mNetworkStateDrawable = new IconicsDrawable(this).sizeDp(24);
        mNetworkStateView.setImageDrawable(mNetworkStateDrawable);

        mNetwork = new NetworkObserver(this);
        mNetwork.register(connected -> {
            mHandler.sendEmptyMessage(MSG_NETWORK_CHANGE);
            if (connected ) {
                Log.d(TAG, "network connected, verify device");
            } else {
                Log.w(TAG, "network connected but permission not granted, ignore");
            }
        });

        Intent intent = new Intent(this, WSService.class);
        bindService(intent, mServiceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "onServiceConnected");
            mDataLoader = (WSService.Binder) service;

            mHandler.post(() -> {
                if (mDataLoader.networkAvailable()) {
                    mNetworkStateDrawable.icon(CommunityMaterial.Icon2.cmd_wifi).color(Color.GREEN);
                } else {
                    mNetworkStateDrawable.icon(CommunityMaterial.Icon2.cmd_wifi_off).color(Color.RED);
                }
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "onServiceDisconnected");
            mDataLoader.setListener(new DataLoader.Listener());
            mDataLoader = null;
        }
    };

    private boolean handleMessage(Message msg) {
        if (isFinishing() || isDestroyed()) return true;
        switch (msg.what) {
            case MSG_NETWORK_CHANGE:
                boolean disconnected = mNetwork.disconnected();
                if (!disconnected) {
                    mNetworkStateDrawable.icon(CommunityMaterial.Icon2.cmd_wifi).color(Color.GREEN);
                } else {
                    mNetworkStateDrawable.icon(CommunityMaterial.Icon2.cmd_wifi_off).color(Color.RED);
                }
                break;
        }
        return false;
    }
}

//package com.zhaoxin.aicamera.data;
//
//
//        import android.app.Service;
//        import android.content.Intent;
//        import android.icu.util.Calendar;
//        import android.net.ConnectivityManager;
//        import android.net.Network;
//        import android.net.NetworkInfo;
//        import android.net.NetworkRequest;
//        import android.os.Environment;
//        import android.os.Handler;
//        import android.os.IBinder;
//        import android.os.StatFs;
//        import android.util.Log;
//
//        import androidx.annotation.NonNull;
//        import androidx.annotation.Nullable;
//
//        import com.google.gson.Gson;
//        import com.zhaoxin.aicamera.App;
//        import com.zhaoxin.aicamera.data.db.TableAuthority;
//        import com.zhaoxin.aicamera.data.db.TableRecord;
//        import com.zhaoxin.aicamera.data.db.TableStaff;
//        import com.zhaoxin.aicamera.data.net.api.APIService;
//        import com.zhaoxin.aicamera.data.net.model.Authority;
//        import com.zhaoxin.aicamera.data.net.model.DeviceConfig;
//        import com.zhaoxin.aicamera.data.net.model.QueryBase;
//        import com.zhaoxin.aicamera.data.net.model.Record;
//        import com.zhaoxin.aicamera.data.net.model.Records;
//        import com.zhaoxin.aicamera.data.net.model.Report;
//        import com.zhaoxin.aicamera.data.net.model.ResultBase;
//        import com.zhaoxin.aicamera.data.net.model.Staff;
//        import com.zhaoxin.aicamera.data.net.model.StaffList;
//        import com.zhaoxin.aicamera.data.net.model.StaffQuery;
//        import com.zhaoxin.aicamera.data.net.model.Verify;
//        import com.zhaoxin.aicamera.data.net.model.WSEvent;
//        import com.zhaoxin.aicamera.utils.Config;
//
//        import java.io.File;
//        import java.io.FileInputStream;
//        import java.io.FileOutputStream;
//        import java.io.IOException;
//        import java.io.InputStream;
//        import java.io.OutputStream;
//        import java.util.Arrays;
//        import java.util.Random;
//        import java.util.concurrent.ExecutorService;
//        import java.util.concurrent.Executors;
//        import java.util.concurrent.TimeUnit;
//
//        import okhttp3.MediaType;
//        import okhttp3.MultipartBody;
//        import okhttp3.OkHttpClient;
//        import okhttp3.Request;
//        import okhttp3.RequestBody;
//        import okhttp3.WebSocket;
//        import okhttp3.WebSocketListener;
//        import retrofit2.Call;
//        import retrofit2.Callback;
//        import retrofit2.Response;
//        import retrofit2.Retrofit;
//        import retrofit2.converter.gson.GsonConverterFactory;
//
//        import static com.zhaoxin.aicamera.data.net.model.Report.OFFLINE;
//        import static com.zhaoxin.aicamera.data.net.model.Report.ONLINE;
//
//public class DataLoader extends Service {
//    private static final String TAG = DataLoader.class.getSimpleName();
//
//    private static final String DUMP_FOLDER = "/data/misc/cameraserver/";
//
//    private static final int REQUEST_RETRY_DELAY = 30 * 1000;
//
//    private static final int MSG_RETRY_RECORD = 1;
//    private static final int MSG_LOAD_AUTHORITY = 3;
//    private static final int MSG_LOAD_CONFIG = 4;
//    private static final int MSG_OPEN_WEBSOCKET = 5;
//    private static final int MSG_UPLOAD_DUMP_FILES = 6;
//    private static final int MSG_UPLOAD_LOGS = 7;
//    private static final int MSG_SCHEDULE_TASKS = 8;
//
//    private static final int WS_CLOSE_CODE = 1000;
//
//    private Binder mBinder = new Binder();
//    private ConnectivityManager mConnectivityManager;
//    private boolean mNetworkAvailable;
//    private APIService mService;
//    private WebSocket mWebSocket;
//    private Gson mGson;
//    private ExecutorService mThreadPool;
//    private Listener mListener = new Listener();
//    private boolean mStopped;
//    private boolean mUploadingDumpFiles = false;
//
//    @Override
//    public void onCreate() {
//        super.onCreate();
//        Log.d(TAG, "onCreate");
//        mGson = new Gson();
//        Retrofit retrofit = new Retrofit.Builder()
//                .baseUrl(Config.apiServer())
//                .addConverterFactory(GsonConverterFactory.create())
//                .build();
//        mService = retrofit.create(APIService.class);
//        mThreadPool = Executors.newFixedThreadPool(5);
//
//        mConnectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
//        assert mConnectivityManager != null;
//        NetworkInfo networkInfo = mConnectivityManager.getActiveNetworkInfo();
//        mNetworkAvailable = (networkInfo != null && networkInfo.isConnected());
//        mConnectivityManager.registerNetworkCallback(new NetworkRequest.Builder().build(), mNetworkCallback);
//    }
//
//    @Nullable
//    @Override
//    public IBinder onBind(Intent intent) {
//        Log.d(TAG, "onBind:" + intent);
//        return mBinder;
//    }
//
//    @Override
//    public boolean onUnbind(Intent intent) {
//        Log.d(TAG, "onUnBind:" + intent);
//        return super.onUnbind(intent);
//    }
//
//    @Override
//    public void onDestroy() {
//        super.onDestroy();
//        Log.d(TAG, "onDestroy");
//        stop();
//        mThreadPool.shutdown();
//        mThreadPool = null;
//        mConnectivityManager.unregisterNetworkCallback(mNetworkCallback);
//    }
//
//    private void start() {
//        mStopped = false;
//        // 0. load device config
//        mHandler.sendEmptyMessage(MSG_LOAD_CONFIG);
//        // 1. load full staff list
//        loadStaff(new int[0]);
//        // 2. open websocket (time-consuming)
//        mHandler.sendEmptyMessage(MSG_OPEN_WEBSOCKET);
//        // 3. upload records that record failed
//        mHandler.sendEmptyMessage(MSG_RETRY_RECORD);
//        // 4. schedule tasks in 1:00-3:00
//        Calendar calendar = Calendar.getInstance();
//        int hour = calendar.get(Calendar.HOUR_OF_DAY);
//        int delay = 24 - hour + 1;
//        Random random = new Random();
//        int salt = random.nextInt(60 * 60);
//        Log.w(TAG, "schedule tasks, now:" + hour + " delay:" + delay + " salt:" + salt);
//        mHandler.sendEmptyMessageDelayed(MSG_SCHEDULE_TASKS, (delay * 60 * 60 + salt) * 1000);
//    }
//
//    private void stop() {
//        mStopped = true;
//        mHandler.removeCallbacksAndMessages(null);
//        closeWebSocket();
//        report(OFFLINE);
//    }
//
//    private void openWebSocket() {
//        Log.d(TAG, "open websocket");
//        if (!mNetworkAvailable) {
//            Log.w(TAG, "network unavailable when open websocket");
//            return;
//        }
//
//        if (mWebSocket != null) {
//            Log.w(TAG, "websocket already opened, close it first");
//            closeWebSocket();
//        }
//
//        OkHttpClient client = new OkHttpClient.Builder().readTimeout(0, TimeUnit.SECONDS).build();
//        Request request = new Request.Builder().url(Config.wsServer() + "api/ws/endpoint").build();
//        client.newWebSocket(request, new WebSocketListener() {
//            @Override
//            public void onOpen(WebSocket webSocket, okhttp3.Response response) {
//                Log.d(TAG, "websocket onOpen");
//                mWebSocket = webSocket;
//                Log.d(TAG, "report device info through websocket");
//                WSEvent event = new WSEvent();
//                event.target = new String[]{"server"};
//                event.what = "report";
//                event.data = Config.Serial;
//                mWebSocket.send(mGson.toJson(event));
//            }
//
//            @Override
//            public void onMessage(WebSocket webSocket, String text) {
//                Log.d(TAG, "onMessage(text):" + text);
//                WSEvent event = mGson.fromJson(text, WSEvent.class);
//                switch (event.what) {
//                    case "feat.add": {
//                        WSEvent.IdArray idArray = mGson.fromJson(event.data, WSEvent.IdArray.class);
//                        loadStaff(idArray.id);
//                        break;
//                    }
//                    case "feat.del": {
//                        WSEvent.IdArray idArray = mGson.fromJson(event.data, WSEvent.IdArray.class);
//                        if (idArray.id != null && idArray.id.length > 0) {
//                            TableStaff.delete(idArray.id);
//                            mListener.onStaffRemoved(idArray.id);
//                            mListener.onInfo("数据更新完成", true, 3000);
//                        }
//                        break;
//                    }
//                    case "permission": {
//                        mHandler.removeMessages(MSG_LOAD_AUTHORITY);
//                        mHandler.sendEmptyMessage(MSG_LOAD_AUTHORITY);
//                        break;
//                    }
//                    case "config": {
//                        DeviceConfig config = mGson.fromJson(event.data, DeviceConfig.class);
//                        mHandler.removeMessages(MSG_LOAD_AUTHORITY);
//                        mHandler.sendEmptyMessage(MSG_LOAD_AUTHORITY);
//                        mListener.onConfigUpdate(config);
//                        break;
//                    }
//                    case "dev.del": {
//                        mListener.onDeviceRemoved();
//                        break;
//                    }
//                    case "dev.upload.dump": {
//                        mHandler.sendEmptyMessage(MSG_UPLOAD_DUMP_FILES);
//                        break;
//                    }
//                    case "dev.upload.log": {
//                        mHandler.removeMessages(MSG_UPLOAD_LOGS);
//                        mHandler.sendEmptyMessage(MSG_UPLOAD_LOGS);
//                        break;
//                    }
//                    case "upgrade": {
//                        mHandler.removeCallbacksAndMessages(null);
//                        mListener.onUpgrade(event.data);
//                        break;
//                    }
//
//                    default:
//                        Log.w(TAG, "unsupported event");
//                        break;
//                }
//            }
//
//            @Override
//            public void onClosed(WebSocket webSocket, int code, String reason) {
//                Log.d(TAG, "websocket onClosed:" + code + "," + reason);
//            }
//
//            @Override
//            public void onFailure(WebSocket webSocket, Throwable t, okhttp3.Response response) {
//                Log.w(TAG, "websocket onFailure:" + t.getMessage());
//                if (mStopped) {
//                    Log.w(TAG, "DataLoader stopped, ignore websocket failure.");
//                    return;
//                }
//                Log.w(TAG, "retry open websocket 10s later");
//                mHandler.removeMessages(MSG_OPEN_WEBSOCKET);
//                mHandler.sendEmptyMessageDelayed(MSG_OPEN_WEBSOCKET, 10 * 1000);
//            }
//        });
//        client.dispatcher().executorService().shutdown();
//    }
//
//    private void closeWebSocket() {
//        Log.d(TAG, "close websocket");
//        if (mWebSocket != null) {
//            mWebSocket.close(WS_CLOSE_CODE, null);
//            mWebSocket = null;
//            Log.d(TAG, "websocket closed successfully");
//        } else {
//            Log.d(TAG, "websocket already closed");
//        }
//    }
//
//    private void loadStaff(int[] staffIdArray) {
//        Log.d(TAG, "load staff:" + Arrays.toString(staffIdArray));
//        if (!mNetworkAvailable) {
//            Log.w(TAG, "network unavailable when load staff list");
//            mListener.onInfo("无可用网络", false, -1);
//            return;
//        }
//
//        mListener.onInfo("正在更新人物", true, -1);
//        StaffQuery query = new StaffQuery(staffIdArray);
//        Call<StaffList> call = mService.getStaffList(query);
//        call.enqueue(new Callback<StaffList>() {
//            @Override
//            public void onResponse(@NonNull Call<StaffList> call, @NonNull Response<StaffList> response) {
//                if (mStopped) return;
//                StaffList list = response.body();
//                if (!success(response) || list == null) {
//                    Log.w(TAG, string(response));
//                    Log.w(TAG, "load staff list failed, retry " + REQUEST_RETRY_DELAY + "s later");
//                    mListener.onInfo("数据下载失败", false, -1);
//                    mHandler.postDelayed(() -> loadStaff(staffIdArray), REQUEST_RETRY_DELAY);
//                    return;
//                }
//
//                Log.d(TAG, "load staff successfully, length:" + list.data.length);
//                for (int i = 0; i < list.data.length; ++i) {
//                    Staff staff = list.data[i];
//                    Log.d(TAG, staff.toString());
//                }
//                if (staffIdArray.length == 0) {
//                    TableStaff.delete();
//                }
//                if (list.data.length > 0) {
//                    TableStaff.insert(list.data);
//                    mListener.onStaffAdded(list.data);
//                } else {
//                    mListener.onInfo("数据更新失败", false, -1);
//                }
//            }
//
//            @Override
//            public void onFailure(@NonNull Call<StaffList> call, @NonNull Throwable t) {
//                if (mStopped) return;
//                Log.e(TAG, "load staff list failed:" + t.getMessage());
//                Log.e(TAG, string(call.request()));
//                mListener.onInfo("数据下载失败", false, -1);
//                mHandler.postDelayed(() -> loadStaff(staffIdArray), REQUEST_RETRY_DELAY);
//            }
//        });
//    }
//
//    private void loadAuthority() {
//        Log.d(TAG, "load authority(" + Config.Serial + ")");
//        if (!mNetworkAvailable) {
//            Log.w(TAG, "network unavailable when load authority");
//            mListener.onInfo("无可用网络", false, -1);
//            return;
//        }
//
//        mListener.onInfo("正在更新权限", true, -1);
//        QueryBase params = new QueryBase();
//        Call<Authority> call = mService.getAuthority(params);
//        call.enqueue(new Callback<Authority>() {
//            @Override
//            public void onResponse(@NonNull Call<Authority> call, @NonNull Response<Authority> response) {
//                if (mStopped) return;
//                Authority authority = response.body();
//                if (!success(response) || authority == null) {
//                    Log.d(TAG, string(response));
//                    Log.w(TAG, "load authority failed, retry " + REQUEST_RETRY_DELAY + "s later");
//                    mListener.onInfo("获取权限信息失败", false, -1);
//                    mHandler.removeMessages(MSG_LOAD_AUTHORITY);
//                    mHandler.sendEmptyMessageDelayed(MSG_LOAD_AUTHORITY, REQUEST_RETRY_DELAY);
//                    return;
//                }
//
//                Log.d(TAG, "load authority successfully, length:" + authority.data.length);
//                Log.d(TAG, Arrays.toString(authority.data));
//                TableAuthority.delete();
//                TableAuthority.insert(authority.data);
//                mListener.onAuthorityUpdate();
//                report(ONLINE);
//            }
//
//            @Override
//            public void onFailure(@NonNull Call<Authority> call, @NonNull Throwable t) {
//                if (mStopped) return;
//                Log.e(TAG, "load authority failed:" + t.getMessage());
//                Log.e(TAG, string(call.request()));
//                mListener.onInfo("获取权限信息失败", false, -1);
//                mHandler.removeMessages(MSG_LOAD_AUTHORITY);
//                mHandler.sendEmptyMessageDelayed(MSG_LOAD_AUTHORITY, REQUEST_RETRY_DELAY);
//            }
//        });
//    }
//
//    private void loadConfig() {
//        Log.d(TAG, "load device config(" + Config.Serial + ")");
//        mHandler.removeMessages(MSG_LOAD_CONFIG);
//        if (!mNetworkAvailable) {
//            Log.w(TAG, "network unavailable when load config");
//            mListener.onInfo("无可用网络", false, -1);
//            return;
//        }
//
//        Verify params = new Verify();
//        Call<DeviceConfig> call = mService.verify(params);
//        call.enqueue(new Callback<DeviceConfig>() {
//            @Override
//            public void onResponse(@NonNull Call<DeviceConfig> call, @NonNull Response<DeviceConfig> response) {
//                if (mStopped) return;
//                DeviceConfig config = response.body();
//                if (!success(response) || config == null) {
//                    Log.d(TAG, string(response));
//                    Log.w(TAG, "load config failed, retry " + REQUEST_RETRY_DELAY + "s later");
//                    mListener.onInfo("获取设备配置失败", false, -1);
//                    mHandler.sendEmptyMessageDelayed(MSG_LOAD_CONFIG, REQUEST_RETRY_DELAY);
//                    return;
//                }
//
//                Log.d(TAG, "get config success: " + config);
//                mListener.onConfigUpdate(config);
//            }
//
//            @Override
//            public void onFailure(@NonNull Call<DeviceConfig> call, @NonNull Throwable t) {
//                if (mStopped) return;
//                Log.e(TAG, "load authority failed:" + t.getMessage());
//                Log.e(TAG, string(call.request()));
//                mListener.onInfo("获取设备配置失败", false, -1);
//                mHandler.sendEmptyMessageDelayed(MSG_LOAD_CONFIG, REQUEST_RETRY_DELAY);
//            }
//        });
//    }
//
//    /**
//     * feat更新之后report "online"
//     * <p>
//     * 退出之后report "offline"
//     */
//    private void report(int status) {
//        Log.d(TAG, "report device status:" + status);
//        if (!mNetworkAvailable) {
//            Log.w(TAG, "network unavailable when report device status");
//            return;
//        }
//
//        Report params = new Report(status);
//        Call<ResultBase> call = mService.report(params);
//        call.enqueue(new Callback<ResultBase>() {
//            @Override
//            public void onResponse(@NonNull Call<ResultBase> call, @NonNull Response<ResultBase> response) {
//                if (mStopped) return;
//                if (success(response)) {
//                    Log.d(TAG, "report device status (" + status + ") successfully");
//                } else {
//                    Log.w(TAG, "report device status failed");
//                    Log.w(TAG, string(response));
//                    if (status == ONLINE)
//                        mHandler.postDelayed(() -> report(status), REQUEST_RETRY_DELAY);
//                }
//            }
//
//            @Override
//            public void onFailure(@NonNull Call<ResultBase> call, @NonNull Throwable t) {
//                if (mStopped) return;
//                Log.w(TAG, "report device status failed:" + t.getMessage());
//                Log.w(TAG, string(call.request()));
//                if (status == ONLINE)
//                    mHandler.postDelayed(() -> report(status), REQUEST_RETRY_DELAY);
//            }
//        });
//    }
//
//    private boolean record(Record params) {
//        Log.d(TAG, "record:" + params.id + " " + params.devtime);
//        if (!mNetworkAvailable) {
//            Log.w(TAG, "network unavailable when record staff");
//            return false;
//        }
//
//        Call<ResultBase> call = mService.record(params);
//        Response<ResultBase> response = null;
//        try {
//            response = call.execute();
//        } catch (IOException e) {
//            Log.w(TAG, "request failed:" + e.getMessage());
//        }
//
//        if (response != null && success(response)) {
//            Log.d(TAG, "record (" + params.id + ") successfully");
//            TableRecord.delete(params);
//            return true;
//        } else {
//            Log.d(TAG, "record (" + params.id + ") failed:" + string(response));
//            return false;
//        }
//    }
//
//    private boolean record(Records params) {
//        Log.d(TAG, "record:" + params.data);
//        if (!mNetworkAvailable) {
//            Log.w(TAG, "network unavailable when record staff");
//            return false;
//        }
//
//        Call<ResultBase> call = mService.record(params);
//        Response<ResultBase> response = null;
//        try {
//            response = call.execute();
//        } catch (IOException e) {
//            Log.w(TAG, "request failed:" + e.getMessage());
//        }
//
//        if (response != null && success(response)) {
//            Log.d(TAG, "record records successfully");
//            TableRecord.delete(params);
//            return true;
//        } else {
//            Log.d(TAG, "record records failed:" + string(response));
//            return false;
//        }
//    }
//
//    private void retryRecord() {
//        if (!mNetworkAvailable) {
//            Log.w(TAG, "network unavailable when retry record");
//            return;
//        }
//
//        if (mThreadPool == null) {
//            Log.w(TAG, "thread pool not ready yet!!!");
//            mHandler.sendEmptyMessageDelayed(MSG_RETRY_RECORD, REQUEST_RETRY_DELAY);
//            return;
//        }
//
//        if (mThreadPool.isShutdown()) {
//            Log.w(TAG, "thread pool has been shutting down already");
//            return;
//        }
//
//        // 在单独的线程中重传上次未上传的考勤记录
//        mThreadPool.execute(() -> {
//            while (true) {
//                Records records = TableRecord.query();
//                Log.d(TAG, "retry record, size=" + records.size());
//                if (records.size() <= 0) {
//                    Log.d(TAG, "no record to upload");
//                    return;
//                }
//                if (!record(records)) break;
//                if (mStopped) return;
//            }
//
//            if (!mHandler.hasMessages(MSG_RETRY_RECORD)) {
//                Log.w(TAG, "upload record failed, schedule a retry msg");
//                mHandler.sendEmptyMessageDelayed(MSG_RETRY_RECORD, REQUEST_RETRY_DELAY);
//            }
//        });
//    }
//
//    private void uploadDumpFiles() {
//        mHandler.removeMessages(MSG_UPLOAD_DUMP_FILES);
//        if (mUploadingDumpFiles) {
//            mHandler.sendEmptyMessageDelayed(MSG_UPLOAD_DUMP_FILES, 5000);
//            return;
//        }
//        mUploadingDumpFiles = true;
//        uploadDumpFilesUnlocked();
//        mUploadingDumpFiles = false;
//    }
//
//    private void deleteDumpFiles() {
//        if (mUploadingDumpFiles) return;
//        mUploadingDumpFiles = true;
//        Log.w(TAG, "disk is full and DataLoader stopped, delete dump files immediately.");
//        File folder = new File(DUMP_FOLDER);
//        if (folder.exists() && folder.canRead()) {
//            File[] files = folder.listFiles((dir, name) -> name.endsWith(".rgba"));
//            if (files != null && files.length > 0) {
//                for (File f : files) {
//                    Log.w(TAG, "dump folder full, delete dump file anyway(" + f.getName() + ")");
//                    if (f.exists() && !f.delete()) {
//                        Log.w(TAG, "delete file failed");
//                    }
//                }
//            }
//        }
//        mUploadingDumpFiles = false;
//    }
//
//    private void uploadDumpFilesUnlocked() {
//        Log.d(TAG, "upload dump files");
//        mHandler.removeMessages(MSG_UPLOAD_DUMP_FILES);
//        mListener.onInfo("正在上传dump数据", true, -1);
//        File folder = new File(DUMP_FOLDER);
//        if (!folder.exists() || !folder.canRead()) {
//            mListener.onInfo("无法访问dump目录", false, -1);
//            Log.e(TAG, "can not read dump folder");
//            return;
//        }
//
//        File[] files = folder.listFiles((dir, name) -> name.endsWith(".rgba"));
//        if (files == null || files.length == 0) {
//            Log.w(TAG, "no dump files");
//            mListener.onInfo("dump目录为空", false, 3000);
//            return;
//        }
//
//        boolean noEnoughSpace = isDumpFolderFull();
//        for (File f : files) {
//            if (!f.exists()) continue;
//            RequestBody body = RequestBody.create(MediaType.parse("application/octet-stream"), f);
//            MultipartBody.Part part = MultipartBody.Part.createFormData("dump_file", Config.Serial + "_" + f.getName(), body);
//            Call<ResultBase> call = mService.upload(part);
//            Response<ResultBase> response = null;
//            try {
//                response = call.execute();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//            if (mStopped) return;
//
//            if (noEnoughSpace) {
//                Log.w(TAG, "dump folder full, delete dump file anyway");
//                if (!f.delete()) {
//                    Log.w(TAG, "delete file failed");
//                }
//            } else if (response != null && success(response)) {
//                Log.d(TAG, "upload (" + f.getName() + ") successfully");
//                if (f.exists() && !f.delete()) {
//                    Log.w(TAG, "delete file failed");
//                }
//            } else {
//                Log.d(TAG, "upload (" + f.getName() + ") failed, retry " + REQUEST_RETRY_DELAY + "ms later");
//                mHandler.sendEmptyMessageDelayed(MSG_UPLOAD_DUMP_FILES, REQUEST_RETRY_DELAY);
//                mListener.onInfo("上传失败", false, -1);
//                return;
//            }
//        }
//        mListener.onInfo("上传完成(" + files.length + ")", false, 3000);
//    }
//
//    private void uploadLogs() {
//        Log.d(TAG, "upload logs");
//        mListener.onInfo("正在上传rlogd数据", true, -1);
//        String logFolder = Environment.getExternalStorageDirectory().getPath() + "/rlogd/";
//        // log
//        File tmpLogFile = createTmpLogFile(logFolder + "AndrLogDir/log");
//        if (tmpLogFile == null) {
//            Log.d(TAG, "upload logs failed, retry " + REQUEST_RETRY_DELAY + "ms later");
//            mHandler.sendEmptyMessageDelayed(MSG_UPLOAD_LOGS, REQUEST_RETRY_DELAY);
//            mListener.onInfo("上传log失败", false, -1);
//            return;
//        }
//        RequestBody body1 = RequestBody.create(MediaType.parse("application/octet-stream"), tmpLogFile);
//        MultipartBody.Part part1 = MultipartBody.Part.createFormData("log_file[]", Config.Serial + "_andr_log.0", body1);
//
//        // log.1
//        File log1File = new File(logFolder + "AndrLogDir/log.1");
//        Call<ResultBase> call;
//        if (log1File.exists()) {
//            RequestBody body2 = RequestBody.create(MediaType.parse("application/octet-stream"), log1File);
//            MultipartBody.Part part2 = MultipartBody.Part.createFormData("log_file[]", Config.Serial + "_andr_log.1", body2);
//            call = mService.upload(part1, part2);
//        } else {
//            call = mService.upload(part1);
//        }
//
//        Response<ResultBase> response = null;
//        try {
//            response = call.execute();
//        } catch (Exception e) {
//            e.printStackTrace();
//        } finally {
//            if (!tmpLogFile.delete()) {
//                Log.w(TAG, "delete tmp log file failed(" + tmpLogFile.getAbsolutePath() + ")");
//            }
//        }
//        if (mStopped) return;
//
//        if (response != null && success(response)) {
//            Log.d(TAG, "upload logs successfully");
//            mListener.onInfo("上传完成(rlogd)", false, 3000);
//        } else {
//            Log.d(TAG, "upload logs failed, retry " + REQUEST_RETRY_DELAY + "ms later");
//            mHandler.sendEmptyMessageDelayed(MSG_UPLOAD_LOGS, REQUEST_RETRY_DELAY);
//            mListener.onInfo("上传log失败", false, -1);
//        }
//    }
//
//    private File createTmpLogFile(String path) {
//        File tmpFile = null;
//        InputStream inputStream = null;
//        OutputStream outputStream = null;
//        try {
//            tmpFile = File.createTempFile("log", null);
//            inputStream = new FileInputStream(path);
//            outputStream = new FileOutputStream(tmpFile);
//            byte[] buffer = new byte[4096];
//            while (true) {
//                int cnt = inputStream.read(buffer);
//                if (cnt > 0) outputStream.write(buffer, 0, cnt);
//                else break;
//            }
//        } catch (IOException e) {
//            Log.e(TAG, "copy file failed:" + e.getMessage());
//        } finally {
//            try {
//                if (inputStream != null)
//                    inputStream.close();
//                if (outputStream != null)
//                    outputStream.close();
//            } catch (Exception ignored) {
//            }
//        }
//
//        return tmpFile;
//    }
//
//    private void timerTask() {
//        if (mThreadPool != null && !mThreadPool.isShutdown()) {
//            mThreadPool.execute(() -> {
//                uploadDumpFiles();
//                uploadLogs();
//            });
//            mHandler.sendEmptyMessageDelayed(MSG_SCHEDULE_TASKS, 24 * 60 * 60 * 1000);
//        } else {
//            Log.w(TAG, "thread pool has been shutting down already");
//        }
//    }
//
//    private String string(Request request) {
//        return "Request => method:" + request.method() +
//                ", url:" + request.url();
//    }
//
//    private String string(@Nullable Response<? extends ResultBase> response) {
//        if (response == null) return "null";
//
//        String str = "Response => success:" + response.isSuccessful()
//                + ", code:" + response.code();
//
//        if (!response.isSuccessful()) {
//            if (response.errorBody() == null) str += ", body:null";
//            else {
//                try {
//                    str += ", body:" + response.errorBody().string();
//                } catch (IOException e) {
//                    str += ", body:read body failed(" + e.getMessage() + ")";
//                }
//            }
//        } else {
//            if (response.body() == null) str += ", body:null";
//            else str += ", body: " + response.body();
//        }
//
//        return str;
//    }
//
//    private boolean success(Response<? extends ResultBase> response) {
//        return response.isSuccessful() &&
//                response.body() != null &&
//                response.body().status == 0;
//    }
//
//    private boolean isDumpFolderFull() {
//        int freeMBytes = 0;
//        try {
//            StatFs statFs = new StatFs(DUMP_FOLDER);
//            freeMBytes = (int) (statFs.getFreeBytes() / 1024 / 1024 - 500);
//            Log.w(TAG, "dump folder free space:" + freeMBytes + "MB");
//        } catch (Exception e) {
//            Log.w(TAG, "cannot access dump folder:" + e.getMessage());
//        }
//        return freeMBytes <= 0;
//    }
//
//    private Handler mHandler = new Handler(msg -> {
//        if (mStopped) {
//            Log.w(TAG, "DataLoader stopped, ignore msg:" + msg.what);
//            return true;
//        }
//        switch (msg.what) {
//            case MSG_RETRY_RECORD:
//                retryRecord();
//                break;
//            case MSG_LOAD_AUTHORITY:
//                loadAuthority();
//                break;
//            case MSG_LOAD_CONFIG:
//                loadConfig();
//                break;
//            case MSG_OPEN_WEBSOCKET:
//                if (mThreadPool != null && !mThreadPool.isShutdown())
//                    mThreadPool.execute(this::openWebSocket);
//                break;
//            case MSG_UPLOAD_DUMP_FILES:
//                if (mThreadPool != null && !mThreadPool.isShutdown())
//                    mThreadPool.execute(this::uploadDumpFiles);
//                break;
//            case MSG_UPLOAD_LOGS:
//                if (mThreadPool != null && !mThreadPool.isShutdown())
//                    mThreadPool.execute(this::uploadLogs);
//                break;
//            case MSG_SCHEDULE_TASKS:
//                timerTask();
//                break;
//        }
//        return true;
//    });
//
//    private ConnectivityManager.NetworkCallback mNetworkCallback = new ConnectivityManager.NetworkCallback() {
//        @Override
//        public void onAvailable(Network network) {
//            Log.w(TAG, "network available");
//            mNetworkAvailable = true;
//            App.getInstance().showToast("网络已连接");
//            start();
//            mListener.onNetworkChanged(true);
//        }
//
//        @Override
//        public void onLost(Network network) {
//            Log.w(TAG, "network lost");
//            mNetworkAvailable = false;
//            App.getInstance().showToast("网络已断开");
//            stop();
//            mListener.onNetworkChanged(false);
//        }
//    };
//
//    public class Binder extends android.os.Binder {
//        public void setListener(@NonNull Listener l) {
//            mListener = l;
//        }
//
//        /**
//         * 记录打卡信息
//         * 1. 存入数据库
//         * 2. 上传服务器
//         * 成功: 从数据库删除
//         * 失败: 延时重试
//         */
//        public void record(int id) {
//            Record params = new Record(id);
//            TableRecord.insert(params);
//            mThreadPool.execute(() -> {
//                boolean ret = DataLoader.this.record(params);
//                if (!ret && !mHandler.hasMessages(MSG_RETRY_RECORD)) {
//                    Log.w(TAG, "record failed, schedule a retry msg");
//                    mHandler.sendEmptyMessageDelayed(MSG_RETRY_RECORD, REQUEST_RETRY_DELAY);
//                }
//            });
//        }
//
//        public void sendMessageViaWebsocket(String msg) {
//            Log.d(TAG, "send message via websocket:" + msg);
//            if (mWebSocket == null) {
//                Log.e(TAG, "websocket is null, can not send message");
//            } else {
//                mWebSocket.send(msg);
//            }
//        }
//
//        public boolean networkAvailable() {
//            return mNetworkAvailable;
//        }
//
//        public void reloadAuthority() {
//            mHandler.removeMessages(MSG_LOAD_AUTHORITY);
//            mHandler.sendEmptyMessage(MSG_LOAD_AUTHORITY);
//        }
//
//        public void checkDumpFolder() {
//            if (isDumpFolderFull()) {
//                if (!mStopped) {
//                    Log.w(TAG, "disk full, dispatch msg(upload files) to handler");
//                    mHandler.sendEmptyMessage(MSG_UPLOAD_DUMP_FILES);
//                } else if (mThreadPool != null && !mThreadPool.isShutdown()) {
//                    Log.w(TAG, "disk full, dispatch task(delete dump files) to thread pool");
//                    mThreadPool.execute(DataLoader.this::deleteDumpFiles);
//                } else {
//                    Log.w(TAG, "disk full and thread pool invalid, delete dump files in ui thread");
//                    deleteDumpFiles();
//                }
//            }
//        }
//    }
//
//    public static class Listener {
//        public void onInfo(String tips, boolean loading, int delay) {
//        }
//
//        public void onStaffAdded(Staff[] data) {
//        }
//
//        public void onStaffRemoved(int[] staffIdArray) {
//        }
//
//        public void onAuthorityUpdate() {
//        }
//
//        public void onConfigUpdate(DeviceConfig config) {
//        }
//
//        public void onDeviceRemoved() {
//        }
//
//        public void onUpgrade(String bootlegs) {
//        }
//
//        public void onNetworkChanged(boolean connected) {
//        }
//    }
//}

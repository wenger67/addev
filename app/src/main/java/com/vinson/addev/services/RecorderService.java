package com.vinson.addev.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.NonNull;

import com.otaliastudios.cameraview.CameraException;
import com.otaliastudios.cameraview.CameraListener;
import com.otaliastudios.cameraview.CameraLogger;
import com.otaliastudios.cameraview.CameraOptions;
import com.otaliastudios.cameraview.CameraUtils;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.PictureResult;
import com.otaliastudios.cameraview.VideoResult;
import com.otaliastudios.cameraview.controls.Engine;
import com.otaliastudios.cameraview.controls.Mode;
import com.otaliastudios.cameraview.controls.Preview;
import com.otaliastudios.cameraview.frame.Frame;
import com.otaliastudios.cameraview.frame.FrameProcessor;
import com.otaliastudios.cameraview.size.AspectRatio;
import com.otaliastudios.cameraview.size.SizeSelector;
import com.otaliastudios.cameraview.size.SizeSelectors;
import com.socks.library.KLog;
import com.vinson.addev.App;
import com.vinson.addev.R;
import com.vinson.addev.data.DataHelper;
import com.vinson.addev.utils.CommonUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.vinson.addev.utils.CommonUtil.MEDIA_TYPE_VIDEO;

public class RecorderService extends Service {
    public static final String RESULT_RECEIVER = "resultReceiver";
    public static final String VIDEO_PATH = "recordedVideoPath";
    public static final int RECORD_RESULT_OK = 0;
    public static final int RECORD_RESULT_DEVICE_NO_CAMERA = 1;
    public static final int RECORD_RESULT_GET_CAMERA_FAILED = 2;
    public static final int RECORD_RESULT_ALREADY_RECORDING = 3;
    public static final int RECORD_RESULT_NOT_RECORDING = 4;
    public static final int RECORD_RESULT_UNSTOPPABLE = 5;
    public static final int COMMAND_START_OBJECT_DETECT = 10;
    private static final String TAG = RecorderService.class.getSimpleName();
    private static final String START_SERVICE_COMMAND = "startServiceCommands";
    private static final int COMMAND_NONE = -1;
    private static final int COMMAND_START_RECORDING = 0;
    private static final int COMMAND_STOP_RECORDING = 1;
    private static final int MSG_PROCESS_PREVIEW_CALLBACK = 101;
    private static final int MSG_READY_TO_UPLOAD = 10;

    private final static boolean USE_FRAME_PROCESSOR = true;
    private final static boolean DECODE_BITMAP = true;

    private static final String SELECTED_CAMERA_FOR_RECORDING = "cameraForRecording";
    private static final boolean SAVE_IMAGE = false;
    private static final boolean RECORD_VIDEO = false;
    private static final int MINIMUM_PREVIEW_SIZE = 320;
    private static final Size DESIRED_PREVIEW_SIZE = new Size(640, 480);
    private static final boolean MAINTAIN_ASPECT = false;
    private static boolean SAVE_PREVIEW_BITMAP = false;
    CameraView mCameraView;
    CameraOptions mCameraOptions;
    private MediaRecorder mMediaRecorder;
    private ExecutorService mThreadPool;
    private boolean mRecording = false;
    private String mRecordingPath = null;
    private Handler mHandler;
    private HandlerThread mHandlerThread;
    private Queue<byte[]> imageList = new ConcurrentLinkedQueue<>();
    /**
     * Used to take picture.
     */
    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            File pictureFile = CommonUtil.getOutputMediaFile(CommonUtil.MEDIA_TYPE_IMAGE);

            if (pictureFile == null) {
                return;
            }

            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };
    private List<File> filesToUpload = new ArrayList<>(4);  // bitmaps wait for upload
    private Handler.Callback callback = (msg -> {
        switch (msg.what) {
            case MSG_PROCESS_PREVIEW_CALLBACK:
                imageList.add((byte[]) msg.obj);
                if (mThreadPool != null && !mThreadPool.isShutdown())

                    break;
            case MSG_READY_TO_UPLOAD:
                if (mThreadPool != null && !mThreadPool.isShutdown())
                    mThreadPool.execute(this::uploadBitmaps);
                break;
            default:
                KLog.w("unknown message " + msg.what);
                break;
        }
        return false;
    });

    public RecorderService() {
    }

    public static void startToStartRecording(Context context, ResultReceiver resultReceiver) {
        Intent intent = new Intent(context, RecorderService.class);
        intent.putExtra(START_SERVICE_COMMAND, COMMAND_START_RECORDING);
        intent.putExtra(RESULT_RECEIVER, resultReceiver);
        context.startService(intent);
    }

    public static void startToStopRecording(Context context, ResultReceiver resultReceiver) {
        Intent intent = new Intent(context, RecorderService.class);
        intent.putExtra(START_SERVICE_COMMAND, COMMAND_STOP_RECORDING);
        intent.putExtra(RESULT_RECEIVER, resultReceiver);
        context.startService(intent);
    }

    public static void startObjectDetect(Context context) {
        Intent intent = new Intent(context, RecorderService.class);
        intent.putExtra(START_SERVICE_COMMAND, COMMAND_START_OBJECT_DETECT);
        context.startService(intent);
    }

    private void uploadBitmaps() {
        List<MultipartBody.Part> files = new ArrayList<>();
        for (File file : filesToUpload) {
            RequestBody requestFile = RequestBody.create(MediaType.parse("multipart/form-data"),
                    file);
            MultipartBody.Part part = MultipartBody.Part.createFormData("files", "fileName.png",
                    requestFile);
            files.add(part);
        }

        KLog.d("######start");
        DataHelper.getInstance().uploadFiles("local", files).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                KLog.d("######success");
                clearFiles();
                try {
                    assert response.body() != null;
                    KLog.d(response.body().string());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                clearFiles();
                KLog.d(t.getMessage());
            }
        });
    }

    private void clearFiles() {
        for (File file : filesToUpload) {
            if (file.exists()) file.delete();
        }
        filesToUpload.clear();
    }

    @Override
    public void onCreate() {
        KLog.d();
        super.onCreate();
        mThreadPool = Executors.newFixedThreadPool(5);
        mHandlerThread = new HandlerThread("picture");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper(), callback);

        CameraLogger.setLogLevel(CameraLogger.LEVEL_VERBOSE);
        enableBackgroundCameraByOverlay();
        setupParams();
        mCameraView.setLifecycleOwner(null);
        mCameraView.setEngine(Engine.CAMERA1);
        mCameraView.addCameraListener(new Listener());
        mCameraView.open();
        processFrame();
    }

    private void setupParams() {

        SizeSelector width = SizeSelectors.minWidth(1000);
        SizeSelector height = SizeSelectors.minHeight(1500);
        SizeSelector dimensions = SizeSelectors.and(width, height);
        SizeSelector ratio = SizeSelectors.aspectRatio(AspectRatio.of(9, 16), 0);
        SizeSelector constrains = SizeSelectors.or(
                SizeSelectors.and(ratio, dimensions),
                ratio,
                SizeSelectors.biggest()
        );
        mCameraView.setPreviewStreamSize(constrains);
        mCameraView.setVideoSize(constrains);
        mCameraView.setPictureSize(constrains);
    }

    private void  processFrame() {
        if (USE_FRAME_PROCESSOR) {
            mCameraView.addFrameProcessor(new FrameProcessor() {
                private long lastTime = System.currentTimeMillis();

                @Override
                public void process(@NonNull Frame frame) {
                    long newTime = frame.getTime();
                    long delay = newTime - lastTime;
                    lastTime = newTime;
//                    KLog.v("Frame delayMillis:" + delay +  " FPS:" + 1000 / delay);
                    if (DECODE_BITMAP) {
                        if (frame.getFormat() == ImageFormat.NV21
                                && frame.getDataClass() == byte[].class) {
                            byte[] data = frame.getData();
                            YuvImage yuvImage = new YuvImage(data,
                                    frame.getFormat(),
                                    frame.getSize().getWidth(),
                                    frame.getSize().getHeight(),
                                    null);
                            KLog.d(frame.getSize().toString());
                            ByteArrayOutputStream jpegStream = new ByteArrayOutputStream();
                            yuvImage.compressToJpeg(new Rect(0, 0,
                                    frame.getSize().getWidth(),
                                    frame.getSize().getHeight()), 100, jpegStream);
                            byte[] jpegByteArray = jpegStream.toByteArray();
                            Bitmap bitmap = BitmapFactory.decodeByteArray(jpegByteArray,
                                    0, jpegByteArray.length);
                            //noinspection ResultOfMethodCallIgnored
                            bitmap.toString();
                        }
                    }
                }
            });
        }
    }


    private class Listener extends CameraListener {
        public Listener() {
            super();
        }

        @Override
        public void onCameraOpened(@NonNull CameraOptions options) {
            super.onCameraOpened(options);
            KLog.d(Arrays.toString(new Collection[]{options.getSupportedFrameProcessingFormats()}));
            mCameraOptions = options;
            Collection<com.otaliastudios.cameraview.size.Size> videoSizes = options.getSupportedVideoSizes();
            Collection<com.otaliastudios.cameraview.size.AspectRatio> videoAspect = options.getSupportedVideoAspectRatios();
            KLog.d(Arrays.toString(new Collection[]{videoSizes}));
            KLog.d(Arrays.toString(new Collection[]{videoAspect}));
        }

        @Override
        public void onCameraClosed() {
            super.onCameraClosed();
            KLog.d();
        }

        @Override
        public void onCameraError(@NonNull CameraException exception) {
            super.onCameraError(exception);
            KLog.e(exception.getMessage());
        }

        @Override
        public void onPictureTaken(@NonNull PictureResult result) {
            super.onPictureTaken(result);
        }

        @Override
        public void onVideoTaken(@NonNull VideoResult result) {
            super.onVideoTaken(result);
            KLog.d(result.getFile().getAbsolutePath() + Objects.requireNonNull(result).getSize());
            mRecording = false;
        }

        @Override
        public void onOrientationChanged(int orientation) {
            super.onOrientationChanged(orientation);
        }

        @Override
        public void onVideoRecordingStart() {
            super.onVideoRecordingStart();
            KLog.d();
        }

        @Override
        public void onVideoRecordingEnd() {
            super.onVideoRecordingEnd();
            KLog.d();
        }
    }

    private void enableBackgroundCameraByOverlay() {
        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(1, 1,
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            params.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        }
        View root =
                LayoutInflater.from(App.getInstance().getApplicationContext()).inflate(R.layout.view_camera_view, null);
        mCameraView = root.findViewById(R.id.camera_view);
        wm.addView(root, params);
    }

    @Override
    public void onDestroy() {
        mHandlerThread.quitSafely();
        try {
            mHandlerThread.join();
            mHandlerThread = null;
            mHandler = null;
        } catch (final InterruptedException e) {
            KLog.d("Exception!");
        }

        mThreadPool.shutdown();
        mThreadPool = null;
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        KLog.d();
        if (intent == null) {
            throw new IllegalStateException("Must start the service with intent");
        }

        switch (intent.getIntExtra(START_SERVICE_COMMAND, COMMAND_NONE)) {
            case COMMAND_START_RECORDING:
                handleStartRecordingCommand(intent);
                break;
            case COMMAND_STOP_RECORDING:
                handleStopRecordingCommand(intent);
                break;
            case COMMAND_START_OBJECT_DETECT:
                handleObjectDetectCommand();
                break;
            default:
                throw new UnsupportedOperationException("Cannot start service with illegal " +
                        "commands");
        }

        return START_NOT_STICKY;
    }

    private void handleStartRecordingCommand(Intent intent) {
        KLog.d();
        if (!CameraUtils.hasCameras(this)) {
            throw new IllegalStateException("There is no device, not possible to start recording");
        }

        final ResultReceiver resultReceiver = intent.getParcelableExtra(RESULT_RECEIVER);

        if (mRecording) {
            // Already recording
            resultReceiver.send(RECORD_RESULT_ALREADY_RECORDING, null);
            return;
        }
//        captureVideo();
        captureVideoSnapshot();
        mRecording = true;
    }

    private void captureVideo() {
        if (mCameraView.getMode() == Mode.PICTURE) {
            KLog.w("Can't record HQ videos while in PICTURE mode.");
            return;
        }
        if (mCameraView.isTakingPicture() || mCameraView.isTakingVideo()) {
            KLog.w("taking picture or video currently.");
            return;
        }
        mCameraView.takeVideo(Objects.requireNonNull(CommonUtil.getOutputMediaFile(MEDIA_TYPE_VIDEO)), 5000);
    }

    private void captureVideoSnapshot() {
        if (mCameraView.isTakingVideo()) {
            KLog.w("already taking video.");
            return;
        }
        if (mCameraView.getPreview() != Preview.GL_SURFACE) {
            KLog.w("Video snapshots are only allowed with the GL_SURFACE preview.");
            return;
        }
        mCameraView.takeVideoSnapshot(Objects.requireNonNull(CommonUtil.getOutputMediaFile(MEDIA_TYPE_VIDEO)), 60000);
    }

    private void handleObjectDetectCommand() {
        KLog.d();
    }

    protected synchronized void runInBackground(final Runnable r) {
        if (mHandler != null) {
            mHandler.post(r);
        }
    }

    private void handleStopRecordingCommand(Intent intent) {
        ResultReceiver resultReceiver = intent.getParcelableExtra(RESULT_RECEIVER);

        Log.d(TAG, "recording is finished.");
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}

package com.vinson.addev.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.YuvImage;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.NonNull;

import com.blankj.utilcode.util.SDCardUtils;
import com.otaliastudios.cameraview.CameraException;
import com.otaliastudios.cameraview.CameraListener;
import com.otaliastudios.cameraview.CameraLogger;
import com.otaliastudios.cameraview.CameraOptions;
import com.otaliastudios.cameraview.CameraUtils;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.PictureResult;
import com.otaliastudios.cameraview.VideoResult;
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
import com.vinson.addev.data.DataManager;
import com.vinson.addev.model.local.FrameSavedImage;
import com.vinson.addev.model.local.FrameSavedImage_;
import com.vinson.addev.model.local.LoopVideoFile;
import com.vinson.addev.model.local.LoopVideoFile_;
import com.vinson.addev.model.local.ObjectDetectResult;
import com.vinson.addev.model.local.ObjectDetectResult_;
import com.vinson.addev.tesnsorflowlite.env.ImageUtils;
import com.vinson.addev.tesnsorflowlite.tflite.Classifier;
import com.vinson.addev.tesnsorflowlite.tflite.TFLiteObjectDetectionAPIModel;
import com.vinson.addev.utils.CommonUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.objectbox.query.QueryBuilder;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.vinson.addev.utils.CommonUtil.MEDIA_TYPE_IMAGE;
import static com.vinson.addev.utils.CommonUtil.MEDIA_TYPE_VIDEO;
import static com.vinson.addev.utils.TfLiteUtil.MINIMUM_CONFIDENCE_TF_OD_API;
import static com.vinson.addev.utils.TfLiteUtil.TF_OD_API_INPUT_SIZE;
import static com.vinson.addev.utils.TfLiteUtil.TF_OD_API_IS_QUANTIZED;
import static com.vinson.addev.utils.TfLiteUtil.TF_OD_API_LABELS_FILE;
import static com.vinson.addev.utils.TfLiteUtil.TF_OD_API_MODEL_FILE;

public class RecorderService extends Service {
    public static final String RESULT_RECEIVER = "resultReceiver";
    public static final int RECORD_RESULT_ALREADY_RECORDING = 3;
    public static final int COMMAND_START_OBJECT_DETECT = 10;
    private static final String START_SERVICE_COMMAND = "startServiceCommands";
    private static final int COMMAND_NONE = -1;
    private static final int COMMAND_START_RECORDING = 0;
    private static final int COMMAND_STOP_RECORDING = 1;
    private static final int MSG_SAVE_FRAME_IMAGE = 101;
    private static final int MSG_DELETE_OLDEST_VIDEO = 11;
    private static final int MSG_READY_TO_UPLOAD = 10;

    private static final int VIDEO_SNAPSHOT_DURATION = 60 * 1000;
    private static final long STORAGE_SIZE_RESERVE = 2 * 1024 * 1024;
    private static final boolean MAINTAIN_ASPECT = false;
    private static boolean USE_FRAME_PROCESSOR = true;
    private static boolean DECODE_BITMAP = true;
    // save cropped bitmap that send into tensorflow
    private static final boolean SAVE_CROPPED_BITMAP = false;
    CameraView mCameraView;
    CameraOptions mCameraOptions;
    com.otaliastudios.cameraview.size.Size previewSize = null;
    Classifier detector;
    private ExecutorService mThreadPool;
    private boolean mLoopRecording = false;
    private Handler mHandler;
    private HandlerThread mHandlerThread;
    private int[] rgbBytes = null;
    private Runnable postInferenceCallback;
    private Runnable imageConverter;
    private Runnable saveImage;
    private Handler.Callback callback = (msg -> {
        switch (msg.what) {
            case MSG_SAVE_FRAME_IMAGE:
                if (mThreadPool != null && !mThreadPool.isShutdown()) {
                    mThreadPool.execute(this.saveImage);
                }
                break;
            case MSG_READY_TO_UPLOAD:
                if (mThreadPool != null && !mThreadPool.isShutdown())
                    mThreadPool.execute(this::uploadBitmaps);
                break;
            case MSG_DELETE_OLDEST_VIDEO:
                if (mThreadPool != null && !mThreadPool.isShutdown())
                    mThreadPool.execute(this::deleteOldest);
                break;
            default:
                KLog.w("unknown message " + msg.what);
                break;
        }
        return false;
    });
    private long lastProcessingTimeMs;
    private boolean computingDetection = false;
    private Bitmap rgbFrameBitmap = null;
    private Bitmap croppedBitmap = null;
    private boolean isProcessingFrame = false;
    private Matrix frameToCropTransform;
    private Matrix cropToFrameTransform;

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
        KLog.d();

        List<FrameSavedImage> images = new ArrayList<>();  // images wait for upload
        List<ObjectDetectResult> results = new ArrayList<>(); // detect results wait for upload
        List<LoopVideoFile> videos = new ArrayList<>(); // videos wait for upload

        images = DataManager.get().savedImageBox.query().orderDesc(FrameSavedImage_.createdAt)
                .build().find(0, 4);
        videos = DataManager.get().recordFileBox.query().orderDesc(LoopVideoFile_.createdAt)
                .build().find(0 , 1);
        results = DataManager.get().objectBox.query().orderDesc(ObjectDetectResult_.createdAt)
                .build().find(0, personCount + motorCount);


        List<MultipartBody.Part> files = new ArrayList<>();
        for (FrameSavedImage file : images) {
            RequestBody requestFile = RequestBody.create(MediaType.parse("image/jpeg"),
                    new File(file.path));
            MultipartBody.Part part = MultipartBody.Part.createFormData("files", file.fileName,
                    requestFile);
            files.add(part);
        }

//        for (LoopVideoFile file : videos) {
//            RequestBody requestFile = RequestBody.create(MediaType.parse("multipart/form-data"),
//                    new File(file.path));
//            MultipartBody.Part part = MultipartBody.Part.createFormData("files", file.fileName,
//                    requestFile);
//            files.add(part);
//        }

        KLog.d(images);
        KLog.d(videos);
        KLog.d(results);

        KLog.d("######start");
        DataHelper.getInstance().uploadFiles("local", files).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                // data prepared, clear flags
                savingImage = false;
                savedImageCount = 0;
                computingDetection = false;

                // delete record
                KLog.d("######success");
                try {
                    assert response.body() != null;
                    KLog.d(response.body().string());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                // data prepared, clear flags
                savingImage = false;
                savedImageCount = 0;
                computingDetection = false;

                KLog.d(t.getMessage());
                for (StackTraceElement stackTraceElement: t.getStackTrace()) {
                    KLog.d(stackTraceElement.toString());
                }
            }
        });
    }

    @Override
    public void onCreate() {
        KLog.d();
        super.onCreate();
        mThreadPool = Executors.newFixedThreadPool(5);
        mHandlerThread = new HandlerThread("picture");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper(), callback);

        CameraLogger.setLogLevel(CameraLogger.LEVEL_ERROR);
        enableBackgroundCameraByOverlay();
        mCameraView.setLifecycleOwner(null);
        setupParams();
        mCameraView.addCameraListener(new Listener());
        mCameraView.open();
    }

    private boolean savingImage = false;
    private int savedImageCount = 0;

    private void setupParams() {
        SizeSelector width = SizeSelectors.maxWidth(720);
        SizeSelector height = SizeSelectors.maxHeight(1280);
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
        previewSize = new com.otaliastudios.cameraview.size.Size(720, 1280);
    }

    private void processFrame() {
        if (USE_FRAME_PROCESSOR) {
            mCameraView.addFrameProcessor(new FrameProcessor() {
                @Override
                public void process(@NonNull Frame frame) {
                    if (isProcessingFrame) {
                        KLog.d("drop frame");
                        return;
                    }
                    if (DECODE_BITMAP) {
                        if (frame.getFormat() == ImageFormat.NV21
                                && frame.getDataClass() == byte[].class) {
                            byte[] data = CommonUtil.NV21_rotate_to_270(frame.getData(),
                                    previewSize.getHeight(), previewSize.getWidth());
                            isProcessingFrame = true;
                            imageConverter = () -> ImageUtils.convertYUV420SPToARGB8888(data,
                                    previewSize.getWidth(), previewSize.getHeight(), rgbBytes);
                            postInferenceCallback = () -> isProcessingFrame = false;

                            YuvImage yuvImage = new YuvImage(data,
                                    frame.getFormat(),
                                    previewSize
                                            .getWidth(),
                                    previewSize.getHeight(),
                                    null);

                            saveImage = () -> {
                                try {
                                    if (savedImageCount < 4) {
                                        savedImageCount ++;
                                        File image = CommonUtil.getOutputMediaFile(MEDIA_TYPE_IMAGE);
                                        final FileOutputStream out = new FileOutputStream(image);
                                        yuvImage.compressToJpeg(new Rect(0, 0,
                                                previewSize.getWidth(),
                                                previewSize.getHeight()), 30, out);
                                        out.flush();
                                        out.close();
                                        // save image into database
                                        if (image != null) {
                                            FrameSavedImage savedImage = new FrameSavedImage(image.getName(),
                                                    image.getAbsolutePath(), image.length());
                                            DataManager.get().savedImageBox.put(savedImage);
                                        }
                                        // continue save image
                                        mHandler.sendEmptyMessage(MSG_SAVE_FRAME_IMAGE);
                                    } else {
                                        // stop save image
                                        // ready to upload
                                        mHandler.sendEmptyMessage(MSG_READY_TO_UPLOAD);
                                    }

                                } catch (final Exception e) {
                                    e.printStackTrace();
                                    KLog.e("Exception!" + e.getMessage());
                                }
                            };

                            processImage();
                        }
                    }
                }
            });
        }
    }

    private void processImage() {
        // No mutex needed as this method is not reentrant.
        if (computingDetection) {
            readyForNextImage();
            return;
        }
        computingDetection = true;
        rgbFrameBitmap.setPixels(getRgbBytes(), 0, previewSize.getWidth(), 0, 0,
                previewSize.getWidth(), previewSize.getHeight());
        readyForNextImage();

        final Canvas canvas = new Canvas(croppedBitmap);
        canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);
        if (SAVE_CROPPED_BITMAP)
            ImageUtils.saveBitmap(croppedBitmap);

        runInBackground(() -> {
            final long startTime = SystemClock.uptimeMillis();
            final List<Classifier.Recognition> results = detector.recognizeImage(croppedBitmap);
            lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;

            List<ObjectDetectResult> persons = new ArrayList<>();
            List<ObjectDetectResult> motors = new ArrayList<>();

            // 检测到结果之后，上传三张照片和一个视频
            for (final Classifier.Recognition result : results) {
                final RectF location = result.getLocation();
                if (location != null && result.getConfidence() >= MINIMUM_CONFIDENCE_TF_OD_API) {
                    cropToFrameTransform.mapRect(location);
                    result.setLocation(location);
                    String title = result.getTitle();
                    if (title.equals("person")) {
                        persons.add(new ObjectDetectResult(title, result.getConfidence(), location.left, location.top, location.right, location.bottom));
                    } else if (title.equals("bicycle") || title.equals("motorcycle")) {
                        motors.add(new ObjectDetectResult(title, result.getConfidence(), location.left, location.top, location.right, location.bottom));
                    }
                    KLog.d(result.toString());
                }
            }

            if (savingImage) return;

            if (persons.size() > 0 || motors.size() > 0) {
                // save images
                savingImage = true;
                mHandler.sendEmptyMessage(MSG_SAVE_FRAME_IMAGE);
            }

            if (persons.size() > 0 && motors.size() > 0) {
                // 检测到人物和摩托车
                personCount = persons.size();
                motorCount = motors.size();
                DataManager.get().objectBox.put(persons);
                DataManager.get().objectBox.put(motors);
            } else if (persons.size() > 0) {
                // 检测到人物
                personCount = persons.size();
                motorCount = 0;
                DataManager.get().objectBox.put(persons);

            } else if (motors.size() > 0) {
                // 检测到摩托车
                motorCount = motors.size();
                personCount = 0;
                DataManager.get().objectBox.put(motors);
            }
        });
    }

    public int personCount;
    public int motorCount;

    protected int[] getRgbBytes() {
        imageConverter.run();
        return rgbBytes;
    }

    protected void readyForNextImage() {
        if (postInferenceCallback != null) {
            postInferenceCallback.run();
        }
    }

    private void enableBackgroundCameraByOverlay() {
        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(1, 1,
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
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

        if (mLoopRecording) {
            // Already recording
            resultReceiver.send(RECORD_RESULT_ALREADY_RECORDING, null);
            return;
        }
        captureVideoSnapshot();
        mLoopRecording = true;
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
        mCameraView.takeVideoSnapshot(Objects.requireNonNull(CommonUtil.getOutputMediaFile(MEDIA_TYPE_VIDEO)), VIDEO_SNAPSHOT_DURATION);
    }

    private void handleObjectDetectCommand() {
        KLog.d();
        USE_FRAME_PROCESSOR = true;
        DECODE_BITMAP = true;
        processFrame();
    }

    protected synchronized void runInBackground(final Runnable r) {
        if (mHandler != null) {
            mHandler.post(r);
        }
    }

    private void handleStopRecordingCommand(Intent intent) {
        ResultReceiver resultReceiver = intent.getParcelableExtra(RESULT_RECEIVER);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private void initTensorFlow() {
        int cropSize = TF_OD_API_INPUT_SIZE;
        try {
            detector =
                    TFLiteObjectDetectionAPIModel.create(
                            getAssets(),
                            TF_OD_API_MODEL_FILE,
                            TF_OD_API_LABELS_FILE,
                            TF_OD_API_INPUT_SIZE,
                            TF_OD_API_IS_QUANTIZED);
            cropSize = TF_OD_API_INPUT_SIZE;
        } catch (final IOException e) {
            e.printStackTrace();
            KLog.e("Exception initializing classifier!");
        }

        rgbBytes = new int[previewSize.getWidth() * previewSize.getHeight()];
        rgbFrameBitmap = Bitmap.createBitmap(previewSize.getWidth(), previewSize.getHeight(),
                Bitmap.Config.ARGB_8888);
        croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Bitmap.Config.ARGB_8888);
        int sensorOrientation = 90 - getScreenOrientation();
        KLog.i("Camera orientation relative to screen canvas: %d", sensorOrientation);
        KLog.i("Initializing at size %dx%d", previewSize.getWidth(), previewSize.getHeight());
        frameToCropTransform =
                ImageUtils.getTransformationMatrix(
                        previewSize.getWidth(), previewSize.getHeight(),
                        cropSize, cropSize,
                        0, MAINTAIN_ASPECT);

        cropToFrameTransform = new Matrix();
        frameToCropTransform.invert(cropToFrameTransform);
    }

    protected int getScreenOrientation() {
        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        switch (wm.getDefaultDisplay().getRotation()) {
            case Surface.ROTATION_270:
                return 270;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_90:
                return 90;
            case Surface.ROTATION_0:
            default:
                return 0;
        }
    }


    private void deleteOldest() {
        QueryBuilder<LoopVideoFile> queryBuilder = DataManager.get().recordFileBox.query();
        queryBuilder.order(LoopVideoFile_.createdAt);
        LoopVideoFile file = queryBuilder.build().findFirst();
        if (file != null) {
            KLog.d(file.toString());
            boolean flag = DataManager.get().recordFileBox.remove(file);
            if (flag) KLog.d("delete " + file.path + " from database success");
            // delete from disk
            File file1 = new File(file.path);
            if (file1.exists()) {
                boolean ret = file1.delete();
                if (ret) {
                    KLog.d("delete " + file.path + " from disk success");
                    long available = SDCardUtils.getExternalAvailableSize();
                    if (available / 1024 < STORAGE_SIZE_RESERVE) {
                        // remove oldest video item
                        mHandler.sendEmptyMessage(MSG_DELETE_OLDEST_VIDEO);
                    }
                } else KLog.d("delete " + file.path + " from disk failed");
            } else {
                KLog.d("file " + file.path + " not exist");
                mHandler.sendEmptyMessage(MSG_DELETE_OLDEST_VIDEO);
            }
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
            Collection<com.otaliastudios.cameraview.size.Size> videoSizes =
                    options.getSupportedVideoSizes();
            Collection<com.otaliastudios.cameraview.size.AspectRatio> videoAspect =
                    options.getSupportedVideoAspectRatios();
            KLog.d(Arrays.toString(new Collection[]{videoSizes}));
            KLog.d(Arrays.toString(new Collection[]{videoAspect}));

            initTensorFlow();
            handleObjectDetectCommand();
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
            LoopVideoFile file = new LoopVideoFile(result.getFile().getName(),
                    result.getFile().getAbsolutePath(), result.getFile().length());
            KLog.d(file.toString());
            // save info database
            DataManager.get().recordFileBox.put(file);
            if (mLoopRecording) {
                long available = SDCardUtils.getExternalAvailableSize();
                if (available / 1024 < STORAGE_SIZE_RESERVE) {
                    // remove oldest video item
                    mHandler.sendEmptyMessage(MSG_DELETE_OLDEST_VIDEO);
                }
                // continue snap shot recording
                mHandler.postDelayed(RecorderService.this::captureVideoSnapshot, 10);
            }

        }

        @Override
        public void onOrientationChanged(int orientation) {
            super.onOrientationChanged(orientation);
            KLog.d(orientation);
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
}

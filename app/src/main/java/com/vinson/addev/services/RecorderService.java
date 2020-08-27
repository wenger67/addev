package com.vinson.addev.services;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.CamcorderProfile;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.ResultReceiver;
import android.os.SystemClock;
import android.os.Trace;
import android.text.TextUtils;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.WindowManager;

import androidx.core.app.ActivityCompat;

import com.socks.library.KLog;
import com.vinson.addev.R;
import com.vinson.addev.data.DataHelper;
import com.vinson.addev.tesnsorflowlite.customview.AutoFitTextureView;
import com.vinson.addev.tesnsorflowlite.env.ImageUtils;
import com.vinson.addev.tesnsorflowlite.tflite.Classifier;
import com.vinson.addev.tesnsorflowlite.tflite.TFLiteObjectDetectionAPIModel;
import com.vinson.addev.utils.CommonUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.vinson.addev.utils.TfLiteUtil.MINIMUM_CONFIDENCE_TF_OD_API;
import static com.vinson.addev.utils.TfLiteUtil.TF_OD_API_INPUT_SIZE;
import static com.vinson.addev.utils.TfLiteUtil.TF_OD_API_IS_QUANTIZED;
import static com.vinson.addev.utils.TfLiteUtil.TF_OD_API_LABELS_FILE;
import static com.vinson.addev.utils.TfLiteUtil.TF_OD_API_MODEL_FILE;

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

    private static final String SELECTED_CAMERA_FOR_RECORDING = "cameraForRecording";
    private static final boolean SAVE_IMAGE = false;
    private static final boolean RECORD_VIDEO = false;
    private static boolean SAVE_PREVIEW_BITMAP = false;
    private static final int MINIMUM_PREVIEW_SIZE = 320;
    private static final Size DESIRED_PREVIEW_SIZE = new Size(640, 480);
    private static final boolean MAINTAIN_ASPECT = false;
    private final Semaphore cameraOpenCloseLock = new Semaphore(1);
    private final CameraCaptureSession.CaptureCallback captureCallback =
            new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureProgressed(
                        final CameraCaptureSession session,
                        final CaptureRequest request,
                        final CaptureResult partialResult) {
                }

                @Override
                public void onCaptureCompleted(
                        final CameraCaptureSession session,
                        final CaptureRequest request,
                        final TotalCaptureResult result) {
                }
            };
    protected int previewWidth = 0;
    protected int previewHeight = 0;
    AutoFitTextureView textureView;
    String mCameraId;
    SurfaceTexture surfaceTexture;
    private int mPreviewWidth;
    private int mPreviewHeight;
    private Camera mCamera;
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
    private CameraDevice cameraDevice;
    private ImageReader previewReader;
    private CaptureRequest.Builder previewRequestBuilder;
    private CameraCaptureSession captureSession;
    private CaptureRequest previewRequest;
    private int[] rgbBytes = null;
    private boolean isProcessingFrame = false;
    private byte[][] yuvBytes = new byte[3][];
    private int yRowStride;
    private Runnable imageConverter;
    private boolean computingDetection = false;
    private long timestamp = 0;
    private Classifier detector;
    private long lastProcessingTimeMs;
    private Bitmap rgbFrameBitmap = null;
    private Bitmap croppedBitmap = null;
    private Bitmap cropCopyBitmap = null;
    private Runnable postInferenceCallback;

    private List<File> filesToUpload = new ArrayList<>(4);  // bitmaps wait for upload
    private Handler.Callback callback = (msg -> {
        switch (msg.what) {
            case MSG_PROCESS_PREVIEW_CALLBACK:
                imageList.add((byte[]) msg.obj);
                if (mThreadPool != null && !mThreadPool.isShutdown())
                    mThreadPool.execute(this::saveImage);
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
    private boolean storeBitmaps = false;
    private Integer sensorOrientation;
    private Size previewSize;
    private Matrix frameToCropTransform;
    ImageReader.OnImageAvailableListener imageListener =
            new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(final ImageReader reader) {
                    // We need wait until we have some size from onPreviewSizeChosen
                    if (previewWidth == 0 || previewHeight == 0) {
                        return;
                    }
                    if (rgbBytes == null) {
                        rgbBytes = new int[previewWidth * previewHeight];
                    }
                    try {
                        final Image image = reader.acquireLatestImage();
                        if (image == null) {
                            return;
                        }

                        if (isProcessingFrame) {
                            image.close();
                            return;
                        }
                        isProcessingFrame = true;
                        final Image.Plane[] planes = image.getPlanes();
                        fillBytes(planes, yuvBytes);
                        yRowStride = planes[0].getRowStride();
                        final int uvRowStride = planes[1].getRowStride();
                        final int uvPixelStride = planes[1].getPixelStride();

                        imageConverter =
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        ImageUtils.convertYUV420ToARGB8888(
                                                yuvBytes[0],
                                                yuvBytes[1],
                                                yuvBytes[2],
                                                previewWidth,
                                                previewHeight,
                                                yRowStride,
                                                uvRowStride,
                                                uvPixelStride,
                                                rgbBytes);
                                    }
                                };

                        postInferenceCallback =
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        image.close();
                                        isProcessingFrame = false;
                                    }
                                };
                        processImage();
                    } catch (final Exception e) {
                        KLog.e("Exception!");
                        return;
                    }
                    Trace.endSection();
                }
            };
    private final CameraDevice.StateCallback stateCallback =
            new CameraDevice.StateCallback() {
                @Override
                public void onOpened(final CameraDevice cd) {
                    // This method is called when the camera is opened.  We start camera preview
                    // here.
                    cameraOpenCloseLock.release();
                    cameraDevice = cd;
                    createCameraPreviewSession();
                }

                @Override
                public void onDisconnected(final CameraDevice cd) {
                    cameraOpenCloseLock.release();
                    cd.close();
                    cameraDevice = null;
                }

                @Override
                public void onError(final CameraDevice cd, final int error) {
                    cameraOpenCloseLock.release();
                    cd.close();
                    cameraDevice = null;
                }
            };
    TextureView.SurfaceTextureListener surfaceTextureListener =
            new TextureView.SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureAvailable(SurfaceTexture surface, int width,
                                                      int height) {
                    KLog.d();
                    surfaceTexture = surface;
                    openCamera(width, height);
                }

                @Override
                public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width,
                                                        int height) {
                    // TODO
                }

                @Override
                public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                    return false;
                }

                @Override
                public void onSurfaceTextureUpdated(SurfaceTexture surface) {

                }
            };

    public RecorderService() {
    }

    public static void startToStartRecording(Context context, int cameraId,
                                             ResultReceiver resultReceiver) {
        Intent intent = new Intent(context, RecorderService.class);
        intent.putExtra(START_SERVICE_COMMAND, COMMAND_START_RECORDING);
        intent.putExtra(SELECTED_CAMERA_FOR_RECORDING, cameraId);
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

    /**
     * Given {@code choices} of {@code Size}s supported by a camera, chooses the smallest one whose
     * width and height are at least as large as the minimum of both, or an exact match if possible.
     *
     * @param choices The list of sizes that the camera supports for the intended output class
     * @param width   The minimum desired width
     * @param height  The minimum desired height
     * @return The optimal {@code Size}, or an arbitrary one if none were big enough
     */
    protected static Size chooseOptimalSize(final Size[] choices, final int width,
                                            final int height) {
        final int minSize = Math.max(Math.min(width, height), MINIMUM_PREVIEW_SIZE);
        final Size desiredSize = new Size(width, height);

        // Collect the supported resolutions that are at least as big as the preview Surface
        boolean exactSizeFound = false;
        final List<Size> bigEnough = new ArrayList<Size>();
        final List<Size> tooSmall = new ArrayList<Size>();
        for (final Size option : choices) {
            if (option.equals(desiredSize)) {
                // Set the size but don't return yet so that remaining sizes will still be logged.
                exactSizeFound = true;
            }

            if (option.getHeight() >= minSize && option.getWidth() >= minSize) {
                bigEnough.add(option);
            } else {
                tooSmall.add(option);
            }
        }

        KLog.i("Desired size: " + desiredSize + ", min size: " + minSize + "x" + minSize);
        KLog.i("Valid preview sizes: [" + TextUtils.join(", ", bigEnough) + "]");
        KLog.i("Rejected preview sizes: [" + TextUtils.join(", ", tooSmall) + "]");

        if (exactSizeFound) {
            KLog.i("Exact size match found.");
            return desiredSize;
        }

        // Pick the smallest of those, assuming we found any
        if (bigEnough.size() > 0) {
            final Size chosenSize = Collections.min(bigEnough, new CompareSizesByArea());
            KLog.i("Chosen size: " + chosenSize.getWidth() + "x" + chosenSize.getHeight());
            return chosenSize;
        } else {
            KLog.e("Couldn't find any suitable preview size");
            return choices[0];
        }
    }

    private void uploadBitmaps() {
        List<MultipartBody.Part> files = new ArrayList<>();
        for (File file : filesToUpload) {
            RequestBody requestFile = RequestBody.create(MediaType.parse("multipart/form-data"), file);
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
        for (File file: filesToUpload) {
            if (file.exists()) file.delete();
        }
        filesToUpload.clear();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mThreadPool = Executors.newFixedThreadPool(5);
        mHandlerThread = new HandlerThread("picture");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper(), callback);
    }

    private void saveImage() {
        byte[] data;
        try {
            data = imageList.poll();
        } catch (Exception e) {
            KLog.e(e.getMessage());
            return;
        }
        YuvImage im = new YuvImage(data, ImageFormat.NV21, mPreviewWidth, mPreviewHeight, null);
        Rect r = new Rect(0, 0, mPreviewWidth, mPreviewHeight);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        im.compressToJpeg(r, 50, baos);
        try {
            String path = CommonUtil.getOutputMediaFile(CommonUtil.MEDIA_TYPE_IMAGE).getPath();
            FileOutputStream output = new FileOutputStream(path);
            output.write(baos.toByteArray());
            output.flush();
            output.close();
        } catch (IOException e) {
            KLog.e("Saving to file failed");
        }
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

        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Log.d(TAG, "recordservoce alive");
            }
        }, 1000, 10000);


        return START_NOT_STICKY;
    }

    private void handleStartRecordingCommand(Intent intent) {
        KLog.d();
        if (!CommonUtil.isCameraExist(this)) {
            throw new IllegalStateException("There is no device, not possible to start recording");
        }

        final ResultReceiver resultReceiver = intent.getParcelableExtra(RESULT_RECEIVER);

        if (mRecording) {
            // Already recording
            resultReceiver.send(RECORD_RESULT_ALREADY_RECORDING, null);
            return;
        }
        mRecording = true;

        final int cameraId = intent.getIntExtra(SELECTED_CAMERA_FOR_RECORDING,
                Camera.CameraInfo.CAMERA_FACING_BACK);
        mCamera = CommonUtil.getCameraInstance(cameraId);
        if (mCamera != null) {
            SurfaceView sv = new SurfaceView(this);

            WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
            WindowManager.LayoutParams params = new WindowManager.LayoutParams(1, 1,
                    WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                    PixelFormat.TRANSLUCENT);

            SurfaceHolder sh = sv.getHolder();

            sv.setZOrderOnTop(true);
            sh.setFormat(PixelFormat.TRANSPARENT);

            sh.addCallback(new SurfaceHolder.Callback() {
                @Override
                public void surfaceCreated(SurfaceHolder holder) {
                    Camera.Parameters params = mCamera.getParameters();
                    mCamera.setParameters(params);
                    Camera.Parameters p = mCamera.getParameters();

                    List<Camera.Size> listSize;

                    listSize = p.getSupportedPreviewSizes();
                    Camera.Size mPreviewSize = listSize.get(2);
                    Log.v(TAG, "preview width = " + mPreviewSize.width
                            + " preview height = " + mPreviewSize.height);
                    p.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
                    mPreviewWidth = mPreviewSize.width;
                    mPreviewHeight = mPreviewSize.height;

                    listSize = p.getSupportedPictureSizes();
                    Camera.Size mPictureSize = listSize.get(5);
                    Log.v(TAG, "capture width = " + mPictureSize.width
                            + " capture height = " + mPictureSize.height);
                    p.setPictureSize(mPictureSize.width, mPictureSize.height);
                    p.setRotation(90);

                    mCamera.setParameters(p);

                    try {
                        mCamera.setPreviewDisplay(holder);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    KLog.d();
                    mCamera.setPreviewCallbackWithBuffer(new Camera.PreviewCallback() {
                        @Override
                        public void onPreviewFrame(byte[] data, Camera camera) {
                            KLog.d(data.length);
                            Message message = new Message();
                            message.what = MSG_PROCESS_PREVIEW_CALLBACK;
                            message.obj = data;
                            mHandler.sendMessage(message);
                            mCamera.addCallbackBuffer(data);
                        }
                    });

                    mCamera.addCallbackBuffer(new byte[(mPreviewSize.width * mPreviewSize.height * 3 / 2)]);
                    mCamera.startPreview();
                    mCamera.unlock();

                    mMediaRecorder = new MediaRecorder();
                    mMediaRecorder.setCamera(mCamera);

                    mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                    mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

                    if (cameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
                        mMediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));
                    } else {
                        mMediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_480P));
                    }

                    mRecordingPath =
                            CommonUtil.getOutputMediaFile(CommonUtil.MEDIA_TYPE_VIDEO).getPath();
                    mMediaRecorder.setOutputFile(mRecordingPath);

                    mMediaRecorder.setPreviewDisplay(holder.getSurface());

                    try {
                        mMediaRecorder.prepare();
                    } catch (IllegalStateException e) {
                        Log.d(TAG,
                                "IllegalStateException when preparing MediaRecorder: " + e.getMessage());
                    } catch (IOException e) {
                        Log.d(TAG, "IOException when preparing MediaRecorder: " + e.getMessage());
                    }
                    mMediaRecorder.start();
                    resultReceiver.send(RECORD_RESULT_OK, null);


                    Log.d(TAG, "Recording is started");
                }

                @Override
                public void surfaceChanged(SurfaceHolder holder, int format, int width,
                                           int height) {
                }

                @Override
                public void surfaceDestroyed(SurfaceHolder holder) {
                }
            });
            wm.addView(sv, params);

        } else {
            Log.d(TAG, "Get Camera from service failed");
            resultReceiver.send(RECORD_RESULT_GET_CAMERA_FAILED, null);
        }
    }

    private void handleObjectDetectCommand() {

        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(1, 1,
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            params.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        }

        textureView = new AutoFitTextureView(this);
        textureView.setSurfaceTextureListener(surfaceTextureListener);
        wm.addView(textureView, params);

    }

    private void openCamera(final int width, final int height) {
        setUpCameraOutputs();
        configureTransform(width, height);

        final CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            if (!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            manager.openCamera(mCameraId, stateCallback, mHandler);
        } catch (final CameraAccessException e) {
            KLog.e("Exception!");
        } catch (final InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
        }
    }

    private void createCameraPreviewSession() {
        try {
            // We configure the size of default buffer to be the size of camera preview we want.
            surfaceTexture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());

            // This is the output Surface we need to start preview.
            final Surface surface = new Surface(surfaceTexture);

            // We set up a CaptureRequest.Builder with the output Surface.
            previewRequestBuilder =
                    cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            previewRequestBuilder.addTarget(surface);

            KLog.i("Opening camera preview: " + previewSize.getWidth() + "x" + previewSize.getHeight());

            // Create the reader for the preview frames.
            previewReader =
                    ImageReader.newInstance(
                            previewSize.getWidth(), previewSize.getHeight(),
                            ImageFormat.YUV_420_888, 2);

            previewReader.setOnImageAvailableListener(imageListener, mHandler);
            previewRequestBuilder.addTarget(previewReader.getSurface());

            // Here, we create a CameraCaptureSession for camera preview.
            cameraDevice.createCaptureSession(
                    Arrays.asList(surface, previewReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(final CameraCaptureSession cameraCaptureSession) {
                            // The camera is already closed
                            if (null == cameraDevice) {
                                return;
                            }

                            // When the session is ready, we start displaying the preview.
                            captureSession = cameraCaptureSession;
                            try {
                                // Auto focus should be continuous for camera preview.
                                previewRequestBuilder.set(
                                        CaptureRequest.CONTROL_AF_MODE,
                                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                                // Flash is automatically enabled when necessary.
                                previewRequestBuilder.set(
                                        CaptureRequest.CONTROL_AE_MODE,
                                        CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);

                                // Finally, we start displaying the camera preview.
                                previewRequest = previewRequestBuilder.build();
                                captureSession.setRepeatingRequest(
                                        previewRequest, captureCallback, mHandler);
                            } catch (final CameraAccessException e) {
                                KLog.e("Exception!");
                            }
                        }

                        @Override
                        public void onConfigureFailed(final CameraCaptureSession cameraCaptureSession) {
                            KLog.d();
                        }
                    },
                    null);
        } catch (final CameraAccessException e) {
            KLog.e("Exception!");
        }
    }

    protected void readyForNextImage() {
        if (postInferenceCallback != null) {
            postInferenceCallback.run();
        }
    }

    protected int[] getRgbBytes() {
        imageConverter.run();
        return rgbBytes;
    }

    private void processImage() {
        KLog.d();
        ++timestamp;
        final long currTimestamp = timestamp;

        // No mutex needed as this method is not reentrant.
        if (computingDetection) {
            readyForNextImage();
            return;
        }
        computingDetection = true;
        KLog.i("Preparing image " + currTimestamp + " for detection in bg thread.");

        rgbFrameBitmap.setPixels(getRgbBytes(), 0, previewWidth, 0, 0, previewWidth, previewHeight);

        readyForNextImage();

        final Canvas canvas = new Canvas(croppedBitmap);
        canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);
        // For examining the actual TF input.
        if (SAVE_PREVIEW_BITMAP && filesToUpload.size() < 4) {
            filesToUpload.add(ImageUtils.saveBitmap(croppedBitmap));
            if (filesToUpload.size() == 4) {
                mHandler.sendEmptyMessage(MSG_READY_TO_UPLOAD);
                SAVE_PREVIEW_BITMAP = false;
            }
        }

        runInBackground(
                new Runnable() {
                    @Override
                    public void run() {
                        KLog.i("Running detection on image " + currTimestamp);
                        final long startTime = SystemClock.uptimeMillis();
                        final List<Classifier.Recognition> results =
                                detector.recognizeImage(croppedBitmap);
                        lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;

                        cropCopyBitmap = Bitmap.createBitmap(croppedBitmap);
                        final Paint paint = new Paint();
                        paint.setColor(Color.RED);
                        paint.setStyle(Paint.Style.STROKE);
                        paint.setStrokeWidth(2.0f);

                        for (final Classifier.Recognition result : results) {
                            final RectF location = result.getLocation();
                            if (location != null && result.getConfidence() >= MINIMUM_CONFIDENCE_TF_OD_API) {
                                result.setLocation(location);
                                if (result.getTitle().equals("person")) {
                                    KLog.d("recognise person: " + result.toString());
                                    if (!SAVE_PREVIEW_BITMAP) SAVE_PREVIEW_BITMAP = true;
                                }
                            }
                        }

                        computingDetection = false;
//                        KLog.d("origin: " + previewWidth + "x" + previewHeight);
//                        KLog.d("croped:" + cropCopyBitmap.getWidth() + "x" + cropCopyBitmap
//                        .getHeight());
//                        KLog.d("timecost:" + lastProcessingTimeMs + "ms");
                    }
                });
    }

    protected void fillBytes(final Image.Plane[] planes, final byte[][] yuvBytes) {
        // Because of the variable row stride it's not possible to know in
        // advance the actual necessary dimensions of the yuv planes.
        for (int i = 0; i < planes.length; ++i) {
            final ByteBuffer buffer = planes[i].getBuffer();
            if (yuvBytes[i] == null) {
                KLog.d("Initializing buffer %d at size %d", i, buffer.capacity());
                yuvBytes[i] = new byte[buffer.capacity()];
            }
            buffer.get(yuvBytes[i]);
        }
    }

    /**
     * Configures the necessary {@link Matrix} transformation to `mTextureView`. This method
     * should be
     * called after the camera preview size is determined in setUpCameraOutputs and also the size of
     * `mTextureView` is fixed.
     *
     * @param viewWidth  The width of `mTextureView`
     * @param viewHeight The height of `mTextureView`
     */
    private void configureTransform(final int viewWidth, final int viewHeight) {
        if (null == textureView || null == previewSize) {
            return;
        }
        final int rotation =
                ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
        final Matrix matrix = new Matrix();
        final RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        final RectF bufferRect = new RectF(0, 0, previewSize.getHeight(), previewSize.getWidth());
        final float centerX = viewRect.centerX();
        final float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            final float scale =
                    Math.max(
                            (float) viewHeight / previewSize.getHeight(),
                            (float) viewWidth / previewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }
        textureView.setTransform(matrix);
    }

    /**
     * Sets up member variables related to camera.
     */
    private void setUpCameraOutputs() {
        final CameraManager manager = (CameraManager) this.getSystemService(Context.CAMERA_SERVICE);
        try {
            mCameraId = Integer.toString(CameraCharacteristics.LENS_FACING_BACK);
            final CameraCharacteristics characteristics =
                    manager.getCameraCharacteristics(mCameraId);
            final StreamConfigurationMap map =
                    characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

            sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);

            // Danger, W.R.! Attempting to use too large a preview size could  exceed the camera
            // bus' bandwidth limitation, resulting in gorgeous previews but the storage of
            // garbage capture data.
            previewSize =
                    chooseOptimalSize(
                            map.getOutputSizes(SurfaceTexture.class),
                            DESIRED_PREVIEW_SIZE.getWidth(),
                            DESIRED_PREVIEW_SIZE.getHeight());

            // We fit the aspect ratio of TextureView to the size of preview we picked.
            final int orientation = getResources().getConfiguration().orientation;
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                textureView.setAspectRatio(previewSize.getWidth(), previewSize.getHeight());
            } else {
                textureView.setAspectRatio(previewSize.getHeight(), previewSize.getWidth());
            }
        } catch (final CameraAccessException e) {
            KLog.e("Exception!");
        } catch (final NullPointerException e) {

            throw new IllegalStateException(getString(R.string.tfe_od_camera_error));
        }
        onPreviewSizeChosen(previewSize.getWidth(), previewSize.getHeight());
    }

    private void onPreviewSizeChosen(int width, int height) {
        previewWidth = width;
        previewHeight = height;
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
            KLog.e("Classifier could not be initialized");
        }

        KLog.i("Initializing at size %dx%d", previewWidth, previewHeight);
        rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888);
        croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Bitmap.Config.ARGB_8888);

        frameToCropTransform =
                ImageUtils.getTransformationMatrix(
                        previewWidth, previewHeight,
                        cropSize, cropSize,
                        sensorOrientation, MAINTAIN_ASPECT);
    }

    protected synchronized void runInBackground(final Runnable r) {
        if (mHandler != null) {
            mHandler.post(r);
        }
    }

    private void handleStopRecordingCommand(Intent intent) {
        ResultReceiver resultReceiver = intent.getParcelableExtra(RESULT_RECEIVER);

        if (!mRecording) {
            // have not recorded
            resultReceiver.send(RECORD_RESULT_NOT_RECORDING, null);
            return;
        }

        try {
            mMediaRecorder.stop();
            mMediaRecorder.release();
        } catch (RuntimeException e) {
            mMediaRecorder.reset();
            resultReceiver.send(RECORD_RESULT_UNSTOPPABLE, new Bundle());
            return;
        } finally {
            mMediaRecorder = null;
            mCamera.stopPreview();
            mCamera.release();

            mRecording = false;
        }

        Bundle b = new Bundle();
        b.putString(VIDEO_PATH, mRecordingPath);
        resultReceiver.send(RECORD_RESULT_OK, b);

        Log.d(TAG, "recording is finished.");
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * Compares two {@code Size}s based on their areas.
     */
    static class CompareSizesByArea implements Comparator<Size> {
        @Override
        public int compare(final Size lhs, final Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum(
                    (long) lhs.getWidth() * lhs.getHeight() - (long) rhs.getWidth() * rhs.getHeight());
        }
    }
}

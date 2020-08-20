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
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

import com.socks.library.KLog;
import com.vinson.addev.App;
import com.vinson.addev.utils.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class RecorderService extends Service {
    private static final String TAG = RecorderService.class.getSimpleName();

    public static final String RESULT_RECEIVER = "resultReceiver";
    public static final String VIDEO_PATH = "recordedVideoPath";

    public static final int RECORD_RESULT_OK = 0;
    public static final int RECORD_RESULT_DEVICE_NO_CAMERA = 1;
    public static final int RECORD_RESULT_GET_CAMERA_FAILED = 2;
    public static final int RECORD_RESULT_ALREADY_RECORDING = 3;
    public static final int RECORD_RESULT_NOT_RECORDING = 4;
    public static final int RECORD_RESULT_UNSTOPPABLE = 5;

    private static final String START_SERVICE_COMMAND = "startServiceCommands";
    private static final int COMMAND_NONE = -1;
    private static final int COMMAND_START_RECORDING = 0;
    private static final int COMMAND_STOP_RECORDING = 1;

    private static final String SELECTED_CAMERA_FOR_RECORDING = "cameraForRecording";

    private Camera mCamera;
    private MediaRecorder mMediaRecorder;

    private boolean mRecording = false;
    private String mRecordingPath = null;

    private Handler mHandler;
    private HandlerThread mHandlerThread;

    public RecorderService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mHandlerThread = new HandlerThread("picture");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
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

        super.onDestroy();
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

    /**
     * Used to take picture.
     */
    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            File pictureFile = util.getOutputMediaFile(util.MEDIA_TYPE_IMAGE);

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
            default:
                throw new UnsupportedOperationException("Cannot start service with illegal commands");
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
        if (!util.isCameraExist(this)) {
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
        mCamera = util.getCameraInstance(cameraId);
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
                    mCamera.startPreview();
                    mCamera.setPreviewCallback(new Camera.PreviewCallback() {
                        @Override
                        public void onPreviewFrame(byte[] data, Camera camera) {
                            KLog.d(data.length);
                            if (data.length == 0) return;

                            YuvImage im = new YuvImage(data, ImageFormat.NV21, 1080,

                           1440, null);

                           Rect r = new Rect(0, 0, 1080, 1440);

                          ByteArrayOutputStream baos = new ByteArrayOutputStream();

                           im.compressToJpeg(r, 50, baos);

                           try {
                              String path = util.getOutputMediaFile(util.MEDIA_TYPE_IMAGE).getPath();
                              FileOutputStream output = new FileOutputStream(path);

                              output.write(baos.toByteArray());

                              output.flush();

                              output.close();

                           } catch (FileNotFoundException e) {

                              System.out.println("Saving to file failed");

                           } catch (IOException e) {

                              System.out.println("Saving to file failed");



                           }

                        }
                    });
//                    runInBackground(new Runnable() {
//                        @Override
//                        public void run() {
//                            mCamera.takePicture(null, null, new Camera.PictureCallback() {
//                                @Override
//                                public void onPictureTaken(byte[] data, Camera camera) {
//                                    //this line decode the preview data(byte arrays) we get in to Bitmap
//                                    Bitmap myBitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
//                                    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
//
//                                    //then compresses the bitmap into jpeg image file type
//                                    myBitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
//
//                                    //this is our picture directory actually the Environment.getExternalStorageDirectory()
//                                    //returns the root of the internal storage
//                                    String path = util.getOutputMediaFile(util.MEDIA_TYPE_IMAGE).getPath();
//
//                                    try {
//                                        FileOutputStream fo = new FileOutputStream(path);
//                                        fo.write(bytes.toByteArray());
//
//                                        //then broadcast other apps there is a new file
//                                        MediaScannerConnection.scanFile(App.getInstance() ,new String[]{path},new String[]{"image/jpeg"}, null);
//                                        fo.close();
//                                    } catch (IOException e1) {
//                                        e1.printStackTrace();
//                                    }
//                                }
//                            });
//                        }
//                    });

//                    mMediaRecorder = new MediaRecorder();
//                    mMediaRecorder.setCamera(mCamera);
//
//                    mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
//                    mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
//
//                    if (cameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
//                        mMediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));
//                    } else {
//                        mMediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_480P));
//                    }
//
//                    mRecordingPath = util.getOutputMediaFile(util.MEDIA_TYPE_VIDEO).getPath();
//                    mMediaRecorder.setOutputFile(mRecordingPath);
//
//                    mMediaRecorder.setPreviewDisplay(holder.getSurface());
//
//                    try {
//                        mMediaRecorder.prepare();
//                    } catch (IllegalStateException e) {
//                        Log.d(TAG, "IllegalStateException when preparing MediaRecorder: " + e.getMessage());
//                    } catch (IOException e) {
//                        Log.d(TAG, "IOException when preparing MediaRecorder: " + e.getMessage());
//                    }
//                    mMediaRecorder.start();

                    resultReceiver.send(RECORD_RESULT_OK, null);
                    Log.d(TAG, "Recording is started");
                }

                @Override
                public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
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
}

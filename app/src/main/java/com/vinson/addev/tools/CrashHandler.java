package com.vinson.addev.tools;

import android.os.Build;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CrashHandler implements Thread.UncaughtExceptionHandler {
    private static final String TAG = "CrashHandler";

    private static final String LOG_DIR = "logs";

    private static final String LOG_NAME_TEMPLATE = "addev_crash_%s.log";

    private static CrashHandler sInstance = new CrashHandler();
    private Thread.UncaughtExceptionHandler mDefaultCrashHandler;
    private SimpleDateFormat formatter =
            new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINA);
    private List<Runnable> mCleanupList = new ArrayList<>();

    private CrashHandler() {
    }

    public static CrashHandler getInstance() {
        return sInstance;
    }

    public void init() {
        mDefaultCrashHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    public void regCleanup(Runnable runnable) {
        mCleanupList.add(runnable);
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        for (Runnable runnable : mCleanupList) {
            runnable.run();
        }

        dump(ex);
        if (mDefaultCrashHandler != null) {
            mDefaultCrashHandler.uncaughtException(thread, ex);
        }
    }

    private void dump(Throwable ex) {
        File file = getDumpFile();
        if (file == null) return;
        Log.d(TAG, "dump crash log to " + file.getAbsolutePath());
        PrintWriter printWriter = null;
        try {
            printWriter = new PrintWriter(new FileOutputStream(file, true));
        } catch (IOException e) {
            Log.e(TAG, "open dump file failed...", e.getCause());
        }
        if (printWriter == null) return;
        printWriter.append(Build.FINGERPRINT);
        printWriter.append("\n");
        printWriter.append("***************************\n");
        printWriter.append("******* CRASH STACK *******\n");
        printWriter.append("***************************\n");
        ex.printStackTrace(printWriter);
        printWriter.append("\n");
        printWriter.append("\n");
        printWriter.append("\n");
        printWriter.append("\n");
        printWriter.append("****************************\n");
        printWriter.append("*******    LOGCAT    *******\n");
        printWriter.append("****************************\n");
        logcat(printWriter);
        printWriter.close();
    }

    private File getDumpFile() {
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            Log.w(TAG, "not external storage mounted");
            return null;
        }
        String dir = Environment.getExternalStorageDirectory().getAbsolutePath()
                + File.separator
                + LOG_DIR;
        File file = new File(dir);
        if (!file.exists() && !file.mkdir()) {
            Log.w(TAG, "can not access dir on sd card!");
            return null;
        }

        String logName = String.format(
                Locale.CHINA, LOG_NAME_TEMPLATE, formatter.format(new Date())
        );

        File logFile = new File(file, logName);
        if (!logFile.canWrite())
            Log.w(TAG, "can not write log to sd card!");

        return logFile;
    }

    private void logcat(PrintWriter printWriter) {
        String[] cmd = new String[]{"logcat", "-vtime", "-d", /*"*:w"*/};
        String line;
        BufferedReader reader = null;
        try {
            Process process = Runtime.getRuntime().exec(cmd);
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            while ((line = reader.readLine()) != null) {
                printWriter.append(line).append("\n");
            }
        } catch (IOException e) {
            Log.e(TAG, "exec logcat failed..." + e.getMessage());
        } finally {
            close(reader);
        }
    }

    private void close(Closeable closeable) {
        if (closeable == null) return;
        try {
            closeable.close();
        } catch (IOException e) {
            Log.w(TAG, "close failed, " + e.getMessage());
        }
    }
}

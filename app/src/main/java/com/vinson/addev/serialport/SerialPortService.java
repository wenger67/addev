package com.vinson.addev.serialport;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.SystemClock;
import android.serialport.SerialPort;
import android.serialport.SerialPortFinder;

import androidx.annotation.Nullable;

import com.github.javafaker.Faker;
import com.socks.library.KLog;
import com.vinson.addev.data.DataHelper;
import com.vinson.addev.model.request.SensorData;
import com.vinson.addev.tools.Config;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SerialPortService extends Service {

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    SerialPort mSerialPort;
    OutputStream mOutputStream;
    InputStream mInputStream;
    private static final boolean MOCK_MODE = true;
    ReadThread mReadThread;
    @Override
    public void onCreate() {
        KLog.d();
        super.onCreate();
        // start thread to receive data from serialport
        try {
            mSerialPort = SerialPortWrapper.getSerialPort();
            if (mSerialPort == null) {
                KLog.e("can not get serial port");
                SerialPortFinder serialPortFinder = new SerialPortFinder();
                KLog.d(Arrays.toString(serialPortFinder.getAllDevicesPath()));
                if (!MOCK_MODE) return;
            } else {
                mOutputStream = mSerialPort.getOutputStream();
                mInputStream = mSerialPort.getInputStream();
            }
            // start read thread
            mReadThread = new ReadThread();
            mReadThread.start();
        } catch (Exception e) {
            e.printStackTrace();
            KLog.d(e.getMessage());
        }

    }

    public interface ISensorDataListener {
        void onSensorData(SensorData data);
    }

    private ISensorDataListener listener;

    public void setListener(ISensorDataListener listener) {
        this.listener = listener;
    }

    private class ReadThread extends Thread {
        @Override
        public void run() {
            super.run();
            while (!isInterrupted()) {
                int size;
                try {
                    byte[] buffer = new byte[64];
                    if (mInputStream == null) {
                        if (!MOCK_MODE)  return;
                    } else {
                        size = mInputStream.read(buffer);
                    }
                    if (MOCK_MODE) size = 10;

                    if (size > 0) {
                        // post data into web server
                        KLog.d(Config.getLiftInfo().getAdDevice().getID());
                        SystemClock.sleep(10000);
                        SensorData data = new SensorData(
                                (float) Faker.instance().number().randomDouble(5, -2, 2),
                                (float) Faker.instance().number().randomDouble(5, -2, 2),
                                (float) Faker.instance().number().randomDouble(5, -2, 2),
                                (float) Faker.instance().number().randomDouble(5, -90, 90),
                                (float) Faker.instance().number().randomDouble(5, -90, 90),
                                (float) Faker.instance().number().randomDouble(5, -90, 90),
                                (float) Faker.instance().number().randomDouble(5, -5, 5),
                                (float) Faker.instance().number().randomDouble(5, -50, 100),
                                (float) Faker.instance().number().randomDouble(5, -1000, 100000)
                        );

                        if (listener != null)
                            listener.onSensorData(data);

                        DataHelper.getInstance().createRunningData(data).enqueue(new Callback<ResponseBody>() {
                            @Override
                            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                                try {
                                    ResponseBody body = response.body();
                                    if (body != null) KLog.d(body.string());
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                            @Override
                            public void onFailure(Call<ResponseBody> call, Throwable t) {
                                KLog.d(t.getMessage());
                            }
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    KLog.d(e.getMessage());
                }
            }
        }
    }
}

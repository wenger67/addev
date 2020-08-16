package com.vinson.addev;

import android.hardware.Sensor;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textview.MaterialTextView;

import java.util.List;

import github.nisrulz.easydeviceinfo.base.EasySensorMod;

public class SensorActivity extends AppCompatActivity {

    MaterialTextView mTvSensor;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor);
        mTvSensor = findViewById(R.id.tv_sensors);
    }

    @Override
    protected void onResume() {
        super.onResume();
        EasySensorMod easySensorMod = new EasySensorMod(this);
        List<Sensor> sensors = easySensorMod.getAllSensors();
        StringBuilder content = new StringBuilder();
        for (Sensor s : sensors) {
            content.append("vendor:").append(s.getVendor()).append(", version:").
                    append(s.getVersion()).append(", power: ").append(s.getPower()).
                    append(", resolution:").append(s.getResolution()).append(", maxrange:").
                    append(s.getMaximumRange()).append(", name:").append(s.getName()).append("\n");
        }
        mTvSensor.setText(content.toString());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}

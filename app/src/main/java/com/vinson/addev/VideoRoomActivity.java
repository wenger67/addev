package com.vinson.addev;

import android.os.Bundle;
import android.view.TextureView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.vinson.addev.tools.Config;

import im.zego.zegoexpress.entity.ZegoCanvas;

public class VideoRoomActivity extends AppCompatActivity {
    private TextureView mRenderView;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_room);
        mRenderView = findViewById(R.id.render);
        App.getEngine().startPlayingStream(Config.getStreamId().equals("stream1")?"stream0":"stream1", new ZegoCanvas(mRenderView));
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
}

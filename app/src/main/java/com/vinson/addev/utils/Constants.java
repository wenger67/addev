package com.vinson.addev.utils;

import com.vinson.addev.BuildConfig;

public class Constants {
    public static final String BASE_URL = "http://" + BuildConfig.BaseUrl;
    public static final String WS_BASE_URL = "ws://" + BuildConfig.BaseUrl;
    public static final String WS_PATH = "/api/ws/endpoint";

    public static final String PREFIX_LIFT = "LIFT_";
    public static final String PREFIX_USER = "USER_";

    public static final String SP_KEY_CONFIGED = "configed";
    public static final String SP_KEY_LIFT_INFO = "lift.info";
    public static final String SP_KEY_DEVICE_SERIAL = "device.serial";
    public static final String SP_KEY_STREAM_ID = "stream.id";
    public static final String SP_KEY_INIT_FLOOR = "lift.init.floor";
    public static final String SP_KEY_INIT_HEIGHT = "lift.init.height";

    /**
     * websocket message what
     */
    public static final String WS_REQUEST_PUSH_STREAM = "request.push.stream";
    public static final String WS_REQUEST_STOP_PUSH_STREAM = "request.stop.push.stream";
    public static final String WS_REMOTE_PUSH_STREAM_SUCCESS = "push.stream.success";
    public static final String WS_REMOTE_STOP_STREAM_SUCCESS = "stop.push.stream.success";
}

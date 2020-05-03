package ro.atm.corden.util.constant;


import android.Manifest;

public class AppConstants {

    public static final String GET_USERS_TYPE = "GET_USERS_TYPE";
    public static final String GET_USERS_ALL = "GET_USERS_ALL";

    public static final String GET_USERNAME = "GET_USERNAME";
    public static final String GET_VIDEO = "GET_VIDEO";

    public static final String EXTRA_CAMERA = "cameraType";

    public static final int USER_DETAIL_ACTIVITY = 10000;

    public static final String ACTION_BROADCAST_DETECTED_ACTIVITY = "action_detected_activity";

    // permissions
    public static final String[] locationPermissions = {Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.WAKE_LOCK,
            Manifest.permission.FOREGROUND_SERVICE};

    public static final String[] webRtcPermissions = {Manifest.permission.CAMERA,
            Manifest.permission.MODIFY_AUDIO_SETTINGS,
            Manifest.permission.RECORD_AUDIO};

    public static final int LOCATION_REQUEST_CODE = 180;
    public static final int WEBRTC_REQUEST_CODE = 181;

    public static final boolean canUseBiometrics = false;

}

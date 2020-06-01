package ro.atm.corden.util.websocket.protocol.events;

public class MediaEventType {
    public static final String ICE_CANDIDATE = "iceCandidate";
    public static final String PLAY_VIDEO_REQ = "playVideoRequest";
    public static final String PAUSE_VIDEO_REQ = "pauseVideoRequest";
    public static final String RESUME_VIDEO = "resumeVideoRequest";
    public static final String GET_VIDEO_POSITION_REQ = "getVideoPositionRequest";
    public static final String SEEK_VIDEO_REQ = "seekVideoRequest";
    public static final String STOP_VIDEO_REQ = "stopVideoRequest";
    public static final String START_LIVE_STREAM = "startVideoStreamRequest";
    public static final String STOP_LIVE_STREAM = "stopVideoStreamRequest";
    public static final String START_VIDEO_WATCH = "startLiveVideoWatch";
    public static final String STOP_VIDEO_WATCH = "stopLiveVideoWatch";
}

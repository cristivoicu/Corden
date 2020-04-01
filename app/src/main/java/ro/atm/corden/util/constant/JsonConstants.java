package ro.atm.corden.util.constant;

/**
 * There should be a documentation for communication protocol between client and application server
 * @author Cristian VOICU
 */
public class JsonConstants {
    // requests (ex. list users)
    // requests response
    public static final String EVENT_LIST_USERS_RESPONSE = "list_users_response";
    public static final String REQ_LIST_VIDEO_RESPONSE = "requestListVideoResponse";
    public static final String EVENT_ENROLL_RESPONSE = "enrollResponse";

    // events
    public static final String EVENT_ICE_CANDIDATE = "iceCandidate";

    public static final String EVENT_START_COMMUNICATION = "startCommunication";
    public static final String EVENT_STOP_COMMUNICATION = "stopCommunication";

    public static final String EVENT_LIVE_RESPONSE = "liveResponse";

    // id send through socket
    public static final String ID_ICE_CANDIDATE = "onIceCandidate";
    public static final String ID_PLAY = "play";
    public static final String ID_STOP_PLAY = "stopPlay";
    public static final String ID_STOP_REC = "stopRec";
    public static final String ID_START_REC = "startRec";
    public static final String ID_LIST_USERS = "listUsers";
    public static final String ID_ENROLL_USER = "enroll";
    public static final String ID_GET_LIVE = "liveVideo";

    // sent live stream to the media server where it is recorded
    public static final String EVENT_RECORD_RESPONSE = "startResponse";
    public static final String EVENT_RECORDING = "recording";

    public static final String STATUS_RECORDING_STARTED = "recording";
    public static final String STATUS_RECORDING_STOPPED = "stopped";

    // media player events
    public static final String EVENT_PLAY_RESPONSE = "playResponse";
    public static final String EVENT_VIDEO_PLAY_END = "playEnd";
    public static final String EVENT_GET_POSITION_RESPONSE = "getPositionResponse";
    public static final String EVENT_VIDEO_INFO = "videoInfo";
    // media player video info
    public static final String VIDEO_INFO_IS_SEEKABLE = "isSeekable";
    public static final String VIDEO_INFO_INIT_SEEKABLE = "initSeekable";
    public static final String VIDEO_INFO_END_SEEKABLE = "endSeekable";
    public static final String VIDEO_INFO_DURATION = "videoDuration";

    // media player ids
    public static final String ID_PLAY_VIDEO = "playVideo";
    public static final String ID_STOP_VIDEO = "stopVideo";
    public static final String ID_PAUSE_VIDEO = "pauseVideo";
    public static final String ID_DO_SEEK_VIDEO = "seekVideo";
    public static final String ID_RESUME_VIDEO = "resumeVideo";
    public static final String ID_GET_POSITION_VIDEO = "getPositionVideo";

    public static final String USERS_TYPE_REQ_ALL = "all";
    public static final String USERS_TYPE_REQ_ONLINE = "online";

    public static final String ID = "id";
    public static final String TYPE = "type";
    public static final String USER = "user";
    public static final String FROM = "from";
    public static final String SDP_OFFER = "sdpOffer";
    public static final String SDP_ANSWER = "sdpAnswer";

    public static final String USE_ICE_FOR_RECORDING = "recording";
    public static final String USE_ICE_FOR_LIVE = "live";
    public static final String USE_ICE_FOR_PLAY = "play";

    public static final String RESPONSE_ACCEPTED = "accepted";
    public static final String RESPONSE_REJECTED = "rejected";

    public static final String ICE_FOR_REC = "iceForRec";
    public static final String ICE_FOR_LIVE = "iceForLive";
    public static final String ICE_FOR_PLAY = "iceForPlay";
}

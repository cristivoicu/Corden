package ro.atm.corden.util.constant;

/**
 * There should be a documentation for communication protocol between client and application server
 *
 * @author Cristian VOICU
 */
public class JsonConstants {

    // media player video info
    public static final String VIDEO_INFO_IS_SEEKABLE = "isSeekable";
    public static final String VIDEO_INFO_INIT_SEEKABLE = "initSeekable";
    public static final String VIDEO_INFO_END_SEEKABLE = "endSeekable";
    public static final String VIDEO_INFO_DURATION = "videoDuration";

    public static final String USE_ICE_FOR_RECORDING = "recording";
    public static final String USE_ICE_FOR_LIVE = "live";
    public static final String USE_ICE_FOR_PLAY = "play";

    public static final String RESPONSE_ACCEPTED = "accepted";
    public static final String RESPONSE_REJECTED = "rejected";

    public static final String ICE_FOR_REC = "iceForRec";
    public static final String ICE_FOR_LIVE = "iceForLive";
    public static final String ICE_FOR_PLAY = "iceForPlay";
}

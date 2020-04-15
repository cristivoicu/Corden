package ro.atm.corden.util.websocket;

import android.os.ConditionVariable;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.framing.Framedata;
import org.java_websocket.handshake.ServerHandshake;

import java.lang.reflect.Type;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import ro.atm.corden.model.user.Role;
import ro.atm.corden.model.user.Action;
import ro.atm.corden.model.user.User;
import ro.atm.corden.model.video.Video;
import ro.atm.corden.model.video.VideoInfo;
import ro.atm.corden.util.websocket.callback.DisableUserListener;
import ro.atm.corden.util.websocket.callback.EnrollListener;
import ro.atm.corden.util.websocket.callback.LoginListener;
import ro.atm.corden.util.websocket.callback.MapItemsSaveListener;
import ro.atm.corden.util.websocket.callback.MediaListener;
import ro.atm.corden.util.websocket.callback.RemoveVideoListener;
import ro.atm.corden.util.websocket.callback.UpdateUserListener;

import static ro.atm.corden.util.constant.JsonConstants.USE_ICE_FOR_LIVE;
import static ro.atm.corden.util.constant.JsonConstants.USE_ICE_FOR_PLAY;
import static ro.atm.corden.util.constant.JsonConstants.USE_ICE_FOR_RECORDING;
import static ro.atm.corden.util.constant.JsonConstants.VIDEO_INFO_DURATION;
import static ro.atm.corden.util.constant.JsonConstants.VIDEO_INFO_END_SEEKABLE;
import static ro.atm.corden.util.constant.JsonConstants.VIDEO_INFO_INIT_SEEKABLE;
import static ro.atm.corden.util.constant.JsonConstants.VIDEO_INFO_IS_SEEKABLE;

/**
 * Socket client used to:
 * <ul>
 *     <li> receive message</li>
 *     <li> notify subscribers about receiving request response</li>
 * </ul>
 */
final class WebSocket extends WebSocketClient {
    private static final String TAG = "WebSocket";
    private static final Gson gson = new GsonBuilder().setDateFormat("MMM dd, yyyy, h:mm:ss a").setPrettyPrinting().create();

    boolean isLoggedIn = false;
    // listeners
    MediaListener.RecordingListener mediaListenerRecord = null;
    MediaListener.LiveStreamingListener liveStreamingListener = null;
    MediaListener.PlaybackListener playbackListener = null;

    LoginListener loginListener = null;
    EnrollListener enrollListener;

    UpdateUserListener updateUserListener;
    MapItemsSaveListener mapItemsSaveListener;
    RemoveVideoListener removeVideoListener;
    DisableUserListener disableUserListener;

    ConditionVariable videosConditionVariable = null;
    ConditionVariable usersConditionVariable = null;
    ConditionVariable timelineConditionVariable = null;

    final List<Video> videos = new ArrayList<>();
    final List<User> users = new ArrayList<>();
    final List<Action> actions = new ArrayList<>();


    public WebSocket(URI serverUri) {
        super(serverUri);
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        Log.i(TAG, "connection established");
        try {
            Role role = Role.valueOf(handshakedata.getFieldValue("role"));
            isLoggedIn = true;
            loginListener.onLoginSuccess(role);
        } catch (NullPointerException e) {
            Log.i(TAG, "No role in headers");
        }
    }

    /**
     * Handles the update method responses from the application server.
     * <p>The received message must have the following format:
     * <ul>
     *     <li> method:update</li>
     *     <li> event:enroll|updateUser|disableUser|removeVideo|mapItems</li>
     *     <li> response:success|fail</li>
     * </ul></p>
     * <p> Depending on the response message and the event, this method notify the listener in fact to
     * inform the user if his action was finished successfully or not.</p>
     * <p> It uses the following interface to notify:
     * <ul>
     *     <li> {@link EnrollListener}</li>
     *     <li> {@link UpdateUserListener}</li>
     *     <li> {@link DisableUserListener}</li>
     *     <li> {@link RemoveVideoListener}</li>
     *     <li> {@link MapItemsSaveListener}</li>
     * </ul></p>
     *
     * @author Cristian VOICU
     */
    private void handleUpdateMethodMessage(JsonObject receivedMessage) {

        String response = receivedMessage.get("response").getAsString();

        boolean isSuccess = false;
        if (response.equals("success")) {
            isSuccess = true;
        }

        switch (receivedMessage.get("event").getAsString()) {
            case "enroll":
                if (isSuccess) {
                    enrollListener.onEnrollSuccess();
                } else {
                    enrollListener.onEnrollError();
                }
                break;
            case "updateUser":
                if (isSuccess) {
                    updateUserListener.onUpdateSuccess();
                } else {
                    updateUserListener.onUpdateFailure();
                }
                break;
            case "disableUser":
                if (isSuccess) {
                    disableUserListener.onDisableUserSuccess();
                } else {
                    disableUserListener.inDisableUserFailure();
                }
                break;
            case "removeVideo":
                if (isSuccess) {
                    removeVideoListener.onRemoveVideoSuccess();
                } else {
                    removeVideoListener.onRemoveVideoFailure();
                }
                break;
            case "mapItems":
                if (isSuccess) {
                    mapItemsSaveListener.onMapItemsSaveSuccess();
                } else {
                    mapItemsSaveListener.onMapItemsSaveFailure();
                }
                break;
            default:
                Log.e(TAG, "Unknown event in update method message!");
        }
    }

    /**
     * Handles the request method responses from the application server.
     * The client receives the requested date in json format
     *
     * @param receivedMessage is the message received from application server
     */
    private void handleRequestMethodMessage(JsonObject receivedMessage) {
        String payload = receivedMessage.get("payload").getAsString();
        switch (receivedMessage.get("event").getAsString()) {
            case "requestTimeline":
                Type actionsListType = new TypeToken<ArrayList<Action>>() {
                }.getType();

                synchronized (actions) {
                    actions.clear();
                    actions.addAll(gson.fromJson(payload, actionsListType));
                    timelineConditionVariable.open();
                }
                break;
            case "requestRecordedVideos":
                Type videoListType = new TypeToken<ArrayList<Video>>() {
                }.getType();

                synchronized (videos) {
                    videos.clear();
                    videos.addAll(gson.fromJson(payload, videoListType));
                    videosConditionVariable.open();
                }
                break;
            case "requestAllUsers":


                Type userListType = new TypeToken<ArrayList<User>>() {
                }.getType();
                synchronized (users) {
                    users.clear();
                    users.addAll(gson.fromJson(payload, userListType));
                    usersConditionVariable.open();
                }
                break;
            case "requestOnlineUsers":

                break;
            default:
                Log.e(TAG, "Json request error");
        }
    }

    private void handleMediaMethodMessage(JsonObject receivedMessage) {
        Log.e(TAG, "HANDLE MEDIA: " + receivedMessage.toString());
        switch (receivedMessage.get("event").getAsString()) {
            case "iceCandidate":
                String icefor = receivedMessage.get("candidate").getAsJsonObject().get("for").getAsString();
                Log.e(TAG, "ICE FOR: " + icefor);
                switch (icefor) {
                    case USE_ICE_FOR_LIVE:
                        if (liveStreamingListener != null)
                            liveStreamingListener.onIceCandidate(receivedMessage.getAsJsonObject("candidate").getAsJsonObject("candidate").getAsJsonObject());
                        break;
                    case USE_ICE_FOR_PLAY:
                        if (playbackListener != null)
                            playbackListener.onIceCandidate(receivedMessage.getAsJsonObject("candidate").getAsJsonObject("candidate").getAsJsonObject());
                        break;
                    case USE_ICE_FOR_RECORDING:
                        if (mediaListenerRecord != null)
                            mediaListenerRecord.onIceCandidate(receivedMessage.getAsJsonObject("candidate").getAsJsonObject("candidate").getAsJsonObject());
                        break;
                }
                break;
            case "videoInfo":
                boolean isSeekable = receivedMessage.get(VIDEO_INFO_IS_SEEKABLE).getAsBoolean();
                long seekableInit = receivedMessage.get(VIDEO_INFO_INIT_SEEKABLE).getAsLong();
                long seekableEnd = receivedMessage.get(VIDEO_INFO_END_SEEKABLE).getAsLong();
                long duration = receivedMessage.get(VIDEO_INFO_DURATION).getAsLong();

                VideoInfo videoInfo = new VideoInfo(isSeekable, seekableInit, seekableEnd, duration);
                playbackListener.onVideoInfo(videoInfo);
                break;
            case "playbackEnd":
                playbackListener.onPlayEnd();
                break;
            case "playVideoRequest":
                String response = receivedMessage.get("response").getAsString();
                if (!response.equals("accepted")) {
                    Log.e(TAG, "Play request rejected!");
                    playbackListener.onPlaybackRejected();
                    break;
                }
                String sdpAnswer = receivedMessage.get("sdpAnswer").getAsString();
                playbackListener.onPlayResponse(sdpAnswer);
                break;
            case "seekVideoRequest":
                Log.e(TAG, "Seek video request failed!");
                break;
            case "getVideoPositionRequest":
                long position = receivedMessage.get("position").getAsLong();
                Log.e(TAG, "POSITION " + position);
                playbackListener.onGotPosition(position);
                break;
            case "startVideoStreaming":
                if (!receivedMessage.get("response").equals("accepted")) {

                }
                break;
            default:
                Log.e(TAG, "Json media error");
        }
    }

    @Override
    public void onMessage(String message) {
        Log.e(TAG, "Received Text message: " + message);
        try {
            Log.i(TAG, "Rec brut: " + message);
            JsonObject receivedMessage = gson.fromJson(message, JsonObject.class);
            switch (receivedMessage.get("method").getAsString()) {
                case "update":
                    handleUpdateMethodMessage(receivedMessage);
                    break;
                case "request":
                    handleRequestMethodMessage(receivedMessage);
                    break;
                case "media":
                    handleMediaMethodMessage(receivedMessage);
                    break;
                default:

            }

            /*switch (receivedMessage.get("id").getAsString()) {
                case EVENT_START_RECODRING:

                    break;
                case EVENT_RECORD_RESPONSE:
                    Log.d(TAG, "Got record response");
                    String sdpAnswer = receivedMessage.get("sdpAnswer").getAsString();
                    mediaListenerRecord.onStartResponse(sdpAnswer);
                case EVENT_STOP_COMMUNICATION:
                    Log.i(TAG, "Got stop communication");
                case EVENT_ICE_CANDIDATE: // rec ice candidate
                    Log.i(TAG, "Got ice candidate");
                    String usedFor = receivedMessage.get("for").getAsString();
                    switch (usedFor) {
                        case USE_ICE_FOR_LIVE:
                            if (livePlayListener != null)
                                livePlayListener.onIceCandidate(receivedMessage.getAsJsonObject("candidate"));
                            break;
                        case USE_ICE_FOR_PLAY:
                            if (playbackListener != null)
                                playbackListener.onIceCandidate(receivedMessage.getAsJsonObject("candidate"));
                            break;
                        case USE_ICE_FOR_RECORDING:
                            if (mediaListenerRecord != null)
                                mediaListenerRecord.onIceCandidate(receivedMessage.getAsJsonObject("candidate"));
                            break;
                    }
                    break;
                case EVENT_PLAY_RESPONSE:
                    String response = receivedMessage.get("response").getAsString();
                    if (!response.equals("accepted")) {
                        Log.e(TAG, "Play request rejected!");
                        break;
                    }
                    String sdpAnwer = receivedMessage.get("sdpAnswer").getAsString();
                    playbackListener.onPlayResponse(sdpAnwer);
                    break;
                case EVENT_GET_POSITION_RESPONSE:
                    long position = receivedMessage.get("position").getAsLong();
                    playbackListener.onGotPosition(position);
                    break;
                case EVENT_VIDEO_PLAY_END:
                    playbackListener.onPlayEnd();
                    break;
                case EVENT_VIDEO_INFO:
                    boolean isSeekable = receivedMessage.get(VIDEO_INFO_IS_SEEKABLE).getAsBoolean();
                    long seekableInit = receivedMessage.get(VIDEO_INFO_INIT_SEEKABLE).getAsLong();
                    long seekableEnd = receivedMessage.get(VIDEO_INFO_END_SEEKABLE).getAsLong();
                    long duration = receivedMessage.get(VIDEO_INFO_DURATION).getAsLong();

                    VideoInfo videoInfo = new VideoInfo(isSeekable, seekableInit, seekableEnd, duration);
                    playbackListener.onVideoInfo(videoInfo);
                    break;
                case EVENT_LIVE_RESPONSE:
                    if (RESPONSE_ACCEPTED.equals(receivedMessage.get("response"))) {
                        Log.d(TAG, "Received sdp answer, live response!");
                        sdpAnswer = receivedMessage.get(SDP_OFFER).getAsString();
                        livePlayListener.onLiveResponse(sdpAnswer);
                    }
                    break;
                case EVENT_RECORDING:
                    String status = receivedMessage.get("status").getAsString();
                    switch (status) {
                        case STATUS_RECORDING_STARTED:
                            Log.d(TAG, "Recording has started!");
                            break;
                        case STATUS_RECORDING_STOPPED:
                            Log.d(TAG, "Recording has stopped");
                            break;
                    }
                    break;
                default:
                    Log.e(TAG, "Client received an unknown message from server!");
                    break;

            }*/
        } catch (JsonSyntaxException e) {
            Log.e(TAG, "json invalid");
        }
    }

    @Override
    public void onWebsocketPing(org.java_websocket.WebSocket conn, Framedata f) {
        super.onWebsocketPing(conn, f);
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        Log.e(TAG, "onClose");
        if (code == 1002) {
            Log.d(TAG, "onClose");
            loginListener.onLoginError();
        }
    }

    @Override
    public void onError(Exception ex) {
        Log.e(TAG, "onError: " + ex.getMessage());
    }
}

package ro.atm.corden.util.websocket;

import android.os.ConditionVariable;
import android.util.Log;
import android.webkit.HttpAuthHandler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;

import java.lang.reflect.Type;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import ro.atm.corden.model.Roles;
import ro.atm.corden.model.transport_model.Action;
import ro.atm.corden.model.transport_model.User;
import ro.atm.corden.model.transport_model.Video;
import ro.atm.corden.model.transport_model.VideoInfo;
import ro.atm.corden.util.websocket.callback.EnrollListener;
import ro.atm.corden.util.websocket.callback.LoginListener;
import ro.atm.corden.util.websocket.callback.MediaListener;

import static ro.atm.corden.util.constant.JsonConstants.EVENT_ENROLL_RESPONSE;
import static ro.atm.corden.util.constant.JsonConstants.EVENT_GET_POSITION_RESPONSE;
import static ro.atm.corden.util.constant.JsonConstants.EVENT_ICE_CANDIDATE;
import static ro.atm.corden.util.constant.JsonConstants.EVENT_LIST_USERS_RESPONSE;
import static ro.atm.corden.util.constant.JsonConstants.EVENT_LIVE_RESPONSE;
import static ro.atm.corden.util.constant.JsonConstants.EVENT_PLAY_RESPONSE;
import static ro.atm.corden.util.constant.JsonConstants.EVENT_RECORDING;
import static ro.atm.corden.util.constant.JsonConstants.EVENT_RECORD_RESPONSE;
import static ro.atm.corden.util.constant.JsonConstants.EVENT_STOP_COMMUNICATION;
import static ro.atm.corden.util.constant.JsonConstants.EVENT_VIDEO_INFO;
import static ro.atm.corden.util.constant.JsonConstants.EVENT_VIDEO_PLAY_END;
import static ro.atm.corden.util.constant.JsonConstants.REQ_LIST_VIDEO_RESPONSE;
import static ro.atm.corden.util.constant.JsonConstants.REQ_TIMELINE_RESPONSE;
import static ro.atm.corden.util.constant.JsonConstants.RESPONSE_ACCEPTED;
import static ro.atm.corden.util.constant.JsonConstants.SDP_OFFER;
import static ro.atm.corden.util.constant.JsonConstants.STATUS_RECORDING_STARTED;
import static ro.atm.corden.util.constant.JsonConstants.STATUS_RECORDING_STOPPED;
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
    MediaListener.LivePlayListener livePlayListener = null;
    MediaListener.PlaybackListener playbackListener = null;
    LoginListener loginListener = null;
    EnrollListener enrollListener;

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
            Roles role = Roles.valueOf(handshakedata.getFieldValue("role"));
            isLoggedIn = true;
            loginListener.onLoginSuccess(role);
        } catch (NullPointerException e) {
            Log.i(TAG, "No role in headers");
        }
    }

    @Override
    public void onMessage(String message) {
        Log.i(TAG, "Received Text message: " + message);
        try {
            Log.i(TAG, "Rec brut: " + message);
            JsonObject jsonObject = gson.fromJson(message, JsonObject.class);
            switch (jsonObject.get("id").getAsString()) {
                case EVENT_RECORD_RESPONSE:
                    Log.d(TAG, "Got record response");
                    String sdpAnswer = jsonObject.get("sdpAnswer").getAsString();
                    mediaListenerRecord.onStartResponse(sdpAnswer);
                case EVENT_STOP_COMMUNICATION:
                    Log.i(TAG, "Got stop communication");
                case EVENT_ICE_CANDIDATE: // rec ice candidate
                    Log.i(TAG, "Got ice candidate");
                    String usedFor = jsonObject.get("for").getAsString();
                    switch (usedFor) {
                        case USE_ICE_FOR_LIVE:
                            if (livePlayListener != null)
                                livePlayListener.onIceCandidate(jsonObject.getAsJsonObject("candidate"));
                            break;
                        case USE_ICE_FOR_PLAY:
                            if (playbackListener != null)
                                playbackListener.onIceCandidate(jsonObject.getAsJsonObject("candidate"));
                            break;
                        case USE_ICE_FOR_RECORDING:
                            if (mediaListenerRecord != null)
                                mediaListenerRecord.onIceCandidate(jsonObject.getAsJsonObject("candidate"));
                            break;
                    }
                    break;
                case EVENT_PLAY_RESPONSE:
                    String response = jsonObject.get("response").getAsString();
                    if (!response.equals("accepted")) {
                        Log.e(TAG, "Play request rejected!");
                        break;
                    }
                    String sdpAnwer = jsonObject.get("sdpAnswer").getAsString();
                    playbackListener.onPlayResponse(sdpAnwer);
                    break;
                case EVENT_GET_POSITION_RESPONSE:
                    long position = jsonObject.get("position").getAsLong();
                    playbackListener.onGotPosition(position);
                    break;
                case EVENT_VIDEO_PLAY_END:
                    playbackListener.onPlayEnd();
                    break;
                case EVENT_VIDEO_INFO:
                    boolean isSeekable = jsonObject.get(VIDEO_INFO_IS_SEEKABLE).getAsBoolean();
                    long seekableInit = jsonObject.get(VIDEO_INFO_INIT_SEEKABLE).getAsLong();
                    long seekableEnd = jsonObject.get(VIDEO_INFO_END_SEEKABLE).getAsLong();
                    long duration = jsonObject.get(VIDEO_INFO_DURATION).getAsLong();

                    VideoInfo videoInfo = new VideoInfo(isSeekable, seekableInit, seekableEnd, duration);
                    playbackListener.onVideoInfo(videoInfo);
                    break;
                case EVENT_ENROLL_RESPONSE:
                    response = jsonObject.get("response").getAsString();
                    if (response.equals("enrollSuccess")) {
                        enrollListener.onEnrollSuccess();
                    }
                    break;
                case EVENT_LIST_USERS_RESPONSE: {
                    response = jsonObject.get("users").getAsString();

                    Type userListType = new TypeToken<ArrayList<User>>() {
                    }.getType();
                    synchronized (users) {
                        users.clear();
                        users.addAll(gson.fromJson(response, userListType));
                        usersConditionVariable.open();
                    }
                }
                case EVENT_LIVE_RESPONSE: {
                    if (RESPONSE_ACCEPTED.equals(jsonObject.get("response"))) {
                        Log.d(TAG, "Received sdp answer, live response!");
                        sdpAnswer = jsonObject.get(SDP_OFFER).getAsString();
                        livePlayListener.onLiveResponse(sdpAnswer);
                    }
                }
                case EVENT_RECORDING:
                    String status = jsonObject.get("status").getAsString();
                    switch (status) {
                        case STATUS_RECORDING_STARTED:
                            Log.d(TAG, "Recording has started!");
                            break;
                        case STATUS_RECORDING_STOPPED:
                            Log.d(TAG, "Recording has stopped");
                            break;
                    }
                    break;
                case REQ_LIST_VIDEO_RESPONSE:
                    response = jsonObject.get("videos").getAsString();

                    Type videoListType = new TypeToken<ArrayList<Video>>() {
                    }.getType();

                    synchronized (videos) {
                        videos.clear();
                        videos.addAll(gson.fromJson(response, videoListType));
                        videosConditionVariable.open();
                    }

                    break;
                case REQ_TIMELINE_RESPONSE:
                {
                    response = jsonObject.get("actions").getAsString();

                    Type actionsListType = new TypeToken<ArrayList<Action>>() {
                    }.getType();

                    synchronized (actions) {
                        actions.clear();
                        actions.addAll(gson.fromJson(response, actionsListType));
                        timelineConditionVariable.open();
                    }

                    break;
                }
                default:
                    Log.e(TAG, "Client received an unknown message from server!");
                    break;

            }
        } catch (JsonSyntaxException e) {
            Log.e(TAG, "json invalid");
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        if(code == 1002){
            Log.d(TAG, "onClose");
            loginListener.onLoginError();
        }
    }

    @Override
    public void onError(Exception ex) {
        Log.d(TAG, "onError: " + ex.getMessage());
    }
}

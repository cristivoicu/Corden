package ro.atm.corden.util.websocket;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.ConditionVariable;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.framing.Framedata;
import org.java_websocket.handshake.ServerHandshake;

import java.lang.reflect.Type;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import ro.atm.corden.R;
import ro.atm.corden.model.map.MapItem;
import ro.atm.corden.model.user.LiveStreamer;
import ro.atm.corden.model.user.Role;
import ro.atm.corden.model.user.Action;
import ro.atm.corden.model.user.Status;
import ro.atm.corden.model.user.User;
import ro.atm.corden.model.video.Video;
import ro.atm.corden.model.video.VideoInfo;
import ro.atm.corden.util.App;
import ro.atm.corden.util.receiver.NotificationReceiver;
import ro.atm.corden.util.services.StreamingIntentService;
import ro.atm.corden.util.websocket.callback.EnrollListener;
import ro.atm.corden.util.websocket.callback.LoginListener;
import ro.atm.corden.util.websocket.callback.MapItemsListener;
import ro.atm.corden.util.websocket.callback.MediaListener;
import ro.atm.corden.util.websocket.callback.RemoveVideoListener;
import ro.atm.corden.util.websocket.callback.UpdateUserListener;
import ro.atm.corden.util.websocket.subscribers.LiveStreamerSubscriber;
import ro.atm.corden.util.websocket.subscribers.UserSubscriber;
import ro.atm.corden.view.activity.LoginActivity;
import ro.atm.corden.view.activity.MainActivityUser;

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
    private Context mApplicationContext;

    boolean isLoggedIn = false;
    // listeners
    MediaListener.RecordingListener mediaListenerRecord = null;
    MediaListener.LiveStreamingListener liveStreamingListener = null;
    MediaListener.PlaybackListener playbackListener = null;

    LoginListener loginListener = null;
    EnrollListener enrollListener;

    UpdateUserListener updateUserListener;
    MapItemsListener mapItemsListener;
    RemoveVideoListener removeVideoListener;

    ConditionVariable conditionVariable = null;
    ConditionVariable videosConditionVariable = null;
    ConditionVariable userDataConditionVariable = null;
    ConditionVariable usersConditionVariable = null;
    ConditionVariable timelineConditionVariable = null;

    LatLng latLng = new LatLng(0.0, 0.0);
    final List<Video> videos = new ArrayList<>();
    final List<User> users = new ArrayList<>();
    final List<LiveStreamer> liveStreamers = new ArrayList<>();
    final List<MapItem> mapItems = new LinkedList<>();
    final List<Action> actions = new ArrayList<>();

    UserSubscriber userSubscriber;
    LiveStreamerSubscriber liveStreamerSubscriber;

    WebSocket(URI serverUri, Context applcationContext) {
        super(serverUri);
        mApplicationContext = applcationContext;
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
     *     <li> {@link RemoveVideoListener}</li>
     *     <li> {@link MapItemsListener}</li>
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
                try {
                    if (isSuccess) {
                        enrollListener.onEnrollSuccess();
                    } else {
                        enrollListener.onEnrollError();
                    }
                } catch (NullPointerException e) {
                    Log.e(TAG, "Enroll listener has not subscribers!");
                }
                break;
            case "updateUser":
                try {
                    if (isSuccess) {
                        updateUserListener.onUpdateSuccess();
                    } else {
                        updateUserListener.onUpdateFailure();
                    }
                } catch (NullPointerException e) {
                    Log.e(TAG, "Update user listener has not subscribers!");
                }
                break;
            case "disableUser":
                try {
                    if (isSuccess) {
                        updateUserListener.onUserDisableSuccess();
                    } else {
                        updateUserListener.onUserDisableFailure();
                    }
                } catch (NullPointerException e) {
                    Log.e(TAG, "Disable user listener has not subscribers!");
                }
                break;
            case "removeVideo":
                try {
                    if (isSuccess) {
                        removeVideoListener.onRemoveVideoSuccess();
                    } else {
                        removeVideoListener.onRemoveVideoFailure();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Remove video listener has not subscribers!");
                }
                break;
            case "mapItems":
                try {
                    if (isSuccess) {
                        mapItemsListener.onMapItemsSaveSuccess();
                    } else {
                        mapItemsListener.onMapItemsSaveFailure();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Map items save listener has not subscribers!");
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
        JsonElement payload = receivedMessage.get("payload");
        switch (receivedMessage.get("event").getAsString()) {
            case "requestLiveStreaming":
                String from = receivedMessage.get("from").getAsString();
/*                new Handler(Looper.getMainLooper())
                        .post(() -> {*/
                Intent intent = new Intent(mApplicationContext, MainActivityUser.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                PendingIntent pendingIntent = PendingIntent.getActivity(mApplicationContext, 0, intent, 0);

                Intent startStreamIntent = new Intent(mApplicationContext, StreamingIntentService.class);
                startStreamIntent.setAction("ActionStream");
                PendingIntent startStreamPendingIntent = PendingIntent.getService(mApplicationContext, 0, startStreamIntent, 0);

                NotificationCompat.Builder builder = new NotificationCompat.Builder(mApplicationContext, App.NOTIFICATION_CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_ondemand_video_black_24dp)
                        .setContentTitle("Admin demand")
                        .setContentText(String.format("%s wants you to start video streaming.", from))
                        .setContentIntent(pendingIntent)
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .addAction(R.drawable.ic_show_video, "Start stream", startStreamPendingIntent)
                        .setAutoCancel(true)
                        .setOngoing(true);
                NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(mApplicationContext);
                notificationManagerCompat.notify(10, builder.build());
                /*   });*/
                break;
            case "requestTimeline":
                Type actionsListType = new TypeToken<ArrayList<Action>>() {
                }.getType();

                synchronized (actions) {
                    actions.clear();
                    actions.addAll(gson.fromJson(payload.getAsString(), actionsListType));
                    timelineConditionVariable.open();
                }
                break;
            case "requestServerLog":
                actionsListType = new TypeToken<ArrayList<Action>>() {
                }.getType();

                synchronized (actions) {
                    actions.clear();
                    actions.addAll(gson.fromJson(payload.getAsString(), actionsListType));
                    timelineConditionVariable.open();
                }
                break;
            case "requestRecordedVideos":
                Type videoListType = new TypeToken<ArrayList<Video>>() {
                }.getType();

                synchronized (videos) {
                    videos.clear();
                    videos.addAll(gson.fromJson(payload.getAsString(), videoListType));
                    videosConditionVariable.open();
                }
                break;
            case "requestUserData": // request one user at a time
                synchronized (users) {
                    users.clear();
                    users.add(User.fromJson(payload.getAsString()));
                    userDataConditionVariable.open();
                }
                break;
            case "requestOnlineUsers": // request a list of users at a time
            case "requestAllUsers":
                Type userListType = new TypeToken<ArrayList<User>>() {
                }.getType();
                synchronized (users) {
                    users.clear();
                    users.addAll(gson.fromJson(payload.getAsString(), userListType));
                    usersConditionVariable.open();
                }
                break;
            case "requestLiveStreamers":
                Type liveStreamersListType = new TypeToken<ArrayList<LiveStreamer>>() {
                }.getType();
                synchronized (liveStreamers) {
                    liveStreamers.clear();
                    if (!payload.getAsString().equals("[]"))
                        liveStreamers.addAll(gson.fromJson(payload.getAsString(), liveStreamersListType));

                    userDataConditionVariable.open();
                }
                break;
            case "userLocations":
                JsonArray payloadArray = payload.getAsJsonArray();
                for (int i = 0; i < payloadArray.size(); i++) {
                    JsonObject object = payloadArray.get(i).getAsJsonObject();
                    String username = object.get("username").getAsString();
                    double lat = object.get("lat").getAsDouble();
                    double lng = object.get("lng").getAsDouble();
                    mapItemsListener.onUserLocationUpdated(username, lat, lng);
                }
                break;
            case "userLocation":
                try {
                    JsonObject object = payload.getAsJsonObject();

                    Double latitude = object.get("lat").getAsDouble();
                    Double longitude = object.get("lng").getAsDouble();
                    this.latLng = new LatLng(latitude, longitude);
                    conditionVariable.open();
                } catch (Exception e) {
                    this.latLng = null;
                    conditionVariable.open();
                }
                break;
            case "requestMapItems":
                Type mapItemsListType = new TypeToken<ArrayList<MapItem>>() {
                }.getType();
                synchronized (mapItems) {
                    mapItems.clear();
                    mapItems.addAll(gson.fromJson(payload.getAsString(), mapItemsListType));

                    conditionVariable.open();
                }
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
                Log.d(TAG, "Got record response");
                sdpAnswer = receivedMessage.get("sdpAnswer").getAsString();
                mediaListenerRecord.onStartResponse(sdpAnswer);
                break;
            case "liveWatchResponse":
                Log.d(TAG, "Got live watch response");
                sdpAnswer = receivedMessage.get("sdpAnswer").getAsString();
                liveStreamingListener.onLiveResponse(sdpAnswer);
                break;
            default:
                Log.e(TAG, "Json media error");
        }
    }

    private void handleSubscribeMethodMessage(JsonObject receivedMessage) {
        switch (receivedMessage.get("event").getAsString()) {
            case "userUpdated":
                String userJson = receivedMessage.get("payload").getAsString();
                userSubscriber.onUserDataChanged(User.fromJson(userJson));
                break;
            case "userStatus":
                String status = receivedMessage.get("status").getAsString();
                String username = receivedMessage.get("username").getAsString();
                userSubscriber.onUserStatusChanged(username, Status.valueOf(status));
                break;
            case "liveStreamers":
                String dataJson = receivedMessage.get("payload").getAsString();
                status = receivedMessage.get("status").getAsString();
                if (status.equals("started")) {
                    liveStreamerSubscriber.onNewSubscriber(LiveStreamer.fromJson(dataJson));
                }
                if (status.equals("stopped")) {
                    liveStreamerSubscriber.onSubscribeStop(LiveStreamer.fromJson(dataJson));
                }
                break;
            case "mapItemLocation":
                username = receivedMessage.getAsJsonObject("payload")
                        .get("username")
                        .getAsString();
                double lat = receivedMessage.getAsJsonObject("payload")
                        .get("lat")
                        .getAsDouble();
                double lng = receivedMessage.getAsJsonObject("payload")
                        .get("lng")
                        .getAsDouble();
                mapItemsListener.onUserLocationUpdated(username, lat, lng);
            default:
                Log.e("TAG", "Unknown subscribe event received from application server");
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
                case "subscribe":
                    handleSubscribeMethodMessage(receivedMessage);
                    break;
                default:
                    Log.e(TAG, "Wrong method received from application server");
            }
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
        Log.e(TAG, String.format("onClose, reason %s, remote: %s, code: %d", reason, remote, code));
        if (code == 1000){ // user logout
            return;
        }
        if (code == 1002) {
            Log.d(TAG, "onClose");
            loginListener.onLoginError();
            return;
        }
        if (code == 4999) {// account was disabled
            logOutUser(code);
            return;
        }
        logOutUser(code);
    }

    private void logOutUser(final int code) {
        new Handler(Looper.getMainLooper())
                .post(() -> {
                    Intent intent = new Intent(mApplicationContext, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra("onClose", code);
                    mApplicationContext.startActivity(intent);
                });
    }

    @Override
    public void onError(Exception ex) {
        Log.e(TAG, "onError: " + ex.getMessage());
    }
}

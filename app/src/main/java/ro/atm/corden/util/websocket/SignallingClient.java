package ro.atm.corden.util.websocket;

import android.annotation.SuppressLint;
import android.media.MediaPlayer;
import android.os.ConditionVariable;
import android.os.Looper;
import android.os.NetworkOnMainThreadException;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.MediatorLiveData;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.neovisionaries.ws.client.ThreadType;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;
import com.neovisionaries.ws.client.WebSocketFrame;
import com.neovisionaries.ws.client.WebSocketState;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Principal;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.security.auth.x500.X500Principal;

import ro.atm.corden.model.Roles;
import ro.atm.corden.model.transport_model.User;
import ro.atm.corden.model.transport_model.Video;
import ro.atm.corden.util.exception.login.LoginListenerNotInitialisedException;
import ro.atm.corden.util.exception.websocket.TransportException;
import ro.atm.corden.util.exception.websocket.UserNotLoggedInException;
import ro.atm.corden.util.websocket.callback.EnrollListener;
import ro.atm.corden.util.websocket.callback.LoginListener;
import ro.atm.corden.util.websocket.callback.MediaListener;

import static ro.atm.corden.util.constant.JsonConstants.*;

/**
 * Singleton class used that has the following functionality
 * <ul>
 *     <li> establish the connection with the application server</li>
 *     <li> parsing the server messages events</li>
 *     <li> has the methods necessary for sending messages and requests to the server</li>
 * </ul>
 * Methods used from this class should not be used from the main thread
 */
public class SignallingClient implements com.neovisionaries.ws.client.WebSocketListener {
    private static final String TAGC = "WebSocket";
    private static final Gson gson = new GsonBuilder().setDateFormat("MMM dd, yyyy, h:mm:ss a").setPrettyPrinting().create();

    // listeners
    private MediaListener.RecordingListener mediaListenerRecord = null;
    private MediaListener.OneToOneCallListener mediaListener = null;
    private MediaListener.LivePlayListener livePlayListener = null;
    private LoginListener loginListener = null;
    private EnrollListener enrollListener;

    private ConditionVariable mConditionVariable = new ConditionVariable(true);

    private List<User> users = null;
    private final List<Video> videos = new ArrayList<>();

    private WebSocket webSocket = null;

    public boolean isChannelReady = false;
    public boolean isInitiator = false;
    public boolean isStarted = false;

    private boolean isLoggedIn = false;

    private static SignallingClient INSTANCE = new SignallingClient();

    /**
     * Class is initialised at application start
     */
    private SignallingClient() {
        try {
            //uri = new URI("wss://192.168.8.100:8443/websocket"); // atunci cand e conectat prin stick
            URI uri = new URI("wss://192.168.0.103:8443/websocket"); // wifi acasa

            WebSocketFactory webSocketFactory = new WebSocketFactory();

            try {
                SSLContext context = SSLContext.getInstance("TLS");
                context.init(null, trustAllCerts, null);
                webSocketFactory.setSSLContext(context);
                webSocketFactory.setVerifyHostname(false);

                webSocket = webSocketFactory.createSocket(uri);

                webSocket.addListener(this);

            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (KeyManagementException e) {
                e.printStackTrace();
            }

        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    //This piece of code should not go into production!!
    //This will help in cases where the node server is running in non-https server and you want to ignore the warnings
    @SuppressLint("TrustAllX509TrustManager")
    private final TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[]{};
        }

        @Override
        public void checkServerTrusted(final X509Certificate[] chain, final String authType) throws CertificateException {
            Log.i(TAGC, ": authType: " + authType);
            PublicKey publicKey = chain[0].getPublicKey();
            Principal principal = chain[0].getSubjectDN();
            X500Principal x500Principal = chain[0].getSubjectX500Principal();
            x500Principal = chain[0].getIssuerX500Principal();

            try {
                chain[0].verify(publicKey);
            } catch (InvalidKeyException e) {
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (NoSuchProviderException e) {
                e.printStackTrace();
            } catch (SignatureException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void checkClientTrusted(final X509Certificate[] chain, final String authType) throws CertificateException {
            Log.i(TAGC, ": authType: " + authType);
        }
    }};

    public static SignallingClient getInstance() {
        return INSTANCE;
    }

    /**
     * Method used to initialise websocket
     * <b>It should be called only at login</b>
     * <p> In this method the websocket listener will be initialised </p>
     * <p> This method must be called from an AsyncTask class</p>
     *
     * @throws LoginListenerNotInitialisedException if there are no subscribers for login listener
     * @throws NetworkOnMainThreadException         if the main thread is used
     * @author Cristian VOICU
     */
    public void logIn(String username, String password) throws LoginListenerNotInitialisedException, NetworkOnMainThreadException {
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            Log.e(TAG, "Main thread is used! in SignallingClient.logIn");
            throw new NetworkOnMainThreadException();
        }
        if (loginListener == null) {
            throw new LoginListenerNotInitialisedException();
        }
        try {
            webSocket.addHeader("username", username);
            webSocket.addHeader("password", password);
            webSocket.connect();
        } catch (WebSocketException e) {
            e.printStackTrace();
        }
    }

    /**
     * This method send a request to the server for getting the users
     * It's package private, can be used only by Repository
     * Should be called from async task
     *
     * @return a list with all users from server database or null in case of error
     * @throws NetworkOnMainThreadException if main thread is used
     * @see Repository
     */
    synchronized List<User> getAllUsersRequest() throws NetworkOnMainThreadException {
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            Log.e(TAG, "Main thread is used! in SignallingClient.getAllUsersRequest");
            throw new NetworkOnMainThreadException();
        }
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(ID, ID_LIST_USERS);
            jsonObject.put(TYPE, USERS_TYPE_REQ_ALL);

            Log.i(TAGC, "Get all users event: " + jsonObject.toString());

            webSocket.sendText(jsonObject.toString());

            while (users == null) {
                // wait for users to arrive
            }
            if (users != null) {
                return users;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Used to send a request for recorded video path list
     *
     * @param forUsername is the username of the user that recorded the videos
     * @return null if an exception is thrown or the videos list
     * @throws NetworkOnMainThreadException if is working on the main thread
     * @throws TransportException           if the server does not return the requested videos
     */
    synchronized List<Video> getVideosForUser(String forUsername)
            throws NetworkOnMainThreadException, TransportException {
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            Log.e(TAG, "Main thread is used! in SignallingClient.getAllUsersRequest");
            throw new NetworkOnMainThreadException();
        }

        JSONObject message = new JSONObject();

        try {
            message.put(ID, "recordedVideos");
            message.put("forUser", forUsername);

            webSocket.sendText(message.toString());
            synchronized (videos) {
                Log.d("ThreadReq", "Before wait.");
                //wait(5000); // waiting 10 seconds
                mConditionVariable.block();
                Log.d("ThreadReq", "After wait.");

                if (videos != null) {
                    return videos;
                }
            }
            throw new TransportException("Could not get the requested videos!");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Methods is package private, can be used from Repository
     * Used to fetch online users from the server database
     * <b>It should not be called from the main thread</b>
     *
     * @throws NetworkOnMainThreadException if main thread is used
     * @see Repository
     */
    void getOnlineUsersRequest() throws NetworkOnMainThreadException {
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            Log.e(TAG, "Main thread is used! in SignallingClient.getOnlineUsersRequest");
            throw new NetworkOnMainThreadException();
        }
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(ID, ID_LIST_USERS);
            jsonObject.put(TYPE, USERS_TYPE_REQ_ONLINE);

            Log.i(TAGC, "Get all users event: " + jsonObject.toString());

            webSocket.sendText(jsonObject.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    /**
     * Used to register users in database
     *
     * @deprecated
     */
    @Deprecated
    public void register(String name) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("id", "register");
            jsonObject.put("name", name);

            Log.i(TAGC, "Register event: " + jsonObject.toString());

            webSocket.sendText(jsonObject.toString());

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * This method is used by admin to register new user in server's database
     * <p>The sender is recognised in the server using the session object</p>
     * <b>It should not be called from the main thread</b>
     *
     * @param user is the user that is enrolled in the database
     * @throws NetworkOnMainThreadException if main thread is used
     */
    public void enrollUser(@NonNull User user) throws NetworkOnMainThreadException {
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            Log.e(TAG, "Main thread is used! in SignallingClient.logIn");
            throw new NetworkOnMainThreadException();
        }
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(ID, ID_ENROLL_USER);
            jsonObject.put(USER, user.toJson());

            Log.i("TAG", "Enroll event: " + jsonObject.toString());

            webSocket.sendText(jsonObject.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Used by admin to intercept user recording session
     *
     * @param from     is the username that is sending live video to the media server for recording
     * @param sdpOffer is the session description offer
     */
    public void sendVideoLiveRequest(@NonNull String from, @NonNull SessionDescription sdpOffer) {
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            Log.e(TAG, "Main thread is used! in SignallingClient.logIn");
            throw new NetworkOnMainThreadException();
        }

        Log.i(TAG, "Sending live request to server");

        JSONObject jsonObject = new JSONObject();

        try {
            jsonObject.put(ID, ID_GET_LIVE);
            jsonObject.put(FROM, from);
            jsonObject.put(SDP_OFFER, sdpOffer.description);

            webSocket.sendText(jsonObject.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * This methods send a request for recording video
     * <b>It should not be called from the main thread</b>
     *
     * @param from     is the username
     * @param sdpOffer is de session description offer
     * @throws NetworkOnMainThreadException if it runs on the main thread
     */
    public void sendVideoForRecord(@NonNull String from, @NonNull SessionDescription sdpOffer) throws NetworkOnMainThreadException {
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            Log.e(TAG, "Main thread is used! in SignallingClient.logIn");
            throw new NetworkOnMainThreadException();
        }

        Log.i(TAG, String.format("Sending stream to server!"));
        JSONObject object = new JSONObject();
        try {
            object.put(ID, ID_START_REC);
            object.put(FROM, from);
            object.put(SDP_OFFER, sdpOffer.description);

            webSocket.sendText(object.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends a request for stopping the video streaming
     * <b>It should not be called from the main thread</b>
     *
     * @param from is the username
     * @throws NetworkOnMainThreadException
     */
    public void stopVideoRecording(@NonNull String from) {
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            Log.e(TAG, "Main thread is used! in SignallingClient.logIn");
            throw new NetworkOnMainThreadException();
        }

        Log.i(TAG, "Stopping streaming to the server!");
        JSONObject message = new JSONObject();
        try {
            message.put(ID, ID_STOP_REC);
            message.put(FROM, from);

            webSocket.sendText(message.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends offer to the server
     * Called when peer is initiator
     *
     * @param sdpOffer should be OFFER type
     * @see SessionDescription.Type
     */
    public void call(String from, String to, SessionDescription sdpOffer) {
        Log.i(TAGC, String.format("Calling from %s to %s", from, to));
        JSONObject object = new JSONObject();
        try {
            object.put("id", "call");
            object.put("from", from);
            object.put("to", to);
            object.put("sdpOffer", sdpOffer.description);

            webSocket.sendText(object.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends sdp offer to the server
     * Called when peer is called
     *
     * @param message should be OFFER type
     * @see SessionDescription.Type
     */
    public void emitIncomingCallResponse(SessionDescription message, String from) {
        JSONObject object = new JSONObject();
        try {
            object.put("id", "incomingCallResponse");
            object.put("from", from);
            object.put("callResponse", "accept");
            object.put("sdpOffer", message.description);

            webSocket.sendText(object.toString());

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Used to send ice candidate from client to the server.
     * Candidates are processed on server
     *
     * @param iceCandidate is the candidate
     * @throws NetworkOnMainThreadException if it runs on the main thread
     */
    public void emitIceCandidate(@NonNull IceCandidate iceCandidate, @NonNull String iceFor) throws NetworkOnMainThreadException {
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            Log.e(TAG, "Main thread is used! in SignallingClient.logIn");
            throw new NetworkOnMainThreadException();
        }
        try {
            JSONObject object = new JSONObject();
            JSONObject candidate = new JSONObject();

            object.put(ID, ID_ICE_CANDIDATE);
            object.put("for", iceFor);
            candidate.put("sdpMLineIndex", iceCandidate.sdpMLineIndex);
            candidate.put("sdpMid", iceCandidate.sdpMid);
            candidate.put("candidate", iceCandidate.sdp);

            object.put("candidate", candidate);

            webSocket.sendText(object.toString());

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * Used to send a play request to the server from client
     *
     * @param from        is the username
     * @param description is the sdp offer
     * @throws NetworkOnMainThreadException if it runs on the main thread
     */
    public void play(String from, SessionDescription description) throws NetworkOnMainThreadException {
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            Log.e(TAG, "Main thread is used! in SignallingClient.logIn");
            throw new NetworkOnMainThreadException();
        }
        Log.i(TAGC, String.format("PLaying from %s.", from));
        JSONObject object = new JSONObject();
        try {
            object.put(ID, ID_PLAY);
            object.put(USER, from);
            object.put(SDP_OFFER, description.description);

            webSocket.sendText(object.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void subscribeLoginListener(@NonNull LoginListener loginListener) {
        this.loginListener = loginListener;
    }

    public void subscribeMediaListenerRecord(@NonNull MediaListener.RecordingListener mediaListenerRecord) throws UserNotLoggedInException {
        if (!isLoggedIn)
            throw new UserNotLoggedInException();
        this.mediaListenerRecord = mediaListenerRecord;
    }

    public void subscribeMediaListener(@NonNull MediaListener.OneToOneCallListener mediaListener) throws UserNotLoggedInException {
        if (!isLoggedIn)
            throw new UserNotLoggedInException();
        this.mediaListener = mediaListener;
    }

    public void subscribeEnrollListener(@NonNull EnrollListener enrollListener) throws UserNotLoggedInException {
        if (!isLoggedIn)
            throw new UserNotLoggedInException();
        this.enrollListener = enrollListener;
    }

    public void subscribeLiveVideoListener(@NonNull MediaListener.LivePlayListener livePlayListener) throws UserNotLoggedInException {
        if (!isLoggedIn)
            throw new UserNotLoggedInException();

        this.livePlayListener = livePlayListener;
    }

    public void unsubscribeLiveVideoListener() {
        this.livePlayListener = null;
    }

    //region Socket listener
    private static final String TAG = "WsListener";

    @Override
    public void onStateChanged(WebSocket websocket, WebSocketState newState) throws Exception {
        Log.i(TAG, "onStateChanged");
    }

    @Override
    public void onConnected(WebSocket websocket, Map<String, List<String>> headers) throws Exception {
        Log.i(TAG, "connection established");

        try {
            Roles role = Roles.valueOf(headers.get("role").get(0));
            isLoggedIn = true;
            loginListener.onLoginSuccess(role);
        } catch (NullPointerException e) {
            Log.i(TAG, "No role in headers");
        }
    }

    @Override
    public void onConnectError(WebSocket websocket, WebSocketException cause) throws Exception {
        Log.i(TAG, "onConnectedError: " + cause.getMessage());

    }

    @Override
    public void onDisconnected(WebSocket websocket, WebSocketFrame serverCloseFrame, WebSocketFrame clientCloseFrame, boolean closedByServer) throws Exception {
        Log.i(TAG, "onDisconnected");
    }

    @Override
    public void onFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {
        Log.i(TAG, "onFrame");
    }

    @Override
    public void onContinuationFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {
        Log.i(TAG, "onContinuationFrame");
    }

    @Override
    public void onTextFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {
        Log.i(TAG, "onTextFrame");
    }

    @Override
    public void onBinaryFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {
        Log.i(TAG, "onBinaryFrame");
    }

    @Override
    public void onCloseFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {
        Log.i(TAG, "onCloseFrame");
    }

    @Override
    public void onPingFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {
        Log.w(TAG, "onPingFrame");
    }

    @Override
    public void onPongFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {
        Log.w(TAG, "onPongFrame");
    }

    @Override
    public void onTextMessage(WebSocket websocket, String text) throws Exception {
        Log.i(TAG, "Received Text message: " + text);
        try {

            Log.i(TAG, "Rec brut: " + text);

            JsonObject jsonObject = gson.fromJson(text, JsonObject.class);
            switch (jsonObject.get("id").getAsString()) {
                case EVENT_START_COMMUNICATION: // sdp answer
                    Log.i(TAG, "Got start communication");
                    String sdpAnwer = jsonObject.get("sdpAnswer").getAsString();
                    mediaListener.onStartCommunication(sdpAnwer);
                    break;
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
                            break;
                        case USE_ICE_FOR_RECORDING:
                            if (mediaListenerRecord != null) {
                                mediaListenerRecord.onIceCandidate(jsonObject.getAsJsonObject("candidate"));
                            }
                            break;
                    }
                    break;
                case EVENT_PLAY_RESPONSE:
                    String response = jsonObject.get("response").getAsString();
                    if (!response.equals("accepted")) {
                        Log.e(TAG, "Play request rejected!");
                        break;
                    }
                    sdpAnwer = jsonObject.get("sdpAnswer").getAsString();
                    mediaListener.onPlayResponse(sdpAnwer);
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

                    users = gson.fromJson(response, userListType);
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

                    Type userListType = new TypeToken<ArrayList<Video>>() {
                    }.getType();

                    synchronized (videos) {
                        videos.addAll(gson.fromJson(response, userListType));

                        Log.d("ThreadResp", "Before notify");
                        mConditionVariable.open();
                        Log.d("ThreadResp", "After notify");
                    }

                    break;
                default:
                    Log.e(TAGC, "Client received an unknown message from server!");
                    break;

            }
        } catch (JsonSyntaxException e) {
            Log.e(TAGC, "json invalid");
        }
    }

    @Override
    public void onTextMessage(WebSocket websocket, byte[] data) throws Exception {
        Log.d(TAG, "onDataMessage");
    }

    @Override
    public void onBinaryMessage(WebSocket websocket, byte[] binary) throws Exception {
        Log.d(TAG, "onBinaryMessage");
    }

    @Override
    public void onSendingFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {
        Log.d(TAG, "onSendingMessage");
    }

    @Override
    public void onFrameSent(WebSocket websocket, WebSocketFrame frame) throws Exception {
        Log.d(TAG, "onFrameSent");
    }

    @Override
    public void onFrameUnsent(WebSocket websocket, WebSocketFrame frame) throws Exception {
        Log.d(TAG, "onFrameUnsent");
    }

    @Override
    public void onThreadCreated(WebSocket websocket, ThreadType threadType, Thread thread) throws Exception {
        Log.d(TAG, "onThreadCreated");
    }

    @Override
    public void onThreadStarted(WebSocket websocket, ThreadType threadType, Thread thread) throws Exception {
        Log.d(TAG, "onThreadStarted");
    }

    @Override
    public void onThreadStopping(WebSocket websocket, ThreadType threadType, Thread thread) throws Exception {
        Log.d(TAG, "onThreadStopping");
    }

    @Override
    public void onError(WebSocket websocket, WebSocketException cause) throws Exception {
        Log.i(TAG, "onError: " + cause.getMessage());
        String message = cause.getMessage();

        if (message.contains("HTTP/1.1 401")) {
            loginListener.onLoginError();
        }
    }

    @Override
    public void onFrameError(WebSocket websocket, WebSocketException cause, WebSocketFrame frame) throws Exception {
        Log.d(TAG, "onFrameError");
    }

    @Override
    public void onMessageError(WebSocket websocket, WebSocketException cause, List<WebSocketFrame> frames) throws Exception {
        Log.d(TAG, "onMessageError");
    }

    @Override
    public void onMessageDecompressionError(WebSocket websocket, WebSocketException cause, byte[] compressed) throws Exception {
        Log.d(TAG, "onMessageDecompressionError");
    }

    @Override
    public void onTextMessageError(WebSocket websocket, WebSocketException cause, byte[] data) throws Exception {
        Log.d(TAG, "onTextMessageError");
    }

    @Override
    public void onSendError(WebSocket websocket, WebSocketException cause, WebSocketFrame frame) throws Exception {
        Log.d(TAG, "onSendError");
    }

    @Override
    public void onUnexpectedError(WebSocket websocket, WebSocketException cause) throws Exception {
        Log.d(TAG, "onUnexpectedError: " + cause.getMessage());
    }

    @Override
    public void handleCallbackError(WebSocket websocket, Throwable cause) throws Exception {
        Log.d(TAG, "handleCallbackError");
    }

    @Override
    public void onSendingHandshake(WebSocket websocket, String requestLine, List<String[]> headers) throws Exception {

        Log.d(TAG, "onSendingHandshake");
    }
    //endregion
}

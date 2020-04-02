package ro.atm.corden.util.websocket;

import android.annotation.SuppressLint;
import android.os.ConditionVariable;
import android.os.Looper;
import android.os.NetworkOnMainThreadException;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.JsonObject;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;

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
public class SignallingClient {
    private static final String TAG = "SignallingClient";

    private WebSocket webSocket;

    public boolean isChannelReady = false;
    public boolean isInitiator = false;
    public boolean isStarted = false;

    private static SignallingClient INSTANCE = new SignallingClient();

    /**
     * Class is initialised at application start
     */
    private SignallingClient() {
        try {
            //uri = new URI("wss://192.168.8.100:8443/websocket"); // atunci cand e conectat prin stick
            URI uri = new URI("wss://192.168.0.103:8443/websocket"); // wifi acasa

            try {
                SSLContext context = SSLContext.getInstance("TLS");
                context.init(null, trustAllCerts, null);

                webSocket = new WebSocket(uri);
                webSocket.setSocketFactory(context.getSocketFactory());

            } catch (NoSuchAlgorithmException e) {
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
            Log.i(TAG, ": authType: " + authType);
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
            Log.i(TAG, ": authType: " + authType);
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
        if (webSocket.loginListener == null) {
            throw new LoginListenerNotInitialisedException();
        }
        webSocket.addHeader("username", username);
        webSocket.addHeader("password", password);
        webSocket.connect();
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

            Log.i(TAG, "Get all users event: " + jsonObject.toString());
            webSocket.send(jsonObject.toString());
            webSocket.usersConditionVariable = new ConditionVariable(false);

            webSocket.usersConditionVariable.block();
            if (webSocket.users != null) {
                return webSocket.users;
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

            webSocket.send(message.toString());
            webSocket.videosConditionVariable = new ConditionVariable(false);
            webSocket.videosConditionVariable.block();
            if (webSocket.videos != null) {
                return webSocket.videos;
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
    void sendOnlineUsersRequest() throws NetworkOnMainThreadException {
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            Log.e(TAG, "Main thread is used! in SignallingClient.getOnlineUsersRequest");
            throw new NetworkOnMainThreadException();
        }
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(ID, ID_LIST_USERS);
            jsonObject.put(TYPE, USERS_TYPE_REQ_ONLINE);

            Log.i(TAG, "Get all users event: " + jsonObject.toString());

            webSocket.send(jsonObject.toString());
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

            Log.i(TAG, "Register event: " + jsonObject.toString());

            webSocket.send(jsonObject.toString());

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

            webSocket.send(jsonObject.toString());
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

            webSocket.send(jsonObject.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Used by admin to send a video playback request to the application server
     * This method should not be used on the main thread
     *
     * @param sdpOffer  is the Session Description Offer {@link SessionDescription} of the admin {@link Roles}user
     * @param videoPath is the video path in media server, user should already have it from the app server
     * @throws NetworkOnMainThreadException if main thread is used
     */
    public void sendPlayVideoRequest(@NonNull SessionDescription sdpOffer, @NonNull String videoPath) {
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            Log.e(TAG, "Main thread is used! in SignallingClient.logIn");
            throw new NetworkOnMainThreadException();
        }
        JsonObject message = new JsonObject();
        message.addProperty(ID, ID_PLAY_VIDEO);
        message.addProperty(SDP_OFFER, sdpOffer.description);
        message.addProperty("videoPath", videoPath);

        webSocket.send(message.toString());
    }

    /**
     * Used to send video pause request
     * It should not be used form the main thread
     */
    public void sendPauseVideoRequest() {
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            Log.e(TAG, "Main thread is used! in SignallingClient.logIn");
            throw new NetworkOnMainThreadException();
        }
        JsonObject message = new JsonObject();
        message.addProperty(ID, ID_PAUSE_VIDEO);

        webSocket.send(message.toString());
    }

    /***/
    public void sendResumeVideoRequest() {
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            Log.e(TAG, "Main thread is used! in SignallingClient.logIn");
            throw new NetworkOnMainThreadException();
        }

        JsonObject message = new JsonObject();
        message.addProperty(ID, ID_RESUME_VIDEO);

        webSocket.send(message.toString());
    }

    /***/
    public void sendGetVideoPositionRequest() {
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            Log.e(TAG, "Main thread is used! in SignallingClient.logIn");
            throw new NetworkOnMainThreadException();
        }

        JsonObject message = new JsonObject();
        message.addProperty(ID, ID_GET_POSITION_VIDEO);

        webSocket.send(message.toString());
    }

    /***/
    public void sendSeekVideoRequest(long position) {
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            Log.e(TAG, "Main thread is used! in SignallingClient.logIn");
            throw new NetworkOnMainThreadException();
        }

        JsonObject message = new JsonObject();
        message.addProperty(ID, ID_DO_SEEK_VIDEO);
        message.addProperty("position", position);

        webSocket.send(message.toString());
    }

    /***/
    public void sendStopVideoRequest() {
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            Log.e(TAG, "Main thread is used! in SignallingClient.logIn");
            throw new NetworkOnMainThreadException();
        }

        JsonObject message = new JsonObject();
        message.addProperty(ID, ID_STOP_VIDEO);

        webSocket.send(message.toString());
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

            webSocket.send(object.toString());
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

            webSocket.send(message.toString());
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

            webSocket.send(object.toString());

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * Used by media player to send a play request to the server from client
     *
     * @param from        is the username
     * @param description is the sdp offer
     * @throws NetworkOnMainThreadException if it runs on the main thread
     * @deprecated
     */
    @Deprecated
    public void play(String from, SessionDescription description) throws NetworkOnMainThreadException {
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            Log.e(TAG, "Main thread is used! in SignallingClient.logIn");
            throw new NetworkOnMainThreadException();
        }
        Log.i(TAG, String.format("PLaying from %s.", from));
        JSONObject object = new JSONObject();
        try {
            object.put(ID, ID_PLAY);
            object.put(USER, from);
            object.put(SDP_OFFER, description.description);

            webSocket.send(object.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void subscribeLoginListener(@NonNull LoginListener loginListener) {
        webSocket.loginListener = loginListener;
    }

    public void subscribeMediaListenerRecord(@NonNull MediaListener.RecordingListener mediaListenerRecord) throws UserNotLoggedInException {
        if (!webSocket.isLoggedIn)
            throw new UserNotLoggedInException();
        webSocket.mediaListenerRecord = mediaListenerRecord;
    }

    public void subscribeEnrollListener(@NonNull EnrollListener enrollListener) throws UserNotLoggedInException {
        if (!webSocket.isLoggedIn)
            throw new UserNotLoggedInException();
        webSocket.enrollListener = enrollListener;
    }

    public void subscribeLiveVideoListener(@NonNull MediaListener.LivePlayListener livePlayListener) throws UserNotLoggedInException {
        if (!webSocket.isLoggedIn)
            throw new UserNotLoggedInException();

        webSocket.livePlayListener = livePlayListener;
    }

    public void subscribePlaybackVideoListener(@NonNull MediaListener.PlaybackListener playbackListener) throws UserNotLoggedInException {
        if (!webSocket.isLoggedIn)
            throw new UserNotLoggedInException();
        webSocket.playbackListener = playbackListener;
    }

    public void unsubscribeLiveVideoListener() {
        webSocket.livePlayListener = null;
    }

    public void unsubscribePlaybackVideoListener() {
        webSocket.playbackListener = null;
    }
}

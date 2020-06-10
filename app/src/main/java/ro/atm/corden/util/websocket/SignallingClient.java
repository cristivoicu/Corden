package ro.atm.corden.util.websocket;

import android.content.Context;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Looper;
import android.os.NetworkOnMainThreadException;
import android.util.Log;

import androidx.annotation.NonNull;

import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import de.adorsys.android.securestoragelibrary.SecurePreferences;
import de.adorsys.android.securestoragelibrary.SecureStorageException;
import ro.atm.corden.R;
import ro.atm.corden.model.user.LoginUser;
import ro.atm.corden.model.user.Role;
import ro.atm.corden.util.exception.login.CertNoPassException;
import ro.atm.corden.util.exception.login.LoginListenerNotInitialisedException;
import ro.atm.corden.util.exception.websocket.UserNotLoggedInException;
import ro.atm.corden.util.websocket.callback.EnrollListener;
import ro.atm.corden.util.websocket.callback.LoginListener;
import ro.atm.corden.util.websocket.callback.MapItemsListener;
import ro.atm.corden.util.websocket.callback.MediaListener;
import ro.atm.corden.util.websocket.callback.RemoveVideoListener;
import ro.atm.corden.util.websocket.callback.UpdateUserListener;
import ro.atm.corden.util.websocket.protocol.Message;
import ro.atm.corden.util.websocket.protocol.events.MediaEventType;
import ro.atm.corden.util.websocket.protocol.events.RequestEventTypes;
import ro.atm.corden.util.websocket.protocol.events.SubscribeEventType;
import ro.atm.corden.util.websocket.protocol.events.UnsubscribeEventType;
import ro.atm.corden.util.websocket.protocol.events.UpdateEventType;
import ro.atm.corden.util.websocket.subscribers.LiveStreamerSubscriber;
import ro.atm.corden.util.websocket.subscribers.UserSubscriber;


/**
 * Singleton class used that has the following functionality
 * <ul>
 *     <li> establish the connection with the application server</li>
 *     <li> parsing the server messages events</li>
 *     <li> has the methods necessary for sending messages and requests to the server</li>
 * </ul>
 * <p>Methods used from this class should not be used from the main thread</p>
 */
public class SignallingClient {
    private static final String TAG = "SignallingClient";

    WebSocket webSocket = null;
    private boolean isReconnect = false;

    public boolean isChannelReady = false;
    public boolean isInitiator = false;
    public boolean isStarted = false;

    private static SignallingClient INSTANCE = new SignallingClient();

    /**
     * Class is initialised at application start
     */
    private SignallingClient() {

    }

    public void initWebSocket(Context context) throws CertNoPassException, IOException {
        try {
            URI uri = new URI("wss://corden.go.ro:8443/websocket"); // wifi acasa

            try {
                String trustPass = SecurePreferences.getStringValue(context, "trustStorePass", "");
                String keyPass = SecurePreferences.getStringValue(context, "keyStorePass", "");

                if(trustPass.isEmpty() || keyPass.isEmpty()){
                    throw new CertNoPassException();
                }

                String keyStoreType = "PKCS12";
                KeyStore trustStore = KeyStore.getInstance(keyStoreType);
                //InputStream serverCert = context.getResources().openRawResource(R.raw.applicationserver);
                InputStream caCert = context.getResources().openRawResource(R.raw.corden_ca);
                trustStore.load(caCert, trustPass.toCharArray());

                InputStream clientCert = context.getResources().openRawResource(R.raw.androidclient);
                KeyStore keyStore = KeyStore.getInstance(keyStoreType);
                keyStore.load(clientCert,  keyPass.toCharArray());

                // initialize key manager factory with the read client certificate
                KeyManagerFactory keyManagerFactory = null;
                keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                keyManagerFactory.init(keyStore, keyPass.toCharArray());

                // Create a TrustManager that trusts the CAs in our KeyStore
                String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
                TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
                tmf.init(trustStore);

                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(keyManagerFactory.getKeyManagers(), tmf.getTrustManagers(), null);

                webSocket = new WebSocket(uri, context.getApplicationContext());
                webSocket.setConnectionLostTimeout(0);

                webSocket.setSocketFactory(sslContext.getSocketFactory());

            } catch (NoSuchAlgorithmException | UnrecoverableKeyException | CertificateException | KeyStoreException | KeyManagementException e) {
                e.printStackTrace();
            }

        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

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
        if (!isReconnect) {
            webSocket.connect();
            isReconnect = true;
        } else
            webSocket.reconnect();
    }

    /**
     * Used by admin to intercept user recording session
     *
     * @param username is the username that is sending live video to the media server for recording
     * @param sdpOffer is the session description offer
     */
    public void sendWatchVideoRequest(@NonNull String username, @NonNull SessionDescription sdpOffer) {
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            Log.e(TAG, "Main thread is used! in SignallingClient.logIn");
            throw new NetworkOnMainThreadException();
        }

        Log.i(TAG, "Sending live request to server");

        Message message = new Message.MediaMessageBuilder()
                .addEvent(MediaEventType.START_VIDEO_WATCH)
                .addSdpOffer(sdpOffer)
                .addUser(username)
                .build();

        webSocket.send(message.toString());
    }

    /**
     * Used by admin to stop intercepting user recording session
     *
     * @param username is the username that is sending live video to the media server for recording
     */
    public void sendStopWatchVideoRequest(@NonNull String username) {
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            Log.e(TAG, "Main thread is used! in SignallingClient.logIn");
            throw new NetworkOnMainThreadException();
        }

        Log.i(TAG, "Sending live request to server");

        Message message = new Message.MediaMessageBuilder()
                .addEvent(MediaEventType.STOP_VIDEO_WATCH)
                .addUser(username)
                .build();

        webSocket.send(message.toString());
    }

    /**
     * Used by admin to send a video playback request to the application server
     * This method should not be used on the main thread
     *
     * @param sdpOffer  is the Session Description Offer {@link SessionDescription} of the admin {@link Role}user
     * @param videoPath is the video path in media server, user should already have it from the app server
     * @throws NetworkOnMainThreadException if main thread is used
     */
    public void sendPlayVideoRequest(@NonNull SessionDescription sdpOffer, @NonNull String videoPath) {
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            Log.e(TAG, "Main thread is used! in SignallingClient.logIn");
            throw new NetworkOnMainThreadException();
        }

        Message message = new Message.MediaMessageBuilder()
                .addEvent(MediaEventType.PLAY_VIDEO_REQ)
                .addSdpOffer(sdpOffer)
                .addVideoPath(videoPath)
                .build();
        Log.e("PLAY VIDEO REQ", message.toString());
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
        Message message = new Message.MediaMessageBuilder()
                .addEvent(MediaEventType.PAUSE_VIDEO_REQ)
                .build();

        webSocket.send(message.toString());
    }

    /***/
    public void sendResumeVideoRequest() {
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            Log.e(TAG, "Main thread is used! in SignallingClient.logIn");
            throw new NetworkOnMainThreadException();
        }
        Message message = new Message.MediaMessageBuilder()
                .addEvent(MediaEventType.RESUME_VIDEO)
                .build();

        Log.e(TAG, "RESUME SENT: " + message.toString());
        webSocket.send(message.toString());
    }

    /***/
    public void sendGetVideoPositionRequest() {
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            Log.e(TAG, "Main thread is used! in SignallingClient.logIn");
            throw new NetworkOnMainThreadException();
        }
        Message message = new Message.MediaMessageBuilder()
                .addEvent(MediaEventType.GET_VIDEO_POSITION_REQ)
                .build();

        webSocket.send(message.toString());
    }

    /***/
    public void sendSeekVideoRequest(long position) {
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            Log.e(TAG, "Main thread is used! in SignallingClient.logIn");
            throw new NetworkOnMainThreadException();
        }
        Message message = new Message.MediaMessageBuilder()
                .addEvent(MediaEventType.SEEK_VIDEO_REQ)
                .addVideoPosition(position)
                .build();

        webSocket.send(message.toString());
    }

    /***/
    public void sendStopVideoRequest() {
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            Log.e(TAG, "Main thread is used! in SignallingClient.logIn");
            throw new NetworkOnMainThreadException();
        }
        Message message = new Message.MediaMessageBuilder()
                .addEvent(MediaEventType.STOP_VIDEO_REQ)
                .build();

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
    public void sendLiveStreamVideo(@NonNull String from, @NonNull SessionDescription sdpOffer) throws NetworkOnMainThreadException {
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            Log.e(TAG, "Main thread is used! in SignallingClient.logIn");
            throw new NetworkOnMainThreadException();
        }
        Message message = new Message.MediaMessageBuilder()
                .addEvent(MediaEventType.START_LIVE_STREAM)
                .addSdpOffer(sdpOffer)
                .build();

        webSocket.send(message.toString());
    }

    /**
     * Sends a request for stopping the video streaming
     * <b>It should not be called from the main thread</b>
     *
     * @param from is the username
     * @throws NetworkOnMainThreadException
     */
    public void stopVideoStreaming(@NonNull String from) {
        StopServiceAsyncTask stopServiceAsyncTask = new StopServiceAsyncTask();
        stopServiceAsyncTask.execute();
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
            Message message = new Message.MediaMessageBuilder()
                    .addEvent(MediaEventType.ICE_CANDIDATE)
                    .addIceCandidate(iceCandidate, iceFor)
                    .build();

            webSocket.send(message.toString());

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /***/
    public void sendStreamRequest(@NonNull String username) {
        new AsyncMessageToStreamRequest().execute(username);
    }

    /***/
    public void sendLiveLocation(Location location) {
/*        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            Log.e(TAG, "Main thread is used! in SignallingClient.logIn");
            throw new NetworkOnMainThreadException();
        }*/

        Message message = new Message.UpdateMessageBuilder()
                .addEvent(UpdateEventType.LOCATION)
                .addLocation(location.getLatitude(), location.getLongitude(), location.getAccuracy())
                .build();

        webSocket.send(message.toString());
    }

    public void sendDetectedActivity(String detectedActivity, int precision) {
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            Log.e(TAG, "Main thread is used! in SignallingClient.logIn");
            throw new NetworkOnMainThreadException();
        }

        Message message = new Message.ActivityMessageBuilder()
                .addEvent(detectedActivity)
                .addPrecision(precision)
                .build();

        webSocket.send(message.toString());
    }

    //region Subscribe and unsubscribe methods
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

    public void unsubscribeEnrollListener() {
        webSocket.enrollListener = null;
    }

    public void subscribeLiveVideoListener(@NonNull MediaListener.LiveStreamingListener liveStreamingListener) throws UserNotLoggedInException {
        if (!webSocket.isLoggedIn)
            throw new UserNotLoggedInException();

        webSocket.liveStreamingListener = liveStreamingListener;
    }

    public void unsubscribeLiveVideoListener() {
        webSocket.liveStreamingListener = null;
    }

    public void subscribePlaybackVideoListener(@NonNull MediaListener.PlaybackListener playbackListener) throws UserNotLoggedInException {
        if (!webSocket.isLoggedIn)
            throw new UserNotLoggedInException();
        webSocket.playbackListener = playbackListener;
    }

    public void unsubscribePlaybackVideoListener() {
        webSocket.playbackListener = null;
    }

    public void subscribeUpdateUserListener(@NonNull UpdateUserListener updateUserListener) throws UserNotLoggedInException {
        if (!webSocket.isLoggedIn) {
            throw new UserNotLoggedInException();
        }
        webSocket.updateUserListener = updateUserListener;
    }

    public void unsubscribeUpdateUserListener() {
        webSocket.updateUserListener = null;
    }

    public void subscribeMapItemsSaveListener(@NonNull MapItemsListener mapItemsListener) throws UserNotLoggedInException {
        if (!webSocket.isLoggedIn) {
            throw new UserNotLoggedInException();
        }
        webSocket.mapItemsListener = mapItemsListener;
    }

    public void ubsubscribeMapItemsSaveListener() {
        webSocket.mapItemsListener = null;
    }

    public void subscribeRemoveVideoListener(@NonNull RemoveVideoListener removeVideoListener) throws UserNotLoggedInException {
        if (!webSocket.isLoggedIn) {
            throw new UserNotLoggedInException();
        }

        webSocket.removeVideoListener = removeVideoListener;
    }

    public void unsubscribeRemoveVideoListener() {
        webSocket.removeVideoListener = null;
    }
    //endregion

    public void subscribeUserListListener(UserSubscriber userSubscriber) {
        webSocket.userSubscriber = userSubscriber;
    }

    public void unsubscribeUserListListener() {
        webSocket.userSubscriber = null;
    }

    /**
     * Method is used to send a message to application server for subscribing to user list
     * changing like: login, logout, user edited, etc.
     * <p>
     * This method must not be used from UI thread.
     */
    public void sendMessageToSubscribeToUserList() {
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            Log.e(TAG, "Main thread is used! in SignallingClient.logIn");
            throw new NetworkOnMainThreadException();
        }

        Message message = new Message.SubscribeMessageBuilder()
                .addEvent(SubscribeEventType.USER_UPDATED)
                .build();

        webSocket.send(message.toString());
    }

    /**
     * Method used to send a message to application server to unsubscribe  from user list changing
     *
     * <p>This method must not be used from UI thread</p>
     */
    public void sendMessageToUnsubscribeFromUserList() {
        new AsyncMessageToUnsubscribe().execute();
    }

    /**
     * Subscribe client to the server events.
     * <p> Client will receive message about user streaming status.</p>
     */
    public void sendMessageToSubscribeToLiveStreamersEvents(LiveStreamerSubscriber liveStreamerSubscriber) {
        /*if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            Log.e(TAG, "Main thread is used! in SignallingClient.logIn");
            throw new NetworkOnMainThreadException();
        }*/
        webSocket.liveStreamerSubscriber = liveStreamerSubscriber;
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                Message message = new Message.SubscribeMessageBuilder()
                        .addEvent(SubscribeEventType.LIVE_STREAMERS)
                        .build();

                webSocket.send(message.toString());
                return null;
            }
        }.execute();

    }

    /**
     * Unsubscribe from live server events about user streaming status.
     */
    public void sendMessageToUnsubscribeFromLiveStreamersEvents() {
        /*if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            Log.e(TAG, "Main thread is used! in SignallingClient.logIn");
            throw new NetworkOnMainThreadException();
        }*/
        webSocket.liveStreamerSubscriber = null;
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                Message message = new Message.UnsubscribeMessageBuilder()
                        .addEvent(UnsubscribeEventType.UNSUBSCRIBE_LIVE_STREAMERS)
                        .build();

                webSocket.send(message.toString());
                return null;
            }
        }.execute();

    }


    public void sendMessageToSubscribeToMapItems() {
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            Log.e(TAG, "Main thread is used! in SignallingClient.logIn");
            throw new NetworkOnMainThreadException();
        }
        Message message = new Message.SubscribeMessageBuilder()
                .addEvent(SubscribeEventType.MAP_ITEMS)
                .build();
        webSocket.send(message.toString());
    }

    /***/
    public void sendMessageToUnsubscribeFromMapItems() {
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            Log.e(TAG, "Main thread is used! in SignallingClient.logIn");
            throw new NetworkOnMainThreadException();
        }
        Message message = new Message.UnsubscribeMessageBuilder()
                .addEvent(UnsubscribeEventType.UNSUBSCRIBE_MAP_ITEM)
                .build();
        webSocket.send(message.toString());
    }

    public void setMapItemListener(MapItemsListener mapItemListener) {
        webSocket.mapItemsListener = mapItemListener;
    }

    public void unSetMapItemListener() {
        webSocket.mapItemsListener = null;
    }

    public void logout() {
        webSocket.close();
    }

    //region Async Tasks
    private static class StopServiceAsyncTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            Message message = new Message.MediaMessageBuilder()
                    .addEvent(MediaEventType.STOP_LIVE_STREAM)
                    .build();

            SignallingClient.getInstance().webSocket.send(message.toString());
            return null;
        }
    }

    private static class AsyncMessageToUnsubscribe extends AsyncTask<Void, Void, Void>{
        @Override
        protected Void doInBackground(Void... voids) {
            Message message = new Message.UnsubscribeMessageBuilder()
                    .addEvent(UnsubscribeEventType.UNSUBSCRIBE_USER_UPDATED)
                    .build();

            SignallingClient.getInstance().webSocket.send(message.toString());
            return null;
        }
    }
    private static class AsyncMessageToStreamRequest extends AsyncTask<String, Void, Void>{
        @Override
        protected Void doInBackground(String... strings) {
            Message message = new Message.RequestMessageBuilder()
                    .addEvent(RequestEventTypes.REQUEST_START_STREAMING)
                    .addUser(strings[0])
                    .build();
            SignallingClient.getInstance().webSocket.send(message.toString());
            return null;
        }
    }

    //endregion
}

package ro.atm.corden.util.services;

import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.gson.JsonObject;

import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera1Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.Logging;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.util.ArrayList;
import java.util.List;

import ro.atm.corden.R;
import ro.atm.corden.model.LoginUser;
import ro.atm.corden.util.App;
import ro.atm.corden.util.constant.JsonConstants;
import ro.atm.corden.util.exception.websocket.UserNotLoggedInException;
import ro.atm.corden.util.receiver.NotificationReceiver;
import ro.atm.corden.util.webrtc.client.Session;
import ro.atm.corden.util.webrtc.interfaces.MediaActivity;
import ro.atm.corden.util.webrtc.observer.SimplePeerConnectionObserver;
import ro.atm.corden.util.webrtc.observer.SimpleSdpObserver;
import ro.atm.corden.util.websocket.SignallingClient;
import ro.atm.corden.util.websocket.callback.MediaListener;
import ro.atm.corden.view.activity.MainActivityUser;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 *
 * Service should not be running if main activity of user is destroyed!
 * @see MainActivityUser
 */
public class StreamingIntentService extends IntentService implements MediaListener.RecordingListener, MediaActivity {
    private static final String TAG = "StreamIntentService";
    private static final String ACTION_STREAM = "ActionStream";

    private PowerManager.WakeLock wakeLock;

    private Session liveSession;

    VideoCapturer videoCapturer;
    private SurfaceTextureHelper surfaceTextureHelper;

    EglBase rootEglBase;

    private boolean gotUserMedia = false;

    public StreamingIntentService() {
        super("StreamingIntentService");
        setIntentRedelivery(true);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SteamIntent:Wakelock");
        wakeLock.acquire(600000); //wake for maximum 10 minutes when user turns off the screen, bc it drain battery

        Intent activityIntent = new Intent(getApplicationContext(), MainActivityUser.class);
        PendingIntent contentInteint = PendingIntent.getActivity(getApplicationContext(), 0, activityIntent, 0);

        Intent broadcastIntent = new Intent(this, NotificationReceiver.class);
        broadcastIntent.setAction("stop");
        PendingIntent actionIntent = PendingIntent.getBroadcast(this,
                0, broadcastIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = new NotificationCompat.Builder(this, App.STR_CHANNEL_ID)
                .setContentTitle("Streaming is live!")
                .setContentText("You are sending video stream to the media server")
                .addAction(R.drawable.ic_stop_black_24dp, "Stop", actionIntent)
                .setLights(getResources().getColor(R.color.colorError), 1000, 1000)
                .setVibrate(new long[]{1000, 500, 1000, 0, 1000, 0, 1000})
                .build();


        startForeground(1, notification);
        SignallingClient.getInstance().isInitiator = true;
        rootEglBase = EglBase.create();
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        Log.d(TAG, "onHandleIntent");
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_STREAM.equals(action)) {
                try {
                    start();
                    while(true){

                    }
                } catch (UserNotLoggedInException e) {
                    Log.e(TAG, "User not logged in exception");
                    //do nothing, service should terminate
                }
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");

        wakeLock.release();
        Log.d(TAG, "wakelock released");

        liveSession.leaveLiveSession();
    }

    public void start() throws UserNotLoggedInException {
        SignallingClient.getInstance().subscribeMediaListenerRecord(this);

        liveSession = new Session(this.getApplicationContext(), rootEglBase, this);
        liveSession.createLiveVideoClient();
        if(SignallingClient.getInstance().isChannelReady)
            onTryToStart();
    }

    private void onTryToStart() {
        if (!SignallingClient.getInstance().isStarted && liveSession.getVideoTrack() != null && SignallingClient.getInstance().isChannelReady) {
            SignallingClient.getInstance().isStarted = true;
            liveSession.createLiveOffer();
        }
    }


    @Override
    public void onStartResponse(String answer) {
        showToast("Received start response from media server");
        Log.d(TAG, "Received sdp answer!");
        liveSession.setRemoteResponse(answer);
    }

    @Override
    public void onIceCandidate(JsonObject data) {
        showToast("Receiving ice candidates");
        Log.d(TAG, "OnIceCandidate Rec");
        liveSession.addIceCandidate(new IceCandidate(data.get("sdpMid").getAsString(),
                                    data.get("sdpMLineIndex").getAsInt(),
                                    data.get("candidate").getAsString()));
    }

    @Override
    public void gotRemoteStream(MediaStream mediaStream) {
        // never receive remote stream
    }

    public void showToast(final String message) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
            }
        });
    }
}

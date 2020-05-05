package ro.atm.corden.util.services;

import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.gson.JsonObject;

import org.webrtc.CameraVideoCapturer;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaStream;
import org.webrtc.SurfaceViewRenderer;

import ro.atm.corden.util.App;
import ro.atm.corden.util.constant.AppConstants;
import ro.atm.corden.util.exception.websocket.UserNotLoggedInException;
import ro.atm.corden.util.webrtc.client.CameraSelector;
import ro.atm.corden.util.webrtc.client.Session;
import ro.atm.corden.util.webrtc.interfaces.MediaActivity;
import ro.atm.corden.util.websocket.SignallingClient;
import ro.atm.corden.util.websocket.callback.MediaListener;
import ro.atm.corden.view.activity.MainActivityUser;

import static org.webrtc.RendererCommon.ScalingType.SCALE_ASPECT_FILL;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * <p>
 * Service should not be running if main activity of user is destroyed!
 *
 * @see MainActivityUser
 */
public class StreamingIntentService extends IntentService implements MediaListener.RecordingListener, MediaActivity {
    private static final String TAG = "StreamIntentService";
    private static final String ACTION_STREAM = "ActionStream";
    private CameraSelector.CameraType cameraType;
    private static boolean isRunning = false;

    public static boolean isRunning() {
        return isRunning;
    }

    private IBinder mBind = new LocalBinder();

    private PowerManager.WakeLock wakeLock;

    private Session liveSession;

    /**
     * used for surface view renderer
     */
    private boolean isInited = false;

    EglBase rootEglBase;

    public StreamingIntentService() {
        super("StreamingIntentService");
        //setIntentRedelivery(true);
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBind;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "StreamIntent:Wakelock");
        wakeLock.acquire(600000); //wake for maximum 10 minutes when user turns off the screen, bc it drain battery

        Intent intent = new Intent(this, MainActivityUser.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        Notification notification = new NotificationCompat.Builder(this, App.STREAM_CHANNEL_ID)
                .setContentTitle("Streaming is live!")
                .setContentText("You are sending video stream to the media server")
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();

        startForeground(15, notification);
        SignallingClient.getInstance().isInitiator = true;
        rootEglBase = EglBase.create();
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        Log.d(TAG, "onHandleIntent");
        if (intent != null) {
            final String action = intent.getAction();
            cameraType = intent.getStringExtra(AppConstants.EXTRA_CAMERA) == null ? CameraSelector.CameraType.BACK : CameraSelector.CameraType.valueOf(intent.getStringExtra(AppConstants.EXTRA_CAMERA));
            if (ACTION_STREAM.equals(action)) {
                try {
                    isRunning = true;
                    start();
                    while (true) {

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

        isRunning = false;
        wakeLock.release();
        Log.d(TAG, "wakelock released");

        assert liveSession != null;
        liveSession.leaveLiveSession();

        SignallingClient.getInstance().isInitiator = false;
        SignallingClient.getInstance().isChannelReady = false;
        SignallingClient.getInstance().isStarted = false;
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
        stopSelf();
    }

    private void start() throws UserNotLoggedInException {
        SignallingClient.getInstance().subscribeMediaListenerRecord(this);

        liveSession = new Session(this.getApplicationContext(), rootEglBase, this);
        liveSession.createLiveVideoClient(cameraType);
        if (SignallingClient.getInstance().isChannelReady)
            onTryToStart();
    }

    private void onTryToStart() {
        if (!SignallingClient.getInstance().isStarted && liveSession.getVideoTrack() != null && SignallingClient.getInstance().isChannelReady) {
            SignallingClient.getInstance().isStarted = true;
            liveSession.createLiveOffer();
        }
    }

    public Session getLiveSession() {
        return liveSession;
    }

    public void showVideo(SurfaceViewRenderer localVideoView) {
        if (!isInited) {
            localVideoView.init(rootEglBase.getEglBaseContext(), null);
            localVideoView.setZOrderMediaOverlay(true);
            localVideoView.setMirror(true);
            localVideoView.setScalingType(SCALE_ASPECT_FILL);
            isInited = true;
        }

        liveSession.getVideoTrack().addSink(localVideoView);
    }

    public void hideVideo(SurfaceViewRenderer localVideoView) {
        liveSession.getVideoTrack().removeSink(localVideoView);
    }

    public void switchCamera(SurfaceViewRenderer localVideoView, boolean isMirrored) {
        CameraVideoCapturer cameraVideoCapturer = (CameraVideoCapturer) liveSession.getVideoCapturer();
        cameraVideoCapturer.switchCamera(null);
        localVideoView.setMirror(isMirrored);
    }

    public void showToast(final String message) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
            }
        });
    }

    public class LocalBinder extends Binder {
        public StreamingIntentService getService() {
            return StreamingIntentService.this;
        }
    }
}

package ro.atm.corden.util.services;

import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;
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
import ro.atm.corden.util.exception.websocket.UserNotLoggedInException;
import ro.atm.corden.util.receiver.NotificationReceiver;
import ro.atm.corden.util.webrtc.SimplePeerConnectionObserver;
import ro.atm.corden.util.webrtc.SimpleSdpObserver;
import ro.atm.corden.util.websocket.SignallingClient;
import ro.atm.corden.util.websocket.callback.MediaListener;
import ro.atm.corden.view.activity.MainActivityUser;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class StreamingIntentService extends IntentService implements MediaListener.RecordingListener {
    private static final String TAG = "StreamIntentService";
    private static final String ACTION_STREAM = "ActionStream";

    private PowerManager.WakeLock wakeLock;

    private PeerConnection localPeer;
    private PeerConnectionFactory peerConnectionFactory;
    private MediaConstraints audioConstraints;
    private MediaConstraints videoConstraints;
    private MediaConstraints sdpConstraints;
    private VideoSource videoSource;
    private VideoTrack localVideoTrack;
    private AudioSource audioSource;
    private AudioTrack localAudioTrack;

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

        Bitmap largeIcon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_video_marketing);

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

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                SignallingClient.getInstance().stopVideoRecording(LoginUser.username);
                return null;
            }
        }.execute();

        try {
            videoCapturer.stopCapture();
            videoCapturer.dispose();
            peerConnectionFactory.dispose();
            localPeer.dispose();
            localVideoTrack.dispose();
            localAudioTrack.dispose();
            videoSource.dispose();;
            audioSource.dispose();

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void start() throws UserNotLoggedInException {
        SignallingClient.getInstance().subscribeMediaListenerRecord(this);
        createPeerConnectionFactory();
        captureFromCamera();
        createPeerConnection();
        if(SignallingClient.getInstance().isChannelReady){
            onTryToStart();
        }
    }

    private void captureFromCamera() {
        //Now create a VideoCapturer instance.
        videoCapturer = createCameraCapturer(new Camera1Enumerator(false));

        //Create MediaConstraints - Will be useful for specifying video and audio constraints.
        audioConstraints = new MediaConstraints();
        videoConstraints = new MediaConstraints();

        //Create a VideoSource instance
        if (videoCapturer != null) {
            surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", rootEglBase.getEglBaseContext());
            videoSource = peerConnectionFactory.createVideoSource(videoCapturer.isScreencast());
            videoCapturer.initialize(surfaceTextureHelper, this, videoSource.getCapturerObserver());
        }
        localVideoTrack = peerConnectionFactory.createVideoTrack("100", videoSource);

        //create an AudioSource instance
        audioSource = peerConnectionFactory.createAudioSource(audioConstraints);
        localAudioTrack = peerConnectionFactory.createAudioTrack("101", audioSource);

        if (videoCapturer != null) {
            videoCapturer.startCapture(1024, 720, 30);
        }
    }

    private void onTryToStart() {
        if (!SignallingClient.getInstance().isStarted && localAudioTrack != null && SignallingClient.getInstance().isChannelReady) {
            SignallingClient.getInstance().isStarted = true;
            doCall();
        }
    }

    /**
     * This method is called when the app is the initiator - We generate the offer and send it over through socket
     * to remote peer
     */
    private void doCall() {
        sdpConstraints = new MediaConstraints();
        sdpConstraints.mandatory.add(
                new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        sdpConstraints.mandatory.add(
                new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
        localPeer.createOffer(new SimpleSdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                super.onCreateSuccess(sessionDescription);
                localPeer.setLocalDescription(new SimpleSdpObserver(), sessionDescription);
                Log.d("onCreateSuccess", "SignallingClient emit ");
                Log.d(TAG, "Sending video for record");
                SignallingClient.getInstance().sendVideoForRecord(LoginUser.username, sessionDescription);
            }
        }, sdpConstraints);
    }


    private void createPeerConnectionFactory() {
        //Initialize PeerConnectionFactory globals.
        PeerConnectionFactory.InitializationOptions initializationOptions =
                PeerConnectionFactory.InitializationOptions.builder(this)
                        .createInitializationOptions();
        PeerConnectionFactory.initialize(initializationOptions);

        //Create a new PeerConnectionFactory instance - using Hardware encoder and decoder.
        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        DefaultVideoEncoderFactory defaultVideoEncoderFactory = new DefaultVideoEncoderFactory(
                rootEglBase.getEglBaseContext(),  /* enableIntelVp8Encoder */true,  /* enableH264HighProfile */true);
        DefaultVideoDecoderFactory defaultVideoDecoderFactory = new DefaultVideoDecoderFactory(rootEglBase.getEglBaseContext());
        peerConnectionFactory = PeerConnectionFactory.builder()
                .setOptions(options)
                .setVideoEncoderFactory(defaultVideoEncoderFactory)
                .setVideoDecoderFactory(defaultVideoDecoderFactory)
                .createPeerConnectionFactory();
    }

    /**
     * Creating the local peerconnection instance
     */
    private void createPeerConnection() {
        List<PeerConnection.IceServer> stunICEServers = new ArrayList<>();
        PeerConnection.IceServer stunServerList = PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer();
        stunICEServers.add(stunServerList);

        PeerConnection.RTCConfiguration rtcConfig = new PeerConnection.RTCConfiguration(stunICEServers);//peerIceServers);
        //PeerConnection.RTCConfiguration rtcConfig = new PeerConnection.RTCConfiguration(peerIceServers);
        // TCP candidates are only useful when connecting to a server that supports
        // ICE-TCP.
        rtcConfig.tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.DISABLED;
        rtcConfig.bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE;
        rtcConfig.rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE;
        rtcConfig.continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY;
        // Use ECDSA encryption.
        rtcConfig.keyType = PeerConnection.KeyType.ECDSA;

        localPeer = peerConnectionFactory.createPeerConnection(rtcConfig, new SimplePeerConnectionObserver() {
            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                super.onIceCandidate(iceCandidate);
                Log.d(TAG, "Received ice candidates");
                onIceCandidateReceived(iceCandidate);
            }
        });

        addStreamToLocalPeer();
        SignallingClient.getInstance().isChannelReady = true;
    }

    /**
     * Received local ice candidate. Send it to remote peer through signalling for negotiation
     */
    public void onIceCandidateReceived(IceCandidate iceCandidate) {
        //we have received ice candidate. We can set it to the other peer.
        SignallingClient.getInstance().emitIceCandidate(iceCandidate);
    }

    /**
     * Adding the stream to the local peer
     */
    private void addStreamToLocalPeer() {
        //creating local media stream
        MediaStream stream = peerConnectionFactory.createLocalMediaStream("102");
        stream.addTrack(localAudioTrack);
        stream.addTrack(localVideoTrack);
        localPeer.addStream(stream);
    }

    @Override
    public void onStartResponse(String answer) {
        showToast("Received start response from media server");
        Log.d(TAG, "Received sdp answer!");
        localPeer.setRemoteDescription(new SimpleSdpObserver(),
                new SessionDescription(SessionDescription.Type.ANSWER, answer));
    }

    @Override
    public void onIceCandidate(JsonObject data) {
        showToast("Receiving ice candidates");
        Log.d(TAG, "OnIceCandidate Rec");
        localPeer.addIceCandidate(new IceCandidate(data.get("sdpMid").getAsString(), data.get("sdpMLineIndex").getAsInt(), data.get("candidate").getAsString()));
    }

    public void showToast(final String message) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private VideoCapturer createCameraCapturer(CameraEnumerator enumerator) {
        final String[] deviceNames = enumerator.getDeviceNames();

        // First, try to find front facing camera
        Log.d(TAG, "Looking for front facing cameras.");
        for (String deviceName : deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                Logging.d(TAG, "Creating front facing camera capturer.");
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        // Front facing camera not found, try something else
        Log.d(TAG, "Looking for other cameras.");
        for (String deviceName : deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                Log.d(TAG, "Creating other camera capturer.");
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        return null;
    }
}

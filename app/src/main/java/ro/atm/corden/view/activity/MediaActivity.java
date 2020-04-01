package ro.atm.corden.view.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.gson.JsonObject;

import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.util.ArrayList;
import java.util.List;

import ro.atm.corden.R;
import ro.atm.corden.databinding.ActivityMediaBinding;
import ro.atm.corden.util.constant.JsonConstants;
import ro.atm.corden.util.exception.websocket.UserNotLoggedInException;
import ro.atm.corden.util.webrtc.observer.SimplePeerConnectionObserver;
import ro.atm.corden.util.webrtc.observer.SimpleSdpObserver;
import ro.atm.corden.util.websocket.SignallingClient;
import ro.atm.corden.util.websocket.callback.MediaListener;

public class MediaActivity extends AppCompatActivity implements MediaListener.LivePlayListener {
    private static final String TAG = "MediaActivity";
    private ActivityMediaBinding binding;

    private PeerConnectionFactory peerConnectionFactory;
    private MediaConstraints audioConstraints;
    private MediaConstraints videoConstraints;
    private MediaConstraints sdpConstraints;
    private VideoSource videoSource;
    private VideoTrack localVideoTrack;
    private AudioSource audioSource;
    private AudioTrack localAudioTrack;
    private SurfaceTextureHelper surfaceTextureHelper;

    private SurfaceViewRenderer remoteVideoView;
    private PeerConnection localPeer;
    private EglBase rootEglBase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_media);

        remoteVideoView = binding.remoteView;
        SignallingClient.getInstance().isInitiator = true;
        start();
    }

    private void start() {
        // keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        try {
            SignallingClient.getInstance().subscribeLiveVideoListener(this);
            initVideos();
            createPeerConnectionFactory();
            createPeerConnection();

            if (SignallingClient.getInstance().isChannelReady) {
                onTryToStart();
            }
        } catch (UserNotLoggedInException e) {
            e.printStackTrace();
        }
    }

    private void onTryToStart() {
        if (!SignallingClient.getInstance().isStarted  && SignallingClient.getInstance().isChannelReady) {
            SignallingClient.getInstance().isStarted = true;
            doCall();
        }
    }


    private void initVideos() {
        rootEglBase = EglBase.create();
        remoteVideoView.init(rootEglBase.getEglBaseContext(), null);
        remoteVideoView.setZOrderMediaOverlay(true);
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
                Log.d(TAG, "Received ice candidate");
                super.onIceCandidate(iceCandidate);
                onIceCandidateReceived(iceCandidate);
            }

            @Override
            public void onAddStream(MediaStream mediaStream) {
                showToast("Received Remote stream");
                Log.d(TAG, "Received media stream");
                super.onAddStream(mediaStream);
                gotRemoteStream(mediaStream);
            }
        });

        SignallingClient.getInstance().isChannelReady = true;
    }

    /**
     * Received remote peer's media stream. We will get the first video track and render it
     */
    private void gotRemoteStream(MediaStream stream) {
        //we have remote video stream. add to the renderer.
        final VideoTrack videoTrack = stream.videoTracks.get(0);
        runOnUiThread(() -> {
            try {
                remoteVideoView.setVisibility(View.VISIBLE);
                videoTrack.addSink(remoteVideoView);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Received local ice candidate. Send it to remote peer through signalling for negotiation
     */
    private void onIceCandidateReceived(IceCandidate iceCandidate) {
        //we have received ice candidate. We can set it to the other peer.
        SignallingClient.getInstance().emitIceCandidate(iceCandidate, JsonConstants.ICE_FOR_LIVE);
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

                CallAsyncTask callAsyncTask = new CallAsyncTask();
                callAsyncTask.from = "voicu.petre";
                callAsyncTask.sdpOffer = sessionDescription;

                callAsyncTask.execute();
            }
        }, sdpConstraints);
    }

    public void showToast(final String msg) {
        runOnUiThread(() -> Toast.makeText(MediaActivity.this, msg, Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onLiveResponse(String answer) {
        showToast("Received sdpAnswer on play");
        Log.d(TAG, "Received sdp answer");
        localPeer.setRemoteDescription(new SimpleSdpObserver(),
                new SessionDescription(SessionDescription.Type.ANSWER, answer));
    }

    @Override
    public void onIceCandidate(JsonObject data) {
        showToast("Receiving ice candidates");
        Log.d(TAG, "OnIceCandidate Rec");
        localPeer.addIceCandidate(new IceCandidate(data.get("sdpMid").getAsString(), data.get("sdpMLineIndex").getAsInt(), data.get("candidate").getAsString()));
    }

    private static final class CallAsyncTask extends AsyncTask<Void, Void, Void> {
        private String from;
        private SessionDescription sdpOffer;

        @Override
        protected Void doInBackground(Void... voids) {
            SignallingClient.getInstance().sendVideoLiveRequest(from, sdpOffer);
            return null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SignallingClient.getInstance().unsubscribeLiveVideoListener();
    }
}

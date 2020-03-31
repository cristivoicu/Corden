package ro.atm.corden.view.Fragment;

import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.Toast;

import com.google.gson.JsonObject;

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
import org.webrtc.VideoTrack;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import ro.atm.corden.R;
import ro.atm.corden.databinding.FragmentPlayerBinding;
import ro.atm.corden.model.transport_model.Video;
import ro.atm.corden.model.transport_model.VideoInfo;
import ro.atm.corden.util.constant.ExtraConstant;
import ro.atm.corden.util.constant.JsonConstants;
import ro.atm.corden.util.exception.websocket.UserNotLoggedInException;
import ro.atm.corden.util.webrtc.SimplePeerConnectionObserver;
import ro.atm.corden.util.webrtc.SimpleSdpObserver;
import ro.atm.corden.util.websocket.SignallingClient;
import ro.atm.corden.util.websocket.callback.MediaListener;

public class PlayerFragment extends Fragment implements MediaListener.PlaybackListener {
    private static final String TAG = "PlayerFragment";
    private boolean isPause = false;
    private boolean isResume = false;
    private int maxPosition = -1;

    private SeekBar seekBar = null;

    private FragmentPlayerBinding binding;

    private PeerConnectionFactory peerConnectionFactory;
    private PeerConnection localPeer;
    private EglBase rootEglBase;

    private String videoPath;

    public PlayerFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_player, container, false);

        seekBar = binding.seekBar;
        binding.play.setOnClickListener(this::onPlayButtonClicked);
        binding.seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            private int progress = -1;
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                Log.d("SEEKBAR", "On progress changed" + progress + "  " + fromUser);
                if(fromUser)
                    this.progress = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                Log.d("SEEKBAR", "onStartTrackongTouch");
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                DoSeekRequestAsyncTask doSeekRequestAsyncTask = new DoSeekRequestAsyncTask();
                if(progress != -1){
                    doSeekRequestAsyncTask.execute(progress);
                }
            }
        });

        Video video = (Video) getArguments().get(ExtraConstant.GET_VIDEO);
        videoPath = video.getName();

        SignallingClient.getInstance().isInitiator = true;

        start();

        return binding.getRoot();
    }

    private void start() {
        // keep screen on
        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        try {
            initVideos();
            SignallingClient.getInstance().subscribePlaybackVideoListener(this); //because it is null
            createPeerConnectionFactory();
            createPeerConnection();
            final GetPositionAsyncTask getPositionAsyncTask = new GetPositionAsyncTask();
            new Timer().scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    SignallingClient.getInstance().sendGetVideoPositionRequest();
                }
            }, 0, 1000);

        } catch (UserNotLoggedInException e) {
            e.printStackTrace(); // should not enter here
        }
    }

    private void initVideos() {
        rootEglBase = EglBase.create();
        binding.remoteView.init(rootEglBase.getEglBaseContext(), null);
        binding.remoteView.setZOrderMediaOverlay(true);
    }

    private void createPeerConnectionFactory() {
        //Initialize PeerConnectionFactory globals.
        PeerConnectionFactory.InitializationOptions initializationOptions =
                PeerConnectionFactory.InitializationOptions.builder(this.getContext())
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

        PeerConnection.RTCConfiguration rtcConfig = new PeerConnection.RTCConfiguration(stunICEServers);
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
    }

    /**
     * Received remote peer's media stream. we will get the first video track and render it
     */
    private void gotRemoteStream(MediaStream stream) {
        //we have remote video stream. add to the renderer.
        final VideoTrack videoTrack = stream.videoTracks.get(0);
        final AudioTrack audioTrack = stream.audioTracks.get(0);
        getActivity().runOnUiThread(() -> {
            try {
                binding.remoteView.setVisibility(View.VISIBLE);
                videoTrack.addSink(binding.remoteView);

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
        SignallingClient.getInstance().emitIceCandidate(iceCandidate, JsonConstants.ICE_FOR_PLAY);
    }

    private void onPlayButtonClicked(View view) {
        if (isResume) {
            isResume = false;
            binding.play.setImageResource(R.drawable.ic_pause);
            ResumeRequestAsyncTask resumeRequestAsyncTask = new ResumeRequestAsyncTask();
            resumeRequestAsyncTask.execute();
            return;
        }
        if (isPause) {
            isResume = true;
            binding.play.setImageResource(R.drawable.ic_play_arrow_white_24dp);
            onPauseButtonClicked();
            return;
        }
        isPause = true;
        binding.play.setImageResource(R.drawable.ic_pause);

        SignallingClient.getInstance().isInitiator = true;
        SignallingClient.getInstance().isChannelReady = true;

        MediaConstraints sdpConstraints = new MediaConstraints();
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

                SignallingClient.getInstance().sendPlayVideoRequest(sessionDescription, videoPath);
            }

            @Override
            public void onCreateFailure(String s) {
                super.onCreateFailure(s);
                Log.e(TAG, "Failed to create a sdp offer");
            }
        }, sdpConstraints);
    }

    private void onPauseButtonClicked() {
        PauseRequestAsyncTask privateRequestAsyncTask = new PauseRequestAsyncTask();
        privateRequestAsyncTask.execute();
    }

    private void showToast(final String msg) {
        getActivity().runOnUiThread(() -> Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onIceCandidate(JsonObject data) {
        showToast("Receiving ice candidates");
        Log.d(TAG, "OnIceCandidate Rec");
        localPeer.addIceCandidate(new IceCandidate(data.get("sdpMid").getAsString(), data.get("sdpMLineIndex").getAsInt(), data.get("candidate").getAsString()));
    }

    @Override
    public void onPlayResponse(String answer) {
        showToast("Received answer: caller");
        Log.d(TAG, "Received sdp answer! caller");
        localPeer.setRemoteDescription(new SimpleSdpObserver(),
                new SessionDescription(SessionDescription.Type.ANSWER, answer));
    }

    @Override
    public void onVideoInfo(VideoInfo videoInfo) {
        Log.d(TAG, "Received video info" + videoInfo.toString());
        if (seekBar != null) {
            seekBar.setMax((int)videoInfo.getSeekableEnd());
            //maxPosition = (int)videoInfo.getSeekableEnd();
            if (videoInfo.getIsSeekable()) {
                seekBar.setEnabled(true);
            }
        }
    }

    @Override
    public void onGotPosition(long position) {
        Log.d(TAG, "God position: " + position);
        if (seekBar != null) {
            seekBar.setProgress((int) position);
        }
    }

    private static class PauseRequestAsyncTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            SignallingClient.getInstance().sendPauseVideoRequest();
            return null;
        }
    }

    private static class ResumeRequestAsyncTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            SignallingClient.getInstance().sendResumeVideoRequest();
            return null;
        }
    }

    private static class DoSeekRequestAsyncTask extends AsyncTask<Integer, Void, Void> {

        @Override
        protected Void doInBackground(Integer... integers) {
            SignallingClient.getInstance().sendSeekVideoRequest(integers[0]);
            return null;
        }
    }

    private static class GetPositionAsyncTask extends AsyncTask<Void, Void, Void>{

        @Override
        protected Void doInBackground(Void... voids) {

            return null;
        }
    }
}

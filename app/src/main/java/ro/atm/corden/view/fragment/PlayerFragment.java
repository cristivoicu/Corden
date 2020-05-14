package ro.atm.corden.view.fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.media.AudioManager;
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
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaStream;
import org.webrtc.RendererCommon;
import org.webrtc.VideoTrack;

import java.util.Timer;
import java.util.TimerTask;

import ro.atm.corden.R;
import ro.atm.corden.databinding.FragmentPlayerBinding;
import ro.atm.corden.model.video.Video;
import ro.atm.corden.model.video.VideoInfo;
import ro.atm.corden.util.constant.AppConstants;
import ro.atm.corden.util.exception.websocket.UserNotLoggedInException;
import ro.atm.corden.util.webrtc.client.Session;
import ro.atm.corden.util.webrtc.interfaces.MediaActivity;
import ro.atm.corden.util.websocket.SignallingClient;
import ro.atm.corden.util.websocket.callback.MediaListener;

public class PlayerFragment extends Fragment implements MediaListener.PlaybackListener, MediaActivity {
    private static final String TAG = "PlayerFragment";

    private boolean isPause = false;
    private boolean isResume = false;
    private boolean isEnded = false;
    private Timer timer;

    private Session playbackSession;
    private SeekBar seekBar = null;
    private FragmentPlayerBinding binding;
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

        AudioManager audioManager = (AudioManager) this.getActivity().getSystemService(Context.AUDIO_SERVICE);
        audioManager.setSpeakerphoneOn(true);
        audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);

        seekBar = binding.seekBar;
        binding.play.setOnClickListener(this::onPlayButtonClicked);
        binding.seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            private int progress = -1;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser)
                    this.progress = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                DoSeekRequestAsyncTask doSeekRequestAsyncTask = new DoSeekRequestAsyncTask();
                if (progress != -1) {
                    doSeekRequestAsyncTask.execute(progress);
                }
            }
        });

        Video video = (Video) getArguments().get(AppConstants.GET_VIDEO);
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
            playbackSession = new Session(getContext(), rootEglBase, this);
            playbackSession.createPlaybackClient();

        } catch (UserNotLoggedInException e) {
            e.printStackTrace(); // should not enter here
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        playbackSession.leavePlaybackSession();
    }

    private void initVideos() {
        rootEglBase = EglBase.create();
        binding.remoteView.init(rootEglBase.getEglBaseContext(), null);
        binding.remoteView.setZOrderMediaOverlay(true);
        binding.remoteView.setEnableHardwareScaler(true);
        binding.remoteView.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
    }


    /**
     * Received remote peer's media stream. we will get the first video track and render it
     */
    @Override
    public void gotRemoteStream(MediaStream stream) {
        //we have remote video stream. add to the renderer.
        final VideoTrack videoTrack = stream.videoTracks.get(0);
        final AudioTrack audioTrack = stream.audioTracks.get(0);
        audioTrack.setEnabled(true);
        audioTrack.setVolume(100);
        getActivity().runOnUiThread(() -> {
            try {
                binding.remoteView.setVisibility(View.VISIBLE);
                videoTrack.addSink(binding.remoteView);
                audioTrack.setEnabled(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }


    @Override
    public void showToast(final String msg) {
        getActivity().runOnUiThread(() -> Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show());
    }

    private void onPlayButtonClicked(View view) {
        if (isResume) {
            isResume = false;
            timer = new Timer();
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    SignallingClient.getInstance().sendGetVideoPositionRequest();
                }
            }, 0, 1000);
            binding.play.setImageResource(R.drawable.ic_pause);
            ResumeRequestAsyncTask resumeRequestAsyncTask = new ResumeRequestAsyncTask();
            resumeRequestAsyncTask.execute();
            return;
        }
        if (isPause) {
            isResume = true;
            timer.cancel();
            timer.purge();
            binding.play.setImageResource(R.drawable.ic_play_arrow_white_24dp);
            onPauseButtonClicked();
            return;
        }
        if(isEnded) {
            isEnded = false;
            playbackSession = new Session(getContext(), rootEglBase, this);
            playbackSession.createPlaybackClient();
        }
        isPause = true;
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                SignallingClient.getInstance().sendGetVideoPositionRequest();
            }
        }, 0, 1000);
        binding.play.setImageResource(R.drawable.ic_pause);

        SignallingClient.getInstance().isInitiator = true;
        SignallingClient.getInstance().isChannelReady = true;

        playbackSession.createPlaybackOffer(videoPath);
    }

    private void onPauseButtonClicked() {
        PauseRequestAsyncTask privateRequestAsyncTask = new PauseRequestAsyncTask();
        privateRequestAsyncTask.execute();
    }


    @Override
    public void onIceCandidate(JsonObject data) {
        showToast("Receiving ice candidates");
        Log.d(TAG, "OnIceCandidate Rec" + data.toString());
        //localPeer.addIceCandidate(new IceCandidate(data.get("sdpMid").getAsString(), data.get("sdpMLineIndex").getAsInt(), data.get("candidate").getAsString()));

        playbackSession.addIceCandidate(new IceCandidate(data.get("sdpMid").getAsString(), data.get("sdpMLineIndex").getAsInt(), data.get("candidate").getAsString()));
    }

    @Override
    public void onPlayResponse(String answer) {
        showToast("Received answer: caller");
        Log.d(TAG, "Received sdp answer! caller");
        /*localPeer.setRemoteDescription(new SimpleSdpObserver(),
                new SessionDescription(SessionDescription.Type.ANSWER, answer));*/
        playbackSession.setRemoteResponse(answer);
    }

    @Override
    public void onVideoInfo(VideoInfo videoInfo) {
        Log.d(TAG, "Received video info" + videoInfo.toString());
        if (seekBar != null) {
            seekBar.setMax((int) videoInfo.getSeekableEnd());
            //maxPosition = (int)videoInfo.getSeekableEnd();
            if (videoInfo.getIsSeekable()) {
                seekBar.setEnabled(true);
            }
        }
    }

    @Override
    public void onGotPosition(long position) {
        Log.d(TAG, "Got position: " + position);
        if (seekBar != null) {
            seekBar.setProgress((int) position);
        }
    }

    @Override
    public void onPlayEnd() {
        Log.d(TAG, "Received end of playback");
        isEnded = true;
        isPause = isResume = false;
        timer.cancel();
        timer.purge();
        seekBar.setProgress(0);
        binding.play.setImageResource(R.drawable.ic_play_arrow_white_24dp);
        playbackSession.leaveLiveSession();
    }

    @Override
    public void onPlaybackRejected() {
        AlertDialog alertDialog = new AlertDialog.Builder(this.getContext())
                .setMessage("You are not authorised to play this video!")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("Ok", null)
                .create();
        alertDialog.show();
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
}

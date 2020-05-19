package ro.atm.corden.view.fragment;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;

import com.google.gson.JsonObject;

import org.webrtc.AudioTrack;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaStream;
import org.webrtc.RendererCommon;
import org.webrtc.VideoTrack;

import ro.atm.corden.R;
import ro.atm.corden.databinding.FragmentLiveWatchBinding;
import ro.atm.corden.util.exception.websocket.UserNotLoggedInException;
import ro.atm.corden.util.webrtc.client.Session;
import ro.atm.corden.util.webrtc.interfaces.MediaActivity;
import ro.atm.corden.util.websocket.SignallingClient;
import ro.atm.corden.util.websocket.callback.MediaListener;

public class LiveWatchFragment extends Fragment
        implements MediaActivity,
        MediaListener.LiveStreamingListener {
    private static final String TAG = "LiveWatchFragment";

    private FragmentLiveWatchBinding mBinding;

    private Session mWatchSession;
    private EglBase mEglBase;
    private String username;

    public LiveWatchFragment() {

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(inflater,
                R.layout.fragment_live_watch,
                container,
                false);

        start();
        mWatchSession.createWatchOffer(username);
        return mBinding.getRoot();
    }



    private void start() {
        try {
            SignallingClient.getInstance().subscribeLiveVideoListener(this);
            initVideos();
            mWatchSession = new Session(this.getActivity().getBaseContext(), mEglBase, this);
            mWatchSession.createLiveWatcherClient();
        } catch (UserNotLoggedInException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        mWatchSession.leaveLiveWatchSession();
    }

    private void initVideos() {
        mEglBase = EglBase.create();
        mBinding.localView.init(mEglBase.getEglBaseContext(), null);
        mBinding.localView.setZOrderMediaOverlay(true);
        mBinding.localView.setEnableHardwareScaler(true);
        mBinding.localView.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
    }

    @Override
    public void gotRemoteStream(MediaStream mediaStream) {
        this.getActivity().runOnUiThread(() -> {
            try {
                VideoTrack videoTrack = mediaStream.videoTracks.get(0);
                AudioTrack audioTrack = mediaStream.audioTracks.get(0);
                videoTrack.addSink(mBinding.localView);
                audioTrack.setEnabled(true);
                audioTrack.setVolume(1000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void showToast(String message) {
        this.getActivity().runOnUiThread(() -> {
            Toast.makeText(this.getActivity(), message, Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onLiveResponse(String answer) {
        showToast("Received answer: watch!");
        Log.d(TAG, "Received answer: " + answer);
        mWatchSession.setRemoteResponse(answer);
    }

    @Override
    public void onIceCandidate(JsonObject data) {
        Log.d(TAG, "Received ice candidate");
        mWatchSession.addIceCandidate(
                new IceCandidate(data.get("sdpMid").getAsString(),
                        data.get("sdpMLineIndex").getAsInt(),
                        data.get("candidate").getAsString()));
    }

    @Override
    public void onLiveStreamingError() {
        AlertDialog alertDialog = new AlertDialog.Builder(this.getActivity().getBaseContext())
                .setMessage("Live watching can't be accomplished")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .create();
        alertDialog.show();
    }

    public void setUsername(String username) {
        this.username = username;
    }
}

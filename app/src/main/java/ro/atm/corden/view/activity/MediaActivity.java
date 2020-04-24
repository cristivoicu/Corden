package ro.atm.corden.view.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.gson.JsonObject;

import org.webrtc.AudioTrack;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaStream;
import org.webrtc.RendererCommon;
import org.webrtc.VideoTrack;

import ro.atm.corden.R;
import ro.atm.corden.databinding.ActivityMediaBinding;
import ro.atm.corden.util.exception.websocket.UserNotLoggedInException;
import ro.atm.corden.util.webrtc.client.Session;
import ro.atm.corden.util.websocket.SignallingClient;
import ro.atm.corden.util.websocket.callback.MediaListener;

public class MediaActivity extends AppCompatActivity
        implements ro.atm.corden.util.webrtc.interfaces.MediaActivity,
        MediaListener.LiveStreamingListener {
    private static final String TAG = "MediaActivity";
    private ActivityMediaBinding binding;

    private Session watchSession;
    private EglBase eglBase;
    private String username = "voicu.cristian";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_media);

        SignallingClient.getInstance().isInitiator = true;

        start();
    }

    private void start() {
        try {
            SignallingClient.getInstance().subscribeLiveVideoListener(this);
            initVideos();
            watchSession = new Session(getBaseContext(), eglBase, this);
            watchSession.createLiveWatcherClient();
        } catch (UserNotLoggedInException e) {
            e.printStackTrace();
        }
    }

    private void initVideos() {
        eglBase = EglBase.create();
        binding.remoteView.init(eglBase.getEglBaseContext(), null);
        binding.remoteView.setZOrderMediaOverlay(true);
        binding.remoteView.setEnableHardwareScaler(true);
        binding.remoteView.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
    }


    @Override
    public void gotRemoteStream(MediaStream mediaStream) {
        runOnUiThread(() -> {
            try {
                VideoTrack videoTrack = mediaStream.videoTracks.get(0);
                AudioTrack audioTrack = mediaStream.audioTracks.get(0);
                videoTrack.addSink(binding.remoteView);
                audioTrack.setEnabled(true);
                audioTrack.setVolume(1000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void showToast(String message) {
        runOnUiThread(() -> {
            Toast.makeText(MediaActivity.this, message, Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onLiveResponse(String answer) {
        showToast("Received answer: watch!");
        Log.d(TAG, "Received answer: " + answer);
        watchSession.setRemoteResponse(answer);
    }

    @Override
    public void onIceCandidate(JsonObject data) {
        Log.d(TAG, "Received ice candidate");
        watchSession.addIceCandidate(
                new IceCandidate(data.get("sdpMid").getAsString(),
                        data.get("sdpMLineIndex").getAsInt(),
                        data.get("candidate").getAsString()));
    }

    @Override
    public void onLiveStreamingError() {
        AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setMessage("Live watching can't be accomplished")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .create();
        alertDialog.show();
    }

    public void onPlayButtonClicked(View view) {
        binding.play.setVisibility(View.INVISIBLE);

        watchSession.createWatchOffer(username);
    }
}

package ro.atm.corden.view.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
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
import ro.atm.corden.view.fragment.LiveStreamersListFragment;
import ro.atm.corden.view.fragment.LiveWatchFragment;

public class MediaActivity extends AppCompatActivity {
    private static final String TAG = "MediaActivity";
    private ActivityMediaBinding binding;
    private FragmentManager fragmentManager = getSupportFragmentManager();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_media);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);


        if(binding.frameLayout != null){
            if(savedInstanceState != null)
                return;

            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            LiveStreamersListFragment liveWatchFragment = new LiveStreamersListFragment();
            fragmentTransaction.add(R.id.frameLayout, liveWatchFragment, null);
            fragmentTransaction.commit();
        }
    }


}

package ro.atm.corden.view.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import org.slf4j.Logger;
import org.webrtc.CameraVideoCapturer;
import org.webrtc.EglBase;

import ro.atm.corden.R;
import ro.atm.corden.databinding.ActivityMainUserBinding;
import ro.atm.corden.model.user.LoginUser;
import ro.atm.corden.util.services.StreamingIntentService;
import ro.atm.corden.util.websocket.SignallingClient;

public class MainActivityUser extends AppCompatActivity {
    private ActivityMainUserBinding binding;

    private boolean isServiceStarted = false;
    private static final String ACTION_STREAM = "ActionStream";

    private StreamingIntentService mService;
    private boolean mBound = false;

    private boolean isShowVideoPressed = false;

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // TODO Auto-generated method stub
            StreamingIntentService.LocalBinder binder = (StreamingIntentService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            // TODO Auto-generated method stub
            mBound = false;

        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main_user);
        binding.frontCamera.setSelected(true);
        binding.showStream.setOnTouchListener((v, event) ->
        {
            Log.d("T", "1");
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                isShowVideoPressed = !isShowVideoPressed;
                Log.d("T", "2  " + isShowVideoPressed);
                binding.showStream.setSelected(isShowVideoPressed);
                binding.showStream.setPressed(isShowVideoPressed);
                if (isServiceStarted) {
                    if (isShowVideoPressed) {
                        mService.showVideo(binding.localView);
                        binding.localView.setVisibility(View.VISIBLE);
                    } else {
                        mService.hideVideo(binding.localView);
                        binding.localView.setVisibility(View.INVISIBLE);
                    }
                    return true;
                }
            }
            return false;
        });

        binding.frontCamera.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (!binding.frontCamera.isSelected()) {
                    binding.frontCamera.setSelected(true);
                    binding.backCamera.setSelected(false);
                    binding.externalCamera.setSelected(false);
                    if (isServiceStarted) {
                       mService.switchCamera(binding.localView, true);
                    }
                }
                return true;
            }
            return false;
        });
        binding.backCamera.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (!binding.backCamera.isSelected()) {
                    binding.frontCamera.setSelected(false);
                    binding.backCamera.setSelected(true);
                    binding.externalCamera.setSelected(false);
                    if (isServiceStarted) {
                        /*CameraVideoCapturer cameraVideoCapturer = (CameraVideoCapturer) mService.getLiveSession().getVideoCapturer();
                        cameraVideoCapturer.switchCamera(null);*/
                        mService.switchCamera(binding.localView, false);
                    }
                }
                return true;
            }
            return false;
        });
        binding.externalCamera.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (!binding.externalCamera.isSelected()) {
                    binding.frontCamera.setSelected(false);
                    binding.backCamera.setSelected(false);
                    binding.externalCamera.setSelected(true);
                }
                return true;
            }
            return false;
        });

    }

    public void startService(View view) {
        Intent serviceIntent = new Intent(this, StreamingIntentService.class);
        serviceIntent.setAction(ACTION_STREAM);

        if (!isServiceStarted) {
            ContextCompat.startForegroundService(this, serviceIntent);
            bindService(serviceIntent, mConnection, BIND_AUTO_CREATE);
            mBound = true;
            isServiceStarted = true;
        } else {
            stopService(serviceIntent);
            if (mBound) {
                mBound = false;
                unbindService(mConnection);
            }
            StopServiceAsyncTask stopServiceAsyncTask = new StopServiceAsyncTask();
            stopServiceAsyncTask.execute();
            isServiceStarted = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Intent serviceIntent = new Intent(this, StreamingIntentService.class);
        if (isServiceStarted) {
            stopService(serviceIntent);
            if (mBound) {
                mBound = false;
                unbindService(mConnection);
            }
            StopServiceAsyncTask stopServiceAsyncTask = new StopServiceAsyncTask();
            stopServiceAsyncTask.execute();
        }
    }

    public void onChangeCameraClicked(View view) {
        if (isServiceStarted) {
            CameraVideoCapturer cameraVideoCapturer = (CameraVideoCapturer) mService.getLiveSession().getVideoCapturer();
            cameraVideoCapturer.switchCamera(null);
        }
    }


    private static class StopServiceAsyncTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            SignallingClient.getInstance().stopVideoRecording(LoginUser.username);
            return null;
        }
    }
}

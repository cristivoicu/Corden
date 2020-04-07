package ro.atm.corden.view.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;

import org.webrtc.CameraVideoCapturer;

import ro.atm.corden.R;
import ro.atm.corden.model.LoginUser;
import ro.atm.corden.util.services.StreamingIntentService;
import ro.atm.corden.util.websocket.SignallingClient;

public class MainActivityUser extends AppCompatActivity {
    private boolean isServiceStarted = false;
    private static final String ACTION_STREAM = "ActionStream";

    private StreamingIntentService mService;
    private boolean mBound = false;

    private ServiceConnection mConnection = new ServiceConnection(){


        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // TODO Auto-generated method stub
            StreamingIntentService.LocalBinder binder = (StreamingIntentService.LocalBinder)service;
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
        setContentView(R.layout.activity_main_user);
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
            if(mBound){
                mBound = false;
                unbindService(mConnection);
            }
            StopServiceAsyncTask stopServiceAsyncTask = new StopServiceAsyncTask();
            stopServiceAsyncTask.execute();
            stopServiceAsyncTask = null;
            isServiceStarted = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Intent serviceIntent = new Intent(this, StreamingIntentService.class);
        if (isServiceStarted) {
            stopService(serviceIntent);
            if(mBound){
                mBound = false;
                unbindService(mConnection);
            }
            StopServiceAsyncTask stopServiceAsyncTask = new StopServiceAsyncTask();
            stopServiceAsyncTask.execute();
            stopServiceAsyncTask = null;
        }
    }

    public void onChangeCameraClicked(View view) {
        if(isServiceStarted){
            CameraVideoCapturer cameraVideoCapturer = (CameraVideoCapturer)mService.getLiveSession().getVideoCapturer();
            cameraVideoCapturer.switchCamera(null);
        }
    }

    private static class StopServiceAsyncTask extends AsyncTask<Void, Void, Void>{
        @Override
        protected Void doInBackground(Void... voids) {
            SignallingClient.getInstance().stopVideoRecording(LoginUser.username);
            return null;
        }
    }
}

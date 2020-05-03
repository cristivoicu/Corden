package ro.atm.corden.view.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;

import org.webrtc.CameraVideoCapturer;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;
import ro.atm.corden.R;
import ro.atm.corden.databinding.ActivityMainUserBinding;
import ro.atm.corden.model.user.LoginUser;
import ro.atm.corden.util.constant.AppConstants;
import ro.atm.corden.util.services.DetectedActivitiesService;
import ro.atm.corden.util.services.LocationService;
import ro.atm.corden.util.services.StreamingIntentService;
import ro.atm.corden.util.webrtc.client.CameraSelector;
import ro.atm.corden.util.websocket.SignallingClient;

public class MainActivityUser extends AppCompatActivity implements EasyPermissions.PermissionCallbacks {
    private ActivityMainUserBinding binding;
    private static final String ACTION_STREAM = "ActionStream";
    private StreamingIntentService mService;
    private boolean mBound = false;
    private boolean isShowVideoPressed = false;
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            StreamingIntentService.LocalBinder binder = (StreamingIntentService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBound = false;
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main_user);
        binding.setLifecycleOwner(this);

        binding.frontCamera.setSelected(true);
        binding.showStream.setOnTouchListener((v, event) ->
        {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                isShowVideoPressed = !isShowVideoPressed;
                binding.showStream.setSelected(isShowVideoPressed);
                binding.showStream.setPressed(isShowVideoPressed);
                if (StreamingIntentService.isRunning()) {
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
                    if (StreamingIntentService.isRunning()) {
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
                    if (StreamingIntentService.isRunning()) {
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
        Intent serviceIntent = new Intent(this, LocationService.class);
        ContextCompat.startForegroundService(this, serviceIntent);
        if (StreamingIntentService.isRunning() && !mBound) {
            bindService(serviceIntent, mConnection, BIND_AUTO_CREATE);
            mBound = true;
        }

        setSupportActionBar(binding.toolbar);

        Intent intent = new Intent(this, DetectedActivitiesService.class);
        startService(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.admin_main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.itemLogout:
                SignallingClient.getInstance().logout();
                // stopping services
                stopService(new Intent(MainActivityUser.this, LocationService.class));
                stopService(new Intent(this, DetectedActivitiesService.class));
                if(StreamingIntentService.isRunning()){
                    stopService(new Intent(MainActivityUser.this, StreamingIntentService.class));
                }
                finish();
                break;
            case R.id.itemMyAccount:
                Intent intent = new Intent(this, EditUserAccountActivity.class);
                startActivity(intent);
                break;
            case R.id.itemSetting:
                intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                break;
        }
        return true;
    }

    @AfterPermissionGranted(AppConstants.WEBRTC_REQUEST_CODE)
    private void checkPermission() {
        if (!EasyPermissions.hasPermissions(this, AppConstants.webRtcPermissions)) {
            EasyPermissions.requestPermissions(this,
                    "You need to allow those permissions for the app to run correctly",
                    AppConstants.WEBRTC_REQUEST_CODE,
                    AppConstants.webRtcPermissions);
        }
    }

    /***/
    private CameraSelector.CameraType determineCamera(){
        if(binding.frontCamera.isSelected() || binding.backCamera.isSelected()){
            binding.externalCamera.setEnabled(false);
            if(binding.frontCamera.isSelected()){
                return CameraSelector.CameraType.FRONT;
            }
            if(binding.backCamera.isSelected()){
                return CameraSelector.CameraType.BACK;
            }

        }
        if(binding.externalCamera.isSelected()){
            binding.frontCamera.setEnabled(false);
            binding.backCamera.setEnabled(false);
            return CameraSelector.CameraType.EXTERNAL;
        }
        return null;
    }

    /***/
    public void startService(View view) {
        checkPermission();
        if (EasyPermissions.hasPermissions(this, AppConstants.webRtcPermissions)) {
            Intent serviceIntent = new Intent(this.getApplicationContext(), StreamingIntentService.class);
            serviceIntent.setAction(ACTION_STREAM);
            serviceIntent.putExtra(AppConstants.EXTRA_CAMERA, determineCamera().name());
            if (StreamingIntentService.isRunning() && !mBound) {
                bindService(serviceIntent, mConnection, BIND_AUTO_CREATE);
                mBound = true;
                return;
            }

            if (!StreamingIntentService.isRunning()) {
                ContextCompat.startForegroundService(this.getApplicationContext(), serviceIntent);
                bindService(serviceIntent, mConnection, BIND_AUTO_CREATE);
                mBound = true;
            } else {
                stopService(serviceIntent);
                if (mBound) {
                    mBound = false;
                    unbindService(mConnection);
                }
                StopServiceAsyncTask stopServiceAsyncTask = new StopServiceAsyncTask();
                stopServiceAsyncTask.execute();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Intent serviceIntent = new Intent(this, StreamingIntentService.class);
        if (StreamingIntentService.isRunning()) {
            stopService(serviceIntent);
            if (mBound) {
                mBound = false;
                unbindService(mConnection);
            }
            StopServiceAsyncTask stopServiceAsyncTask = new StopServiceAsyncTask();
            stopServiceAsyncTask.execute();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {

    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
        if (EasyPermissions.somePermissionDenied(this, AppConstants.webRtcPermissions)) {
            new AppSettingsDialog.Builder(this).build().show();
        }
        if( EasyPermissions.somePermissionPermanentlyDenied(this, Arrays.asList(AppConstants.webRtcPermissions))){
            new AppSettingsDialog.Builder(this).build().show();
        }
    }


    private static class StopServiceAsyncTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            SignallingClient.getInstance().stopVideoStreaming(LoginUser.username);
            return null;
        }
    }

    @Override
    public void onBackPressed() {

    }
}

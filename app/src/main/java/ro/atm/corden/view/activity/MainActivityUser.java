package ro.atm.corden.view.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;

import ro.atm.corden.R;
import ro.atm.corden.model.LoginUser;
import ro.atm.corden.util.services.StreamingIntentService;
import ro.atm.corden.util.websocket.SignallingClient;

public class MainActivityUser extends AppCompatActivity {
    private boolean isServiceStarted = false;
    private static final String ACTION_STREAM = "ActionStream";

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
            isServiceStarted = true;
        } else {
            stopService(serviceIntent);
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
            StopServiceAsyncTask stopServiceAsyncTask = new StopServiceAsyncTask();
            stopServiceAsyncTask.execute();
            stopServiceAsyncTask = null;
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

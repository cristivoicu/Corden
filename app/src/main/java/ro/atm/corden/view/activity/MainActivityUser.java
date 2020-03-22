package ro.atm.corden.view.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import ro.atm.corden.R;
import ro.atm.corden.util.services.StreamingIntentService;

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
            isServiceStarted = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Intent serviceIntent = new Intent(this, StreamingIntentService.class);
        stopService(serviceIntent);
    }
}

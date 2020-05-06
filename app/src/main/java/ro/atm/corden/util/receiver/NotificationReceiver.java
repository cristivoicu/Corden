package ro.atm.corden.util.receiver;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import ro.atm.corden.util.services.StreamingIntentService;

public class NotificationReceiver extends BroadcastReceiver {
    private static final String ACTION_START_STREAMING = "startStreaming";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null) {
            return;
        }
        if (action.equals(ACTION_START_STREAMING)) {

            Intent serviceIntent = new Intent(context.getApplicationContext(), StreamingIntentService.class);
            serviceIntent.setAction("ActionStream");
            ContextCompat.startForegroundService(context, serviceIntent);
        }
    }
}

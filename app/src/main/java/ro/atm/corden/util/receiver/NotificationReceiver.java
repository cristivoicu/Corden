package ro.atm.corden.util.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class NotificationReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if(action == null){
            return;
        }
        if(action.equals("action")){
            Toast.makeText(context, "User pressed stop", Toast.LENGTH_LONG).show();
        }
    }
}

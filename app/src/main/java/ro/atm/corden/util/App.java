package ro.atm.corden.util;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

import ro.atm.corden.R;

public class App extends Application {
    public static final String STREAM_CHANNEL_ID = "StreamServiceChannel";
    public static final String LOCATION_CHANNEL_ID = "StreamServiceChannel";
    public static final String NOTIFICATION_CHANNEL_ID = "NotificationChannel";

    @Override
    public void onCreate() {
        super.onCreate();

        createNotificationChannel();
    }

    private void createNotificationChannel(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel streamServiceChannel = new NotificationChannel(
                    STREAM_CHANNEL_ID,
                    "Streaming service channel",
                    NotificationManager.IMPORTANCE_HIGH
            );
            NotificationChannel locationServiceChannel = new NotificationChannel(
                    LOCATION_CHANNEL_ID,
                    "Location service channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationChannel notificationServiceChannel = new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    "Push notification service channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(locationServiceChannel);
            manager.createNotificationChannel(notificationServiceChannel);
            manager.createNotificationChannel(streamServiceChannel);
        }
    }
}

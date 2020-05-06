package ro.atm.corden.util.services;

import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.tasks.Task;

public class DetectedActivitiesIntentService extends IntentService {
    private static final String TAG = DetectedActivitiesIntentService.class.getSimpleName();

    private Intent mIntentService;
    private PendingIntent mPendingIntent;
    private ActivityRecognitionClient mActivityRecognitionClient;

    IBinder mBinder = new DetectedActivitiesIntentService.LocalBinder();

    public class LocalBinder extends Binder {
        public DetectedActivitiesIntentService getServerInstance() {
            return DetectedActivitiesIntentService.this;
        }
    }

    public DetectedActivitiesIntentService()
    {
        super("DetectedActivitiesService");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        while(true){}

    }

    @Override
    public void onCreate() {
        super.onCreate();
        mActivityRecognitionClient = new ActivityRecognitionClient(this);
        mIntentService = new Intent(this, ActivityDetectorIntentService.class);
        mPendingIntent = PendingIntent.getService(this, 1, mIntentService, PendingIntent.FLAG_UPDATE_CURRENT);
        requestActivityUpdatesButtonHandler();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    public void requestActivityUpdatesButtonHandler() {
        Task<Void> task = mActivityRecognitionClient.requestActivityUpdates(
                20 * 1000,
                mPendingIntent);

        task.addOnSuccessListener(result -> Toast.makeText(getApplicationContext(),
                "Successfully requested activity updates",
                Toast.LENGTH_SHORT)
                .show());

        task.addOnFailureListener(e -> Toast.makeText(getApplicationContext(),
                "Requesting activity updates failed to start",
                Toast.LENGTH_SHORT)
                .show());
    }

    public void removeActivityUpdatesButtonHandler() {
        Task<Void> task = mActivityRecognitionClient.removeActivityUpdates(
                mPendingIntent);
        task.addOnSuccessListener(result -> Toast.makeText(getApplicationContext(),
                "Removed activity updates successfully!",
                Toast.LENGTH_SHORT)
                .show());

        task.addOnFailureListener(e -> Toast.makeText(getApplicationContext(), "Failed to remove activity updates!",
                Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        removeActivityUpdatesButtonHandler();
    }
}


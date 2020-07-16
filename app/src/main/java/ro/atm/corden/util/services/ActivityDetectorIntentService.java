package ro.atm.corden.util.services;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import java.util.ArrayList;

import ro.atm.corden.model.user.LoginUser;
import ro.atm.corden.util.websocket.SignallingClient;
import ro.atm.corden.util.websocket.protocol.events.ActivityEventType;

public class ActivityDetectorIntentService extends IntentService {
    private static final String TAG = ActivityDetectorIntentService.class.getSimpleName();

    public ActivityDetectorIntentService() {
        super("ActivityDetectorIntentService");
    }

    private String lastKnownActivity = ActivityEventType.UNKNOWN;

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (ActivityRecognitionResult.hasResult(intent)) {
            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);

            ArrayList<DetectedActivity> detectedActivities = (ArrayList<DetectedActivity>) result.getProbableActivities();

            for (DetectedActivity detectedActivity : detectedActivities) {
                Log.e(TAG, "Detected activity: " + detectedActivity.getType() + ", " + detectedActivity.getConfidence());
                broadcastDetectedActivity(detectedActivity);
            }
        }
    }

    private void broadcastDetectedActivity(DetectedActivity detectedActivity) {
        String label = ActivityEventType.UNKNOWN;
        int confidence = detectedActivity.getConfidence();

        switch (detectedActivity.getType()) {
            case DetectedActivity.IN_VEHICLE: {
                label = ActivityEventType.IN_VEHICLE;
                break;
            }
            case DetectedActivity.ON_BICYCLE: {
                label = ActivityEventType.ON_BICYCLE;
                break;
            }
            case DetectedActivity.ON_FOOT: {
                label = ActivityEventType.ON_FOOT;
                break;
            }
            case DetectedActivity.RUNNING: {
                label = ActivityEventType.RUNNING;
                break;
            }
            case DetectedActivity.STILL: {
                label = ActivityEventType.STILL;
                break;
            }
            case DetectedActivity.TILTING: {
                label = ActivityEventType.TILTING;
                break;
            }
            case DetectedActivity.WALKING: {
                label = ActivityEventType.WALKING;
                break;
            }
            case DetectedActivity.UNKNOWN: {
                label = ActivityEventType.UNKNOWN;
                break;
            }
        }
        Log.e(TAG, label + ", " + confidence);
        if(confidence > 70) {
            if(!lastKnownActivity.equals(label)) {
                lastKnownActivity = label;
                if(label.equals(ActivityEventType.RUNNING)) {
                    //motionListener.onMotion();
                    SignallingClient.getInstance().sendDetectedActivity(label, confidence);
                    Intent streamService = new Intent(this.getApplicationContext(), StreamingIntentService.class);
                    ContextCompat.startForegroundService(this.getApplicationContext(), streamService);
                }
                if(label.equals(ActivityEventType.WALKING) || label.equals(ActivityEventType.ON_FOOT)){
                    //motionListener.onMotion();
                    Intent streamService = new Intent(this.getApplicationContext(), StreamingIntentService.class);
                    streamService.setAction("ActionStream");
                    ContextCompat.startForegroundService(this.getApplicationContext(), streamService);
                }
                if(label.equals(ActivityEventType.STILL)){
                    if(lastKnownActivity.equals(ActivityEventType.RUNNING) || lastKnownActivity.equals(ActivityEventType.WALKING) || label.equals(ActivityEventType.ON_FOOT)){
                        SignallingClient.getInstance().sendDetectedActivity(label, confidence);
                        SignallingClient.getInstance().stopVideoStreaming(LoginUser.username);
                        Intent streamService = new Intent(this.getApplicationContext(), StreamingIntentService.class);
                        stopService(streamService);
                    }
                }
            }
        }
    }
}

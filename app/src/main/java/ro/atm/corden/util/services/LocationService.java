package ro.atm.corden.util.services;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.IntentService;
import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.location.LocationManagerCompat;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.JsonObject;

import java.util.List;

import ro.atm.corden.R;
import ro.atm.corden.model.user.User;
import ro.atm.corden.util.App;
import ro.atm.corden.util.websocket.SignallingClient;
import ro.atm.corden.util.websocket.protocol.Message;
import ro.atm.corden.util.websocket.protocol.events.UpdateEventType;

public class LocationService extends IntentService {
    private static final String TAG = "LocationService";
    public static final long UPDATE_INTERVAL = 4000; // 4 seconds

    private LocationManager mLocationManager;
    private LocationListener mLocationListener;

    public LocationService() {
        super("LocationService");
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        while(true){}
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onCreate() {
        super.onCreate();

        mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        mLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                saveUserLocation(location);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
                Log.d("LocationListener", String.format("onStatusChanged: provider %s; status: %d, extras: %s", provider, status, extras.toString()));
            }

            @Override
            public void onProviderEnabled(String provider) {
                Log.d("LocationListener", "onProviderEnabled");
            }

            @Override
            public void onProviderDisabled(String provider) {
                Log.d("LocationListener", "onProviderDisabled");
            }
        };

        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 1, mLocationListener);
        Location location = getLastKnownLocation();
        if(location != null)
            saveUserLocation(location);

        Notification notification = new NotificationCompat.Builder(this, App.LOCATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_location)
                .setContentTitle("Sending live location")
                .setContentText("You are sending live location to the application server")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();

        startForeground(2, notification);
    }

    private Location getLastKnownLocation() {
        mLocationManager = (LocationManager)getApplicationContext().getSystemService(LOCATION_SERVICE);
         List<String> providers = mLocationManager.getProviders(true);
        Location bestLocation = null;
        for (String provider : providers) {
            @SuppressLint("MissingPermission")
            Location l = mLocationManager.getLastKnownLocation(provider);
            if (l == null) {
                continue;
            }
            if (bestLocation == null || l.getAccuracy() < bestLocation.getAccuracy()) {
                // Found best last known location: %s", l);
                bestLocation = l;
            }
        }
        return bestLocation;
    }

    private void saveUserLocation(Location location){
        SignallingClient.getInstance().sendLiveLocation(location);
        Log.e(TAG, String.format("SEND LIVE LOCATION: %f %f", location.getLatitude(), location.getLongitude()));
    }
}

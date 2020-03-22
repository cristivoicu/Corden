package ro.atm.corden.util.webrtc;

import android.util.Log;

import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;

public class SimpleSdpObserver implements SdpObserver {
    private static final String TAG = "SimpleSdpObserver";

    @Override
    public void onCreateSuccess(SessionDescription sessionDescription) {

    }

    @Override
    public void onSetSuccess() {

    }

    @Override
    public void onCreateFailure(String s) {
        Log.d(TAG, "On create failure");
    }

    @Override
    public void onSetFailure(String s) {
        Log.d(TAG, "On set failure");
    }

}

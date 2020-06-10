package ro.atm.corden.util.webrtc.client;

import android.content.Context;
import android.util.Log;

import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.Logging;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;


/**
 * Static class for getting cameras
 */
public class CameraSelector {
    private static final String TAG = "CameraControl";

    public enum CameraType {
        BACK,
        FRONT,
        EXTERNAL
    }

    static VideoCapturer createCameraCapturer(Camera2Enumerator enumerator) {
        final String[] deviceNames = enumerator.getDeviceNames();

        // First, try to find front facing camera
        Log.d(TAG, "Looking for front facing cameras.");
        for (String deviceName : deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                Log.d(TAG, "Creating front facing camera capturer.");
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        // Front facing camera not found, try something else
        Log.d(TAG, "Looking for other cameras.");
        for (String deviceName : deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                Log.d(TAG, "Creating other camera capturer.");
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }
        return null;
    }

    static VideoCapturer getFrontCamera(Camera2Enumerator enumerator){
        final String[] deviceNames = enumerator.getDeviceNames();
        for(String deviceName : deviceNames){
            if(enumerator.isFrontFacing(deviceName)){
                Log.d(TAG, "Getting front camera");
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);
                return videoCapturer;
            }
        }
        return null;
    }

    static VideoCapturer getBackCamera(Camera2Enumerator enumerator){
        final String[] deviceNames = enumerator.getDeviceNames();
        for(String deviceName : deviceNames){
            if(enumerator.isBackFacing(deviceName)){
                Log.d(TAG, "Getting back camera");
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);
                return videoCapturer;
            }
        }
        return null;
    }
}

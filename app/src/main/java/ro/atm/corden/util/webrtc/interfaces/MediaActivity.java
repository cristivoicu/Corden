package ro.atm.corden.util.webrtc.interfaces;

import org.webrtc.MediaStream;

public interface MediaActivity {
    void gotRemoteStream(MediaStream mediaStream);
    void showToast(String message);
}

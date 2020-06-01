package ro.atm.corden.util.webrtc.client;

import android.content.Context;
import android.util.Log;

import org.webrtc.EglBase;
import org.webrtc.MediaConstraints;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SessionDescription;

import ro.atm.corden.util.constant.JsonConstants;
import ro.atm.corden.util.webrtc.interfaces.MediaActivity;
import ro.atm.corden.util.webrtc.observer.SimpleSdpObserver;
import ro.atm.corden.util.websocket.SignallingClient;

public class LiveWatcherClient extends Client {
    private String mUsername = null;
    protected LiveWatcherClient(MediaActivity mediaActivity, PeerConnectionFactory peerConnectionFactory) {
        super(mediaActivity, peerConnectionFactory, JsonConstants.ICE_FOR_LIVE);
    }

    void createWatchOffer(String username){
        mUsername = username;
        MediaConstraints sdpConstraints = new MediaConstraints();
        sdpConstraints.mandatory.add(
                new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        sdpConstraints.mandatory.add(
                new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));

        localPeer.createOffer(new SimpleSdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                super.onCreateSuccess(sessionDescription);
                localPeer.setLocalDescription(new SimpleSdpObserver(), sessionDescription);
                Log.d(TAG, "On create success: SignallingClient emit ");

                SignallingClient.getInstance().sendWatchVideoRequest(username, sessionDescription);
            }

            @Override
            public void onCreateFailure(String s) {
                super.onCreateFailure(s);
                Log.e(TAG, "Failed to create a sdp offer");
            }
        }, sdpConstraints);
    }

    @Override
    public void dispose() {
        super.dispose();
        if(mUsername != null)
            SignallingClient.getInstance().sendStopWatchVideoRequest(mUsername);
    }
}

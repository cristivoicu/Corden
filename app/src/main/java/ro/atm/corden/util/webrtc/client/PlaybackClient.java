package ro.atm.corden.util.webrtc.client;

import android.util.Log;

import org.webrtc.MediaConstraints;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SessionDescription;

import ro.atm.corden.util.constant.JsonConstants;
import ro.atm.corden.util.webrtc.interfaces.MediaActivity;
import ro.atm.corden.util.webrtc.observer.SimpleSdpObserver;
import ro.atm.corden.util.websocket.SignallingClient;

class PlaybackClient extends Client {
    PlaybackClient(MediaActivity mediaActivity, PeerConnectionFactory peerConnectionFactory){
        super(mediaActivity, peerConnectionFactory, JsonConstants.ICE_FOR_PLAY);
    }

    void createPlaybackOffer(String videoPath){
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

                SignallingClient.getInstance().sendPlayVideoRequest(sessionDescription, videoPath);
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
    }
}

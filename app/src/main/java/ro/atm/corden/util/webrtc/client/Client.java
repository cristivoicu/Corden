package ro.atm.corden.util.webrtc.client;

import android.app.Activity;
import android.content.Context;
import android.media.AudioTrack;
import android.media.MediaActionSound;
import android.util.Log;

import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SessionDescription;
import org.webrtc.VideoTrack;

import java.util.ArrayList;
import java.util.List;

import ro.atm.corden.util.constant.JsonConstants;
import ro.atm.corden.util.webrtc.interfaces.MediaActivity;
import ro.atm.corden.util.webrtc.observer.SimplePeerConnectionObserver;
import ro.atm.corden.util.webrtc.observer.SimpleSdpObserver;
import ro.atm.corden.util.websocket.SignallingClient;

/***/
public abstract class Client {
    protected static final String TAG = "ClientPeerConnection";

    protected MediaActivity mediaActivity;

    protected PeerConnection localPeer;

    protected Client(MediaActivity mediaActivity, PeerConnectionFactory peerConnectionFactory, String iceFor){
        this.mediaActivity = mediaActivity;
        createPeerConnection(peerConnectionFactory, iceFor);
    }

    protected void createPeerConnection(PeerConnectionFactory peerConnectionFactory, String iceFor){
        List<PeerConnection.IceServer> stunICEServers = new ArrayList<>();
        PeerConnection.IceServer stunServerList = PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer();
        stunICEServers.add(stunServerList);

        PeerConnection.RTCConfiguration rtcConfig = new PeerConnection.RTCConfiguration(stunICEServers);
        //PeerConnection.RTCConfiguration rtcConfig = new PeerConnection.RTCConfiguration(peerIceServers);
        // TCP candidates are only useful when connecting to a server that supports
        // ICE-TCP.
        rtcConfig.tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.DISABLED;
        rtcConfig.bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE;
        rtcConfig.rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE;
        rtcConfig.continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY;
        // Use ECDSA encryption.
        rtcConfig.keyType = PeerConnection.KeyType.ECDSA;
        localPeer = peerConnectionFactory.createPeerConnection(rtcConfig, new SimplePeerConnectionObserver() {
            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                Log.d(TAG, "Received ice candidate");
                super.onIceCandidate(iceCandidate);
                SignallingClient.getInstance().emitIceCandidate(iceCandidate, iceFor);
            }

            @Override
            public void onAddStream(MediaStream mediaStream) {
                mediaActivity.showToast("Received Remote stream");
                Log.d(TAG, "Received media stream");
                super.onAddStream(mediaStream);
                mediaActivity.gotRemoteStream(mediaStream);
            }
        });
        SignallingClient.getInstance().isChannelReady = true;
    }

    void addIceCandidate(IceCandidate iceCandidate){
        localPeer.addIceCandidate(iceCandidate);
    }

    void setRemoteResponse(String answer){
        localPeer.setRemoteDescription(new SimpleSdpObserver(),
                new SessionDescription(SessionDescription.Type.ANSWER, answer));
    }

    public void dispose(){
        if(localPeer != null){
            localPeer.close();
        }
    }
}

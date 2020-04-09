package ro.atm.corden.util.webrtc.client;

import android.content.Context;

import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoTrack;

import ro.atm.corden.util.webrtc.interfaces.MediaActivity;
import ro.atm.corden.util.websocket.callback.MediaListener;

/***/
public class Session {
    private PeerConnectionFactory peerConnectionFactory;
    private Context context;
    private EglBase eglBase;
    private Client client;
    private MediaActivity mediaActivity;

    public Session(Context context, EglBase eglBase, MediaActivity mediaActivity) {
        this.context = context;
        this.eglBase = eglBase;
        this.mediaActivity = mediaActivity;

        createPeerConnectionFactory();
    }

    public void createPlaybackClient() {
        this.client = new PlaybackClient(mediaActivity, peerConnectionFactory);
    }

    public void createLiveVideoClient() {
        this.client = new LiveVideoClient(eglBase, context, mediaActivity, peerConnectionFactory);
    }

    private void createPeerConnectionFactory() {
        PeerConnectionFactory.InitializationOptions initializationOptions =
                PeerConnectionFactory.InitializationOptions.builder(context)
                        .createInitializationOptions();
        PeerConnectionFactory.initialize(initializationOptions);

        //Create a new PeerConnectionFactory instance - using Hardware encoder and decoder.
        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        DefaultVideoEncoderFactory defaultVideoEncoderFactory =
                new DefaultVideoEncoderFactory(eglBase.getEglBaseContext(),
                        true,
                        true);
        DefaultVideoDecoderFactory defaultVideoDecoderFactory =
                new DefaultVideoDecoderFactory(eglBase.getEglBaseContext());
        peerConnectionFactory = PeerConnectionFactory.builder()
                .setOptions(options)
                .setVideoEncoderFactory(defaultVideoEncoderFactory)
                .setVideoDecoderFactory(defaultVideoDecoderFactory)
                .createPeerConnectionFactory();
    }

    public void createPlaybackOffer(String videoPath) {
        if (client instanceof PlaybackClient) {
            ((PlaybackClient) client).createPlaybackOffer(videoPath);
        }// throw incopatibleExc
    }

    public void createLiveOffer() {
        if (client instanceof LiveVideoClient) {
            ((LiveVideoClient) client).createOffer();
        }// throw incopatibleExc
    }

    public void addIceCandidate(IceCandidate iceCandidate) {
        this.client.addIceCandidate(iceCandidate);
    }

    public void setRemoteResponse(String answer) {
        this.client.setRemoteResponse(answer);
    }

    public VideoTrack getVideoTrack() {
        return ((LiveVideoClient) client).getVideoTrack();
    }

    public VideoCapturer getVideoCapturer(){
        return ((LiveVideoClient)client).getVideoCapturer();
    }

    public void leaveLiveSession() {
        ((LiveVideoClient)client).dispose();
        if (peerConnectionFactory != null) {
            peerConnectionFactory.dispose();
            peerConnectionFactory = null;
        }
    }

    public void leavePlaybackSession() {
        ((PlaybackClient)client).dispose();
        if (peerConnectionFactory != null) {
            peerConnectionFactory.dispose();
            peerConnectionFactory = null;
        }
    }
}

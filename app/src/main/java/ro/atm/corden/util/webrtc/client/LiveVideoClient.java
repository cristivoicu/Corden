package ro.atm.corden.util.webrtc.client;

import android.content.Context;
import android.util.Log;

import com.serenegiant.usb.UVCCamera;

import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.EglBase;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import ro.atm.corden.model.user.LoginUser;
import ro.atm.corden.util.constant.JsonConstants;
import ro.atm.corden.util.webrtc.interfaces.MediaActivity;
import ro.atm.corden.util.webrtc.observer.SimpleSdpObserver;
import ro.atm.corden.util.webrtc.usb_camera.UsbCapturer;
import ro.atm.corden.util.websocket.SignallingClient;

public class LiveVideoClient extends Client {
    private MediaStream mediaStream;
    private VideoTrack videoTrack;
    private VideoSource videoSource;
    private AudioTrack audioTrack;
    private AudioSource audioSource;

    private MediaConstraints audioConstraints;
    private MediaConstraints videoConstraints;
    private MediaConstraints sdpConstraints;

    private VideoCapturer videoCapturer;
    private SurfaceTextureHelper surfaceTextureHelper;

    LiveVideoClient(EglBase eglBase,
                    Context context,
                    MediaActivity mediaActivity,
                    PeerConnectionFactory peerConnectionFactory,
                    CameraSelector.CameraType cameraType) {
        super(mediaActivity, peerConnectionFactory, JsonConstants.ICE_FOR_REC);
        captureFromCamera(context, eglBase, peerConnectionFactory, cameraType);
        addStreamToLocalPeer(peerConnectionFactory);
    }

    private void captureFromCamera(Context context,
                                   EglBase eglBase,
                                   PeerConnectionFactory peerConnectionFactory,
                                   CameraSelector.CameraType cameraType){

        //Now create a VideoCapturer instance.
        switch (cameraType){
            case  BACK:
                    videoCapturer = CameraSelector.getBackCamera(new Camera2Enumerator(context));
                break;
            case FRONT:
                    videoCapturer = CameraSelector.getFrontCamera(new Camera1Enumerator(true));
                break;
            case EXTERNAL:
                    videoCapturer = CameraSelector.getExternalCamera(context);
                break;
            default:
                videoCapturer = CameraSelector.createCameraCapturer(new Camera1Enumerator(false));
                Log.e(TAG, "UNKNOWN CAMERA TYPE!");
        }

        //Create MediaConstraints - Will be useful for specifying video and audio constraints.
        audioConstraints = new MediaConstraints();
        videoConstraints = new MediaConstraints();

        //Create a VideoSource instance
        if (videoCapturer != null) {
            surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread",
                    eglBase.getEglBaseContext());
            videoSource = peerConnectionFactory.createVideoSource(videoCapturer.isScreencast());
            videoSource.adaptOutputFormat(UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT, 30);
            videoCapturer.initialize(surfaceTextureHelper, context , videoSource.getCapturerObserver());
        }
        videoTrack = peerConnectionFactory.createVideoTrack("100", videoSource);

        //create an AudioSource instance
        audioSource = peerConnectionFactory.createAudioSource(audioConstraints);
        audioTrack = peerConnectionFactory.createAudioTrack("101", audioSource);
        audioTrack.setEnabled(true);
        audioTrack.setVolume(1000);

        if (videoCapturer != null) {
            // videoCapturer.startCapture(1280, 720, 30);
            videoCapturer.startCapture(UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT, 30); // by default, max in full hd
        }
    }

    private void addStreamToLocalPeer(PeerConnectionFactory peerConnectionFactory){
        MediaStream stream = peerConnectionFactory.createLocalMediaStream("102");
        stream.addTrack(audioTrack);
        stream.addTrack(videoTrack);
        localPeer.addStream(stream);
    }

    void createOffer(){
        sdpConstraints = new MediaConstraints();
        sdpConstraints.mandatory.add(
                new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        sdpConstraints.mandatory.add(
                new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
        localPeer.createOffer(new SimpleSdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                super.onCreateSuccess(sessionDescription);
                localPeer.setLocalDescription(new SimpleSdpObserver(), sessionDescription);
                Log.d("onCreateSuccess", "SignallingClient emit ");
                Log.d(TAG, "Sending video for record");
                SignallingClient.getInstance().sendLiveStreamVideo(LoginUser.username, sessionDescription);
            }
        }, sdpConstraints);
    }

    VideoTrack getVideoTrack(){
        return videoTrack;
    }

    VideoCapturer getVideoCapturer(){
        return videoCapturer;
    }

    @Override
    public void dispose() {
        super.dispose();
        if(videoTrack != null){
            videoCapturer.dispose();
            videoCapturer = null;
        }
        if(surfaceTextureHelper != null){
            surfaceTextureHelper.dispose();
            surfaceTextureHelper = null;
        }
    }
}

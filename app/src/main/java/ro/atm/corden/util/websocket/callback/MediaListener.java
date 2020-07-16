package ro.atm.corden.util.websocket.callback;

import com.google.gson.JsonObject;

import ro.atm.corden.model.video.VideoInfo;

/**
 * Contain interfaces for different calls with media server
 */
public interface MediaListener {
    /**
     * Defines method that a media player activity must implement for gaining tha capacity to
     * playback video data from the server
     */

    interface PlaybackListener {
        /**
         * Called by web socket client
         * Used in media stream activity for sending the forward message:
         * <ul>
         *     <li>id : iceCandidate</li>
         *     <li>candidate : <candidate></li>
         * </ul>
         */
        void onIceCandidate(JsonObject data);

        /**
         * Called by web socket client
         * Used to process sdp answer received from server
         * Used when user wants to play an recorded video
         */
        void onPlayResponse(String answer);

        /***/
        void onVideoInfo(VideoInfo videoInfo);

        void onGotPosition(long position);

        void onPlayEnd();

        void onPlaybackRejected();
    }

    /**
     * Defines methods that a media activity should implement for sending streaming to a server
     */
    interface RecordingListener {
        /**
         * Called by web socket client
         * Used to notify user that his call was accepted by the media server
         *
         * @param answer is the sdpAnswer from media server
         */
        void onStartResponse(String answer);

        /**
         * Called by web socket client
         * Used in media stream activity for sending the forward message:
         * <ul>
         *     <li>id : iceCandidate</li>
         *     <li>candidate : <candidate></li>
         * </ul>
         */
        void onIceCandidate(JsonObject data);
    }

    interface LiveStreamingListener {
        /**
         * Called by web socket client
         * Used to notify user that his call was accepted by the media server
         *
         * @param answer is the sdpAnswer from media server
         */
        void onLiveResponse(String answer);

        /**
         * Called by web socket client
         * Used in media stream activity for sending the forward message:
         * <ul>
         *     <li>id : iceCandidate</li>
         *     <li>candidate : <candidate></li>
         * </ul>
         */
        void onIceCandidate(JsonObject data);

        void onLiveStreamingError(String message);
    }

}

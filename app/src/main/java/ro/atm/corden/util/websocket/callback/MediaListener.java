package ro.atm.corden.util.websocket.callback;

import com.google.gson.JsonObject;

/**
 * Contain interfaces for different calls with media server
 */
public interface MediaListener {
    /**
     * Defines method that a media activity must implement for one to one video call
     */
    interface OneToOneCallListener {
        /**
         * Called by web socket client
         * Used in media stream activity for sending the forward message:
         * <ul>
         *     <li>id : incomingCallResponse</li>
         *     <li>from : username</li>
         *     <li>response : accepted</li>
         *     <li>sdpOffer : <sdp></li>
         * </ul>
         */
        void onIncomingCallResponse(String from);


        /**
         * Called by web socket client
         * Used to process sdp answer received from server
         * Used when user is callee
         */
        void onStartCommunication(String answer);

        /**
         * Called by web socket client
         * Used to process sdp answer received from server
         * Used when user is caller
         */
        void onCallResponse(String answer);
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


        void onTryToStart();

        void callRefused();
    }

    /**
     * Defines methods that a media activity should implement for sending streaming to a server
     */
    interface RecordingListener {
        /**
         * Called by web socket client
         * Used to notify user that his call was accepted by the media server
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

    interface LivePlayListener{
        /**
         * Called by web socket client
         * Used to notify user that his call was accepted by the media server
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
    }

}

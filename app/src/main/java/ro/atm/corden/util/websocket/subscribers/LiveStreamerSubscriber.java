package ro.atm.corden.util.websocket.subscribers;

import ro.atm.corden.model.user.LiveStreamer;

public interface LiveStreamerSubscriber {
    void onNewSubscriber(LiveStreamer liveStreamer);
    void onSubscribeStop(LiveStreamer liveStreamer);
}

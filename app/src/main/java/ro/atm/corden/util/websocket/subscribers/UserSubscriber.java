package ro.atm.corden.util.websocket.subscribers;

import ro.atm.corden.model.user.User;

public interface UserSubscriber {
    void onUserUpdated(User user);
}

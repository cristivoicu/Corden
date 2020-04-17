package ro.atm.corden.util.websocket.subscribers;

import ro.atm.corden.model.user.Status;
import ro.atm.corden.model.user.User;

public interface UserSubscriber {
    void onUserDataChanged(User user);
    void onUserStatusChanged(String username, Status status);
}

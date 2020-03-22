package ro.atm.corden.util.websocket.callback;

import ro.atm.corden.model.transport_model.User;

public interface GetUsersListener {

    void gotAllUsers(User[] users);
}

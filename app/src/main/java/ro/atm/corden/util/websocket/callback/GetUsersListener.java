package ro.atm.corden.util.websocket.callback;

import ro.atm.corden.model.user.User;

public interface GetUsersListener {

    void gotAllUsers(User[] users);
}

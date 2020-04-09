package ro.atm.corden.util.websocket.callback;

import ro.atm.corden.model.user.Role;

public interface LoginListener {
    void onLoginError();

    void onLoginSuccess(Role role);
}

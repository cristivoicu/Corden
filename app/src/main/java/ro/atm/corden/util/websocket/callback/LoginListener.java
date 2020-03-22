package ro.atm.corden.util.websocket.callback;

import ro.atm.corden.model.Roles;

public interface LoginListener {
    void onLoginError();

    void onLoginSuccess(Roles role);
}

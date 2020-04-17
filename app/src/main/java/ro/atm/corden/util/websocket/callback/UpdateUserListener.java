package ro.atm.corden.util.websocket.callback;

public interface UpdateUserListener {
    void onUpdateSuccess();

    void onUpdateFailure();

    void onUserDisableSuccess();

    void onUserDisableFailure();
}

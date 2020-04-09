package ro.atm.corden.model.user;

import ro.atm.corden.util.exception.login.EmptyTextException;
import ro.atm.corden.util.exception.login.LoginListenerNotInitialisedException;
import ro.atm.corden.util.websocket.SignallingClient;

public class LoginUser {
    public static String username = "";
    private String strUsername = "";
    private String strPassword = "";

    public LoginUser(String UserName, String Password) {
        strUsername = UserName;
        strPassword = Password;
    }

    public String getStrUsername() {
        return strUsername;
    }

    public String getStrPassword() {
        return strPassword;
    }

    /**
     * Used to login the user to the application server
     *
     * @throws EmptyTextException if username and/or password are empty
     */
    public void checkLogin() throws EmptyTextException, LoginListenerNotInitialisedException {
        if (strUsername == null && strPassword == null)
            throw new EmptyTextException();
        if (strPassword.trim().isEmpty() || strUsername.trim().isEmpty()) {
            throw new EmptyTextException();
        }
        username = strUsername;
        SignallingClient.getInstance().logIn(strUsername, strPassword);
    }
}

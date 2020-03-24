package ro.atm.corden.util.exception.websocket;

public class UserNotLoggedInException extends WebSocketException {
    public UserNotLoggedInException(){
        super("no message");
    }

    public UserNotLoggedInException(String message) {
        super(message);
    }
}

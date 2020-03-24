package ro.atm.corden.util.exception.websocket;

import androidx.annotation.Nullable;

class WebSocketException extends Exception {
    final private String message;

    WebSocketException(String message){
        this.message = message;
    }

    @Nullable
    @Override
    public String getMessage() {
        return message;
    }
}

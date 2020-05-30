package ro.atm.corden.util.websocket;

/***/
public class AuthenticatedUser {
    private String username;
    private String token;

    private AuthenticatedUser() {
    }

    private static AuthenticatedUser INSTANCE = new AuthenticatedUser();

    public static AuthenticatedUser getInstance(){
        return INSTANCE;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}


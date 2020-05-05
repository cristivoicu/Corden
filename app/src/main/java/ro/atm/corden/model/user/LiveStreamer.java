package ro.atm.corden.model.user;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

import java.io.Serializable;

public class LiveStreamer implements Serializable {
    @Expose
    private String name;
    @Expose
    private String username;

    public LiveStreamer(String name, String username) {
        this.name = name;
        this.username = username;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public static LiveStreamer fromJson(String json){
        Gson gson = new GsonBuilder().setDateFormat("MMM dd, yyyy, h:mm:ss a").setPrettyPrinting().create();
        return gson.fromJson(json, LiveStreamer.class);
    }
}

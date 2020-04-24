package ro.atm.corden.model.user;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.Date;

public class Action implements Serializable {

    public Action(long id,
                  String description,
                  Date date,
                  String serverLogActionType,
                  String imporance,
                  String username) {
        this.id = id;
        this.description = description;
        this.date = date;
        this.serverLogActionType = serverLogActionType;
        this.importance = imporance;
        this.username = username;
    }

    @Expose
    private long id;
    @Expose
    private String description;
    @Expose
    @SerializedName("datetime")
    private Date date;
    @Expose
    private String serverLogActionType;
    @Expose
    private String importance;
    @Expose
    private String username;

    public long getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getServerLogActionType() {
        return serverLogActionType;
    }

    public void setServerLogActionType(String serverLogActionType) {
        this.serverLogActionType = serverLogActionType;
    }

    public String getImportance() {
        return importance;
    }

    public void setImportance(String imporance) {
        this.importance = imporance;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}

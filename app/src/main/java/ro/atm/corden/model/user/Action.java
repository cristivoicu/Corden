package ro.atm.corden.model.user;

import com.google.gson.annotations.Expose;

import java.io.Serializable;
import java.util.Date;

public class Action implements Serializable {

    public Action(long id, String description, Date date) {
        this.id = id;
        this.description = description;
        this.date = date;
    }

    @Expose
    private long id;
    @Expose
    private String description;
    @Expose
    private Date date;

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
}

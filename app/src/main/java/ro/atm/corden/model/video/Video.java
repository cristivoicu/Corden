package ro.atm.corden.model.video;

import java.io.Serializable;
import java.util.Date;

public class Video implements Serializable {
    //region Constructors
    protected Video(){

    }

    public Video(String name, Date date, String username) {
        this.name = name;
        this.date = date;
        this.username = username;
    }
    //endregion

    //region Members
    private Long id;
    private String name;
    private Date date;
    private String username;
    //endregion
    // region Getters and setters
    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    //endregion
}

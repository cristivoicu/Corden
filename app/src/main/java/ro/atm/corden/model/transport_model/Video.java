package ro.atm.corden.model.transport_model;

import java.io.Serializable;
import java.util.Date;

public class Video implements Serializable {
    //region Constructors
    protected Video(){

    }

    public Video(String name, Date date) {
        this.name = name;
        this.date = date;
    }
    //endregion

    //region Members
    private Long id;
    private String name;
    private Date date;
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
    //endregion
}

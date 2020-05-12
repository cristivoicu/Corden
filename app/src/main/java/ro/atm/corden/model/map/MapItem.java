package ro.atm.corden.model.map;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.annotations.Expose;

import java.io.Serializable;
import java.util.List;

import ro.atm.corden.util.interfaces.Jsonable;

public abstract class MapItem implements Jsonable, Serializable {
    @Expose(serialize = false, deserialize = false)
    private String id;
    @Expose
    private String name;
    @Expose
    private String description;
    @Expose
    private int color;
    @Expose
    private List<LatLng> coordinates;

    public MapItem(String id, String name, String description, int color, List<LatLng> coordinates) {
        this.name = name;
        this.description = description;
        this.color = color;
        this.coordinates = coordinates;
    }

    public MapItem(String id, String name, String description, int color) {
        this.name = name;
        this.description = description;
        this.color = color;
    }


    //region Getters and setters

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public List<LatLng> getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(List<LatLng> coordinates) {
        this.coordinates = coordinates;
    }

    public String getId() {
        return id;
    }

    //endregion

    @Override
    public String toJson() {
        return gson.toJson(this, MapItem.class);
    }

    public static Object fromJson(String json) {
        return gson.fromJson(json, MapItem.class);
    }


}

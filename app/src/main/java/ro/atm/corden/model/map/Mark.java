package ro.atm.corden.model.map;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.annotations.Expose;

import java.util.List;

public class Mark extends MapItem {
    @Expose
    private LatLng coordinates;

    public Mark(String name, String description, int color, LatLng coordinates){
        super(name, description, color);
        this.coordinates = coordinates;
    }
}

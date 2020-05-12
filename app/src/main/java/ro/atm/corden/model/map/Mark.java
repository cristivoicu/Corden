package ro.atm.corden.model.map;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.gson.annotations.Expose;

import java.util.List;

public class Mark extends MapItem {
    @Expose
    private LatLng coordinates;

    @Expose(serialize = false, deserialize = false)
    private Marker mMarker;

    public Mark(Marker marker, String name, String description, int color, LatLng coordinates){
        super(marker.getId(), name, description, color);
        this.coordinates = coordinates;
        mMarker = marker;
    }

    public LatLng getCoordinate() {
        return coordinates;
    }

    public Marker getMarker() {
        return mMarker;
    }
}

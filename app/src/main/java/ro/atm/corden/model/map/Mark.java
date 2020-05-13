package ro.atm.corden.model.map;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.gson.annotations.Expose;

import java.util.List;

import ro.atm.corden.util.interfaces.Jsonable;

public class Mark extends MapItem {
    @Expose(serialize = false, deserialize = false)
    private Marker mMarker;

    public Mark(Marker marker, String name, String description, int color, LatLng coordinates) {
        super(marker.getId(), name, description, color, coordinates);
        mMarker = marker;
    }

    public Marker getMarker() {
        return mMarker;
    }

/*    @Override
    public String toJson() {
        return gson.toJson(this, Mark.class);
    }*/
}

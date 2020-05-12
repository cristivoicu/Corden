package ro.atm.corden.model.map;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polygon;
import com.google.gson.annotations.Expose;

import java.util.List;

public class Zone extends MapItem {
    @Expose(serialize = false, deserialize = false)
    private Polygon mPolygon;

    public Zone(Polygon polygon, String name, String description, int color, List<LatLng> coordinates) {
        super(polygon.getId(), name, description, color, coordinates);
        mPolygon = polygon;
    }

    public Polygon getPolygon() {
        return mPolygon;
    }
}

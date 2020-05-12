package ro.atm.corden.model.map;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.gson.annotations.Expose;

import java.util.List;

public class Path extends MapItem {
    @Expose(serialize = false, deserialize = false)
    private Polyline mPolyline;

    public Path(Polyline polyline, String name, String description, int color, List<LatLng> coordinates) {
        super(polyline.getId(), name, description, color, coordinates);
        mPolyline = polyline;
    }

    public Polyline getPolyline() {
        return mPolyline;
    }
}

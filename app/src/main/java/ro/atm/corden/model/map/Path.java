package ro.atm.corden.model.map;

import com.google.android.gms.maps.model.LatLng;

import java.util.List;

public class Path extends MapItem {
    public Path(String name, String description, int color, List<LatLng> coordinates) {
        super(name, description, color, coordinates);
    }
}

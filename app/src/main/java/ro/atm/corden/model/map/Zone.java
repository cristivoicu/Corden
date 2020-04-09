package ro.atm.corden.model.map;

import com.google.android.gms.maps.model.LatLng;

import java.util.List;

public class Zone extends MapItem {
    public Zone(String name, String description, int color, List<LatLng> coordinates) {
        super(name, description, color, coordinates);
    }
}

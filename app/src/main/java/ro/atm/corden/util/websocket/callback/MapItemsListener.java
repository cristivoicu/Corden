package ro.atm.corden.util.websocket.callback;

public interface MapItemsListener {
    void onMapItemsSaveSuccess();
    void onMapItemsSaveFailure();

    void onUserLocationUpdated(String username, double lat, double lng);
}

package ro.atm.corden.view.activity;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.FragmentActivity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import ro.atm.corden.R;
import ro.atm.corden.databinding.ActivityUserJobsMapsBinding;
import ro.atm.corden.model.map.MapItem;
import ro.atm.corden.model.map.Mark;
import ro.atm.corden.model.map.Path;
import ro.atm.corden.model.map.Zone;
import ro.atm.corden.util.websocket.Repository;
import ro.atm.corden.view.dialog.SaveMapItemDialog;

public class UserJobsMapsActivity extends AppCompatActivity
        implements OnMapReadyCallback,
        SaveMapItemDialog.SaveMapDialogListener,
        GoogleMap.OnPolygonClickListener,
        GoogleMap.OnPolylineClickListener {
    private ActivityUserJobsMapsBinding binding;

    private GoogleMap mMap;
    private UiSettings mUiSettings;

    private boolean isZone = false;
    private boolean isPath = false;
    private boolean isLocation = false;

    private Polyline currentPolyline;
    private PolylineOptions polylineOptions;

    private Polygon currentPolygon;
    private PolygonOptions polygonOptions;
    private List<LatLng> points = new LinkedList<>();

    private Marker currentMarker = null;
    private MarkerOptions markerOptions;

    private List<MapItem> mapItems = new LinkedList<>();
    private Map<String, LatLng> liveLocations = new ConcurrentHashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_user_jobs_maps);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_user_jobs_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        setSupportActionBar(binding.toolbar);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.map_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.userLiveLocation:
                if(item.isChecked()){
                    item.setChecked(false);
                }else{
                    item.setChecked(true);
                }
                return false;
            case R.id.mapPaths:
                if(item.isChecked()){
                    item.setChecked(false);
                }else{
                    item.setChecked(true);
                }
                return true;
            case R.id.mapZones:
                if(item.isChecked()){
                    item.setChecked(false);
                }else{
                    item.setChecked(true);
                }
                return true;
            case R.id.mapLocations:
                if(item.isChecked()){
                    item.setChecked(false);
                }else{
                    item.setChecked(true);
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        mMap.setOnPolygonClickListener(this);
        mMap.setOnPolylineClickListener(this);

        // UI settings
        mUiSettings = mMap.getUiSettings();
        mUiSettings.setMyLocationButtonEnabled(true);
        mMap.setMyLocationEnabled(true);
        mUiSettings.setZoomControlsEnabled(true);

        polylineOptions = new PolylineOptions();
        polygonOptions = new PolygonOptions();
        currentPolyline = googleMap.addPolyline(polylineOptions);

        markerOptions = new MarkerOptions();

        // Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng(-34, 151);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                if (isPath) {
                    points.add(latLng);
                    currentPolyline.setPoints(points);

                    return;
                }
                if (isLocation) {
                    if (currentMarker != null) {
                        currentMarker.remove();
                        markerOptions = new MarkerOptions();
                    }
                    markerOptions.position(latLng);
                    currentMarker = mMap.addMarker(markerOptions);
                    return;
                }
                if (isZone) {
                    points.add(latLng);
                    currentPolyline.setPoints(points);
                    return;
                }
            }
        });
    }

    public void onZoneButtonClicked(View view) {
        points.clear();
        polylineOptions = new PolylineOptions();
        currentPolyline = mMap.addPolyline(polylineOptions);
        isZone = !isZone;
        if (isZone) {
            binding.buttonLayout.setVisibility(View.GONE);
            binding.currentTaskTextView.setText("Set the zone!");
            binding.currentTaskTextView.setVisibility(View.VISIBLE);
        } else {
            binding.currentTaskTextView.setVisibility(View.GONE);
        }
    }

    public void onLocationButtonClicked(View view) {
        isLocation = !isLocation;
        if (isLocation) {
            binding.buttonLayout.setVisibility(View.GONE);
            binding.currentTaskTextView.setText("Set the location for current user!");
            binding.currentTaskTextView.setVisibility(View.VISIBLE);
        } else {
            binding.currentTaskTextView.setVisibility(View.GONE);
        }
    }

    public void onPathButtonClicked(View view) {
        points.clear();
        polylineOptions = new PolylineOptions();
        currentPolyline = mMap.addPolyline(polylineOptions);
        isPath = !isPath;
        if (isPath) {
            binding.buttonLayout.setVisibility(View.GONE);
            binding.currentTaskTextView.setText("Set the path!");
            binding.currentTaskTextView.setVisibility(View.VISIBLE);
        } else {
            binding.currentTaskTextView.setVisibility(View.GONE);
        }
    }

    public void onTextViewClicked(View view) {
        binding.buttonLayout.setVisibility(View.VISIBLE);
        binding.currentTaskTextView.setVisibility(View.GONE);
        openSaveDialog();
        if (isPath) {
            polylineOptions = new PolylineOptions();
            return;
        }
        if (isLocation) {

            return;
        }
        if (isZone) {
            for (LatLng latLng : currentPolyline.getPoints()) {
                polygonOptions.add(latLng);
            }
            currentPolyline.remove();
            polylineOptions = new PolylineOptions();
            currentPolygon = mMap.addPolygon(polygonOptions);
            polygonOptions = new PolygonOptions();
            return;
        }
    }

    public void openSaveDialog() {
        SaveMapItemDialog saveMapItemDialog = new SaveMapItemDialog();
        saveMapItemDialog.show(getSupportFragmentManager(), "save map dialog");
    }

    @Override
    public void saveMap(String name, String description, int color) {
        if (isPath) {
            isPath = !isPath;
            currentPolyline.setColor(color);
            currentPolyline.setTag(name);
            currentPolyline.setWidth(10);
            currentPolyline.setClickable(true);

            mapItems.add(new Path(name, description, color, currentPolyline.getPoints()));
        } else {
            if (isZone) {
                isZone = !isZone;
                currentPolygon.setFillColor(adjustColor(color, (float) 0.6));
                currentPolygon.setTag(name);
                currentPolygon.setStrokeColor(color);
                currentPolygon.setClickable(true);
                mapItems.add(new Zone(name, description, color, currentPolygon.getPoints()));
            } else {
                if (isLocation) {
                    isLocation = !isLocation;
                    markerOptions.title(name)
                            .snippet(description)
                            .alpha(0.6f);
                    currentMarker.remove();
                    currentMarker = mMap.addMarker(markerOptions);

                    mapItems.add(new Mark(name, description, color, currentMarker.getPosition()));
                    currentMarker = null;
                    markerOptions = new MarkerOptions();
                }
            }
        }

    }

    @Override
    public void cancel() {
        if (isPath) {
            isPath = !isPath;
            currentPolyline.remove();
        } else {
            if (isZone) {
                isZone = !isZone;
                currentPolygon.remove();
            } else {
                if (isLocation) {
                    isLocation = !isLocation;
                    currentMarker.remove();
                    currentMarker = null;
                }
            }
        }
    }

    @ColorInt
    private int adjustColor(@ColorInt int color, float factor) {
        int alpha = Math.round(Color.alpha(color) * factor);
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        return Color.argb(alpha, red, green, blue);
    }

    @Override
    public void onPolygonClick(Polygon polygon) {
        Toast.makeText(this, "Clicked polygon TAG: " + polygon.getTag().toString(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPolylineClick(Polyline polyline) {
        Toast.makeText(this, "Clicked polyline TAG: " + polyline.getTag().toString(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBackPressed() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("You are about to exit!")
                .setMessage("Do you want to save the modified map?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        UserJobsMapsActivity.super.onBackPressed();
                        Repository.getInstance().saveMapItems(mapItems);
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        UserJobsMapsActivity.super.onBackPressed();
                    }
                });
        builder.show();
    }
}

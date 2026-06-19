package com.example.animalcatch;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.osmdroid.config.Configuration;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

public class MainActivity extends AppCompatActivity {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private MapView map = null;
    private MyLocationNewOverlay locationOverlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Crucial configuration step for osmdroid tile caching
        Configuration.getInstance().load(getApplicationContext(), getSharedPreferences("osmdroid", MODE_PRIVATE));
        setContentView(R.layout.activity_main);

        map = findViewById(R.id.mapview);
        map.setBuiltInZoomControls(true);
        map.setMultiTouchControls(true);

        // Start map view at a baseline global zoom level
        map.getController().setZoom(22);

        // Check and request runtime location tracking permissions
        checkLocationPermissions();
    }

    private void checkLocationPermissions() {
        if (ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED) {

            // Request permission from the user
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            // Permission already granted, initialize tracking overlay
            enableUserLocation();
        }
    }

    private void enableUserLocation() {
        // Initialize the location tracking overlay component
        locationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), map);

        // Enable device tracking and follow the user's movement on map view
        locationOverlay.enableMyLocation();
        locationOverlay.enableFollowLocation();

        // Inject the tracking layer directly onto our canvas overlay queue
        map.getOverlays().add(locationOverlay);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                enableUserLocation();
            else
                Toast.makeText(
                    this,
                    "Location permission is required to view your current position.",
                    Toast.LENGTH_LONG
                ).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        map.onResume();
        if (locationOverlay != null)
            locationOverlay.enableMyLocation();
    }

    @Override
    protected void onPause() {
        super.onPause();
        map.onPause();
        if (locationOverlay != null) {
            locationOverlay.disableMyLocation();
            locationOverlay.disableFollowLocation();
        }
    }
}

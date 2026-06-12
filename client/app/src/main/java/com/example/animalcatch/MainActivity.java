package com.example.animalcatch;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

public class MainActivity extends AppCompatActivity {
    private MapView map = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load default user agent configuration
        Configuration.getInstance().load(getApplicationContext(),
                getSharedPreferences("osmdroid", MODE_PRIVATE));

        setContentView(R.layout.activity_main);

        map = findViewById(R.id.mapview);
        map.setBuiltInZoomControls(true);
        map.setMultiTouchControls(true);

        // Center on New York City coordinates
        map.getController().setZoom(12.0);
        map.getController().setCenter(new GeoPoint(40.7128, -74.0060));
    }
}

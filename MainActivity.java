package com.example.gpsmaps;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import androidx.activity.EdgeToEdge;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;


public class MainActivity extends AppCompatActivity implements LocationListener {

    SwipeRefreshLayout swipeRefreshLayout;
    private TextView text_network;
    private TextView text_gps;
    private static String TAG = "2022";
    private static final int MY_PERMISSION_ACCESS_FINE_LOCATION = 1;
    private static final int MY_PERMISSION_ACCESS_COARSE_LOCATION = 2;
    private TextView bestprovider;
    private TextView longitude;
    private TextView latitude;
    private TextView arcgivaldata;
    private LocationManager locationManager;
    private Criteria criteria;
    private Location location;
    private String bp;
    private MapView osm;
    private MapController mapController;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);

        swipeRefreshLayout = findViewById(R.id.refreshLayout);
        text_network = findViewById(R.id.text_network);
        text_gps = findViewById(R.id.text_gps);
        bestprovider = findViewById(R.id.bestprovider);
        longitude = findViewById(R.id.longitude);
        latitude = findViewById(R.id.latitude);
        arcgivaldata = findViewById(R.id.archival_data);

        criteria = new Criteria();
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        bp = locationManager.getBestProvider(criteria, true);

        startMapAndLocation();

        swipeRefreshLayout.setOnRefreshListener(() -> {
            swipeRefreshLayout.setRefreshing(false);
            boolean connection = isNetworkAvailable();
            if (connection) {
                text_network.setText("internet connect");
                text_network.setTextColor(Color.GREEN);
            } else {
                text_network.setText("no internet");
                text_network.setTextColor(Color.RED);
            }
            swipeRefreshLayout.setColorSchemeColors(Color.YELLOW);

            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSION_ACCESS_FINE_LOCATION);
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, MY_PERMISSION_ACCESS_COARSE_LOCATION);
                return;
            }

            bp = locationManager.getBestProvider(criteria, true);
            location = locationManager.getLastKnownLocation(bp);
            locationManager.requestLocationUpdates(
                    "" + bp,
                    500,
                    0.5f,
                    this
            );
        });

        osm = findViewById(R.id.osm);
        registerForContextMenu(osm);
    }

    private void startMapAndLocation() {
        boolean connection = isNetworkAvailable();
        if (connection) {
            text_network.setText("Internet connected");
            text_network.setTextColor(Color.GREEN);
        } else {
            text_network.setText("No internet");
            text_network.setTextColor(Color.RED);
        }

        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            text_gps.setText("GPS: ON");
            text_gps.setTextColor(Color.GREEN);
        } else {
            text_gps.setText("GPS: OFF");
            text_gps.setTextColor(Color.RED);
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSION_ACCESS_FINE_LOCATION);
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, MY_PERMISSION_ACCESS_COARSE_LOCATION);
            return;
        }

        bp = locationManager.getBestProvider(criteria, true);
        if (bp != null) {
            location = locationManager.getLastKnownLocation(bp);
            locationManager.requestLocationUpdates(
                    "" + bp,
                    500,
                    0.5f,
                    this
            );
        }

        osm = findViewById(R.id.osm);
        Context context = getApplicationContext();
        Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context));

        osm.setTileSource(TileSourceFactory.MAPNIK);
        osm.setBuiltInZoomControls(true);
        osm.setMultiTouchControls(true);

        mapController = (MapController) osm.getController();
        mapController.setZoom(12);

        if (location != null) {
            GeoPoint geoPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
            mapController.setCenter(geoPoint);
            mapController.animateTo(geoPoint);
            onLocationChanged(location);
        }

        osm.setMapListener(new MapListener() {
            @Override
            public boolean onScroll(ScrollEvent event) {
                return false;
            }

            @Override
            public boolean onZoom(ZoomEvent event) {
                return false;
            }
        });
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager != null ? connectivityManager.getActiveNetworkInfo() : null;
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case MY_PERMISSION_ACCESS_FINE_LOCATION:
            case MY_PERMISSION_ACCESS_COARSE_LOCATION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permissions were granted", Toast.LENGTH_SHORT).show();
                    startMapAndLocation();
                } else {
                    Toast.makeText(this, "Permissions were denied", Toast.LENGTH_SHORT).show();
                }
                break;
            }
            default:
                Log.d(TAG, "Another permissions");
        }
    }

    public void AddMarkerToMap(GeoPoint center) {
        if (osm == null) return;
        Marker marker = new Marker(osm);
        marker.setPosition(center);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        osm.getOverlays().clear();
        osm.getOverlays().add(marker);
        osm.invalidate();
        marker.setTitle("My position");
    }

    @Override
    public void onLocationChanged(Location location) {
        this.location = location;
        text_gps.setTextColor(Color.GREEN);
        bestprovider.setText("Best provider: " + bp);
        longitude.setText("Longitude: " + location.getLongitude());
        latitude.setText("Latitude: " + location.getLatitude());
        arcgivaldata.append("\n" + location.getLongitude() + " | " + location.getLatitude());

        GeoPoint currentPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
        AddMarkerToMap(currentPoint);
        if (mapController != null) {
            mapController.animateTo(currentPoint);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();


        if (itemId == R.id.menu_send_sms) {

            if (location != null) {
                String smsBody = location.getLongitude() + " " + location.getLatitude();

                Intent smsIntent = new Intent(Intent.ACTION_VIEW);
                smsIntent.setData(Uri.parse("sms:"));
                smsIntent.putExtra("sms_body", smsBody);
                startActivity(smsIntent);
            }
            return true;


        } else if (itemId == R.id.menu_save_coords) {
            osm.setDrawingCacheEnabled(true);
            Bitmap bitmap = osm.getDrawingCache();

            String fileName = "map_" + System.currentTimeMillis() + ".png";
            File picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            File file = new File(picturesDir, fileName);

            try {
                FileOutputStream outputStream = new FileOutputStream(file);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                outputStream.flush();
                outputStream.close();

                Toast.makeText(this, "Map saved", Toast.LENGTH_LONG).show();
            } catch (IOException e) {
                Toast.makeText(this, "Failed to save map", Toast.LENGTH_SHORT).show();
            }
            osm.setDrawingCacheEnabled(false);
            return true;


        } else if (itemId == R.id.menu_share_results) {

            if (location != null) {
                String shareText = location.getLongitude() + " " + location.getLatitude();

                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);

                startActivity(Intent.createChooser(shareIntent, "Share Location"));
            }
            return true;


        } else if (itemId == R.id.menu_weather) {
            Toast.makeText(this, "Pogoda~", Toast.LENGTH_SHORT).show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
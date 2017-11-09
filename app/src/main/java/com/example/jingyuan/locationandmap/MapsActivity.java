package com.example.jingyuan.locationandmap;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.provider.Settings;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private LocationManager locationManager;
    private DatabaseHelper dbHelper;
    private ArrayList<CheckPoint> list;
    private ArrayList<CheckPoint> list_markers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        dbHelper = new DatabaseHelper(this);
        list = new ArrayList<>();
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
//        // Add a marker in Sydney and move the camera
//        LatLng sydney = new LatLng(-34, 151);
//        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney").draggable(true));
//        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        // OnClick to add a mark
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(final LatLng latLng) {
                // New dialog for inputting description of a new location
                AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
                final EditText input = new EditText(MapsActivity.this);
                input.setSingleLine();
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                input.setHint("Enter a name of the point");

                builder.setView(input)

                // OK button
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String name = input.getText().toString();

                        mMap.addMarker(new MarkerOptions()
                                .position(latLng)
                                .title(name)
                                .draggable(true)
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));

                        // Add to database
                        String lat = String.valueOf(latLng.latitude);
                        String lng = String.valueOf(latLng.longitude);
                        DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss MM/dd/yyyy");
                        Date date = new Date();

                        dbHelper.addMarker(lat, lng, dateFormat.format(date), name);
                    }
                })

                // cancel button
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                })
                .show();



            }
        });
        centerOnMyLocation();
        setMarkers();
    }

    private void setMarkers() {
        list = dbHelper.getAllPoints();
        for (CheckPoint point : list) {
            LatLng pnt = new LatLng(Double.valueOf(point.getLat()), Double.valueOf(point.getLng()));
            mMap.addMarker(new MarkerOptions()
                    .position(pnt)
                    .title(point.getName())
                    .draggable(true)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA)));
        }
        list_markers = dbHelper.getAllMarkers();
        for (CheckPoint point : list_markers) {
            LatLng pnt = new LatLng((Double.valueOf(point.getLat())), Double.valueOf(point.getLng()));
            mMap.addMarker(new MarkerOptions()
                    .position(pnt)
                    .title(point.getName())
                    .draggable(true)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));
        }
    }

    private void centerOnMyLocation() {
        if ( (ContextCompat.checkSelfPermission( this, android.Manifest.permission.ACCESS_COARSE_LOCATION ) == PackageManager.PERMISSION_GRANTED) &&
                (ContextCompat.checkSelfPermission( this, android.Manifest.permission.ACCESS_FINE_LOCATION ) == PackageManager.PERMISSION_GRANTED)) {

            mMap.setMyLocationEnabled(true); // Enable my location
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) || locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                String lp = LocationManager.GPS_PROVIDER;
                Location loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                // fall back to network if GPS is not available
                if (loc == null) {
                    loc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    lp = LocationManager.NETWORK_PROVIDER;
                }
                if (loc != null) {
                    LatLng myLocation = new LatLng(loc.getLatitude(), loc.getLongitude());
//                    mMap.moveCamera(CameraUpdateFactory.newLatLng(myLocation));
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myLocation, 13));

                    CameraPosition cameraPosition = new CameraPosition.Builder()
                            .target(myLocation).zoom(16).build();
                    mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                }
//                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, locationListener);
//                else {
//                    locationManager.requestLocationUpdates(lp, 3000, 1, locationListener);
//                    showLocation(loc);
//                }

            } else {
                Toast.makeText(this, "Please open GPS services", Toast.LENGTH_SHORT).show();
                Intent i = new Intent();
                i.setAction(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(i);
            }

        }
    }
}

package com.example.jingyuan.locationandmap;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.provider.Settings;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.StreetViewPanoramaCamera;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private DatabaseHelper dbHelper;
    private ArrayList<CheckPoint> list;
    private ArrayList<CheckPoint> list_markers;
    private HashSet<String> popups;
    private PopupWindow popup;


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
        popup = null;
        popups = new HashSet<>();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(locationManager!=null){
            // Remove listener
            locationManager.removeUpdates(locationListener);
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

        mMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(Marker marker) {

            }

            @Override
            public void onMarkerDrag(Marker marker) {

            }

            @Override
            public void onMarkerDragEnd(Marker marker) {
                LatLng markerLatLng = marker.getPosition();

                String lat = String.format("%.6f", markerLatLng.latitude);
                String lng = String.format("%.6f", markerLatLng.longitude);
                String name = marker.getTitle();
                if ((int)marker.getTag() == 0) { // Check-ins
                    dbHelper.updatePoints("checkins", name, lat, lng);
                    // Refresh list
                    list.clear();
                    list = dbHelper.getAllPoints();
                } else {
                    dbHelper.updatePoints("markers", name, lat, lng);
                    // Refresh list_markers
                    list_markers.clear();
                    list_markers = dbHelper.getAllMarkers();
                }
            }
        });

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                Log.v("maps status", "on location changed");
                showPopup(location);
            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }

            @Override
            public void onProviderEnabled(String s) {

            }

            @Override
            public void onProviderDisabled(String s) {

            }
        };

        centerOnMyLocation();
        setMarkers();
    }

    private void showPopup(Location location) {
        final String name = compareDist(location.getLatitude(), location.getLongitude());
        // If there is a neighbor point, show popup
        if (name != null && popups.add(name)) {
            if (popup != null) popup.dismiss();

            Log.v("popup status", "popup window");
            View contentView = LayoutInflater.from(this).inflate(R.layout.popup_window, null);
            // Set popup text
            TextView tv = (TextView) contentView.findViewById(R.id.tv_popup);
            tv.setText("Near check-in point: " + name);
            TextView time = (TextView) contentView.findViewById(R.id.tv_poptime);
            time.setText("Last check-in: " + dbHelper.findTime(name));
            Button close = (Button) contentView.findViewById(R.id.button_popup);

            popup = new PopupWindow(contentView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            popup.setTouchable(true);
            popup.setAnimationStyle(android.R.style.Animation_Dialog);
            popup.setTouchInterceptor(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    Log.v("popup status", "on Touch");
                    return false;
                }
            });
            popup.setBackgroundDrawable(getResources().getDrawable(R.color.colorWhite));
            popup.showAtLocation(findViewById(android.R.id.content), Gravity.BOTTOM, 0, 0);
            close.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    popup.dismiss();
                    popups.remove(name);
                }
            });
        } else if (name == null){
            //TODO: If there is a popup window, close it
            if (popup != null) popup.dismiss();
            popups.clear();
        }
    }

    private String compareDist(double lat, double lng) {

        for (int i = 0; i < list.size(); i++)
            if (MainActivity.distance(lat, lng, Double.valueOf(list.get(i).getLat()), Double.valueOf(list.get(i).getLng())) < 30)
                return list.get(i).getName();
        for (int i = 0; i < list_markers.size(); i++)
            if (MainActivity.distance(lat, lng, Double.valueOf(list_markers.get(i).getLat()), Double.valueOf(list_markers.get(i).getLng())) < 30)
                return list_markers.get(i).getName();
        return null;
    }

    private void setMarkers() {
        // Get check-in points
        list = dbHelper.getAllPoints();
        if (list.size() != 0)
        for (CheckPoint point : list) {
            LatLng pnt = new LatLng(Double.valueOf(point.getLat()), Double.valueOf(point.getLng()));
            Marker mCheckin = mMap.addMarker(new MarkerOptions()
                    .position(pnt)
                    .title(point.getName())
                    .draggable(true)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA)));
            mCheckin.setTag(0);
        }
        // Get markers
        list_markers = dbHelper.getAllMarkers();
        if (list_markers.size() != 0)
        for (CheckPoint point : list_markers) {
            Log.v("maps status", "markers size" + list_markers.size());
            Log.v("maps status", "point" + point.getName());
            Log.v("maps status", "point" + point.getLat());
            LatLng pnt = new LatLng((Double.valueOf(point.getLat())), Double.valueOf(point.getLng()));
            Marker mMarker = mMap.addMarker(new MarkerOptions()
                    .position(pnt)
                    .title(point.getName())
                    .draggable(true)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));
            mMarker.setTag(1);
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
                locationManager.requestLocationUpdates(lp, 300000, 1, locationListener);
            } else {
                Toast.makeText(this, "Please open GPS services", Toast.LENGTH_SHORT).show();
                Intent i = new Intent();
                i.setAction(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(i);
            }

        }
    }
}

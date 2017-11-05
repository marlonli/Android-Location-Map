package com.example.jingyuan.locationandmap;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private final int LOC_PERMISSION_REQUEST_CODE = 1;
    private final int FINE_LOC_PERMISSION_REQUEST_CODE = 2;
    private TextView locationTV;
    private TextView addressTV;
    private LocationManager locationManager;
    private Button checkIn;
    private LocationListener locationListener;
    private ListView listView;
    private List<CheckPoint> list;
    private mAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initialization();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(locationManager!=null){
            // Remove listener
            locationManager.removeUpdates(locationListener);
        }
    }

    private void initialization() {
        // Initialization
        locationTV = (TextView) findViewById(R.id.tv_position);
        addressTV = (TextView) findViewById(R.id.tv_address);
        listView = (ListView) findViewById(R.id.lv_checkins);
        list = new ArrayList<>();
        checkIn = (Button) findViewById(R.id.button);
        list.add(new CheckPoint("30", "120", "12:12:12", "400 Plymouth Place, Somerset, New Jersey 08873"));
        adapter = new mAdapter(this, list);
        listView.setAdapter(adapter);

        // Get location manager
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                showLocation(location);
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

        checkIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                if ( (ContextCompat.checkSelfPermission( MainActivity.this, android.Manifest.permission.ACCESS_COARSE_LOCATION ) == PackageManager.PERMISSION_GRANTED) &&
//                        (ContextCompat.checkSelfPermission( MainActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION ) == PackageManager.PERMISSION_GRANTED)) {
//
//                    if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) || locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
//                        String lp = LocationManager.GPS_PROVIDER;
//                        Location loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
//                        // fall back to network if GPS is not available
//                        if (loc == null) {
//                            loc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
//                            lp = LocationManager.NETWORK_PROVIDER;
//                        }
//                        if (loc != null) {
//                            showLocation(loc);
//                        }
//                    }
//                }
                String[] coor = locationTV.getText().toString().split(",");
                DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss MM/dd/yyyy");
                Date date = new Date();

                CheckPoint cp = new CheckPoint(coor[0].replace("Current Location: (", ""),
                        coor[1].replace(")", ""), dateFormat.format(date), addressTV.getText().toString().replace("Current Address: ", ""));
                list.add(cp);
                adapter.notifyDataSetChanged();
            }
        });


        // Get permission
        if ( (ContextCompat.checkSelfPermission( this, android.Manifest.permission.ACCESS_COARSE_LOCATION ) != PackageManager.PERMISSION_GRANTED) || (
                ContextCompat.checkSelfPermission( this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) ) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
                Toast.makeText(this, "Permission denied! Check settings", Toast.LENGTH_SHORT).show();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION}, LOC_PERMISSION_REQUEST_CODE);
            }
        } else {
            updateLocation();
        }

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, locationListener);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        Log.v("Permission status", "on Request permissions result");

        if (requestCode == LOC_PERMISSION_REQUEST_CODE) {
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.v("Permission status", " FINE Permission authorized");
                updateLocation();
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
                Log.v("Permission status", "FINE Permission denied");
            }
        }
    }

    private void showLocation(Location location) {
        // Get coordinates
        double myLat = location.getLatitude();
        double myLng = location.getLongitude();
        // transfer to String
        String lat = String.format("%.3f", myLat);
        String lng = String.format("%.3f", myLng);
        // Show coordinates
        locationTV.setText("Current Location: (" + lat + "," + lng + ")");
        Geocoder geocoder = new Geocoder(this);
        // Get and show address
        try {
            List<Address> addresses = geocoder.getFromLocation(myLat, myLng, 1);
            if (addresses.size()!= 0) {
                Address address = addresses.get(0);
                StringBuilder sb = new StringBuilder();
                if (address.getSubThoroughfare() != null) sb.append(address.getSubThoroughfare()).append(", ");
                if (address.getThoroughfare() != null) sb.append(address.getThoroughfare()).append(", ");
                if (address.getLocality() != null) sb.append(address.getLocality()).append(", ");
                if (address.getAdminArea() != null) sb.append(address.getAdminArea()).append(" ");
                if (address.getPostalCode() != null) sb.append(address.getPostalCode());
                addressTV.setText("Current Address: " + sb.toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void updateLocation() {
        // Check permission
        if ( (ContextCompat.checkSelfPermission( this, android.Manifest.permission.ACCESS_COARSE_LOCATION ) == PackageManager.PERMISSION_GRANTED) &&
                (ContextCompat.checkSelfPermission( this, android.Manifest.permission.ACCESS_FINE_LOCATION ) == PackageManager.PERMISSION_GRANTED)) {

            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) || locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                String lp = LocationManager.GPS_PROVIDER;
                Location loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                // fall back to network if GPS is not available
                if (loc == null) {
                    loc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    lp = LocationManager.NETWORK_PROVIDER;
                }
                if (loc != null) {
                    showLocation(loc);
                }
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

        } else {
            Toast.makeText(this, "Permission Denied!", Toast.LENGTH_SHORT);
        }

    }
}

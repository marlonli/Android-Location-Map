package com.example.jingyuan.locationandmap;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
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
    private TextView locationTV;
    private TextView addressTV;
    private LocationManager locationManager;
    private Button checkIn;
    private Button mapBtn;
    private LocationListener locationListener;
    private ListView listView;
    private EditText nameET;
    private List<CheckPoint> list;
    private mAdapter adapter;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initialization();
        Log.v("activity status", "on create");

    }

    @Override
    protected void onResume() {
        super.onResume();

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
        nameET = (EditText) findViewById(R.id.et_name);
        listView = (ListView) findViewById(R.id.lv_checkins);
        mapBtn = (Button) findViewById(R.id.button_map);

        list = new ArrayList<>();
        dbHelper = new DatabaseHelper(this);
        checkIn = (Button) findViewById(R.id.button_checkin);
        list.addAll(dbHelper.getAllPoints());
//        list.add(new CheckPoint("30", "120", "12:12:12", "400 Plymouth Place, Somerset, New Jersey 08873"));

        adapter = new mAdapter(this, list);
        listView.setAdapter(adapter);

        // Get location manager
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                Log.v("location status", "on location changed");
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
                String[] coor = locationTV.getText().toString().split(",");
                DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss MM/dd/yyyy");
                Date date = new Date();

                CheckPoint cp = new CheckPoint(nameET.getText().toString(),
                        coor[0].replace("Current Location: (", ""),
                        coor[1].replace(")", ""),
                        dateFormat.format(date),
                        addressTV.getText().toString().replace("Current Address: ", ""));

                Log.v("event status", "check in clicked " + dateFormat.format(date));
                ArrayList<CheckPoint> neighbors = compareDist(cp);
                if (neighbors == null || neighbors.size() == 0) { // if no check in point which dist < 30m to cp
                    // add new check in point to database, refresh the list
                    dbHelper.addPoint(cp);
                    list.clear();
                    list.addAll(dbHelper.getAllPoints());
                    adapter.notifyDataSetChanged();
                } else { // if there is a check in point near cp (dist < 30m)
                    dbHelper.addAssociate(cp, neighbors);
                    Toast.makeText(MainActivity.this, "Associated to other points", Toast.LENGTH_SHORT).show();
                }

            }
        });

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {

                dbHelper.deletePoint(list.get(i).getId());
                list.clear();
                list.addAll(dbHelper.getAllPoints());
                adapter.notifyDataSetChanged();
                return true;
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

        mapBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, MapsActivity.class);
                startActivity(intent);
            }
        });
    }

    // return the position of check in point which dist < 30m
    private ArrayList<CheckPoint> compareDist(CheckPoint cp) {
        ArrayList<CheckPoint> neighbors = new ArrayList<>();
        for (int i = 0; i < list.size(); i++)
            if (distance(Double.valueOf(cp.getLat()), Double.valueOf(cp.getLng()), Double.valueOf(list.get(i).getLat()), Double.valueOf(list.get(i).getLng())) < 30)
                neighbors.add(list.get(i));

        return neighbors;
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
        String lat = String.format("%.6f", myLat);
        String lng = String.format("%.6f", myLng);
        // Show coordinates
        locationTV.setText("Current Location: (" + lat + "," + lng + ")");
        Log.v("location status", "show lat, lng");
        Geocoder geocoder = new Geocoder(this);
        List<Address> addresses = new ArrayList<>();
        // Get and show address
        try {

            addresses.addAll(geocoder.getFromLocation(myLat, myLng, 1));

        } catch (IOException e) {
            e.printStackTrace();
        }

        if (addresses!= null && addresses.size() != 0) {
            Log.v("location status", "show address");
            Address address = addresses.get(0);
            StringBuilder sb = new StringBuilder();
            if (address.getSubThoroughfare() != null) sb.append(address.getSubThoroughfare()).append(", ");
            if (address.getThoroughfare() != null) sb.append(address.getThoroughfare()).append(", ");
            if (address.getLocality() != null) sb.append(address.getLocality()).append(", ");
            if (address.getAdminArea() != null) sb.append(address.getAdminArea()).append(" ");
            if (address.getPostalCode() != null) sb.append(address.getPostalCode());
            addressTV.setText("Current Address: " + sb.toString());

        }
    }

    public void updateLocation() {
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
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 6000, 1, locationListener);
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

    private double distance(double lat1, double lng1, double lat2, double lng2) {
        double a, b, R;
        R = 6378137; // Radius of earth
        lat1 = lat1 * Math.PI / 180.0;
        lat2 = lat2 * Math.PI / 180.0;
        a = lat1 - lat2;
        b = (lng1 - lng2) * Math.PI / 180.0;
        double d;
        double sa2, sb2;
        sa2 = Math.sin(a / 2.0);
        sb2 = Math.sin(b / 2.0);
        d = 2 * R * Math.asin(Math.sqrt(sa2 * sa2 + Math.cos(lat1)
                * Math.cos(lat2) * sb2 * sb2));
        return d;
    }
}

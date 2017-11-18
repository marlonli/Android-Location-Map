package com.example.jingyuan.locationandmap;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Looper;
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
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class MainActivity extends AppCompatActivity {

    private final int LOC_PERMISSION_REQUEST_CODE = 1;
    private final String UPDATE_CHECKIN_FROM_SERVICE = "autoCI";
    private final int MODE_AUTO = 10;
    private final int MODE_GPS = 11;
    private final int MODE_NETWORK = 12;
    private boolean MODE_RADAR_ON = false;
    private TextView locationTV;
    private TextView addressTV;
    private LocationManager locationManager;
    private Button checkIn;
    private Button mapBtn;
    private Button update;
    private Switch autoCheckin;
    private Switch radar;
    private LocationListener locationListener;
    private ListView listView;
    private EditText nameET;
    private static List<CheckPoint> list;
    private mAdapter adapter;
    private DatabaseHelper dbHelper;
    private AutocheckReceiver receiver;
    private final Lock lock = new ReentrantLock();
    private int locMode = MODE_AUTO;
    private Spinner spinner;
    private long currT;
    private WifiManager wifiManager;
    private WifiReceiver receiverWifi;
    private List<ScanResult> scanResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initialization();

        // Get thread ID
//        Log.v("thread status", "UI thread" + android.os.Process.getThreadPriority(android.os.Process.myTid()));
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            Log.v("thread status", "UI thread" + android.os.Process.getThreadPriority(android.os.Process.myTid()));
        }

        // Listening broadcast
        IntentFilter filter = new IntentFilter();
        receiver = new AutocheckReceiver();
        filter.addAction(UPDATE_CHECKIN_FROM_SERVICE);
        registerReceiver(receiver, filter);
        Log.v("activity status", "on create");

    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.v("activity status", "on resume");
//        // Refresh list
//        list.clear();
//        list = dbHelper.getAllPoints();
//        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        removeLocationListener();
        if (receiver != null)
            unregisterReceiver(receiver);
        if (receiverWifi != null)
            unregisterReceiver(receiverWifi);
    }

    private void initialization() {

        // Initialization
        locationTV = (TextView) findViewById(R.id.tv_position);
        addressTV = (TextView) findViewById(R.id.tv_address);
        nameET = (EditText) findViewById(R.id.et_name);
        listView = (ListView) findViewById(R.id.lv_checkins);
        mapBtn = (Button) findViewById(R.id.button_map);
        update = (Button) findViewById(R.id.button_update);
        autoCheckin = (Switch) findViewById(R.id.switch_autocheck);
        spinner = (Spinner) findViewById(R.id.spinner_mode);
        radar = (Switch) findViewById(R.id.switch_RADAR);
        wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        currT = 0;


        scanResult = new ArrayList<>();
        list = new ArrayList<>();
        dbHelper = new DatabaseHelper(this);
        checkIn = (Button) findViewById(R.id.button_checkin);
        list.addAll(dbHelper.getAllPoints());

        adapter = new mAdapter(this, list);
        listView.setAdapter(adapter);

        // Get location manager
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                Log.v("location status", "on location changed");

                Log.v("delay status", "location changed" + (System.currentTimeMillis() - currT)/1000000.0);
                showLocation(location);
                // Get thread ID
                Log.v("thread status", "locationListener thread " + android.os.Process.getThreadPriority(android.os.Process.myTid()));
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
                // Scan wifis and record strength
//                radarLocation();
                String name = nameET.getText().toString();

                String[] coor = locationTV.getText().toString().split(",");
                DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss MM/dd/yyyy");
                Date date = new Date();

                CheckPoint cp = new CheckPoint(name,
                        coor[0].replace("Current Location: (", ""),
                        coor[1].replace(")", ""),
                        dateFormat.format(date),
                        addressTV.getText().toString().replace("Current Address: ", ""));

                Log.v("event status", "check in clicked " + dateFormat.format(date));
                ArrayList<CheckPoint> neighbors = compareDist(cp);
//
                // Not RADAR Mode, store wifi infomation
                if (!MODE_RADAR_ON) {
                    if (scanResult != null && scanResult.size() > 0) {
                        cp.setSignalName1(scanResult.get(0).SSID);
                        cp.setSignalStrength1(scanResult.get(0).level);
                    }
                    if (scanResult.size() > 1){
                        cp.setSignalName2(scanResult.get(1).SSID);
                        cp.setSignalStrength2(scanResult.get(1).level);
                    }
                    if (neighbors == null || neighbors.size() == 0) { // if no check in point which dist < 30m to cp
                        // add new check in point to database, refresh the list
                        dbHelper.addPoint(cp);
                        adapter.notifyDataSetChanged();
                    } else { // if there is a check in point near cp (dist < 30m)
                        dbHelper.addAssociate(cp, neighbors.get(0));
                        Toast.makeText(MainActivity.this, "Associated to other points", Toast.LENGTH_SHORT).show();
                    }

                    list.clear();
                    list.addAll(dbHelper.getAllPoints());
                    adapter.notifyDataSetChanged();
                } else { // RADAR mode
                    CheckPoint closest = dbHelper.findClosest(scanResult);
                    if (closest != null) {
                        Toast.makeText(MainActivity.this, "Found the closest fit!", Toast.LENGTH_SHORT).show();
                        closest.setName(name);
                        closest.setTime(dateFormat.format(date));
                        dbHelper.addPoint(closest);
                        list.clear();
                        list.addAll(dbHelper.getAllPoints());
                        adapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(MainActivity.this, "Closest fit not found!", Toast.LENGTH_SHORT).show();
                    }

                }
            }
        });

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                Log.v("spinner status", "Selected " + i);
                if (i == 0)
                    locMode = MODE_AUTO;
                else if (i == 1)
                    locMode = MODE_GPS;
                else
                    locMode = MODE_NETWORK;
                updateLocation();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

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

        // Get permission and update location
        if ( (ContextCompat.checkSelfPermission( this, android.Manifest.permission.ACCESS_COARSE_LOCATION ) != PackageManager.PERMISSION_GRANTED) || (
                ContextCompat.checkSelfPermission( this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) ||
                (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_WIFI_STATE) != PackageManager.PERMISSION_GRANTED) ||
                (ContextCompat.checkSelfPermission(this, Manifest.permission.CHANGE_WIFI_STATE) != PackageManager.PERMISSION_GRANTED)){

            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
                Toast.makeText(this, "Permission denied! Check settings", Toast.LENGTH_SHORT).show();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_WIFI_STATE,Manifest.permission.CHANGE_WIFI_STATE}, LOC_PERMISSION_REQUEST_CODE);
            }
        } else {
            updateLocation();
            radarLocation();
        }

        // start maps activity
        mapBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, MapsActivity.class);
                intent.putExtra("mode", locMode);
                startActivity(intent);
            }
        });

        autoCheckin.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                Intent intent = new Intent(MainActivity.this, AutocheckService.class);
                if (isChecked){
                    if (receiverWifi != null)
                        unregisterReceiver(receiverWifi);
                    startService(intent);
                }
                else
                    stopService(intent);
            }
        });

        radar.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked) {
                    MODE_RADAR_ON = true;
                    updateLocation();
                }
                else
                    MODE_RADAR_ON = false;
            }
        });

        update.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                currT = System.currentTimeMillis();
                updateLocation();
            }
        });
    }

    // return the position of check in point which dist < 30m
    protected static ArrayList<CheckPoint> compareDist(CheckPoint cp) {
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
        float accuracy = location.getAccuracy();
        Log.v("test accuracy", "location accuracy: " + accuracy);
//        Toast.makeText(this, "Accuracy: " + accuracy, Toast.LENGTH_LONG).show();

        // transfer to String
        String lat = String.format("%.6f", myLat);
        String lng = String.format("%.6f", myLng);
        // Show coordinates
        locationTV.setText("Current Location: (" + lat + "," + lng + ")");
        Geocoder geocoder = new Geocoder(this);
        List<Address> addresses = new ArrayList<>();
        // Get and show address
        try {

            addresses.addAll(geocoder.getFromLocation(myLat, myLng, 1));

        } catch (IOException e) {
            e.printStackTrace();
        }

        // Lock the location update process to solve race condition
        if (lock.tryLock()) {
            try {
                // Show coordinates
                locationTV.setText("Current Location: (" + lat + "," + lng + ")");
                if (addresses!= null && addresses.size() != 0) {
                    Address address = addresses.get(0);
                    StringBuilder sb = new StringBuilder();
                    if (address.getSubThoroughfare() != null) sb.append(address.getSubThoroughfare()).append(", ");
                    if (address.getThoroughfare() != null) sb.append(address.getThoroughfare()).append(", ");
                    if (address.getLocality() != null) sb.append(address.getLocality()).append(", ");
                    if (address.getAdminArea() != null) sb.append(address.getAdminArea()).append(" ");
                    if (address.getPostalCode() != null) sb.append(address.getPostalCode());
                    addressTV.setText("Current Address: " + sb.toString());
                }
            } finally {
                lock.unlock();
            }
        }
    }

    public void updateLocation() {
        // Check permission
        if ( (ContextCompat.checkSelfPermission( this, android.Manifest.permission.ACCESS_COARSE_LOCATION ) == PackageManager.PERMISSION_GRANTED) &&
                (ContextCompat.checkSelfPermission( this, android.Manifest.permission.ACCESS_FINE_LOCATION ) == PackageManager.PERMISSION_GRANTED)) {

            if (!MODE_RADAR_ON && (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) || locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))) {
                Location loc = null;
                // if AUTO or GPS
                if (locMode != MODE_NETWORK)
                    loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                // fall back to network if GPS is not available
                if (loc == null) {
                    // GPS mode
                    if (locMode == MODE_GPS) {
                        Toast.makeText(this, "Please open GPS services", Toast.LENGTH_SHORT).show();
                        Intent i = new Intent();
                        i.setAction(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(i);
                    } else { // Network or AUTO
                        Log.v("location status", "GPS not available!");
                        loc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    }
                }
                if (loc != null) {
                    showLocation(loc);
                } else
                    Toast.makeText(this, "Location not available", Toast.LENGTH_SHORT).show();
                // Add location update listener
                if (locMode != MODE_NETWORK){
                    removeLocationListener();
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000, 1, locationListener);
                }
                else{
                    removeLocationListener();
                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 3000, 1, locationListener);
                }

            } else if (MODE_RADAR_ON) {
                // RADAR Mode
                removeLocationListener();
//                showLocation(loc);
            } else {
                    Toast.makeText(this, "Please open GPS services", Toast.LENGTH_SHORT).show();
                    Intent i = new Intent();
                    i.setAction(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(i);
                }

        } else {
            Toast.makeText(this, "Permission Denied!", Toast.LENGTH_SHORT).show();
        }

    }

    private void radarLocation() {

        wifiManager.startScan();
        Toast.makeText(this, "Start scanning Wifi", Toast.LENGTH_LONG).show();
        receiverWifi = new WifiReceiver();
        registerReceiver(receiverWifi, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

    }

    public static double distance(double lat1, double lng1, double lat2, double lng2) {
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

    public void removeLocationListener() {
        if(locationManager!=null){
            // Remove listener
            locationManager.removeUpdates(locationListener);
        }
    }

    private class AutocheckReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.v("service status", "received broadcast");
            String action = intent.getAction();
            if (action.equals(UPDATE_CHECKIN_FROM_SERVICE)) {
                list.clear();
                list.addAll(dbHelper.getAllPoints());
                adapter.notifyDataSetChanged();
            }
        }
    }

    class WifiReceiver extends BroadcastReceiver {
        public void onReceive(Context c, Intent intent) {
            // Get the first 3 strong signal wifis
            if ( ContextCompat.checkSelfPermission( MainActivity.this, android.Manifest.permission.ACCESS_COARSE_LOCATION ) != PackageManager.PERMISSION_GRANTED)
                   {
                if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
                    Toast.makeText(MainActivity.this, "Permission denied! Check settings", Toast.LENGTH_SHORT).show();
                } else {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, LOC_PERMISSION_REQUEST_CODE);
                }
            }

            PriorityQueue<ScanResult> queue = new PriorityQueue<>(3, new Comparator<ScanResult>() {
                @Override
                public int compare(ScanResult scanResult, ScanResult t1) {
                    return  t1.level - scanResult.level;
                }
            });
            List<ScanResult> wifiList = wifiManager.getScanResults();
            queue.addAll(wifiList);

            // The wifilist orderd by signal strength
            scanResult.clear();
            scanResult.addAll(queue);

//            if (scanResult != null && scanResult.size() > 0) {
//                latest.setSignalName1(scanResult.get(0).SSID);
//                latest.setSignalStrength1(scanResult.get(0).level);
//            }
//            if (scanResult.size() > 1){
//                latest.setSignalName2(scanResult.get(1).SSID);
//                latest.setSignalStrength2(scanResult.get(1).level);
//            }
//
//            // Update database
//            dbHelper.addSignalInfo(latest);

            for (int i = 0; i < scanResult.size(); i++) {
                ScanResult result = scanResult.get(i);
                Log.v("wifi", "id: " + result.SSID +  " strength: " + result.level);
            }

        }
    }
}

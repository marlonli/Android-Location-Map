package com.example.jingyuan.locationandmap;

import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Handler;

import static android.R.id.list;

public class AutocheckService extends Service {
    private final String UPDATE_CHECKIN_FROM_SERVICE = "autoCI";
    private LocationManager locationManager;
    private LocationListener locationListener;
    private DatabaseHelper dbHelper;
    private static Looper threadLooper = null;

    public AutocheckService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        // New thread for auto check-in
        Thread thread = new Thread(new Runnable() { public void run() {
            Looper.prepare();
            Log.v("service status", "Thread loop start");

            dbHelper = new DatabaseHelper(AutocheckService.this);
            locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            locationListener = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {

                    // Get lat, lng
                    Double lat = location.getLatitude();
                    Double lng = location.getLongitude();
                    // Get time and date
                    DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss MM/dd/yyyy");
                    Date date = new Date();
                    String addr = null;

                    // Get address
                    Geocoder geocoder = new Geocoder(AutocheckService.this);
                    List<Address> addresses = new ArrayList<>();
                    try {
                        addresses.addAll(geocoder.getFromLocation(lat, lng, 1));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if (addresses!= null && addresses.size() != 0) {
                        Log.v("service status", "show address");
                        Address address = addresses.get(0);
                        StringBuilder sb = new StringBuilder();
                        if (address.getSubThoroughfare() != null) sb.append(address.getSubThoroughfare()).append(", ");
                        if (address.getThoroughfare() != null) sb.append(address.getThoroughfare()).append(", ");
                        if (address.getLocality() != null) sb.append(address.getLocality()).append(", ");
                        if (address.getAdminArea() != null) sb.append(address.getAdminArea()).append(" ");
                        if (address.getPostalCode() != null) sb.append(address.getPostalCode());
                        addr = sb.toString();
                    }

                    // Create a check point
                    CheckPoint cp = new CheckPoint("AutoCheck " + dateFormat.format(date),
                            String.format("%.6f", lat),
                            String.format("%.6f", lng),
                            dateFormat.format(date),
                            addr);

                    Log.v("service status", "auto checkin" + dateFormat.format(date));
                    ArrayList<CheckPoint> neighbors = MainActivity.compareDist(cp);
                    if (neighbors == null || neighbors.size() == 0) { // if no check in point which dist < 30m to cp
                        // add new check in point to database, refresh the list
                        dbHelper.addPoint(cp);
                    } else { // if there is a check in point near cp (dist < 30m)
                        dbHelper.addAssociate(cp, neighbors.get(0));
                    }

                    // broadcast to update the list
                    Intent update = new Intent();
                    update.setAction(UPDATE_CHECKIN_FROM_SERVICE);
                    sendBroadcast(update);
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
            autoCheckin();
            threadLooper = Looper.myLooper();
            Looper.loop();

            if (locationManager != null)
                locationManager.removeUpdates(locationListener);
        } });
        thread.start();


        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // End the thread when stop service
        threadLooper.quit();
        Log.v("service status", "on destroy");
    }

    private void autoCheckin() {
        if ( (ContextCompat.checkSelfPermission( this, android.Manifest.permission.ACCESS_COARSE_LOCATION ) == PackageManager.PERMISSION_GRANTED) &&
                (ContextCompat.checkSelfPermission( this, android.Manifest.permission.ACCESS_FINE_LOCATION ) == PackageManager.PERMISSION_GRANTED)) {

            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                Log.v("service status", "GPS provider");
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 300000, 100, locationListener); // update every 5min or 100m
            }
            else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                Log.v("service status", "Network provider");
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 300000, 100, locationListener);
            }

            else {
                Intent i = new Intent();
                i.setAction(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(i);
            }

        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

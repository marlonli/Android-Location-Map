package com.example.jingyuan.locationandmap;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.wifi.ScanResult;
import android.util.Log;
import android.widget.ArrayAdapter;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

/**
 * Created by jingyuan on 11/5/17.
 */

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE = "checkins";
    private static final String ASSOCIATE_DATABASE = "associates";
    private static final String MARKER_DATABASE = "markers";
    private static final String SIGNAL_DATABASE = "signal";
    private static final String KEY_NAME = "name";
    private static final String KEY_LAT = "latitude";
    private static final String KEY_LNG = "longitude";
    private static final String KEY_TIME = "time";
    private static final String KEY_ADDRESS = "address";
    private static final String KEY_CHECKINID = "checkinID";
    private static final String KEY_STRENGTH1 = "strength1";
    private static final String KEY_SIGNAL_NAME1 = "signalNames1";
    private static final String KEY_STRENGTH2 = "strength2";
    private static final String KEY_SIGNAL_NAME2 = "signalNames2";
    private static final String DELETE_QUERY = "DELETE FROM "+ DATABASE +" WHERE _id=";
    private static final String DELETE_QUERY_ASSO = "DELETE FROM "+ ASSOCIATE_DATABASE +" WHERE assID=";

    public DatabaseHelper(Context context) {
        super(context, DATABASE, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        String createTable = "CREATE TABLE "+ DATABASE +" (_id INTEGER PRIMARY KEY AUTOINCREMENT, " + KEY_LAT + " TEXT, "+
                KEY_LNG +" TEXT, "+ KEY_TIME +" TEXT, "+ KEY_ADDRESS +" TEXT, " + KEY_NAME + " TEXT, " + KEY_SIGNAL_NAME1 +
                " TEXT, " + KEY_STRENGTH1 + " FLOAT, "+ KEY_SIGNAL_NAME2 +
                " TEXT, " + KEY_STRENGTH2 + " FLOAT " +")";
        String createAssociate = "CREATE TABLE " + ASSOCIATE_DATABASE + " (assID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                KEY_CHECKINID + " TEXT, " + KEY_LAT + " TEXT, " + KEY_LNG + " TEXT, " + KEY_TIME + " TEXT)";
        String createMarkers = "CREATE TABLE " + MARKER_DATABASE + " (markerID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                KEY_LAT + " TEXT, " + KEY_LNG + " TEXT, " + KEY_TIME + " TEXT, " + KEY_NAME + " TEXT)";
        sqLiteDatabase.execSQL(createTable);
        sqLiteDatabase.execSQL(createAssociate);
        sqLiteDatabase.execSQL(createMarkers);
        Log.v("database status", "on create");
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + DATABASE);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + ASSOCIATE_DATABASE);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + MARKER_DATABASE);
//        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + SIGNAL_DATABASE);
        onCreate(sqLiteDatabase);
        Log.v("database status", "on upgrade");
    }

    public void addPoint(CheckPoint point) {
        Log.v("database status", "add point");
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_LAT, point.getLat());
        values.put(KEY_LNG, point.getLng());
        values.put(KEY_TIME, point.getTime());
        values.put(KEY_ADDRESS, point.getAddress());
        values.put(KEY_NAME, point.getName());
        values.put(KEY_SIGNAL_NAME1, point.getSignalName1());
        values.put(KEY_STRENGTH1, point.getSignalStrength1());
        values.put(KEY_SIGNAL_NAME2, point.getSignalName2());
        values.put(KEY_STRENGTH2, point.getSignalStrength2());
        db.insert(DATABASE, null, values);
        db.close();
    }

    public void deletePoint(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        if (id < 100)
            db.execSQL(DELETE_QUERY + id);
        else
            db.execSQL(DELETE_QUERY_ASSO + (id - 100));
        db.close();
    }

    public ArrayList<CheckPoint> getAllPoints() {
        SQLiteDatabase db = this.getReadableDatabase();
        ArrayList<CheckPoint> result = new ArrayList<>();
        Hashtable<String, String> id_nameaddr = new Hashtable<>();

        Cursor cursor = db.query(DATABASE,null,null,null,null,null,null);
        Log.v("database status", "get all points" + cursor.getCount());
        while (cursor.moveToNext())
        {
            int id = cursor.getInt(0);
            Log.v("database status", "id" + id);
            String lat = cursor.getString(1);
            Log.v("database status", "lat" + lat);
            String lng = cursor.getString(2);
            Log.v("database status", "lng" + lng);
            String time = cursor.getString(3);
            Log.v("database status", "time" + time);
            String addr = cursor.getString(4);
            Log.v("database status", "addr" + addr);
            String name = cursor.getString(5);
            Log.v("database status", "name" + name);
            String sname1 = cursor.getString(6);
            float strength1 = cursor.getFloat(7);
            String sname2 = cursor.getString(8);
            float strength2 = cursor.getFloat(9);
            id_nameaddr.put(String.valueOf(id), name + ":" + addr);
            CheckPoint point = new CheckPoint(id, name, lat, lng, time, addr, sname1, strength1, sname2, strength2);
            result.add(point);
        }
        cursor = db.query(ASSOCIATE_DATABASE,null,null,null,null,null,null);
        while (cursor.moveToNext())
        {
            int id = cursor.getInt(0);
            Log.v("database status", "id" + id);
            String checkinID = cursor.getString(1);
            Log.v("database status", "checkin id" + checkinID);
            String lat = cursor.getString(2);
            Log.v("database status", "lat" + lat);
            String lng = cursor.getString(3);
            Log.v("database status", "lng" + lng);
            String time = cursor.getString(4);
            Log.v("database status", "time" + time);
            String[] nameaddr = {"",""};
            if (id_nameaddr.get(checkinID) != null)
                nameaddr = id_nameaddr.get(checkinID).split(":");

            CheckPoint point = new CheckPoint(id + 100, nameaddr[0], lat, lng, time, nameaddr[1]);
            result.add(point);
        }
        db.close();
        return result;
    }

    public void addAssociate(CheckPoint cp, CheckPoint checkPoint) {
        Log.v("database status", "add associate");
        int id = checkPoint.getId();
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_LAT, cp.getLat());
        values.put(KEY_LNG, cp.getLng());
        values.put(KEY_TIME, cp.getTime());
        values.put(KEY_CHECKINID, id);
        db.insert(ASSOCIATE_DATABASE, null, values);
        db.close();
    }

    public void addMarker(String lat, String lng, String time, String name) {
        Log.v("database status", "add marker");
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_LAT, lat);
        values.put(KEY_LNG, lng);
        values.put(KEY_TIME, time);
        values.put(KEY_NAME, name);
        db.insert(MARKER_DATABASE, null, values);
        db.close();
    }

    public void addSignal(String name, float strength) {
        Log.v("database status", "add signals");
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_SIGNAL_NAME1, name);
        values.put(KEY_STRENGTH1, strength);
        String replaceOrInsert = "replace into student( _id , name ,age ) VALUES ( 1,'zz7zz7zz',35)";
        db.insert(SIGNAL_DATABASE, null, values);
        db.close();
    }

    public void updateSignals(String name, float strength) {
        SQLiteDatabase db = this.getWritableDatabase();
        String updateDB = "UPDATE " + SIGNAL_DATABASE + " SET "  + KEY_STRENGTH1 + "='" + strength + "' WHERE " +  KEY_SIGNAL_NAME1 +"='" + name + "'";
        db.execSQL(updateDB);
        db.close();
    }

    public ArrayList<CheckPoint> getAllMarkers() {
        SQLiteDatabase db = this.getReadableDatabase();
        ArrayList<CheckPoint> result = new ArrayList<>();

        Cursor cursor = db.query(MARKER_DATABASE, null, null, null, null, null, null);
        Log.v("database status", "get all markers");
        while (cursor.moveToNext()) {
            Log.v("database status", "id " + cursor.getString(0));
            int id = cursor.getInt(0);
            String lat = cursor.getString(1);
            Log.v("database status", "lat" + lat);
            String lng = cursor.getString(2);
            Log.v("database status", "lng" + lng);
            String time = cursor.getString(3);
            Log.v("database status", "time" + time);
            String name = cursor.getString(4);
            Log.v("database status", "name" + name);

            CheckPoint marker = new CheckPoint(id, name, lat, lng, time);
            result.add(marker);
        }
        db.close();
        return result;
    }

    public void updatePoints(String type, String name, String lat, String lng) {
        SQLiteDatabase db = this.getWritableDatabase();
        String dbName = null;
        String ID = null;
        if ("checkins".equals(type)) {
            dbName = DATABASE;
            ID = "_id";
        }

        else {
            dbName = MARKER_DATABASE;
            ID = "markerID";
        }

        String updateDB = "UPDATE " + dbName + " SET " + KEY_LAT + "='" + lat + "', " + KEY_LNG + "='" + lng + "' WHERE " +  KEY_NAME +"='" + name + "'";
        db.execSQL(updateDB);
        db.close();
    }

    public String findTime(String name) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(DATABASE, new String[] {KEY_TIME}, KEY_NAME + " = ?", new String[] {name}, null, null, null);
        if (cursor.moveToFirst()) return cursor.getString(0);
        else {
            cursor = db.query(MARKER_DATABASE, new String[] {KEY_TIME}, KEY_NAME + " = ?", new String[] {name}, null, null, null);
            if (cursor.moveToFirst()) return cursor.getString(0);
        }
        db.close();
        return null;
    }

    public void addSignalInfo(CheckPoint latest) {
        SQLiteDatabase db = getWritableDatabase();
        String name = latest.getName();
        String sname1 = latest.getSignalName1();
        String sname2 = latest.getSignalName2();
        float strength1 = latest.getSignalStrength1();
        float strength2 = latest.getSignalStrength2();

        String updateDB = "UPDATE " + DATABASE + " SET " + KEY_SIGNAL_NAME1 + "='" + sname1 + "', " + KEY_STRENGTH1 + "=" + strength1 + ", " + KEY_SIGNAL_NAME2
                 + "='" + sname2 + "', " + KEY_STRENGTH2 + "=" + strength2 + " WHERE " + KEY_NAME + "='" + name + "' ";
        db.execSQL(updateDB);
        db.close();
    }

    public CheckPoint findClosest(List<ScanResult> scanResult) {
        int id = -1;
        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = db.query(DATABASE, new String[] {"_id"}, KEY_SIGNAL_NAME1 + " = ?", new String[] {scanResult.get(0).SSID}, null, null, null);
        if (cursor.moveToFirst())
            id = cursor.getInt(0);
        else {
            cursor = db.query(DATABASE, new String[] {"_id"}, KEY_SIGNAL_NAME2 + " = ?", new String[] {scanResult.get(0).SSID}, null, null, null);
            if (cursor.moveToFirst())
                id = cursor.getInt(0);
            else {
                cursor = db.query(DATABASE, new String[] {"_id"}, KEY_SIGNAL_NAME1 + " = ?", new String[] {scanResult.get(1).SSID}, null, null, null);
                if (cursor.moveToFirst())
                    id = cursor.getInt(0);
                else {
                    cursor = db.query(DATABASE, new String[] {"_id"}, KEY_SIGNAL_NAME1 + " = ?", new String[] {scanResult.get(0).SSID}, null, null, null);
                    if (cursor.moveToFirst())
                        id = cursor.getInt(0);
                    else return null;
                }
            }
        }
        if (id == -1)
            return null;
        else {
            cursor = db.query(DATABASE, new String[] {"_id", KEY_LAT, KEY_LNG, KEY_TIME, KEY_ADDRESS, KEY_NAME, KEY_SIGNAL_NAME1, KEY_STRENGTH1, KEY_SIGNAL_NAME2, KEY_STRENGTH2},
                     "_id = ?", new String[] {String.valueOf(id)}, null, null, null);
            if (cursor.moveToNext()) {
                int _id = cursor.getInt(0);
                String lat = cursor.getString(1);
                String lng = cursor.getString(2);
                String time = cursor.getString(3);
                String addr = cursor.getString(4);
                String name = cursor.getString(5);
                String sname1 = cursor.getString(6);
                float strength1 = cursor.getFloat(7);
                String sname2 = cursor.getString(8);
                float strength2 = cursor.getFloat(9);
                Log.v("database status", " " + lat + " " + lng + " " + time + " " + sname1 + " " + strength1 + "" + sname2 + " " + strength2 );
                CheckPoint cp = new CheckPoint(_id, name, lat, lng, time, addr, sname1, strength1, sname2, strength2);
                return cp;
            }
            else return null;
        }
    }
}

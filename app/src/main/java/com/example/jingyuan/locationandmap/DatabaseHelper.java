package com.example.jingyuan.locationandmap;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.widget.ArrayAdapter;

import java.util.ArrayList;

/**
 * Created by jingyuan on 11/5/17.
 */

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE = "checkins";
    private static final String ASSOCIATE_DATABASE = "associates";
    private static final String KEY_NAME = "name";
    private static final String KEY_LAT = "latitude";
    private static final String KEY_LNG = "longitude";
    private static final String KEY_TIME = "time";
    private static final String KEY_ADDRESS = "address";
    private static final String KEY_CHECKINID = "checkinID";
    private static final String DELETE_QUERY = "DELETE FROM "+ DATABASE +" WHERE _id=";

    public DatabaseHelper(Context context) {
        super(context, DATABASE, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        String createTable = "CREATE TABLE "+ DATABASE +" (_id INTEGER PRIMARY KEY AUTOINCREMENT, " + KEY_LAT + " TEXT, "+
                KEY_LNG +" TEXT, "+ KEY_TIME +" TEXT, "+ KEY_ADDRESS +" TEXT, " + KEY_NAME + ")";
        String createAssociate = "CREATE TABLE " + ASSOCIATE_DATABASE + " (assID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                KEY_CHECKINID + " TEXT, " + KEY_LAT + " TEXT, " + KEY_LNG + " TEXT, " + KEY_TIME + ")";
        sqLiteDatabase.execSQL(createTable);
        sqLiteDatabase.execSQL(createAssociate);
        Log.v("database status", "on create");
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + DATABASE);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + ASSOCIATE_DATABASE);
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
        db.insert(DATABASE, null, values);
        db.close();
    }

    public void deletePoint(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL(DELETE_QUERY + id);
    }

    public ArrayList<CheckPoint> getAllPoints() {
        SQLiteDatabase db = this.getReadableDatabase();
        ArrayList<CheckPoint> result = new ArrayList<>();


        Cursor cursor = db.query(DATABASE,null,null,null,null,null,null);
        Log.v("database status", "get all " + cursor.getCount());
        while (cursor.moveToNext())
        {
            int id = cursor.getInt(0);
            String lat = cursor.getString(1);
            String lng = cursor.getString(2);
            String time = cursor.getString(3);
            String addr = cursor.getString(4);
            String name = cursor.getString(5);
            CheckPoint point = new CheckPoint(id, name, lat, lng, time, addr);
            result.add(point);
            Log.v("database status", "result ids " + point.getId());
        }

        return result;
    }

    public void addAssociate(CheckPoint cp, ArrayList<CheckPoint> checkPoints) {
        Log.v("database status", "add associate");
        String ids = "";
        for (CheckPoint i : checkPoints)
            ids = ids + i.getId() + ",";
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_LAT, cp.getLat());
        values.put(KEY_LNG, cp.getLng());
        values.put(KEY_TIME, cp.getTime());
        values.put(KEY_CHECKINID, ids);
        db.insert(ASSOCIATE_DATABASE, null, values);
        db.close();
    }
}

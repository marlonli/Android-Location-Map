package com.example.jingyuan.locationandmap;

import java.io.Serializable;

/**
 * Created by jingyuan on 11/4/17.
 */

public class CheckPoint implements Serializable{
    private int _id;
    private String name;
    private String lat;
    private String lng;
    private String time;
    private String address;
    private String signalName1;
    private float signalStrength1;
    private String signalName2;
    private float signalStrength2;

    public int getId() {
        return _id;
    }

    public void setId(int id) {
        this._id = id;
    }

    public CheckPoint(int id, String name, String lat, String lng, String time, String address, String sname1, float strength1, String sname2, float strength2) {
        this._id = id;
        this.name = name;
        this.lat = lat;
        this.lng = lng;
        this.time = time;
        this.address = address;
        signalName1 = sname1;
        signalStrength1 = strength1;
        signalName2 = sname2;
        signalStrength2 = strength2;
    }

    public CheckPoint(int id, String name, String lat, String lng, String time, String address) {
        this._id = id;
        this.name = name;
        this.lat = lat;
        this.lng = lng;
        this.time = time;
        this.address = address;
        signalName1 = "";
        signalStrength1 = 0;
        signalName2 = "";
        signalStrength2 = 0;
    }

    public CheckPoint(String name, String lat, String lng, String time, String address) {
        this.name = name;
        this.lat = lat;
        this.lng = lng;
        this.time = time;
        this.address = address;
        signalName1 = "";
        signalStrength1 = 0;
        signalName2 = "";
        signalStrength2 = 0;
    }

    public CheckPoint(int id, String name, String lat, String lng, String time) {
        this._id = id;
        this.name = name;
        this.lat = lat;
        this.lng = lng;
        this.time = time;
        this.address = "";
        signalName1 = "";
        signalStrength1 = 0;
        signalName2 = "";
        signalStrength2 = 0;
    }

    public String getLat() {

        return lat;
    }

    public float getSignalStrength1() {
        return signalStrength1;
    }

    public void setSignalStrength1(float signalStrength) {
        this.signalStrength1 = signalStrength;
    }

    public String getSignalName1() {
        return signalName1;
    }

    public void setSignalName1(String signalName) {
        this.signalName1 = signalName;
    }

    public String getSignalName2() {
        return signalName2;
    }

    public void setSignalName2(String signalName2) {
        this.signalName2 = signalName2;
    }

    public float getSignalStrength2() {
        return signalStrength2;
    }

    public void setSignalStrength2(float signalStrength2) {
        this.signalStrength2 = signalStrength2;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setLat(String lat) {
        this.lat = lat;
    }

    public String getLng() {
        return lng;
    }

    public void setLng(String lng) {
        this.lng = lng;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}

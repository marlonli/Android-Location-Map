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

    public int getId() {
        return _id;
    }

    public void setId(int id) {
        this._id = id;
    }

    public CheckPoint(int id, String name, String lat, String lng, String time, String address) {
        this._id = id;
        this.name = name;
        this.lat = lat;
        this.lng = lng;
        this.time = time;
        this.address = address;
    }

    public CheckPoint(String name, String lat, String lng, String time, String address) {
        this.name = name;
        this.lat = lat;
        this.lng = lng;
        this.time = time;
        this.address = address;
    }

    public CheckPoint(String name, String lat, String lng, String time) {
        this.name = name;
        this.lat = lat;
        this.lng = lng;
        this.time = time;
        this.address = "";
    }

    public String getLat() {

        return lat;
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

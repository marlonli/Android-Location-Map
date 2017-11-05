package com.example.jingyuan.locationandmap;

import java.io.Serializable;

/**
 * Created by jingyuan on 11/4/17.
 */

public class CheckPoint implements Serializable{
    private String lat;
    private String lng;
    private String time;
    private String address;

    public CheckPoint(String lat, String lng, String time, String address) {
        this.lat = lat;
        this.lng = lng;
        this.time = time;
        this.address = address;
    }

    public String getLat() {

        return lat;
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

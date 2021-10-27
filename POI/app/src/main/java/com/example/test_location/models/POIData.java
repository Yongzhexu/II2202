package com.example.test_location.models;

import java.io.Serializable;

public class POIData implements Serializable {
    double lat;
    double lon;
    String title;
    double dist;

    public POIData(double lat, double lon, String title, double dist) {
        this.lat = lat;
        this.lon = lon;
        this.title = title;
        this.dist = dist;
    }

    public double getDist() {
        return dist;
    }

    public void setDist(double dist) {
        this.dist = dist;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}

package com.example.test_location.models;

import java.io.Serializable;

public class BlurredLocation implements Serializable {
    private final double lat;
    private final double lon;
    private final double range;

    public BlurredLocation(double lat, double lon, double range) {
        this.lat = lat;
        this.lon = lon;
        this.range = range;
    }

    public double getLat() {
        return lat;
    }
    public double getLon() {
        return lon;
    }
    public double getRange() {
        return range;
    }
}

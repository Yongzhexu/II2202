package com.example.test_location.models;

import java.io.Serializable;

public class P2PRequest implements Serializable {
    private final double lat;
    private final double lon;
    private final double range;
    private final String queryType;

    public P2PRequest(double lat, double lon, double range, String queryType) {
        this.lat = lat;
        this.lon = lon;
        this.range = range;
        this.queryType = queryType;
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

    public String getQueryType() {
        return queryType;
    }
}

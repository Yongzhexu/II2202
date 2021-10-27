package com.example.test_location.models;

import java.net.Socket;
import java.security.PublicKey;
import java.util.ArrayList;

public class PeerInfo {
    double lat;
    double lon;
    double range;
    Socket socket;
    PublicKey publicKey;

    ArrayList<POIData> results = new ArrayList<>();

    String peerCerInfo;

    private static PeerInfo instance = new PeerInfo();

    private PeerInfo(){}

    public static PeerInfo getInstance(){
        return instance;
    }

    public PeerInfo(double lat, double lon, double range) {
        this.lat = lat;
        this.lon = lon;
        this.range = range;
    }

    public String getPeerCerInfo() {
        return peerCerInfo;
    }

    public void setPeerCerInfo(String peerCerInfo) {
        this.peerCerInfo = peerCerInfo;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(PublicKey publicKey) {
        this.publicKey = publicKey;
    }

    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public synchronized double getLat() {
        return lat;
    }

    public synchronized void setLat(double lat) {
        this.lat = lat;
    }

    public synchronized double getLon() {
        return lon;
    }

    public synchronized void setLon(double lon) {
        this.lon = lon;
    }

    public synchronized double getRange() {
        return range;
    }

    public synchronized void setRange(double range) {
        this.range = range;
    }

    public synchronized ArrayList<POIData> getResults() {
        return results;
    }

    public synchronized void setResults(ArrayList<POIData> results) {
        if(results == null) {
            this.results.clear();
            return;
        }
        this.results.clear();
        this.results.addAll(results);
        System.out.println("PeerGeo: "+this.results.size());
    }
}

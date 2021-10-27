package com.example.test_location.models;

import java.io.Serializable;
import java.util.ArrayList;

public class PeerPOIPackage implements Serializable {
    private ArrayList<POIData> results;
    private byte[] signature;
    private boolean isBusy;

    public PeerPOIPackage(ArrayList<POIData> results, byte[] signature, boolean isBusy) {
        this.results = results;
        this.signature = signature;
        this.isBusy = isBusy;
    }

    public ArrayList<POIData> getResults() {
        return results;
    }

    public void setResults(ArrayList<POIData> results) {
        this.results = results;
    }

    public byte[] getSignature() {
        return signature;
    }

    public void setSignature(byte[] signature) {
        this.signature = signature;
    }

    public boolean isBusy() {
        return isBusy;
    }

    public void setBusy(boolean busy) {
        isBusy = busy;
    }
}

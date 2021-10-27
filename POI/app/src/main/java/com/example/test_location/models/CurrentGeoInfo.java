package com.example.test_location.models;

import com.amap.api.maps.model.Marker;
import com.example.test_location.GeoTools.GeoLocationBlurTool;
import com.example.test_location.controller.ModeController;
import com.example.test_location.services.BloomService;

import java.util.ArrayList;
import java.util.Iterator;

public class CurrentGeoInfo {
    private double securityIndex = 0.1;
    private boolean locationIsGot = false;

    public boolean isLocationIsGot() {
        return locationIsGot;
    }

    public void setLocationIsGot(boolean locationIsGot) {
        this.locationIsGot = locationIsGot;
    }

    private double currentLatOriginal = 0.0;
    private double currentLonOriginal = 0.0;
    private double currentLatBlurred = 0.0;
    private double currentLonBlurred = 0.0;
    private double requestRange;
    private double originalRange = 100.0;
    private boolean bloomFilterEnabled = false;

    private boolean isFromPeer = false;

    public boolean isFromPeer() {
        return isFromPeer;
    }

    public void setFromPeer(boolean fromPeer) {
        isFromPeer = fromPeer;
    }

    public void setOriginalRange(double originalRange) {
        this.originalRange = originalRange;
    }

    private boolean queryInProgress = false;
    GeoLocationBlurTool geoLocationBlurTool = GeoLocationBlurTool.getInstance();

    private ArrayList<POIData> originalPOIDataArrayList = new ArrayList<>();
    private ArrayList<POIData> reconstructedPOIArrayList = new ArrayList<>();

    public ArrayList<POIData> getOriginalPOIArrayList() {
        return originalPOIDataArrayList;

    }

    public void setOriginalPOIArrayList(ArrayList<POIData> originalPOIDataArrayList) {
        this.originalPOIDataArrayList = originalPOIDataArrayList;
        ArrayList<POIData> resultSent = new ArrayList<>();

        if(ModeController.getInstance().isBlurred()){
            for(POIData POIData : originalPOIDataArrayList) {
                double dist = geoLocationBlurTool.calculateDist(currentLatOriginal,
                        currentLonOriginal, POIData.getLat(), POIData.getLon());
                if(dist < originalRange){
                    resultSent.add(new POIData(POIData.getLat(), POIData.getLon(), POIData.getTitle(), dist));
                }
            }
            reconstructedPOIArrayList.clear();
            reconstructedPOIArrayList.addAll(resultSent);
        }
        else {
            reconstructedPOIArrayList.clear();
            reconstructedPOIArrayList.addAll(originalPOIDataArrayList);
        }

        if(bloomFilterEnabled){
            // filter out the invalid data
            Iterator<POIData> iterator = reconstructedPOIArrayList.iterator();
            while (iterator.hasNext()) {
                POIData poiData = iterator.next();
                if(!BloomService.getInstance().verify(poiData.getTitle())){
                    System.out.println("error receiving invalid data!! " + poiData.getTitle());
                    iterator.remove();
                }
            }
        }
    }

    public void setSecurityIndex(double securityIndex) {
        this.securityIndex = securityIndex;
    }

    public ArrayList<POIData> getReconstructedPOIArrayList() {
        return reconstructedPOIArrayList;
    }

    public void setReconstructedPOIArrayList(ArrayList<POIData> reconstructedPOIArrayList) {
        this.reconstructedPOIArrayList = reconstructedPOIArrayList;
    }

    public void setBloomFilterEnabled(boolean bloomFilterEnabled){
        this.bloomFilterEnabled = bloomFilterEnabled;
    }

    public boolean isQueryInProgress() {
        return queryInProgress;
    }

    public void setQueryInProgress(boolean queryInProgress) {
        this.queryInProgress = queryInProgress;
    }

    private static CurrentGeoInfo instance = new CurrentGeoInfo();

    private CurrentGeoInfo(){}

    public static CurrentGeoInfo getInstance(){
        return instance;
    }

    public boolean isBloomFilterEnabled(){
        return bloomFilterEnabled;
    }


    public synchronized double getCurrentLatOriginal() {
        return currentLatOriginal;
    }

    public synchronized double getSecurityIndex() {
        return securityIndex;
    }

    public synchronized void setCurrentLatOriginal(double currentLatOriginal) {
        this.currentLatOriginal = currentLatOriginal;
    }

    public synchronized double getCurrentLonOriginal() {
        return currentLonOriginal;
    }

    public synchronized void setCurrentLonOriginal(double currentLonOriginal) {
        this.currentLonOriginal = currentLonOriginal;
    }

    public synchronized double getCurrentLatBlurred() {
        return currentLatBlurred;
    }

    public synchronized void setCurrentLatBlurred(double currentLatBlurred) {
        this.currentLatBlurred = currentLatBlurred;
    }

    public synchronized double getCurrentLonBlurred() {
        return currentLonBlurred;
    }

    public synchronized void setCurrentLonBlurred(double currentLonBlurred) {
        this.currentLonBlurred = currentLonBlurred;
    }

    public synchronized double getRequestRange() {
        return requestRange;
    }

    public synchronized void setRequestRange(double requestRange) {
        this.requestRange = requestRange;
    }

    public synchronized double getOriginalRange() {
        return originalRange;
    }

    public String printDebugInfo(){
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("request range: " + originalRange);
        stringBuilder.append("\n");
        stringBuilder.append("queried " + originalPOIDataArrayList.size() + " POI data");
        stringBuilder.append("\n");
        stringBuilder.append("reconstructed " + reconstructedPOIArrayList.size() + " POI data");

        return String.valueOf(stringBuilder);
    }
}

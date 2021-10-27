package com.example.test_location.services;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.example.test_location.GeoTools.CoordinateTransformUtil;
import com.example.test_location.models.CurrentGeoInfo;


public class GPSService extends Service implements LocationListener {
    //Intent used to send GPS information
    private double currentLat;
    private double currentLon;
    final Intent pOISearchIntent = new Intent();
    protected LocationManager locationManager;

    private CurrentGeoInfo currentGeoInfo = CurrentGeoInfo.getInstance();


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        pOISearchIntent.setAction("GPSLocation");

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        //initialise GPS service
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        //locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        //locationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, 0, 0, this);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);

        currentLat = 0.0;
        currentLon = 0.0;
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {

        double[] result = CoordinateTransformUtil.wgs84togcj02(location.getLongitude(), location.getLatitude());

        double modifiedGPSLat = result[1];
        double modifiedGPSLon =result[0];


        if(Math.abs(modifiedGPSLon - currentLon) < 0.002
                && Math.abs(modifiedGPSLat - currentLat) < 0.002){
            return;
        }

        currentLon = result[0];
        currentLat = result[1];

        pOISearchIntent.putExtra("lon", currentLon);
        pOISearchIntent.putExtra("lat", currentLat);

        currentGeoInfo.setCurrentLatOriginal(currentLat);
        currentGeoInfo.setCurrentLonOriginal(currentLon);
        sendBroadcast(pOISearchIntent);
        System.out.println("GPS received" + currentLat+ " " + currentLon);
    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {
    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }
}

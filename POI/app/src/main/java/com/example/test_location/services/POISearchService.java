package com.example.test_location.services;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import androidx.annotation.Nullable;

import com.amap.api.services.poisearch.PoiSearch;
import com.example.test_location.GeoTools.GeoLocationBlurTool;
import com.example.test_location.controller.ModeController;
import com.example.test_location.models.BlurredLocation;
import com.example.test_location.models.CurrentGeoInfo;
import com.example.test_location.models.POIData;
import com.example.test_location.models.PeerInfo;
import com.example.test_location.enums.SoftwareMode;

import java.util.ArrayList;

public class POISearchService extends Service{

    private CurrentGeoInfo currentGeoInfo = CurrentGeoInfo.getInstance();

    private PoiSearch.Query query;
    private final Intent poiResultIntent = new Intent();
    private final GeoLocationBlurTool geoLocationBlurTool = GeoLocationBlurTool.getInstance();//-----

    private final ModeController modeController = ModeController.getInstance();

    @Override
    public void onCreate() {
        super.onCreate();

        IntentFilter filter = new IntentFilter();
        filter.addAction("GPSLocation");
        registerReceiver(new GPSReceiver(), filter);

        filter = new IntentFilter();
        filter.addAction("querySelfResult");
        registerReceiver(new selfQueryResultReceiver(), filter);

        filter = new IntentFilter();
        filter.addAction("UpdatePOI");
        registerReceiver(new ButtonReceiver(), filter);

        filter = new IntentFilter();
        filter.addAction("WIFIDirectQuery");
        registerReceiver(new WIFIDirectReceiver(), filter);

        filter = new IntentFilter();
        filter.addAction("PeerQuery");
        registerReceiver(new PeerQueryReceiver(), filter);

        poiResultIntent.setAction("POISelfResult");

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void query(boolean isPeer){

        if(!currentGeoInfo.isQueryInProgress()) {
            Intent queryIntent = new Intent(POISearchService.this, QueryThread.class);

            if(isPeer){
                queryIntent.putExtra("lat", PeerInfo.getInstance().getLat());
                queryIntent.putExtra("lon", PeerInfo.getInstance().getLon());
                queryIntent.putExtra("range", PeerInfo.getInstance().getRange());
                queryIntent.putExtra("security_index", currentGeoInfo.getSecurityIndex());
            }else {
                if(ModeController.getInstance().isBlurred()){
                    queryIntent.putExtra("lat", currentGeoInfo.getCurrentLatBlurred());
                    queryIntent.putExtra("lon", currentGeoInfo.getCurrentLonBlurred());
                    queryIntent.putExtra("range", currentGeoInfo.getRequestRange());
                    queryIntent.putExtra("security_index", currentGeoInfo.getSecurityIndex());
                }else {
                    queryIntent.putExtra("lat", currentGeoInfo.getCurrentLatOriginal());
                    queryIntent.putExtra("lon", currentGeoInfo.getCurrentLonOriginal());
                    queryIntent.putExtra("range", currentGeoInfo.getOriginalRange());
                    queryIntent.putExtra("security_index", currentGeoInfo.getSecurityIndex());
                }
            }

            queryIntent.putExtra("isPeer", isPeer);
            startService(queryIntent);

            currentGeoInfo.setQueryInProgress(true);
        }
    }

    public void blurLocation(double lat, double lon){
        if(!currentGeoInfo.isQueryInProgress()) {
            currentGeoInfo.setCurrentLatOriginal(lat);
            currentGeoInfo.setCurrentLonOriginal(lon);
            BlurredLocation blurredLocation = geoLocationBlurTool.blurLocation(currentGeoInfo.getCurrentLatOriginal(),
                    currentGeoInfo.getCurrentLonOriginal(), currentGeoInfo.getOriginalRange());

            currentGeoInfo.setCurrentLatBlurred(blurredLocation.getLat());
            currentGeoInfo.setCurrentLonBlurred(blurredLocation.getLon());
            currentGeoInfo.setRequestRange(blurredLocation.getRange());
        }
    }

    public class selfQueryResultReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            currentGeoInfo.setQueryInProgress(false);

            if(intent.getStringExtra("status").equals("no result!")){
                poiResultIntent.putExtra("status", "no result!");
                sendBroadcast(poiResultIntent);
                return;
            }
            else if(!intent.getStringExtra("status").equals("succeed")){
                return;
            }

            ArrayList<POIData> resultList =  (ArrayList<POIData>)
                    intent.getSerializableExtra("result");

            currentGeoInfo.setOriginalPOIArrayList(resultList);

            poiResultIntent.putExtra("status", "succeed");
            sendBroadcast(poiResultIntent);
        }
    }

    public class ButtonReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            if(modeController.getSoftwareMode() == SoftwareMode.SAFE && !modeController.isServingNode()) return;
            blurLocation(currentGeoInfo.getCurrentLatOriginal(), currentGeoInfo.getCurrentLonOriginal());
            query(false);
            currentGeoInfo.setFromPeer(false);
        }
    }

    public class GPSReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            if(modeController.getSoftwareMode() == SoftwareMode.SAFE && !modeController.isServingNode()) return;

            System.out.println("GPS received!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            blurLocation(intent.getDoubleExtra("lat", 0.0),
                    intent.getDoubleExtra("lon", 0.0));
            query(false);

            CurrentGeoInfo.getInstance().setLocationIsGot(true);
            currentGeoInfo.setFromPeer(false);
        }
    }

    public class WIFIDirectReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {

        }
    }

    public class PeerQueryReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            PeerInfo.getInstance().setLat(intent.getDoubleExtra("lat", 0.0));
            PeerInfo.getInstance().setLon(intent.getDoubleExtra("lon", 0.0));
            PeerInfo.getInstance().setRange(intent.getDoubleExtra("range", 0.0));

            System.out.println("peer query started");
            query(true);
        }
    }
}

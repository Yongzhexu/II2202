package com.example.test_location.services;

import android.app.IntentService;
import android.content.Intent;

import androidx.annotation.Nullable;

import com.amap.api.services.core.AMapException;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.core.PoiItem;
import com.amap.api.services.poisearch.PoiResult;
import com.amap.api.services.poisearch.PoiSearch;
import com.example.test_location.models.CurrentGeoInfo;
import com.example.test_location.models.POIData;
import com.example.test_location.models.PeerInfo;
import com.example.test_location.models.QueryType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class QueryThread extends IntentService {
    private boolean isPeer;
    int pageCount = -1;

    private PoiSearch.Query query;
    private int currentPage = 1;
    private boolean queryOnProgress = false;

    private final Intent resultsIntent = new Intent();
    ArrayList<POIData> resultList = new ArrayList<>();

    public QueryThread() {
        super("");
    }

    public void doSearch(double lat, double lon, double range){
        //System.out.println(range + "  %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
        // set new query
        query = new PoiSearch.Query("", QueryType.getInstance().getQueryType(), "");
        System.out.println( QueryType.getInstance().getQueryType());
        query.setPageSize(20);
        query.setPageNum(currentPage);

        PoiSearch poiSearch = new PoiSearch(this, query);
        poiSearch.setBound(new PoiSearch.SearchBound(new LatLonPoint(lat, lon), (int)range));

        poiSearch.setOnPoiSearchListener(new PoiSearch.OnPoiSearchListener() {
            @Override
            public void onPoiSearched(PoiResult poiResult, int i) {
                if(pageCount == -1) pageCount = poiResult.getPageCount();
                //System.out.println("#############################" + poiResult.getPageCount());
                System.out.println("#############################  Page: " + currentPage + " " +pageCount+" "+poiResult.getPois().size());
                if(pageCount == 0){
                    resultsIntent.putExtra("status", "no result!");
                    if(isPeer){PeerInfo.getInstance().setResults(null);}
                    sendBroadcast(resultsIntent);
                    pageCount = -1;
                    CurrentGeoInfo.getInstance().setQueryInProgress(false);
                    return;
                }

                if (i == AMapException.CODE_AMAP_SUCCESS) {
                    if (poiResult != null && poiResult.getQuery() != null) {
                        if (poiResult.getQuery().equals(query)) {
                            List<PoiItem> poiItems = poiResult.getPois();
                            for(PoiItem item : poiItems){
                                LatLonPoint llp = item.getLatLonPoint();
                                POIData POIData = new POIData(llp.getLatitude(), llp.getLongitude(), item.getTitle(), item.getDistance());

                                resultList.add(POIData);
                            }

                            //if(poiItems.size() == 20){
                            if(currentPage != pageCount){
                                System.out.println(currentPage + " " + pageCount);
                                currentPage++;
                                doSearch(lat, lon, range);
                                return;
                            }

                            resultsIntent.putExtra("status", "succeed");
                            resultsIntent.putExtra("Long", lon);
                            resultsIntent.putExtra("Lat", lat);
                            System.out.println("query thread size " + resultList.size());
                            if(!isPeer) {
                                resultsIntent.putExtra("result", (Serializable) resultList);
                            }else
                            {
                                PeerInfo.getInstance().setResults(resultList);
                            }
                            sendBroadcast(resultsIntent);
                            pageCount = -1;
                            CurrentGeoInfo.getInstance().setQueryInProgress(false);
                            currentPage = 1;
                            resultList.clear();
                            queryOnProgress = false;

                            System.out.println("################################### query finished!");
                        }
                    }
                } else {
                    resultsIntent.putExtra("status", "error");

                    sendBroadcast(resultsIntent);
                    CurrentGeoInfo.getInstance().setQueryInProgress(false);
                    currentPage = 1;
                    queryOnProgress = false;
                    pageCount = -1;
                }
            }

            @Override
            public void onPoiItemSearched(PoiItem poiItem, int i) {
            }
        });
        poiSearch.searchPOIAsyn();
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        System.out.println("query started!");

        assert intent != null;
        double lat = intent.getDoubleExtra("lat", 0.0);
        double lon = intent.getDoubleExtra("lon", 0.0);
        double range = intent.getDoubleExtra("range", 0.0);
        this.isPeer = intent.getBooleanExtra("isPeer", false);
        System.out.println("^^^^^^^^^^^^^^^^^^^^^^^^^^^^    "+range);

        // If it's the query for current mobile, the result will be received by main activity. Otherwise
        // the result will be received by P2P thread
        if(!isPeer){
            resultsIntent.setAction("querySelfResult");
        }
        else {
            resultsIntent.setAction("queryPeerResult");
        }
        doSearch(lat, lon, range);
    }
}

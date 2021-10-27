package com.example.test_location;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.Circle;
import com.amap.api.maps.model.CircleOptions;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.LatLngBounds;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.example.test_location.BloomFilter.BloomFilter;
import com.example.test_location.GeoTools.GeoLocationBlurTool;
import com.example.test_location.PKI.VPKIManagerVoid;
import com.example.test_location.controller.ModeController;
import com.example.test_location.models.CurrentGeoInfo;
import com.example.test_location.models.POIData;
import com.example.test_location.models.PeerInfo;
import com.example.test_location.models.QueryType;
import com.example.test_location.network.BloomFilterRequestThread;
import com.example.test_location.services.BloomService;
import com.example.test_location.services.WIFIDirectService;
import com.example.test_location.services.GPSService;
import com.example.test_location.services.POISearchService;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Iterator;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;

public class MainActivity extends AppCompatActivity{
    private static final int CODE_REQ_PERMISSIONS = 665;

    MapView mMapView = null;
    AMap aMap = null;
    boolean isConnected = false;

    // marker for map
    ArrayList<Marker> markers = new ArrayList<>();
    Marker selfMarker;
    Circle circleMarker;
    Marker locationSent;
    Circle rangeSent;

    Intent gpsService;
    Intent poiSearch;
    Intent wifiDirectService;
    Intent buttonIntent = new Intent();
    Intent updateIntent = new Intent();

    TextView deviceList;
    TextView connectInfo;
    TextView rangeText;
    TextView eTextView;

    Spinner spinner = null;

    ProgressBar progressBar;
    SeekBar seekBar;
    SeekBar rangeSeekBar;

    private ArrayAdapter<String> adapter = null;

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    Switch mSwitch;
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    Switch bloomFilterSwitch;

    // Data base for geo-info
    CurrentGeoInfo currentGeoInfo = CurrentGeoInfo.getInstance();

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        deviceList = findViewById(R.id.device_list);
        connectInfo = findViewById(R.id.connected);
        mSwitch = findViewById(R.id.switch1);
        bloomFilterSwitch = findViewById(R.id.bloomfilter);
        progressBar = findViewById(R.id.progressBar);
        spinner = findViewById(R.id.spinner);
        seekBar = findViewById(R.id.seekBar);
        rangeSeekBar = findViewById(R.id.range);
        rangeText = findViewById(R.id.rangeText);
        eTextView = findViewById(R.id.e);

        String [] spinnerList ={"restaurant","supermarket","medical"};

        progressBar.setMax(100);
        progressBar.setProgress(25);

        //seekBar.setMax(1);
        //seekBar.setMin(10);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(progress == 0) progress = 1;
                double value = (double) progress /100;
                currentGeoInfo.setSecurityIndex(value);
                GeoLocationBlurTool.getInstance().setSecurityIndex(value);
                eTextView.setText("E = "+value);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });


        rangeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress < 10) progress = 10;
                CurrentGeoInfo.getInstance().setOriginalRange(progress);
                rangeText.setText(progress+" M");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        mSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    ModeController.getInstance().setBlurred(true);
                }else {
                    ModeController.getInstance().setBlurred(false);
                }
            }});

        bloomFilterSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    if(!BloomService.getInstance().isUpdated()){
                        showToast("Bloom filter is not updated!");
                        bloomFilterSwitch.setChecked(false);
                    }else {
                        showToast("Bloom filter updated!");
                        currentGeoInfo.setBloomFilterEnabled(true);
                    }
                }else {
                    currentGeoInfo.setBloomFilterEnabled(false);
                }
            }});

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }


        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                String[] languages = getResources().getStringArray(R.array.spinnerclass);
                //System.out.println(languages[pos]);
                QueryType.getInstance().setQueryType(languages[pos]);
                //System.out.println(languages[pos]);
                //Toast.makeText(MainActivity.this, "你点击的是:"+languages[pos], 2000).show();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Another interface callback
            }
        });

        // init map
        mMapView = (MapView) findViewById(R.id.map);
        mMapView.onCreate(savedInstanceState);

        if (aMap == null) {
            aMap = mMapView.getMap();
        }


        // init intent & intent filter
        IntentFilter filter = new IntentFilter();
        filter.addAction("POISelfResult");
        registerReceiver(new POIResultReceiver(), filter);

        IntentFilter wifiFilter = new IntentFilter();
        wifiFilter.addAction("WIFIService");
        registerReceiver(new WIFIServiceReceiver(), wifiFilter);

        IntentFilter peerFilter = new IntentFilter();
        peerFilter.addAction("PeerQueryMain");
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                showToast("new peer query");
            }
        }, peerFilter);

        IntentFilter peerSucceedFilter = new IntentFilter();
        peerSucceedFilter.addAction("queryPeerResult");
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                showToast("peer query finished");
            }
        }, peerSucceedFilter);

        buttonIntent.setAction("CommandToP2P");
        updateIntent.setAction("UpdatePOI");

        gpsService = new Intent(this, GPSService.class);
        startService(gpsService);

        poiSearch = new Intent(this, POISearchService.class);
        startService(poiSearch);

        wifiDirectService = new Intent(this, WIFIDirectService.class);
        startService(wifiDirectService);

        checkPermission();
        try {
            initBloomFilter();
        } catch (NoSuchAlgorithmException | ClassNotFoundException | KeyManagementException | IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //System.out.println("called destory!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        //在activity执行onDestroy时执行mMapView.onDestroy()，销毁地图
        mMapView.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //在activity执行onResume时执行mMapView.onResume ()，重新绘制加载地图
        mMapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //在activity执行onPause时执行mMapView.onPause ()，暂停地图的绘制
        mMapView.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mMapView.onSaveInstanceState(outState);
    }

    // send "connect" command to P2P service
    public void connect(View view){
        buttonIntent.putExtra("action", "connect");
        sendBroadcast(buttonIntent);
    }

    // send "quit" command to P2P service
    public void quit(View view){
        buttonIntent.putExtra("action", "quit");
        sendBroadcast(buttonIntent);
    }

    // send "update" command to POI service
    public void update(View view){
        if(!currentGeoInfo.isLocationIsGot()){
            showToast("GPS not located yet!");
            return;
        }
        if(currentGeoInfo.getCurrentLatOriginal() == 0.0 || currentGeoInfo.getCurrentLonOriginal() == 0.0){
            showToast("GPS is not ready");
            return;
        }
        sendBroadcast(updateIntent);
    }

    public void setMainProgressBar(){
        int securityLevel = 25;
        if(currentGeoInfo.isBloomFilterEnabled()){
            securityLevel += 25;
        }
        if(ModeController.getInstance().isBlurred()){
            securityLevel += 25;
        }
        if(isConnected){
            securityLevel += 25;
        }
        System.out.println(securityLevel+"^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
        progressBar.setProgress(securityLevel);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CODE_REQ_PERMISSIONS) {
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
            }
        }
    }

    public void checkPermission() {
        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{Manifest.permission.CHANGE_NETWORK_STATE, Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.ACCESS_WIFI_STATE, Manifest.permission.CHANGE_WIFI_STATE,
                        Manifest.permission.ACCESS_FINE_LOCATION}, CODE_REQ_PERMISSIONS);
    }

    private void showNormalDialog(){
        final AlertDialog.Builder normalDialog =
                new AlertDialog.Builder(MainActivity.this);
        normalDialog.setTitle("Debug");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(currentGeoInfo.printDebugInfo());
        stringBuilder.append("\n");
        if(BloomService.getInstance().isInvalidPOIExists() && CurrentGeoInfo.getInstance().isFromPeer()) {
            stringBuilder.append("\ninvalid data detected!\n\n");

            stringBuilder.append(PeerInfo.getInstance().getPeerCerInfo());
            BloomService.getInstance().setInvalidPOIExists(false);
        }
        normalDialog.setMessage(stringBuilder);

        normalDialog.setPositiveButton("ok",
                (dialog, which) -> {

                });
        normalDialog.show();
    }

    private void initBloomFilter() throws NoSuchAlgorithmException, KeyManagementException, IOException, ClassNotFoundException {
        BloomFilterRequestThread bloomFilterRequestThread = new BloomFilterRequestThread();
        bloomFilterRequestThread.execute();
    }

    // receiver for receiving message from POI service(Unsafe mode) or P2P service(Safe mode)
    public class POIResultReceiver extends BroadcastReceiver {
        @SuppressLint("SetTextI18n")
        @Override
        public void onReceive(Context context, Intent intent) {
            currentGeoInfo.setQueryInProgress(false);

            if(intent.getStringExtra("status").equals("peerNoResponse")){
                showToast("peer busy!");
                return;
            }
            if(intent.getStringExtra("status").equals("no result!")){
                // check if marker already exists
                if(selfMarker != null) selfMarker.remove();
                if(circleMarker != null) circleMarker.remove();
                if(locationSent != null) locationSent.remove();
                if(rangeSent != null) rangeSent.remove();

                // draw the original location in the map
                MarkerOptions markerOptions = new MarkerOptions().position(new LatLng(currentGeoInfo.getCurrentLatOriginal(),
                        currentGeoInfo.getCurrentLonOriginal())).title("original").icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory
                        .decodeResource(getResources(),R.drawable.location1)));
                selfMarker = aMap.addMarker(markerOptions);

                // draw the original range in the map
                CircleOptions circleOptions=new CircleOptions();
                circleOptions.center(new LatLng(currentGeoInfo.getCurrentLatOriginal(), currentGeoInfo.getCurrentLonOriginal()));
                circleOptions.radius(currentGeoInfo.getOriginalRange());
                circleOptions.strokeWidth(8);
                circleOptions.strokeColor(Color.RED);
                circleMarker = aMap.addCircle(circleOptions);

                if(ModeController.getInstance().isBlurred()) {
                    // draw the locationSent in the map
                    markerOptions = new MarkerOptions().position(new LatLng(currentGeoInfo.getCurrentLatBlurred(),
                            currentGeoInfo.getCurrentLonBlurred())).title("sent").icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory
                            .decodeResource(getResources(), R.drawable.location2)));
                    locationSent = aMap.addMarker(markerOptions);

                    // draw the rangeSent in the map
                    circleOptions = new CircleOptions();
                    circleOptions.center(new LatLng(currentGeoInfo.getCurrentLatBlurred(), currentGeoInfo.getCurrentLonBlurred()));
                    circleOptions.radius(currentGeoInfo.getRequestRange());
                    circleOptions.strokeWidth(8);
                    circleOptions.strokeColor(Color.RED);
                    rangeSent = aMap.addCircle(circleOptions);
                }

                for(Marker marker:markers){
                    marker.remove();
                }
                markers.clear();
                adjustCamera(new LatLng(currentGeoInfo.getCurrentLatOriginal(),
                        currentGeoInfo.getCurrentLonOriginal()), aMap);
            }
            if(intent.getStringExtra("status").equals("succeed")){
                ArrayList<POIData> resultList = currentGeoInfo.getReconstructedPOIArrayList();

                // check if marker already exists
                if(selfMarker != null) selfMarker.remove();
                if(circleMarker != null) circleMarker.remove();
                if(locationSent != null) locationSent.remove();
                if(rangeSent != null) rangeSent.remove();

                // draw the original location in the map
                MarkerOptions markerOptions = new MarkerOptions().position(new LatLng(currentGeoInfo.getCurrentLatOriginal(),
                        currentGeoInfo.getCurrentLonOriginal())).title("original").icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory
                        .decodeResource(getResources(),R.drawable.location1)));
                selfMarker = aMap.addMarker(markerOptions);

                // draw the original range in the map
                CircleOptions circleOptions=new CircleOptions();
                circleOptions.center(new LatLng(currentGeoInfo.getCurrentLatOriginal(), currentGeoInfo.getCurrentLonOriginal()));
                circleOptions.radius(currentGeoInfo.getOriginalRange());
                circleOptions.strokeWidth(8);
                circleOptions.strokeColor(Color.RED);
                circleMarker = aMap.addCircle(circleOptions);

                if(ModeController.getInstance().isBlurred()) {
                    // draw the locationSent in the map
                    markerOptions = new MarkerOptions().position(new LatLng(currentGeoInfo.getCurrentLatBlurred(),
                            currentGeoInfo.getCurrentLonBlurred())).title("sent").icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory
                            .decodeResource(getResources(), R.drawable.location2)));
                    locationSent = aMap.addMarker(markerOptions);

                    // draw the rangeSent in the map
                    circleOptions = new CircleOptions();
                    circleOptions.center(new LatLng(currentGeoInfo.getCurrentLatBlurred(), currentGeoInfo.getCurrentLonBlurred()));
                    circleOptions.radius(currentGeoInfo.getRequestRange());
                    circleOptions.strokeWidth(8);
                    circleOptions.strokeColor(Color.RED);
                    rangeSent = aMap.addCircle(circleOptions);
                }
                // update the marker list
                Iterator<Marker> iterator = markers.iterator();
                while (iterator.hasNext()) {
                    Marker marker = iterator.next();
                    boolean isFound = false;
                    for(POIData reconstructedPOI:resultList){
                        if(reconstructedPOI.getTitle().equals(marker.getTitle())){
                            isFound = true;
                            break;
                        }
                    }
                    if(!isFound){
                        marker.remove();
                        iterator.remove();
                    }
                }

                for(POIData reconstructedPOI:resultList){
                    boolean isFound = false;
                    for(Marker marker:markers){
                        if(reconstructedPOI.getTitle().equals(marker.getTitle())){
                            isFound = true;
                            break;
                        }
                    }
                    if(!isFound){
                        Marker marker = aMap.addMarker(new MarkerOptions()
                                .position(new LatLng(reconstructedPOI.getLat(), reconstructedPOI.getLon()))
                                .title(reconstructedPOI.getTitle()+" "+reconstructedPOI.getDist()));
                        markers.add(marker);
                    }
                }

                // zoom in
                adjustCamera(new LatLng(currentGeoInfo.getCurrentLatOriginal(),
                        currentGeoInfo.getCurrentLonOriginal()), aMap);

                setMainProgressBar();
                showNormalDialog();
            }

        }

        // zoom in
        private void adjustCamera(LatLng centerLatLng, AMap aMap) {
            //System.out.println("hahahahhahahahahahhahahahahahhaha");
            LatLngBounds.Builder newbounds = new LatLngBounds.Builder();
            newbounds.include(selfMarker.getPosition());
            for (int i = 0; i < markers.size(); i++) {
                newbounds.include(markers.get(i).getPosition());
            }

            aMap.animateCamera(CameraUpdateFactory.newLatLngBounds(newbounds.build(),
                    300));
            aMap.moveCamera(CameraUpdateFactory.changeLatLng(centerLatLng));
        }
    }

    // receiver for receiving intent from P2P service
    public class WIFIServiceReceiver extends BroadcastReceiver{
        // print available device
        @Override
        public void onReceive(Context context, Intent intent) {
            ArrayList<String> result = intent.getStringArrayListExtra("device_list");
            if(result != null) {
                StringBuilder stringBuilder = new StringBuilder();

                for (String s : result) {
                    stringBuilder.append(s);
                    stringBuilder.append("\n");
                }
                deviceList.setText(stringBuilder);
            }

            String status = intent.getStringExtra("connect_status");
            isConnected = !status.contains("not");

            if(status != null) connectInfo.setText(status);
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
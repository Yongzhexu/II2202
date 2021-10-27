package com.example.test_location.services;

import android.Manifest;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.os.IBinder;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import com.example.test_location.GeoTools.GeoLocationBlurTool;
import com.example.test_location.controller.ModeController;
import com.example.test_location.models.BlurredLocation;
import com.example.test_location.models.CurrentGeoInfo;
import com.example.test_location.enums.SoftwareMode;
import com.example.test_location.network.P2PClientSSLSocket;
import com.example.test_location.network.P2PServerSSLSocket;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;

public class WIFIDirectService extends Service {
    // necessary vars for P2P connection
    private final IntentFilter intentFilter = new IntentFilter();
    private WifiP2pManager.Channel channel;
    private WifiP2pManager manager;
    private final ArrayList<WifiP2pDevice> deviceList = new ArrayList<>();

    private final CurrentGeoInfo currentGeoInfo = CurrentGeoInfo.getInstance();
    private final GeoLocationBlurTool geoLocationBlurTool = GeoLocationBlurTool.getInstance();
    private final ModeController modeController = ModeController.getInstance();

    final Intent sendIntent = new Intent();
    private InetAddress hostAddress;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);

        // init intent & intentFilter
        IntentFilter filter = new IntentFilter();
        filter.addAction("CommandToP2P");
        registerReceiver(new WIFIDirectIntentReceiver(), filter);

        IntentFilter GPSFilter = new IntentFilter();
        GPSFilter.addAction("GPSLocation");
        registerReceiver(new GPSReceiver(), GPSFilter);

        filter = new IntentFilter();
        filter.addAction("UpdatePOI");
        registerReceiver(new ButtonReceiver(), filter);

        sendIntent.setAction("WIFIService");

        try {
            discoverPeers();
        } catch (IOException e) {
            e.printStackTrace();
        }

        checkConnection();
    }

    private void discoverPeers() throws IOException {
        WiFiDirectBroadcastReceiver wiFiDirectBroadcastReceiver = new WiFiDirectBroadcastReceiver(this);
        registerReceiver(wiFiDirectBroadcastReceiver, intentFilter);

        ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);

        manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
            }

            @Override
            public void onFailure(int reasonCode) {
                showToast("discover peers failed");
            }
        });
    }


    private void changeConnectionStatus(String status) throws IOException {
        sendIntent.putExtra("connect_status", status);
        sendBroadcast(sendIntent);

        // open server socket if the role is "serving"
        if (modeController.isServingNode()){
            Intent queryIntent = new Intent(WIFIDirectService.this, P2PServerSSLSocket.class);
            startService(queryIntent);
        }
    }

    private void checkConnection(){
        manager.requestConnectionInfo(channel, info -> {
            if (info.groupOwnerAddress == null) {
                try {
                    discoverPeers();
                    hostAddress = null;
                    modeController.setServingNode(false);
                    modeController.setSoftwareMode(SoftwareMode.UNSAFE);
                    changeConnectionStatus("not connected");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                modeController.setServingNode(info.isGroupOwner);
                modeController.setSoftwareMode(SoftwareMode.SAFE);
                hostAddress = info.groupOwnerAddress;
                try {
                    String role = modeController.isServingNode()?"serving":"normal";
                    changeConnectionStatus("connected: " + role);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void connect() {
        WifiP2pDevice targetDevice = null;
        for(WifiP2pDevice device: deviceList){
            // only connect to HUAWEI mobile, which is the other testing mobile phone
            if(device.deviceName.contains("HUA")){
                targetDevice = device;
                break;
            }
        }

        if(targetDevice == null) {
            showToast("No available device");
            return;
        } else
        {
            showToast("Trying to connect");
        }

        WifiP2pConfig config = new WifiP2pConfig();

        config.deviceAddress = targetDevice.deviceAddress;
        config.wps.setup = WpsInfo.PBC;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

        }
        manager.connect(channel, config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                showToast("Succeed");
            }

            @Override
            public void onFailure(int reason) {
                showToast("Connection failed");
            }
        });
    }

    public void setIsWifiP2pEnabled(boolean isWifiP2pEnabled_){
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    // receiver to receive status changes
    public class WiFiDirectBroadcastReceiver extends BroadcastReceiver {
        private final WIFIDirectService activity;

        public WiFiDirectBroadcastReceiver(WIFIDirectService activity){
            this.activity = activity;
        }

        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
                int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
                activity.setIsWifiP2pEnabled(state == WifiP2pManager.WIFI_P2P_STATE_ENABLED);

            }
            // peer status changes, like new devices are discovered
            else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {

                WifiP2pDeviceList mPeers = intent.getParcelableExtra(WifiP2pManager.EXTRA_P2P_DEVICE_LIST);
                deviceList.clear();
                deviceList.addAll(mPeers.getDeviceList());

                ArrayList<String> result = new ArrayList<>();
                for(WifiP2pDevice device: deviceList){
                    if(device.deviceName.contains("HUA")){
                        result.add(device.deviceName);
                    }
                }

                sendIntent.putStringArrayListExtra("device_list", result);
                sendBroadcast(sendIntent);
            }
            // connection changes. Like new connection is established, or disconnected.
            else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
                if (manager == null) {
                    return;
                }
                checkConnection();

            }
            // self status changes. Like WIFI is disabled.
            else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            }
        }
    }

    public class WIFIDirectIntentReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getStringExtra("action");
            switch (action) {
                case "connect":
                    connect();
                    break;
                case "quit":
                    manager.removeGroup(channel, new WifiP2pManager.ActionListener() {
                        @Override
                        public void onSuccess() {
                            modeController.setServingNode(false);
                            modeController.setSoftwareMode(SoftwareMode.UNSAFE);
                            hostAddress = null;
                        }

                        @Override
                        public void onFailure(int reason) {
                        }
                    });
                    break;
            }
        }
    }

    public class GPSReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            // only called when software mode is "SAFE" and the mobile is not "serving" node
            if(modeController.getSoftwareMode() == SoftwareMode.SAFE && !modeController.isServingNode()){
                CurrentGeoInfo.getInstance().setLocationIsGot(true);
                currentGeoInfo.setCurrentLatOriginal(intent.getDoubleExtra("lat", 0.0));
                currentGeoInfo.setCurrentLonOriginal(intent.getDoubleExtra("lon", 0.0));
                BlurredLocation blurredLocation = geoLocationBlurTool.blurLocation(currentGeoInfo.getCurrentLatOriginal(),
                        currentGeoInfo.getCurrentLonOriginal(), currentGeoInfo.getOriginalRange());
                currentGeoInfo.setCurrentLatBlurred(blurredLocation.getLat());
                currentGeoInfo.setCurrentLonBlurred(blurredLocation.getLon());
                currentGeoInfo.setRequestRange(blurredLocation.getRange());

                Intent clientIntent = new Intent(WIFIDirectService.this, P2PClientSSLSocket.class);

                clientIntent.putExtra("address", hostAddress.getHostAddress());
                startService(clientIntent);
            }
        }
    }

    public class ButtonReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            // only called when software mode is "SAFE" and the mobile is not "serving" node
            if(modeController.getSoftwareMode() == SoftwareMode.SAFE && !modeController.isServingNode()){
                BlurredLocation blurredLocation = geoLocationBlurTool.blurLocation(currentGeoInfo.getCurrentLatOriginal(),
                        currentGeoInfo.getCurrentLonOriginal(), currentGeoInfo.getOriginalRange());
                currentGeoInfo.setCurrentLatBlurred(blurredLocation.getLat());
                currentGeoInfo.setCurrentLonBlurred(blurredLocation.getLon());
                currentGeoInfo.setRequestRange(blurredLocation.getRange());

                Intent clientIntent = new Intent(WIFIDirectService.this, P2PClientSSLSocket.class);

                clientIntent.putExtra("address", hostAddress.getHostAddress());
                startService(clientIntent);
            }
        }
    }
}


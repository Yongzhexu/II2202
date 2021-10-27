package com.example.test_location.network;

import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.example.test_location.models.BlurredLocation;
import com.example.test_location.models.CurrentGeoInfo;
import com.example.test_location.models.OwnKeyInfo;
import com.example.test_location.models.P2PRequest;
import com.example.test_location.models.POIData;
import com.example.test_location.models.PeerInfo;
import com.example.test_location.models.PeerPOIPackage;
import com.example.test_location.models.QueryType;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class ServerThread extends IntentService {
    private Socket socket;
    private boolean isFinished = false;


    public ServerThread() {
        super("");
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        socket = PeerInfo.getInstance().getSocket();

        BufferedReader br = null;
        String mess = null;

        try {
            InputStream is = socket.getInputStream();
            ObjectInputStream obj = new ObjectInputStream(is);
            P2PRequest p2PRequest = (P2PRequest) obj.readObject();

            //System.out.println("客户端：" + informationSent.getLat() +" | "+ informationSent.getLon() + " | " + informationSent.getRange());

            if(CurrentGeoInfo.getInstance().isQueryInProgress()){
                ObjectOutputStream objectOutput = new ObjectOutputStream(socket.getOutputStream());
                objectOutput.writeObject(new PeerPOIPackage(null, null, true));
                socket.close();
                return;
            }
            Intent intent1 = new Intent();
            intent1.setAction("PeerQuery");
            intent1.putExtra("lat", p2PRequest.getLat());
            intent1.putExtra("lon", p2PRequest.getLon());
            intent1.putExtra("range", p2PRequest.getRange());
            QueryType.getInstance().setQueryType(p2PRequest.getQueryType());

            sendBroadcast(intent1);

            IntentFilter filter = new IntentFilter();
            filter.addAction("queryPeerResult");
            PeerThreadReceiver peerThreadReceiver = new PeerThreadReceiver();
            registerReceiver(peerThreadReceiver, filter);

            while (!isFinished){Thread.sleep(500);}

            ArrayList<POIData> results = PeerInfo.getInstance().getResults();
            //System.out.println("result size  " + results.size());

            ObjectOutputStream objectOutput = new ObjectOutputStream(socket.getOutputStream());

            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, OwnKeyInfo.getInstance().getPrivateKey());
            byte[] p = objectToBytes(results);
            byte[] a = digest.digest(p);
            byte[] result = cipher.doFinal(a);
            byte[] encoded = Base64.getEncoder().encode(result);

            objectOutput.writeObject(new PeerPOIPackage(results, encoded, false));

            //System.out.println("result sent");
            socket.close();
            unregisterReceiver(peerThreadReceiver);
        } catch (IOException | ClassNotFoundException | InterruptedException | InvalidKeyException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        }

    }

    public byte[] objectToBytes(final Serializable object) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos  =  null;
        try {
            oos = new ObjectOutputStream(baos);
            oos.writeObject(object);
            oos.flush();
            return baos.toByteArray();
        } finally {
            if (oos != null)  {
                oos.close();
            }
            if (baos != null) {
                baos.close();
            }
        }
    }

    public class PeerThreadReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            isFinished = true;
        }
    }


    /*

    public ServerThread(Socket socket){
        this.socket = socket;
    }

    @Override
    public void run() {
        BufferedReader br = null;

        String mess = null;

        try {
            InputStream is = socket.getInputStream();
            ObjectInputStream obj = new ObjectInputStream(is);
            BlurredLocation informationSent = (BlurredLocation) obj.readObject();

            System.out.println("客户端：" + informationSent.getLat() +" | "+ informationSent.getLon() + " | " + informationSent.getRange());



        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

    }

 */
}

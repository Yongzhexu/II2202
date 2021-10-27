package com.example.test_location.network;

import android.app.IntentService;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.example.test_location.PKI.VPKIManager;
import com.example.test_location.R;
import com.example.test_location.controller.ModeController;
import com.example.test_location.models.BlurredLocation;
import com.example.test_location.models.CurrentGeoInfo;
import com.example.test_location.models.P2PRequest;
import com.example.test_location.models.POIData;
import com.example.test_location.models.PeerInfo;
import com.example.test_location.models.PeerPOIPackage;
import com.example.test_location.models.QueryType;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.net.SocketFactory;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManagerFactory;

public class P2PClientSSLSocket extends IntentService {
    Certificate ca;

    public P2PClientSSLSocket() {
        super("");
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onHandleIntent(@Nullable Intent intent) {

        String host = intent.getStringExtra("address");
        InitCA();
        Socket socket = null;
        try {
            int port = 8000;
            socket = createSocket(host, port);
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("client created");

        try {
            OutputStream os = socket.getOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(os);

            double lat;
            double lon;
            double range;
            if(ModeController.getInstance().isBlurred()) {
                lat = CurrentGeoInfo.getInstance().getCurrentLatBlurred();
                lon = CurrentGeoInfo.getInstance().getCurrentLonBlurred();
                range = CurrentGeoInfo.getInstance().getRequestRange();
            }else {
                lat = CurrentGeoInfo.getInstance().getCurrentLatOriginal();
                lon = CurrentGeoInfo.getInstance().getCurrentLonOriginal();
                range = CurrentGeoInfo.getInstance().getOriginalRange();
            }

            System.out.println("%%%%%%%%%%%%%%%%%%%%%%"+range);

            oos.writeObject(new P2PRequest(lat, lon, range, QueryType.getInstance().getQueryTypeText()));

            ObjectInputStream objectInput = new ObjectInputStream(socket.getInputStream());
            Object object = objectInput.readObject();
            PeerPOIPackage results =  (PeerPOIPackage) object;

            Intent intent1 = new Intent();
            intent1.setAction("POISelfResult");

            if(results.isBusy()){
                intent1.putExtra("status", "peerNoResponse");
            }else {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] sigFromServer = results.getSignature();
                byte[] sigFromResult = digest.digest(objectToBytes(results.getResults()));
                Cipher cipher = Cipher.getInstance("RSA");
                cipher.init(Cipher.DECRYPT_MODE, PeerInfo.getInstance().getPublicKey());

                byte[] decryptedSig = cipher.doFinal(Base64.getDecoder().decode(sigFromServer));

                //System.out.println(Arrays.toString(sigFromResult));
                //System.out.println(Arrays.toString(decryptedSig));
                System.out.println(results.getResults().size());
                if(Arrays.equals(decryptedSig, sigFromResult)){
                    intent1.putExtra("status", "succeed");
                    CurrentGeoInfo.getInstance().setOriginalPOIArrayList(results.getResults());
                }else {
                    System.out.println("invalid data");
                    intent1.putExtra("status", "invalid data");
                }
            }

            sendBroadcast(intent1);
            CurrentGeoInfo.getInstance().setFromPeer(true);
            socket.close();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
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

    private void InitCA(){
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            try (InputStream caInput = this.getResources().openRawResource(R.raw.ca_cer)) {
                ca = cf.generateCertificate(caInput);
                //System.out.println("CA%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% "+ca.getPublicKey());
            }
        } catch (Exception e){
            System.out.println(e);
        }
    }

    SSLSocket createSocket(String host, int port) throws Exception
    {
        KeyStore ks = KeyStore.getInstance("BKS");

        InputStream input = this.getResources().openRawResource(R.raw.client1);
        ks.load(input, "yongzhe".toCharArray());

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        tmf.init(ks);

        SSLContext context = SSLContext.getInstance("TLSv1.2");

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, "yongzhe".toCharArray());

        context.init(kmf.getKeyManagers(), new VPKIManager[]{new VPKIManager(ca)}, null);

        input.close();

        SocketFactory sf = context.getSocketFactory();

        int retryCount = 0;
        boolean isConnected = false;

        SSLSocket client = null;
        while(retryCount < 5){
            try {
                client = (SSLSocket)sf.createSocket(host, port);
                isConnected = true;
                break;
            } catch (IOException e) {
                e.printStackTrace();
                retryCount++;
                System.out.println("$$$$$$$$$$$$$$$$$$$$$    retry: "+retryCount);
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException interruptedException) {
                    interruptedException.printStackTrace();
                }
            }
        }

        if(!isConnected) return null;
        return client;
    }
}

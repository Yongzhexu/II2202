package com.example.test_location.network;


import android.app.IntentService;
import android.content.Intent;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.example.test_location.MainActivity;
import com.example.test_location.PKI.VPKIManager;
import com.example.test_location.R;
import com.example.test_location.models.CurrentGeoInfo;
import com.example.test_location.models.OwnKeyInfo;
import com.example.test_location.models.PeerInfo;

import java.io.InputStream;
import java.net.Socket;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

public class P2PServerSSLSocket extends IntentService {
    Certificate ca;

    public P2PServerSSLSocket() {
        super("");
    }

    private void InitCA(){
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            try (InputStream caInput = this.getResources().openRawResource(R.raw.ca_cer)) {
                ca = cf.generateCertificate(caInput);
            }
        } catch (Exception e){
            System.out.println(e);
        }
    }

    SSLServerSocket createServerSocket(int port) throws Exception
    {
        KeyStore ks = KeyStore.getInstance("BKS");
        InputStream input = this.getResources().openRawResource(R.raw.client2);
        ks.load(input, "yongzhe".toCharArray());

        OwnKeyInfo.getInstance().setPrivateKey(ks.getKey("server_cert", "yongzhe".toCharArray()));

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, "yongzhe".toCharArray());


        SSLContext context = SSLContext.getInstance("TLSv1.2");

        context.init(kmf.getKeyManagers(),
                new VPKIManager[]{new VPKIManager(ca)},
                null);

        input.close();

        SSLServerSocketFactory ssf = context.getServerSocketFactory();

        SSLServerSocket serverSocket = (SSLServerSocket)ssf.createServerSocket(port);
        serverSocket.setNeedClientAuth(true);
        return serverSocket;
    }


    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        InitCA();

        SSLServerSocket serverSocket;

        try {
            serverSocket = createServerSocket(8000);
            Socket client;

            while(true){
                client = serverSocket.accept();

                Intent intent1 = new Intent();
                intent1.setAction("PeerQueryMain");
                sendBroadcast(intent1);

                PeerInfo.getInstance().setSocket(client);
                Intent serverIntent = new Intent(this, ServerThread.class);
                startService(serverIntent);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

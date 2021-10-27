package com.example.test_location.network;

import android.os.AsyncTask;

import com.example.test_location.BloomFilter.BloomFilter;
import com.example.test_location.PKI.VPKIManagerVoid;
import com.example.test_location.services.BloomService;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;

public class BloomFilterRequestThread extends AsyncTask {
    public BloomFilterRequestThread(){}

    @Override
    protected Object doInBackground(Object[] objects) {


        SSLContext context = null;
        try {
            context = SSLContext.getInstance("TLSv1.2");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        try {
            context.init(null, new VPKIManagerVoid[]{new VPKIManagerVoid()}, null);
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }
        SocketFactory sf = context.getSocketFactory();

        Socket s = null;
        try {
            s = (SSLSocket)sf.createSocket("192.168.0.108", 4488);

            InputStream is = null;
            is = s.getInputStream();
            ObjectInputStream obj = null;
            obj = new ObjectInputStream(is);
            BloomFilter<String> bloomFilter = null;
            bloomFilter = (BloomFilter<String>) obj.readObject();
            BloomService.getInstance().setBloomFilter(bloomFilter);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }


        return null;
    }



}

package com.example.test_location.models;

import java.security.Key;
import java.security.PrivateKey;

public class OwnKeyInfo {
    private Key privateKey;
    private static OwnKeyInfo instance = new OwnKeyInfo();

    private OwnKeyInfo(){}

    public static OwnKeyInfo getInstance(){
        return instance;
    }

    public Key getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(Key privateKey) {
        this.privateKey = privateKey;
    }
}

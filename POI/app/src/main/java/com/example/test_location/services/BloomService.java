package com.example.test_location.services;

import com.example.test_location.BloomFilter.BloomFilter;

public class  BloomService {
    private BloomFilter<String> bloomFilter;
    private boolean isUpdated = false;
    private boolean invalidPOIExists = false;

    private static BloomService instance = new BloomService();

    private BloomService(){}

    public static BloomService getInstance(){
        return instance;
    }

    public void setBloomFilter(BloomFilter<String> bloomFilter){
        assert bloomFilter!=null;
        this.bloomFilter = bloomFilter;
        this.isUpdated = true;
        System.out.println("filter set!!!!!!!!!!!!!!!!!!!!!");
    }

    public void setInvalidPOIExists(boolean invalidPOIExists) {
        this.invalidPOIExists = invalidPOIExists;
    }

    public boolean verify(String title){
        //System.out.println(title + " verified");
        boolean result = bloomFilter.contains(title);
        if(!result){
            invalidPOIExists = true;
        }
        return result;
    }

    public boolean isInvalidPOIExists() {
        return invalidPOIExists;
    }


    public boolean isUpdated(){
        return isUpdated;
    }
}

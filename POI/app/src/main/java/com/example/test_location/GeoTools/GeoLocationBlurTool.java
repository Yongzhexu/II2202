package com.example.test_location.GeoTools;

import com.example.test_location.models.BlurredLocation;
import com.example.test_location.models.CurrentGeoInfo;

public class GeoLocationBlurTool {
    private double securityIndex = 0.1;
    final private double earthRadius = 6367000;
    private static GeoLocationBlurTool instance = new GeoLocationBlurTool();

    public double getSecurityIndex() {
        return securityIndex;
    }

    public void setSecurityIndex(double securityIndex) {
        this.securityIndex = securityIndex;
    }

    private GeoLocationBlurTool(){ }

    public static GeoLocationBlurTool getInstance(){
        return instance;
    }

    public BlurredLocation blurLocation(double lat, double lon, double range){
        double rangeRequest = range + (-1/securityIndex*(lambertW((0.95-1)/Math.E) + 1));

        double theta = Math.random()*2*Math.PI;
        double p = Math.random();
        if(p == 1) p = 0.99999999999;

        System.out.println("generated theta: " + theta + "|" + "generated p: " + p);

        double distance = -(1/securityIndex)*(lambertW((p-1)/Math.E) + 1);
        System.out.println("dis!!!!!! "+ distance);
        double radianC = distance/earthRadius;

        // start to regenerate the blurred location
        double latDiff = Math.acos(Math.cos((90 - lat)*Math.PI/180) * Math.cos(radianC)
                + Math.sin((90 - lat)*Math.PI/180) * Math.sin(radianC) * Math.cos(theta));

        double lonDiff = Math.asin(Math.sin(radianC) * Math.sin(theta) / Math.sin(latDiff));

        double lat_o = 90 - latDiff*180/Math.PI;
        double lon_o = lon + lonDiff*180/Math.PI;

        return new BlurredLocation(lat_o, lon_o, rangeRequest);
    }

    private double lambertW(double x){
        double minDiff = 1e-10;
        if(x == 0){
            return 0;
        }else if(x == -1/Math.E){
            return -1;
        }else{
            double a = Math.log(-x);
            double b = 1;
            while(Math.abs(b - a) > minDiff){
                b = (a*a + x/Math.exp(a))/(a + 1);
                a=(b*b + x/Math.exp(b))/(b + 1);
            }
            return (Math.round(1000000*a)/1000000.0);
        }
    }

    public double calculateDist(double lat, double lon, double lat_o, double lon_o){
        double a = Math.pow(Math.sin(((lat*Math.PI/180) - (lat_o*Math.PI/180))/2),2);
        double b = Math.cos(lat*Math.PI/180)*Math.cos(lat_o*Math.PI/180)*
                Math.pow(Math.sin((lon*Math.PI/180-lon_o*Math.PI/180)/2),2);
        double result = 2*earthRadius*Math.asin(Math.sqrt(a+b));

        return (double) Math.round(result * 100) / 100;
    }
}

package com.example.test_location.controller;

import com.example.test_location.enums.SoftwareMode;

public class ModeController{
    private static ModeController instance = new ModeController();
    private SoftwareMode softwareMode = SoftwareMode.UNSAFE;

    private boolean isBlurred = false;
    private boolean isServingNode = false;


    private ModeController(){}

    public static ModeController getInstance(){
        return instance;
    }

    public synchronized void setSoftwareMode(SoftwareMode softwareMode) {
        this.softwareMode = softwareMode;
    }

    public void setServingNode(boolean servingNode) {
        isServingNode = servingNode;
    }

    public SoftwareMode getSoftwareMode() {
        return softwareMode;
    }

    public boolean isServingNode() {
        return isServingNode;
    }

    public boolean isBlurred() {
        return isBlurred;
    }

    public void setBlurred(boolean blurred) {
        isBlurred = blurred;
    }
}

package com.example.testrealsense;

import java.io.Serializable;

public class DetectableObject implements Serializable {
    private int distance;
    private boolean status;
    private String name;

    public DetectableObject(int distance, boolean status, String name) {
        this.distance = distance;
        this.status = status;
        this.name = name;
    }

    public int getDistance() {
        return distance;
    }

    public void setDistance(int distance) {
        this.distance = distance;
    }

    public boolean getStatus(boolean b) {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
package com.example.testrealsense;

import java.io.Serializable;

public class SimpleLog implements Serializable {
    String datastamp;
    String distance;
    String object;

    public SimpleLog(String datastamp, String distance, String object) {
        this.datastamp = datastamp;
        this.distance = distance;
        this.object = object;
    }

    public String getDatastamp() {
        return datastamp;
    }

    public void setDatastamp(String datastamp) {
        this.datastamp = datastamp;
    }

    public String getDistance() {
        return distance;
    }

    public void setDistance(String distance) {
        this.distance = distance;
    }

    public String getObject() {
        return object;
    }

    public void setObject(String object) {
        this.object = object;
    }
}

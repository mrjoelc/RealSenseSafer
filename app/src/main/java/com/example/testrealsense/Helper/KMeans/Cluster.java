package com.example.testrealsense.Helper.KMeans;

import java.util.ArrayList;
import java.util.List;

public class Cluster {
    private float center;
    private List<Float> points;

    public Cluster(float center) {
        this.center = center;
        points = new ArrayList<>();
    }

    public void addPointToCluster(Float point){
        points.add(point);
    }

    public float getCenter() {
        return center;
    }

    public List<Float> getPoints() {
        return points;
    }

    public void computeCenter() {
        if(points.size() == 0) return;
        if(points.size() == 1) {
            this.center = points.get(0);
            return;
        }

        float centerValue = 0;
        for (Float p : points) {
            centerValue+=p;
        }
        this.center = centerValue/points.size();
        points.clear();
    }
}

package com.example.testrealsense.Helper.KMeans;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class KMeans {
    Random rand;
    static List<Float> points;
    static List<Cluster> listOfClusters;
    int k;

    public KMeans(int k) {
        this.k = k;
        listOfClusters = new ArrayList<>();
        points = new ArrayList<>();
        rand = new Random();
    }

    public void setPoints(List<Float> points){
        KMeans.points =points;
        //System.out.print("POINTS: ");
        //System.out.println(points);
    }

    public List<Float> getCentroids(){
        List<Float> centroids = new ArrayList<>();
        for (Cluster c: listOfClusters) {
            centroids.add(c.getCenter());
        }
        return centroids;
    }

    public void computeClusters(int iterations){
        initClusters();
        assignPointsToClusters();
        for(int i=0; i<iterations; i++){
            relocateCentroid();
            assignPointsToClusters();

        }
        for (Cluster c: listOfClusters) {
            System.out.println(c.getPoints().size());
            //System.out.println(c.getPoints().contains(0.0f));

        }
    }

    public void initClusters(){
        List<Integer> listOfIndex = new ArrayList<>();
        int index = rand.nextInt(points.size());
        listOfIndex.add(index);

        for(int i=1; i<k; i++){
            index = rand.nextInt(points.size());
            if(listOfIndex.contains(index)) i-=1;
            else listOfIndex.add(index);
        }

        for (int i : listOfIndex) {
            listOfClusters.add(new Cluster(points.get(i)));
        }

        //printAllCentroids();
    }

    public void assignPointsToClusters(){
        for (Float point : points) {
            nearestCentroid(point).addPointToCluster(point);
        }
    }

    public void relocateCentroid(){
        for (Cluster c: listOfClusters) {
            c.computeCenter();
        }
    }

    public Cluster nearestCentroid(Float point){
        float minimumDistance = Float.MAX_VALUE;
        Cluster nearest = null;
        for (Cluster c: listOfClusters) {
            float currentDistance = euclideanDistance(point, c.getCenter());
            if (currentDistance < minimumDistance) {
                minimumDistance = currentDistance;
                nearest = c;
            }
        }
        return nearest;
    }

    public float euclideanDistance(float p1, float p2){
        return Math.abs(p1-p2);
    }

    public void printAllCentroids(){
        System.out.println("CENTROIDS: ");
        for (Cluster c: listOfClusters) {
            System.out.println(c.getCenter());
        }
    }

    public void printAllClusters(){
        System.out.println("CLUSTERS: ");
        for (Cluster c: listOfClusters) {
            System.out.println(c.getPoints());
        }
    }
}

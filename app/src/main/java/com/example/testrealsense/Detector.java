package com.example.testrealsense;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.MediaPlayer;
import android.os.SystemClock;

import com.example.testrealsense.Helper.DatabaseUtils;
import com.example.testrealsense.Helper.GraphicOverlay;
import com.example.testrealsense.Helper.KMeans.KMeans;
import com.example.testrealsense.Helper.ObjectGraphics;
import com.example.testrealsense.Helper.Utils;
import com.intel.realsense.librealsense.DepthFrame;

import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.task.vision.detector.Detection;
import org.tensorflow.lite.task.vision.detector.ObjectDetector;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import androidx.annotation.NonNull;

public class Detector {
    ObjectDetector objectDetector;
    ObjectDetector.ObjectDetectorOptions options;
    Context context;
    BottomsheetC bs;
    boolean alarm;
    String assetmodel;
    static TensorImage image;

    GraphicOverlay graphicOverlay;
    ObjectGraphics drawBoundingBoxLabel;

    HashMap<String, Float> objectDict;

    Float scaleFactor;
    MediaPlayer mp;

    public Detector(Context context, GraphicOverlay graphicOverlay, HashMap<String, Float> objectDict, BottomsheetC bs) {
        this.context = context;
        this.bs=bs;
        this.objectDict = objectDict;
        this.graphicOverlay = graphicOverlay;
        scaleFactor = Utils.calculateScaleFactor(graphicOverlay, 640);
        mp = MediaPlayer.create(context, R.raw.alert_attention);
        setLocalModel();
    }

    public void setLocalModel(){
        assetmodel = bs.getModelML_spinner().getSelectedItem().toString();
        bs.getNthread_value();
        options = ObjectDetector.ObjectDetectorOptions.builder()
                .setScoreThreshold(0.5f)  // Evaluate your model in the Google Cloud Console
                .setNumThreads(Integer.parseInt(bs.getNthread_value().getText().toString()))
                // to determine an appropriate value.
                .build();
        try {
            objectDetector = ObjectDetector.createFromFileAndOptions(context, "models/"+assetmodel, options);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setImageToDetect(TensorImage image){
        Detector.image = image;
    }

    public void startDetection(){
        long startTime = SystemClock.elapsedRealtime();

        List<Detection> results = objectDetector.detect(image);

        computeDetectionTime(startTime);

        for (Detection detectedObject : results) {
            if (detectedObject.getCategories().size() > 0
                    && objectDict!=null
                    && objectDict.containsKey(detectedObject.getCategories().get(0).getLabel())){

                ((MainActivity)context).runOnUiThread(new Runnable()
                {
                    public void run()
                    {
                        drawBoundingBoxLabel = new ObjectGraphics(detectedObject, graphicOverlay, scaleFactor, 0, false);
                        drawBoundingBoxLabel.drawBoundingBoxAndLabel();
                    }
                });
            }


        }
        computeFPS(startTime);

    }

    public void startDetectionForRealSenseStream(DepthFrame depth){
        long startTime = SystemClock.elapsedRealtime();

        List<Detection> results = objectDetector.detect(image);

        computeDetectionTime(startTime);

        graphicOverlay.clear();

        for (Detection detectedObject : results) {
            //System.out.println("#" + results.size());
            if (detectedObject.getCategories().size() > 0
                    && objectDict!=null
                    && objectDict.containsKey(detectedObject.getCategories().get(0).getLabel())){

                //System.out.println("INSIDE DETECTOR" + objectDict.toString());

                String label = detectedObject.getCategories().get(0).getLabel();
                float depthValue = 0.0f;
                boolean alarm = false;

               depthValue = getCorrectDistance(depth, detectedObject);
               //System.out.println(depth.getDistance((int)detectedObject.getBoundingBox().centerX(), (int)detectedObject.getBoundingBox().centerY()));
               //System.out.println(detectedObject.getBoundingBox());

               /*if (depthValue>0.0f && depthValue < objectDict.get(label)) {
                    DatabaseUtils.writeTooCloseDistanceLog(depthValue, label);
                    System.out.println("SUONA NOTIFICA");
                    mp.start();
                    alarm = true;
                }*/

                //System.out.println(label);

                drawBoundingBoxLabel = new ObjectGraphics(detectedObject, graphicOverlay, scaleFactor, depthValue, alarm);
                drawBoundingBoxLabel.drawBoundingBoxAndLabel();
            }
        }
        computeFPS(startTime);
    }

    public int fixValueX(int i){
        int x = i;
        if(i<=0) x=1;
        if(i>=640) x=639;
        return x;
    }

    public int fixValueY(int j){
        int y = j;
        if(j<=0) y=1;
        if(j>=480) y=479;
        return y;
    }

    public float getCorrectDistance(DepthFrame depth, Detection detectedObject){
        float depthValue = -1f;
        switch (bs.distance_spinner.getSelectedItem().toString()){
            case "Minimum":
                float minDistance=10f;
                System.out.println(detectedObject.getBoundingBox());
                for(int j= (int)detectedObject.getBoundingBox().top; j<detectedObject.getBoundingBox().bottom; j++){
                    for (int i= (int)detectedObject.getBoundingBox().left; i<detectedObject.getBoundingBox().right; i++){

                        //System.out.println(x + " " +y);
                        float currentDistance = depth.getDistance(fixValueX(i),fixValueY(j));
                        //System.out.println(currentDistance);
                        if(currentDistance!=0.0f && currentDistance<minDistance) {
                            minDistance = currentDistance;
                            //System.out.println(minDistance);
                        }
                    }
                }
                depthValue = minDistance;
                break;
            case "Average":
                float sum=0;
                float n=0;
                for(float j=detectedObject.getBoundingBox().top; j<detectedObject.getBoundingBox().bottom; j++){
                    for (float i=detectedObject.getBoundingBox().left; i<detectedObject.getBoundingBox().right; i++){
                        sum += depth.getDistance((int)i,(int)j);
                        n+=1;
                    }
                }
                depthValue = sum/n;
                break;
            case "Clustering":
                List<Float> points = new ArrayList<Float>();
                for(float j=detectedObject.getBoundingBox().top; j<detectedObject.getBoundingBox().bottom; j++){
                    for (float i=detectedObject.getBoundingBox().left; i<detectedObject.getBoundingBox().right; i++){
                        points.add(depth.getDistance((int)i,(int)j));
                    }
                }
                KMeans k = new KMeans(2);
                k.setPoints(points);
                k.computeClusters(10);
                depthValue =  Collections.min(k.getCentroids());
                break;
            default: break;
        }
        return depthValue;
    }


    void computeFPS(float startTime) {
        float endTime2 = SystemClock.elapsedRealtime();
        float elapsedMilliSeconds2 = endTime2 - startTime;
        float elapsedSeconds2 =  1000 / elapsedMilliSeconds2;
        bs.getFps().setText(String.format("%sfps", elapsedSeconds2));
    }

    void computeDetectionTime(float startTime) {
        float endTime = SystemClock.elapsedRealtime();
        float elapsedMilliSeconds = endTime - startTime;
        bs.getMsDetection().setText(elapsedMilliSeconds+"ms");
    }


}

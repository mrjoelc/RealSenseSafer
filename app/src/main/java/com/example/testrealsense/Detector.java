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

        for (Detection detectedObject : results) {
            if (detectedObject.getCategories().size() > 0
                    && objectDict!=null
                    && objectDict.containsKey(detectedObject.getCategories().get(0).getLabel())){

                //System.out.println("INSIDE DETECTOR" + objectDict.toString());

                String label = detectedObject.getCategories().get(0).getLabel();
                float depthValue = 0.0f;
                boolean alarm = false;

               //depthValue = getCorrectDistance(depth, detectedObject);
                if (depthValue>0 && depthValue < objectDict.get(label)) {
                    DatabaseUtils.writeTooCloseDistanceLog(depthValue, label);
                    System.out.println("SUONA NOTIFICA");
                    mp.start();
                    alarm = true;
                }

                System.out.println(label);

                drawBoundingBoxLabel = new ObjectGraphics(detectedObject, graphicOverlay, scaleFactor, 0, alarm);
                drawBoundingBoxLabel.drawBoundingBoxAndLabel();
            }
        }
        computeFPS(startTime);
    }

    public float getCorrectDistance(DepthFrame depth, Detection detectedObject){
        float depthValue = -1f;
        switch (bs.distance_spinner.getSelectedItem().toString()){
            case "Minimum":
                float minDistance=10f;
                for(float j=detectedObject.getBoundingBox().top; j<detectedObject.getBoundingBox().bottom; j++){
                    for (float i=detectedObject.getBoundingBox().left; i<detectedObject.getBoundingBox().right; i++){
                        float currentDistance = depth.getDistance((int)i,(int)j);
                        if(currentDistance<minDistance) minDistance = currentDistance;
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

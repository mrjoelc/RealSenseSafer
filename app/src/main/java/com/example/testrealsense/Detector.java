package com.example.testrealsense;

import android.content.Context;
import android.graphics.Color;
import android.graphics.RectF;
import android.media.MediaPlayer;
import android.os.SystemClock;

import com.example.testrealsense.Helper.DatabaseUtils;
import com.example.testrealsense.Helper.GraphicOverlay;
import com.example.testrealsense.Helper.KMeans.KMeans;
import com.example.testrealsense.Helper.ObjectGraphics;
import com.example.testrealsense.Helper.RectOverlay;
import com.example.testrealsense.Helper.Utils;
import com.intel.realsense.librealsense.DepthFrame;

import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.task.vision.detector.Detection;
import org.tensorflow.lite.task.vision.detector.ObjectDetector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

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

    float depthValue;

    List<Float> points;

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


    public void startDetectionForRealSenseStream(DepthFrame depth){
        long startTime = SystemClock.elapsedRealtime();

        List<Detection> results = objectDetector.detect(image);

        computeDetectionTime(startTime);

        System.out.println("Object detected: #" + results.size());


        graphicOverlay.clear();
        for (Detection detectedObject : results) {
            if (detectedObject.getCategories().size() > 0
                    && objectDict!=null
                    && objectDict.containsKey(detectedObject.getCategories().get(0).getLabel())){


                System.out.println("Considero " + detectedObject.getCategories().get(0).getLabel());
                List<RectF> intersectionsRects = new ArrayList<>();
                List<RectF> containedRects = new ArrayList<>();
                //Controllo se ci sono intersezioni, nel caso aggiungi ad una lista i corrispondenti rect
                for(int i=0; i<results.size() && objectDict.containsKey(results.get(i).getCategories().get(0).getLabel()); i++){
                    //bb e' il rect che cambia sempre, detected Object è quello corrente
                    RectF bb = results.get(i).getBoundingBox();
                    //se l'intersezione c'è tra i due e il rect che cambia sempre e' diverso da quello corrente
                    if(results.get(i)!=detectedObject && RectF.intersects(detectedObject.getBoundingBox(), bb)){
                        if(detectedObject.getBoundingBox().contains(results.get(i).getBoundingBox())){
                            containedRects.add(results.get(i).getBoundingBox());
                        } else intersectionsRects.add(getIntersectionBoundinBox(detectedObject, results.get(i),true));
                    }
                }

                String label = detectedObject.getCategories().get(0).getLabel();

                depthValue = 0.0f;
                alarm = false;

               //valuta depthValue scartando valori presenti nei rect che intersecano
               depthValue = getNotOccludedDistance(depth, detectedObject.getBoundingBox(), intersectionsRects, containedRects);
               System.out.println(depthValue);


               if (depthValue>0.0f && depthValue < Float.parseFloat(String.valueOf(objectDict.get(label)))) {
                    DatabaseUtils.writeTooCloseDistanceLog(depthValue, label);
                    System.out.println("SUONA NOTIFICA");
                    mp.start();
                    alarm = true;
                }

                //System.out.println(label);

                drawBoundingBoxLabel = new ObjectGraphics(detectedObject, graphicOverlay, scaleFactor, depthValue, alarm);
                drawBoundingBoxLabel.drawBoundingBoxAndLabel();
            }
        }
        computeFPS(startTime);
    }

    public RectF getIntersectionBoundinBox(Detection mainObj, Detection otherObj, boolean draw){
        System.out.println( mainObj.getCategories().get(0).getLabel()+ " intersect with " + otherObj.getCategories().get(0).getLabel());

        float x5 = Math.max(mainObj.getBoundingBox().left, otherObj.getBoundingBox().left);
        float y5 = Math.max(mainObj.getBoundingBox().top, otherObj.getBoundingBox().top);
        float x6 = Math.min(mainObj.getBoundingBox().right, otherObj.getBoundingBox().right);
        float y6 = Math.min(mainObj.getBoundingBox().bottom, otherObj.getBoundingBox().bottom);

        if(draw){
            float[] rect1 = Utils.getScaledBoundingBox(mainObj,scaleFactor);
            float[] rect2 = Utils.getScaledBoundingBox(otherObj,scaleFactor);
            float x5scaled = Math.max(rect1[0], rect2[0]);
            float y5scaled = Math.max(rect1[1], rect2[1]);
            float x6scaled = Math.min(rect1[2], rect2[2]);
            float y6scaled = Math.min(rect1[3], rect2[3]);

            RectF intersectionRect = new RectF( x5scaled, y5scaled, x6scaled, y6scaled );
            RectOverlay rectOverlay = new RectOverlay(graphicOverlay, intersectionRect, Color.BLUE) ;
            graphicOverlay.add(rectOverlay);
        }

        return new RectF(x5, y5, x6, y6);
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

    public boolean pointIsNotInsideRects(int x, int y, List<RectF> intersectionRects, List<RectF> contaniedRects){
        if (intersectionRects==null) return true;
        for (RectF rect: intersectionRects) {
           if(rect.contains(x, y)){
               //System.out.println("intersection(" + intersectionRects.get(0).left + " " + intersectionRects.get(0).top + ") (" + intersectionRects.get(0).right + " " + intersectionRects.get(0).bottom+ ")");
               return false;
            }
        }
        return true;
    }

    private boolean objectIsContanied(RectF boundingBox, List<RectF> contaniedRects) {
        if (contaniedRects==null) return true;
        for (RectF rect: contaniedRects) {
            if(boundingBox.equals(rect)){
                return false;
            }
        }
        return true;
    }

    public float getNotOccludedDistance(DepthFrame depth, RectF boundingBox, List<RectF> intersectionRects, List<RectF> contaniedRects){
        float depthValue = -1f;
        switch (bs.distance_spinner.getSelectedItem().toString()){
            case "Minimum":
                float minDistance=10f;
                System.out.println("original(" + boundingBox.left + " " + boundingBox.top + ") (" + boundingBox.right + " " + boundingBox.bottom+ ")");
                for(int j= (int)boundingBox.top; j<boundingBox.bottom; j++){
                    for (int i= (int)boundingBox.left; i<boundingBox.right; i++){
                        if(pointIsNotInsideRects(i, j, intersectionRects, contaniedRects) || objectIsContanied(boundingBox, contaniedRects)) {
                            float currentDistance = depth.getDistance(fixValueX(i), fixValueY(j));
                            if (currentDistance != 0.0f && currentDistance < minDistance) {
                                minDistance = currentDistance;
                            }
                        }
                    }
                }
                depthValue = minDistance;
                break;
            case "Average":
                float sum=0;
                float n=0;
                for(int j= (int)boundingBox.top; j<boundingBox.bottom; j++){
                    for (int i= (int) boundingBox.left; i<boundingBox.right; i++){
                        float point = depth.getDistance(fixValueX(i),fixValueY(j));
                        if(point>0.0f && ( pointIsNotInsideRects(i, j, intersectionRects, contaniedRects) || objectIsContanied(boundingBox, contaniedRects)) ){
                            sum += depth.getDistance(fixValueX(i),fixValueY(j));
                            n+=1;
                        }

                    }
                }
                depthValue = sum/n;
                System.out.println(depthValue);
                break;
            case "Median":
                points = new ArrayList<Float>();
                for(int j= (int)boundingBox.top; j<boundingBox.bottom; j++){
                    for (int i= (int) boundingBox.left; i<boundingBox.right; i++){
                        float point = depth.getDistance(fixValueX(i),fixValueY(j));
                        if(point>0.0f && ( pointIsNotInsideRects(i, j, intersectionRects, contaniedRects) || objectIsContanied(boundingBox, contaniedRects)) ){
                            points.add(depth.getDistance(fixValueX(i),fixValueY(j)));
                        }

                    }
                }
                int middle = points.size() / 2;
                if (points.size() % 2 == 0){
                    Float left = points.get(middle - 1);
                    Float right = points.get(middle);
                    depthValue = (left + right) / 2;
                }
                else depthValue = points.get(middle);
                break;
            case "Clustering":
                points = new ArrayList<Float>();
                for(int j= (int)boundingBox.top; j<boundingBox.bottom; j++){
                    for (int i= (int)boundingBox.left; i<boundingBox.right; i++){
                        float point = depth.getDistance(fixValueX(i),fixValueY(j));
                        if(point>0.0f  && ( pointIsNotInsideRects(i, j, intersectionRects, contaniedRects) || objectIsContanied(boundingBox, contaniedRects)) ){
                            points.add(point);
                        }
                    }
                }

                System.out.println(points.contains(0.000000f));
                KMeans k = new KMeans(2);
                k.setPoints(points);
                k.computeClusters(10);
                System.out.println(k.getCentroids());
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


}

package com.example.testrealsense;

import android.content.Context;
import android.os.SystemClock;

import com.example.testrealsense.Helper.GraphicOverlay;
import com.example.testrealsense.Helper.ObjectGraphics;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.common.model.LocalModel;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.objects.DetectedObject;
import com.google.mlkit.vision.objects.ObjectDetection;
import com.google.mlkit.vision.objects.ObjectDetector;
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions;

import java.util.HashMap;
import java.util.List;

import androidx.annotation.NonNull;

public class Detector {
    private LocalModel localModel;
    ObjectDetector objectDetector;
    CustomObjectDetectorOptions customObjectDetectorOptions;
    Context context;
    BottomsheetC bs;
    boolean alarm;
    String assetmodel;

    GraphicOverlay graphicOverlay;
    ObjectGraphics drawBoundingBoxLabel;

    HashMap<String, Float> objectDict;

    Float scaleFactor;
    float[] points;

    public Detector(Context context, GraphicOverlay graphicOverlay, HashMap<String, Float> objectDict, BottomsheetC bs) {
        this.context = context;
        this.bs=bs;
        this.objectDict = objectDict;
        this.graphicOverlay = graphicOverlay;
        scaleFactor = Utils.calculateScaleFactor(graphicOverlay, 640);


        assetmodel = bs.getModelML_spinner().getSelectedItem().toString();

        localModel = new LocalModel.Builder()
                //.setAssetFilePath("models/lite-model_object_detection_mobile_object_labeler_v1_1.tflite")
                .setAssetFilePath("models/"+assetmodel)
                .build();
        customObjectDetectorOptions =
                new CustomObjectDetectorOptions.Builder(localModel)
                        .setDetectorMode(CustomObjectDetectorOptions.SINGLE_IMAGE_MODE)
                        .enableMultipleObjects()
                        .enableClassification()
                        .setClassificationConfidenceThreshold(0.5f)
                        .setMaxPerObjectLabelCount(1)
                        .build();
    }

    public void startDetection(InputImage image){
        objectDetector = ObjectDetection.getClient(customObjectDetectorOptions);
        long startTime = SystemClock.elapsedRealtime();

        objectDetector.process(image).addOnCompleteListener(new OnCompleteListener<List<DetectedObject>>() {
            @Override
            public void onComplete(@NonNull Task<List<DetectedObject>> task) {
                graphicOverlay.clear();

                computeDetectionTime(startTime);

                List<DetectedObject> detectedObjects = task.getResult();
                for (DetectedObject detectedObject : detectedObjects) {
                    //System.out.println(detectedObject.getLabels().get(0).getText());
                    /** Controllo della presenza dell'ggetto identificato nella lista di oggetti critici **/
                   // if (detectedObject.getLabels().size() > 0  && objectDict.containsKey(detectedObject.getLabels().get(0).getText())) {
                        //String label = detectedObject.getLabels().get(0).getText();
                        //Utils.calculateScaleFactor(graphicOverlay, image.getWidth());
                        alarm = false;
                        points = Utils.getScaledBoundingBox(detectedObject, scaleFactor);

                        //System.out.println(detectedObject.getBoundingBox().toString());

                        drawBoundingBoxLabel = new ObjectGraphics(detectedObject, graphicOverlay, scaleFactor, 5, alarm);
                        drawBoundingBoxLabel.drawBoundingBoxAndLabel();
                   // }

                }

                computeFPS(startTime);
            }
        });
    }


    void computeFPS(float startTime) {
        float endTime2 = SystemClock.elapsedRealtime();
        float elapsedMilliSeconds2 = endTime2 - startTime;
        float elapsedSeconds2 =  1000 / elapsedMilliSeconds2;
        bs.getFps().setText(elapsedSeconds2+"fps");
    }

    void computeDetectionTime(float startTime) {
        float endTime = SystemClock.elapsedRealtime();
        float elapsedMilliSeconds = endTime - startTime;
        bs.getMsDetection().setText(elapsedMilliSeconds+"ms");
    }


}

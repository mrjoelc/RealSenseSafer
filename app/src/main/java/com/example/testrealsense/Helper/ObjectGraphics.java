package com.example.testrealsense.Helper;

import android.graphics.Color;
import android.graphics.RectF;


import org.tensorflow.lite.task.vision.detector.Detection;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class ObjectGraphics {
    final static private Random mRandom = new Random(System.currentTimeMillis());
    private final Detection detectedObject;
    private final GraphicOverlay graphicOverlay;
    static private final Map<Integer,Integer> mapID = new HashMap<Integer,Integer>();
    private final float scaleFactor;
    private final float objectDepth;
    private static boolean alarm;

    public ObjectGraphics(Detection detectedObject, GraphicOverlay graphicOverlay, float scaleFactor, float objectDepth, boolean alarm){
        this.detectedObject=detectedObject;
        this.graphicOverlay=graphicOverlay;
        this.scaleFactor = scaleFactor;
        this.objectDepth = objectDepth;
        ObjectGraphics.alarm = alarm;
    }

    public void drawBoundingBoxAndLabel(){

        float[] scaledPoints = Utils.getScaledBoundingBox(detectedObject, scaleFactor);
        RectF boundingBox = new RectF(scaledPoints[0],scaledPoints[1],scaledPoints[2],scaledPoints[3]);

        int color = generateColor();
        RectOverlay rectOverlay = new RectOverlay(graphicOverlay, boundingBox, color) ;
        graphicOverlay.add(rectOverlay);

        String objectLabel;
        if(detectedObject.getCategories().size()>0) objectLabel = detectedObject.getCategories().get(0).getLabel();
        else objectLabel = "Unknown";

        TextOverlay labelOverlay = new TextOverlay(graphicOverlay, objectLabel, scaledPoints[2], scaledPoints[3], color);
        graphicOverlay.add(labelOverlay);

        TextOverlay depthOverlay = new TextOverlay(graphicOverlay, String.valueOf(objectDepth), scaledPoints[2], scaledPoints[3] - 45, color);
        graphicOverlay.add(depthOverlay);


    }


    static private int generateColor() {
        final int baseColor = Color.WHITE;

        final int baseRed = Color.red(baseColor);
        final int baseGreen = Color.green(baseColor);
        final int baseBlue = Color.blue(baseColor);

        /*final int red = (baseRed + mRandom.nextInt(256)) / 2;
        final int green = (baseGreen + mRandom.nextInt(256)) / 2;
        final int blue = (baseBlue + mRandom.nextInt(256)) / 2;*/

        if (alarm) {
            return Color.rgb(baseRed, 0, 0);
        }
        else {
            return Color.rgb(0, baseGreen, 0);
        }
    }


}

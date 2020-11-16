package com.example.testrealsense.Helper;

import android.content.Context;
import android.graphics.Color;
import android.graphics.RectF;
import android.widget.Toast;

import com.example.testrealsense.MainActivity;
import com.google.mlkit.vision.objects.DetectedObject;
import com.intel.realsense.librealsense.DepthFrame;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class ObjectGraphics {
    final static private Random mRandom = new Random(System.currentTimeMillis());
    private DetectedObject detectedObject;
    private GraphicOverlay graphicOverlay;
    private int imageWidth;
    static private Map<Integer,Integer> mapID = new HashMap<Integer,Integer>();
    static final int color = generateRandomColor();
    private float scaleFactor;
    private float objectDepth;


    public ObjectGraphics(DetectedObject detectedObject, GraphicOverlay graphicOverlay, int imageWidth, float objectDepth){
        this.detectedObject=detectedObject;
        this.graphicOverlay=graphicOverlay;
        this.imageWidth=imageWidth;
        this.scaleFactor = calculateScaleFactor();
        this.objectDepth = objectDepth;
    }

    public void drawBoundingBoxAndLabel(){
        System.out.println("scaleFactor: " + scaleFactor);
        float left = detectedObject.getBoundingBox().right * scaleFactor ;
        float top = detectedObject.getBoundingBox().top * scaleFactor;
        float right = detectedObject.getBoundingBox().left * scaleFactor;
        float bottom = detectedObject.getBoundingBox().bottom * scaleFactor;

        RectF boundingBox = new RectF(left,top,right,bottom);


        RectOverlay rectOverlay = new RectOverlay(graphicOverlay, boundingBox, color) ;
        graphicOverlay.add(rectOverlay);

        String objectLabel;
        if(detectedObject.getLabels().size()>0) objectLabel = detectedObject.getLabels().get(0).getText();
        else objectLabel = "Unknown";

        TextOverlay labelOverlay = new TextOverlay(graphicOverlay, objectLabel, right, bottom,color);
        graphicOverlay.add(labelOverlay);

        TextOverlay depthOverlay = new TextOverlay(graphicOverlay, String.valueOf(objectDepth), right, bottom - 45, color);
        graphicOverlay.add(depthOverlay);


    }

    private float calculateScaleFactor(){
        return (float) graphicOverlay.getWidth() / imageWidth;
    }

    public float getScaleFactor() {
        return scaleFactor;
    }

    static private int generateRandomColor() {
        // This is the base color which will be mixed with the generated one
        final int baseColor = Color.WHITE;

        final int baseRed = Color.red(baseColor);
        final int baseGreen = Color.green(baseColor);
        final int baseBlue = Color.blue(baseColor);

        final int red = (baseRed + mRandom.nextInt(256)) / 2;
        final int green = (baseGreen + mRandom.nextInt(256)) / 2;
        final int blue = (baseBlue + mRandom.nextInt(256)) / 2;

        return Color.rgb(red, green, blue);
    }


}

package com.example.testrealsense.Helper;

import android.graphics.Color;
import android.graphics.RectF;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.objects.DetectedObject;

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

    public ObjectGraphics(DetectedObject detectedObject, GraphicOverlay graphicOverlay, int imageWidth){
        this.detectedObject=detectedObject;
        this.graphicOverlay=graphicOverlay;
        this.imageWidth=imageWidth;

    }

    public void drawBoundingBoxAndLabel(){
        float scaleFactor = calculateScaleFactor();
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
        TextOverlay textOverlay = new TextOverlay(graphicOverlay, objectLabel, right, bottom,color);
        graphicOverlay.add(textOverlay);
    }

    private float calculateScaleFactor(){
        return (float) graphicOverlay.getWidth() / imageWidth;
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

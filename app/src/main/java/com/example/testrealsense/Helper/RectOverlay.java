package com.example.testrealsense.Helper;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

public class RectOverlay extends  GraphicOverlay.Graphic {
    private float mStrokeWidth = 10.0f;
    private Paint mRectPaint;
    private GraphicOverlay graphicOverlay;
    private Rect rect;
    private RectF rectF;
    private int color;

    public RectOverlay(GraphicOverlay graphicOverlay, Rect rect, int color) {
        super(graphicOverlay);
        mRectPaint = new Paint();

        mRectPaint.setStyle(Paint.Style.STROKE);
        mRectPaint.setStrokeWidth(mStrokeWidth);

        //this.graphicOverlay = graphicOverlay;
        this.rect = rect;
        this.color = color;

        postInvalidate();
    }

    public RectOverlay(GraphicOverlay graphicOverlay, RectF rectf, int color) {
        super(graphicOverlay);
        mRectPaint = new Paint();
        mRectPaint.setStyle(Paint.Style.STROKE);
        mRectPaint.setStrokeWidth(mStrokeWidth);

        //this.graphicOverlay = graphicOverlay;
        this.rectF = rectf;
        this.color = color;
        postInvalidate();
    }

    @Override
    public void draw(Canvas canvas) {
        mRectPaint.setColor(color);

        if(rect==null){
            canvas.drawRoundRect(rectF, 15, 15, mRectPaint);
            //canvas.drawRect(rectF, mRectPaint);
        }
        else{
            //canvas.drawRoundRect(rect, 6, 6, mRectPaint);
            canvas.drawRect(rect, mRectPaint);}

    }
}

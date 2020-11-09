package com.example.testrealsense.Helper;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

public class RectOverlay extends  GraphicOverlay.Graphic {
    private int mRectColor = Color.GREEN;
    private float mStrokeWidth = 4.0f;
    private Paint mRectPaint;
    private GraphicOverlay graphicOverlay;
    private Rect rect;
    private RectF rectF;

    public RectOverlay(GraphicOverlay graphicOverlay, Rect rect) {
        super(graphicOverlay);
        mRectPaint = new Paint();
        mRectPaint.setColor(mRectColor);
        mRectPaint.setStyle(Paint.Style.STROKE);
        mRectPaint.setStrokeWidth(mStrokeWidth);

        //this.graphicOverlay = graphicOverlay;
        this.rect = rect;

        postInvalidate();
    }

    public RectOverlay(GraphicOverlay graphicOverlay, RectF rectf) {
        super(graphicOverlay);
        mRectPaint = new Paint();
        mRectPaint.setColor(mRectColor);
        mRectPaint.setStyle(Paint.Style.STROKE);
        mRectPaint.setStrokeWidth(mStrokeWidth);

        //this.graphicOverlay = graphicOverlay;
        this.rectF = rectf;

        postInvalidate();
    }

    @Override
    public void draw(Canvas canvas) {
        /*RectF rectF = new RectF(rect);
        rectF.left = translateX(rectF.left);
        rectF.top = translateX(rectF.top);
        rectF.right = translateX(rectF.right);
        rectF.bottom = translateX(rectF.bottom);*/
        if(rect==null)
            canvas.drawRect(rectF, mRectPaint);
        else
            canvas.drawRect(rect, mRectPaint);

    }
}

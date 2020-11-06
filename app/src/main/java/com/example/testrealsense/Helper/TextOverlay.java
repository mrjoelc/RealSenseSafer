package com.example.testrealsense.Helper;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;

public class TextOverlay extends  GraphicOverlay.Graphic {
    private int mTextColor = Color.GREEN;
    private float mStrokeWidth = 4.0f;
    private Paint mTextPaint, trPaint;
    private GraphicOverlay graphicOverlay;
    private String text;
    private float x,y;


    public TextOverlay(GraphicOverlay graphicOverlay, String text, float x, float y) {
        super(graphicOverlay);
        trPaint = new Paint();
        trPaint.setColor(0xff00ff00);
        trPaint.setStyle(Paint.Style.FILL);

        mTextPaint = new Paint();
        mTextPaint.setColor(Color.BLACK);
        mTextPaint.setTextSize(45);
        Typeface tf = Typeface.create("Helvetica",Typeface.NORMAL);
        mTextPaint.setTypeface(tf);
        this.graphicOverlay = graphicOverlay;
        this.text = text;
        this.x=x;
        this.y=y;



        postInvalidate();
    }

    @Override
    public void draw(Canvas canvas) {
        float text_width = mTextPaint.measureText(text)/2;
        float text_size = mTextPaint.getTextSize();
        float text_center_x = x - 2;
        float text_center_y = y - text_size;
        canvas.drawRect(text_center_x, text_center_y, (float) (text_center_x + 2.2 * text_width), text_center_y + text_size, trPaint);
        canvas.drawText(text,x,y, mTextPaint);

    }
}

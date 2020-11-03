package com.example.testrealsense;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.View;

public class DrawView extends View {
    Paint paint = new Paint();

    public DrawView(Context context) {
        super(context);
    }

    public void onDraw(Canvas canvas, Rect boundybox) {
        paint.setColor(Color.RED);
        paint.setStrokeWidth(3);
        canvas.drawRect(boundybox, paint);
        paint.setStrokeWidth(0);

    }

}
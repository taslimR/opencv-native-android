package com.example.test_opencv;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.view.View;

import androidx.annotation.RequiresApi;

public class Box extends View {
    private Paint paint = new Paint();
    StaticLayout staticLayout;
    TextPaint textPaint;
    String text0 = "Place your NID card inside this box";

    Box(Context context) {
        super(context);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onDraw(Canvas canvas) { // Override the onDraw() Method
        super.onDraw(canvas);

        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.WHITE);
        paint.setStrokeWidth(5);


        //center
        int x0 = canvas.getWidth()/2;
        int y0 = canvas.getWidth()/2;
        int dx = canvas.getWidth()/2;
        int dy = (int) ((Double.valueOf(canvas.getWidth()/2)* 0.6));

        //draw guide box
//        canvas.drawRect(x0-dx+(dx/10), y0-dy, x0+dx-(dx/10), y0+dy, paint);
        canvas.drawRoundRect(x0-dx+(dx/10), y0-dy, x0+dx-(dx/10), y0+dy, 50, 50, paint);

        float textSize = 20 * getResources().getDisplayMetrics().density;

        Rect myRect = new Rect();
        myRect.set(x0-dx+(dx/10),y0 -(int)textSize, x0+dx-(dx/10),y0 + (int)textSize/2);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        paint.setAlpha(127);
        canvas.drawRect(myRect, paint);


        // text0
        textPaint = new TextPaint();
//        textPaint.setColor(Color.parseColor("#008C3C"));
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(textSize);
        textPaint.setTextAlign(Paint.Align.CENTER);
//        textPaint.setAlpha(200);
        canvas.drawText(text0, x0, y0, textPaint);



    }
}

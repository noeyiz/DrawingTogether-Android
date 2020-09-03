package com.hansung.drawingtogether.view.drawing;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Xfermode;

import com.hansung.drawingtogether.data.remote.model.MyLog;

public class Rect extends DrawingComponent {
    DrawingEditor de = DrawingEditor.getInstance();

    @Override
    public void draw(Canvas canvas) {
        //canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR); // Clear the canvas with a transparent color
        //drawComponent(canvas);

        if(canvas == de.getMyCurrentCanvas()) {
            de.clearMyCurrentBitmap();
            drawComponent(canvas);
        } else if(canvas == de.getCurrentCanvas()) {
            de.clearCurrentBitmap();
            de.drawOthersCurrentComponent(null);
        }
    }

    @Override
    public void drawComponent(Canvas canvas) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        if(isErased) {
            Xfermode xmode = new PorterDuffXfermode(PorterDuff.Mode.CLEAR);
            paint.setXfermode(xmode);
            paint.setStrokeWidth(this.strokeWidth + 1);
        } else {
            paint.setStrokeWidth(this.strokeWidth);
        }

        Point from = this.beginPoint;
        Point to = this.endPoint;

        try {
            paint.setStyle(Paint.Style.FILL);       //채우기
            try {
                paint.setColor(Color.parseColor(this.fillColor));
            } catch(NullPointerException e) {
                MyLog.w("catch", "parseColor");
            }
            paint.setAlpha(this.fillAlpha);
            canvas.drawRect(from.x * xRatio, from.y * yRatio, to.x * xRatio, to.y * yRatio, paint);

            paint.setStyle(Paint.Style.STROKE);     //윤곽선
            try {
                paint.setColor(Color.parseColor(this.strokeColor));
            } catch(NullPointerException e) {
                MyLog.w("catch", "parseColor");
            }
            paint.setAlpha(this.strokeAlpha);
            canvas.drawRect(from.x * xRatio, from.y * yRatio, to.x * xRatio, to.y * yRatio, paint);
        }catch(NullPointerException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        return null;
    }
}

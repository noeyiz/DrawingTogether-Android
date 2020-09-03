package com.hansung.drawingtogether.view.drawing;

import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.MaskFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Xfermode;

import com.hansung.drawingtogether.data.remote.model.MyLog;


public class Stroke extends DrawingComponent {
    DrawingEditor de = DrawingEditor.getInstance();

    @Override
    public void draw(Canvas canvas) {
        //Log.i("drawing", this.id + " draw()");

        //de.clearCurrentBitmap();
        //canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR); // Clear the canvas with a transparent color

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
        Path path = new Path();
        float mX, mY;
        float x, y;

        if(this.points.size() < 1) {
            return;
        } else if(this.points.size() == 1) {
            path.moveTo(this.points.get(0).x  * xRatio, this.points.get(0).y * yRatio);
            path.lineTo(this.points.get(0).x * xRatio, this.points.get(0).y * yRatio);

        } else {
            mX = this.points.get(0).x  * xRatio;
            mY = this.points.get(0).y * yRatio;
            path.moveTo(mX, mY);

            for(int i=0; i<this.points.size()-1; i++) {

                try {
                    x = this.points.get(i+1).x * xRatio;
                    y = this.points.get(i+1).y * yRatio;
                    path.quadTo(mX, mY, (x + mX)/2, (y + mY)/2);
                    mX = x;
                    mY = y;

                }catch(NullPointerException e) {
                    e.printStackTrace();
                }
            }

            path.lineTo(mX, mY);
        }


        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        /*if(isErased) {
            Xfermode xmode = new PorterDuffXfermode(PorterDuff.Mode.CLEAR);
            paint.setXfermode(xmode);
            paint.setStrokeWidth(this.strokeWidth + 1);
        } else {
            paint.setStrokeWidth(this.strokeWidth);
        }*/

        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStyle(Paint.Style.STROKE);

        if(this.penMode == PenMode.NEON) {
            try {
                paint.setColor(Color.parseColor(this.strokeColor));
            } catch(NullPointerException e) {
                MyLog.w("catch", "parseColor");
            }
            paint.setStrokeWidth(this.strokeWidth);
            paint.setMaskFilter(new BlurMaskFilter(this.strokeWidth + 10, BlurMaskFilter.Blur.NORMAL));
            canvas.drawPath(path, paint);

            Paint paint2 = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint2.setStrokeWidth(this.strokeWidth);
            paint2.setStrokeCap(Paint.Cap.ROUND);
            paint2.setStrokeJoin(Paint.Join.ROUND);
            paint2.setStyle(Paint.Style.STROKE);
            paint2.setColor(Color.WHITE);
            canvas.drawPath(path, paint2);

        } else {
            try {
                paint.setColor(Color.parseColor(this.strokeColor));
            } catch(NullPointerException e) {
                MyLog.w("catch", "parseColor");
            }

            if(this.penMode == PenMode.HIGHLIGHT) {
                paint.setAlpha(de.getHighlightAlpha());
                paint.setStrokeWidth(this.strokeWidth * 2);
            } else if (this.penMode == PenMode.NORMAL) {
                paint.setAlpha(de.getNormalAlpha());
                paint.setStrokeWidth(this.strokeWidth);
            }

            canvas.drawPath(path, paint);
        }

    }

    @Override
    public String toString() {
        return null;
    }

}

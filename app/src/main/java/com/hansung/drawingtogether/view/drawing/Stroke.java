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

        /*Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStrokeWidth(this.strokeWidth);
        try {
            paint.setColor(Color.parseColor(this.strokeColor));
        } catch(NullPointerException e) {
            MyLog.w("catch", "parseColor");
        }
        paint.setAlpha(this.strokeAlpha);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStyle(Paint.Style.STROKE);*/

        //fixme minj
        //Point from = (this.preSize == 0) ? this.points.get(preSize) : this.points.get(preSize-1);
        //Point to = this.points.get(preSize);
        /*Point from, to;
        if(this.points.size() == 1) {
            from = this.points.get(0);
            to = this.points.get(0);
        } else {
            from = this.points.get(this.points.size()-2);
            to = this.points.get(this.points.size()-1);
        }*/

        /*try {
            canvas.drawLine(from.x * xRatio, from.y * yRatio, to.x * xRatio, to.y * yRatio, paint);
        }catch(NullPointerException e) {
            e.printStackTrace();
        }*/
        //Log.i("drawing", this.points.toString());
        //Log.i("drawing", "(" + from.x * xRatio + ", " +  from.y * yRatio + ") -> (" +  to.x * xRatio + ", " + to.y * yRatio + ")");

        /*Point from, to;
        if(this.points.size() == 1) {
            from = this.points.get(0);
            to = this.points.get(0);

            try {
                canvas.drawLine(from.x * xRatio, from.y * yRatio, to.x * xRatio, to.y * yRatio, paint);
            }catch(NullPointerException e) {
                e.printStackTrace();
            }

        } else if(this.points.size() > 1 && this.preSize < this.points.size()) {
            for(int i=this.preSize; i<this.points.size(); i++) {
                from = this.points.get(i-1);
                to = this.points.get(i);

                try {
                    canvas.drawLine(from.x * xRatio, from.y * yRatio, to.x * xRatio, to.y * yRatio, paint);
                }catch(NullPointerException e) {
                    e.printStackTrace();
                }
            }
        }*/
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
                //Point from = this.points.get(i);
                //Point to = this.points.get(i+1);

                try {
                    x = this.points.get(i+1).x * xRatio;
                    y = this.points.get(i+1).y * yRatio;
                    path.quadTo(mX, mY, (x + mX)/2, (y + mY)/2);
                    mX = x;
                    mY = y;

                    //path.lineTo(this.points.get(i+1).x * xRatio, this.points.get(i+1).y * yRatio);

                    //canvas.drawLine(from.x * xRatio, from.y * yRatio, to.x * xRatio, to.y * yRatio, paint);
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

        /*try {
            paint.setColor(Color.parseColor(this.strokeColor));
        } catch(NullPointerException e) {
            MyLog.w("catch", "parseColor");
        }*/
        //paint.setAlpha(this.strokeAlpha);

        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStyle(Paint.Style.STROKE);
        //paint.setMaskFilter(new BlurMaskFilter(30, BlurMaskFilter.Blur.NORMAL));

        if(this.penMode == PenMode.NEON) {
            paint.setStrokeWidth(this.strokeWidth);
            try {
                paint.setColor(Color.parseColor(this.strokeColor));
            } catch(NullPointerException e) {
                MyLog.w("catch", "parseColor");
            }
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





        /*Paint paint2 = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint2.setStrokeWidth(this.strokeWidth);
        paint2.setColor(Color.parseColor("#FFFFFF"));
        paint2.setAlpha(255);
        paint2.setStrokeCap(Paint.Cap.ROUND);
        paint2.setStrokeJoin(Paint.Join.ROUND);
        paint2.setStyle(Paint.Style.STROKE);

        canvas.drawPath(path, paint2);*/

    }

    @Override
    public String toString() {
        return null;
    }

}

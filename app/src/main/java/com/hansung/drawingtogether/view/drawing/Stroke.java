package com.hansung.drawingtogether.view.drawing;

import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Xfermode;

import com.hansung.drawingtogether.data.remote.model.MyLog;


public class Stroke extends DrawingComponent {
    //DrawingEditor de = DrawingEditor.getInstance();

    @Override
    public void draw(Canvas canvas) {
        //Log.i("drawing", this.id + " draw()");

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStrokeWidth(this.strokeWidth);
        try {
            paint.setColor(Color.parseColor(this.strokeColor));
        } catch(NullPointerException e) {
            MyLog.w("catch", "parseColor");
        }
        paint.setAlpha(this.strokeAlpha);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStyle(Paint.Style.STROKE);

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

        Point from, to;
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
        }
    }

    @Override
    public void drawComponent(Canvas canvas) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        /*if(isErased) {
            Xfermode xmode = new PorterDuffXfermode(PorterDuff.Mode.CLEAR);
            paint.setXfermode(xmode);
            paint.setStrokeWidth(this.strokeWidth + 1);
        } else {
            paint.setStrokeWidth(this.strokeWidth);
        }*/
        paint.setStrokeWidth(this.strokeWidth);
        try {
            paint.setColor(Color.parseColor(this.strokeColor));
        } catch(NullPointerException e) {
            MyLog.w("catch", "parseColor");
        }
        paint.setAlpha(this.strokeAlpha);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStyle(Paint.Style.STROKE);
        //paint.setMaskFilter(new BlurMaskFilter(30, BlurMaskFilter.Blur.NORMAL));


        Path path = new Path();

        if(this.points.size() < 1) {
            return;
        } else if(this.points.size() == 1) {
            path.moveTo(this.points.get(0).x  * xRatio, this.points.get(0).y * yRatio);
            path.lineTo(this.points.get(0).x * xRatio, this.points.get(0).y * yRatio);

        } else {

            path.moveTo(this.points.get(0).x  * xRatio, this.points.get(0).y * yRatio);

            for(int i=0; i<this.points.size()-1; i++) {
                //Point from = this.points.get(i);
                //Point to = this.points.get(i+1);

                try {
                    path.lineTo(this.points.get(i+1).x * xRatio, this.points.get(i+1).y * yRatio);
                    //canvas.drawLine(from.x * xRatio, from.y * yRatio, to.x * xRatio, to.y * yRatio, paint);
                }catch(NullPointerException e) {
                    e.printStackTrace();
                }
            }
        }

        canvas.drawPath(path, paint);


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

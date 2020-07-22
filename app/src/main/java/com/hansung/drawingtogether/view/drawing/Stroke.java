package com.hansung.drawingtogether.view.drawing;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Xfermode;


public class Stroke extends DrawingComponent {
    //DrawingEditor de = DrawingEditor.getInstance();

    @Override
    public void draw(Canvas canvas) {
        //Log.i("drawing", this.id + " draw()");

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStrokeWidth(this.strokeWidth);
        paint.setColor(Color.parseColor(this.strokeColor));
        paint.setAlpha(this.strokeAlpha);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);

        //fixme minj
        Point from = (this.preSize == 0) ? this.points.get(preSize) : this.points.get(preSize-1);
        Point to = this.points.get(preSize);
        /*Point from, to;
        if(this.points.size() == 1) {
            from = this.points.get(0);
            to = this.points.get(0);
        } else {
            from = this.points.get(this.points.size()-2);
            to = this.points.get(this.points.size()-1);
        }*/

        try {
            canvas.drawLine(from.x * xRatio, from.y * yRatio, to.x * xRatio, to.y * yRatio, paint);
        }catch(NullPointerException e) {
            e.printStackTrace();
        }
        //Log.i("drawing", this.points.toString());
        //Log.i("drawing", "(" + from.x * xRatio + ", " +  from.y * yRatio + ") -> (" +  to.x * xRatio + ", " + to.y * yRatio + ")");
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
        paint.setColor(Color.parseColor(this.strokeColor));
        paint.setAlpha(this.strokeAlpha);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);

        for(int i=0; i<this.points.size()-1; i++) {
            Point from = this.points.get(i);
            Point to = this.points.get(i+1);

            try {
                canvas.drawLine(from.x * xRatio, from.y * yRatio, to.x * xRatio, to.y * yRatio, paint);
            }catch(NullPointerException e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    public String toString() {
        return null;
    }

}

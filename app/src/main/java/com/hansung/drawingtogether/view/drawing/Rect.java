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
        /*Point from = this.beginPoint;
        Point to = this.endPoint;

        Point prePoint = (this.preSize == 0) ? this.points.get(preSize) : this.points.get(preSize-1);

        int width = Math.abs(to.x - from.x);
        int height = Math.abs(to.y - from.y);

        int preWidth = Math.abs(prePoint.x - from.x);
        int preHeight = Math.abs(prePoint.y - from.y);

        if(width < preWidth || height < preHeight) {
            de.redraw();
            this.drawComponent(canvas);

        } else {
            this.drawComponent(canvas);
        }*/

        de.redraw(this.usersComponentId);
        drawComponent(canvas);
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

        paint.setStrokeWidth(this.strokeWidth);

        Point from = this.beginPoint;
        Point to = this.endPoint;

        try {
            paint.setStyle(Paint.Style.FILL);       //채우기
            try {
                paint.setColor(Color.parseColor(this.fillColor));
            } catch(NullPointerException e) {
                MyLog.i("catch", "parseColor");
            }
            paint.setAlpha(this.fillAlpha);
            canvas.drawRect(from.x * xRatio, from.y * yRatio, to.x * xRatio, to.y * yRatio, paint); //fixme alpha 적용되면 strokeWidth/2만큼 작은 사각형

            paint.setStyle(Paint.Style.STROKE);     //윤곽선
            try {
                paint.setColor(Color.parseColor(this.strokeColor));
            } catch(NullPointerException e) {
                MyLog.i("catch", "parseColor");
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

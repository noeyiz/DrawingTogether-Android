package com.hansung.drawingtogether.view.drawing;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.Xfermode;

public class Oval  extends DrawingComponent {
    DrawingEditor de = DrawingEditor.getInstance();

    @Override
    public void draw(Canvas canvas) {
        de.redraw(this.usersComponentId);
        this.drawComponent(canvas);
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
            RectF oval = new RectF(from.x * xRatio, from.y * yRatio, to.x * xRatio, to.y * yRatio);
            RectF fillOval = new RectF(from.x * xRatio, from.y * yRatio, to.x * xRatio, to.y * yRatio);   //fixme alpha 적용되면 strokeWidth/2만큼 작은 사각형

            paint.setStyle(Paint.Style.FILL);       //채우기
            paint.setColor(Color.parseColor(this.fillColor));
            paint.setAlpha(this.fillAlpha);
            canvas.drawOval(oval, paint);

            paint.setStyle(Paint.Style.STROKE);     //윤곽선
            paint.setColor(Color.parseColor(this.strokeColor));
            paint.setAlpha(this.strokeAlpha);
            canvas.drawOval(fillOval, paint);
        }catch(NullPointerException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        return null;
    }
}

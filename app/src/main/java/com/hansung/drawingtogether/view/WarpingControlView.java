package com.hansung.drawingtogether.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewTreeObserver;

import androidx.appcompat.widget.AppCompatImageView;

import com.hansung.drawingtogether.BitmapProcess;
import com.hansung.drawingtogether.data.remote.model.MQTTClient;
import com.hansung.drawingtogether.data.remote.model.WarpData;
import com.hansung.drawingtogether.util.DelaunayTriangulation;
import com.hansung.drawingtogether.util.DoublePoint;
import com.hansung.drawingtogether.util.Triangle;
import com.hansung.drawingtogether.view.drawing.DrawingEditor;
import com.hansung.drawingtogether.view.drawing.JSONParser;
import com.hansung.drawingtogether.view.drawing.MqttMessageFormat;
import com.hansung.drawingtogether.view.main.WarpingMessage;

import java.util.Vector;


public class WarpingControlView extends AppCompatImageView {
	private DrawingEditor de = DrawingEditor.getInstance();
	private MQTTClient client = MQTTClient.getInstance();
	private JSONParser parser = JSONParser.getInstance();
	private Bitmap undo=null;
	private Bitmap image=null;
	private Bitmap warpingImage = null;
	private int touchX[] = new int[2];
	private int touchY[] = new int[2];
	private int touchUpX[] = new int[2];
	private int touchUpY[] = new int[2];
	private int limit;
	int pointerCount = 1;
	boolean flag = false;
	boolean flag2 = false;
	private boolean cancel = false;

	public WarpingControlView(Context context) {
		super(context);
	}

	public WarpingControlView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public void setImage(final Bitmap img) {
		if (img == null)
			image = null;
		else
			image = Bitmap.createScaledBitmap(img, getWidth(), getHeight(), true);
		invalidate();
	}

	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		this.setImageBitmap(image);
	}

	public Bitmap getImage(){
		return image;
	}

	public void undo(){
		if(undo!=null){
			image = undo;
			invalidate();
		}
	}

	public void sendDrawMqttMessage(int action, WarpingMessage warpingMessage) {
		MqttMessageFormat messageFormat = new MqttMessageFormat(de.getMyUsername(), de.getCurrentMode(), de.getCurrentType(), action, warpingMessage);
		client.publish(client.getTopic_data(),  parser.jsonWrite(messageFormat));
	}

	public boolean onTouchEvent(MotionEvent event) {
		if (warping(event)) {
			sendDrawMqttMessage(event.getAction(), new WarpingMessage(event, getWidth(), getHeight()));
		}
		return true;
	}

	public void dispatchEvent(MotionEvent event) {
		warping(event);
	}

	public void setCancel(boolean cancel) {
		this.cancel = cancel;
	}

	public Boolean warping(MotionEvent event) {
		if (image == null)
			return false;
		int pointer_count = event.getPointerCount(); //현재 터치 발생한 포인트 수를 얻는다.
		if(pointer_count >= 2) {
			pointer_count = 2; //2개 이상의 포인트를 터치했더라도 2개까지만 처리를 한다.
			pointerCount = 2;
		}
		Log.e("count", pointer_count + " ");


		int action = event.getAction();
		switch (action & MotionEvent.ACTION_MASK){
			case MotionEvent.ACTION_DOWN:
				touchX[0] = (int)event.getX();
				touchY[0] = (int)event.getY();
				flag = false;
				flag2 = false;
				break;
			case MotionEvent.ACTION_POINTER_DOWN:
				flag = false;
				flag2 = false;
				for (int i = 0; i < pointer_count; i++) {
					touchX[i] = (int)event.getX(i);
					touchY[i] = (int)event.getY(i);
				}
				flag2 = Math.abs(touchX[0] - touchX[1]) > Math.abs(touchY[0] - touchY[1]);
				if (Math.abs(touchX[0] - touchX[1]) > Math.abs(touchY[0] - touchY[1]))
					limit = (touchX[0] + touchX[1])/2;
				else
					limit = (touchY[0] + touchY[1])/2;
				break;
			case MotionEvent.ACTION_MOVE:
				for (int i = 0; i < pointer_count; i++) {
					touchUpX[i] = (int)event.getX(i);
					touchUpY[i] = (int)event.getY(i);
				}
				Vector<DoublePoint> vector = new Vector<DoublePoint>();
				vector.add(new DoublePoint(0, 0));
				vector.add(new DoublePoint(this.getWidth()-1,0));
				vector.add(new DoublePoint(this.getWidth()-1,this.getHeight()-1));
				vector.add(new DoublePoint(0,this.getHeight()-1));
				for (int i = 0; i < pointer_count; i++) {
					if (touchX[i] < 1 || touchX[i] > getWidth() - 2 || touchY[i] < 1 || touchY[i] > getHeight() - 2)
						return false;
					else if ((int)event.getX(i) < 1 || (int)event.getX(i) > getWidth() - 2 || (int)event.getY(i) < 1 || (int)event.getY(i) > getHeight() - 2)
						return false;
					vector.add(new DoublePoint(touchX[i], touchY[i], (int)event.getX(i), (int)event.getY(i)));
					if (Math.abs(touchX[i] - touchUpX[i]) > 50 || Math.abs(touchY[i] - touchUpY[i]) > 50) {
						flag = true;
					}
				}
				if (pointer_count > 1) {
					if (flag2) {
						if (touchX[0] < touchX[1])
							flag = !(limit < touchUpX[0] || limit > touchUpX[1]);
						else
							flag = !(limit > touchUpX[0] || limit < touchUpX[1]);
					} else {
						if (touchY[0] < touchY[1])
							flag = !(limit < touchUpY[0] || limit > touchUpY[1]);
						else
							flag = !(limit > touchUpY[0] || limit < touchUpY[1]);
					}
				}
				if (flag) {
					pointerCount = 1;
					Triangle srcTriangle = new Triangle();
					Triangle dstTriangle = new Triangle();
					DelaunayTriangulation dt = new DelaunayTriangulation(vector, srcTriangle, dstTriangle);
					dt.Triangulation();

//					undo = Bitmap.createBitmap(image);
					warpingImage = BitmapProcess.warping(image, srcTriangle, dstTriangle);
					image = warpingImage;
					for (int i = 0; i < pointer_count; i++) {
						touchX[i] = (int)event.getX(i);
						touchY[i] = (int)event.getY(i);
					}
					flag = false;
				}
				break;
			case MotionEvent.ACTION_UP:
				flag = false;
				flag2 = false;
				cancel = false;
				break;
		}
		return true;
	}

	public void warping2(WarpingMessage message) {
		if (message.getWarpData() == null)
			return;
		WarpData data = message.getWarpData();
		int action = data.getAction();
		Point[] points = data.getPoints();
		for (int i = 0; i < points.length; i++) {
			points[i].x = points[i].x * getWidth() / message.getWidth();
			points[i].y = points[i].y * getHeight() / message.getHeight();
			if (points[i].x < 0 || points[i].x > getWidth() - 1 || points[i].y < 0 || points[i].y > getHeight() - 1)
				return;
		}

		switch (action & MotionEvent.ACTION_MASK){
			case MotionEvent.ACTION_DOWN:
				touchX[0] = points[0].x;
				touchY[0] = points[0].y;
				flag = false;
				flag2 = false;
				break;
			case MotionEvent.ACTION_POINTER_DOWN:
				flag = false;
				flag2 = false;
				for (int i = 0; i < points.length; i++) {
					touchX[i] = points[i].x;
					touchY[i] = points[i].y;
				}
				flag2 = Math.abs(touchX[0] - touchX[1]) > Math.abs(touchY[0] - touchY[1]);
				if (Math.abs(touchX[0] - touchX[1]) > Math.abs(touchY[0] - touchY[1]))
					limit = (touchX[0] + touchX[1])/2;
				else
					limit = (touchY[0] + touchY[1])/2;
				break;
			case MotionEvent.ACTION_MOVE:
				for (int i = 0; i < points.length; i++) {
					touchUpX[i] = points[i].x;
					touchUpY[i] = points[i].y;
				}
				Vector<DoublePoint> vector = new Vector<DoublePoint>();
				vector.add(new DoublePoint(0, 0));
				vector.add(new DoublePoint(this.getWidth()-1,0));
				vector.add(new DoublePoint(this.getWidth()-1,this.getHeight()-1));
				vector.add(new DoublePoint(0,this.getHeight()-1));
				for (int i = 0; i < points.length; i++) {
					vector.add(new DoublePoint(touchX[i], touchY[i], points[i].x, points[i].y));
					if (Math.abs(touchX[i] - touchUpX[i]) > 50 || Math.abs(touchY[i] - touchUpY[i]) > 50) {
						flag = true;
					}
				}
				if (points.length > 1) {
					if (flag2) {
						if (touchX[0] < touchX[1])
							flag = !(limit < touchUpX[0] || limit > touchUpX[1]);
						else
							flag = !(limit > touchUpX[0] || limit < touchUpX[1]);
					} else {
						if (touchY[0] < touchY[1])
							flag = !(limit < touchUpY[0] || limit > touchUpY[1]);
						else
							flag = !(limit > touchUpY[0] || limit < touchUpY[1]);
					}
				}
				if (flag) {
					pointerCount = 1;
					Triangle srcTriangle = new Triangle();
					Triangle dstTriangle = new Triangle();
					DelaunayTriangulation dt = new DelaunayTriangulation(vector, srcTriangle, dstTriangle);
					dt.Triangulation();

//					undo = Bitmap.createBitmap(image);
					image = BitmapProcess.warping(image, srcTriangle, dstTriangle);
					invalidate();
					for (int i = 0; i < points.length; i++) {
						touchX[i] = points[i].x;
						touchY[i] = points[i].y;
					}
					flag = false;
				}
				break;
			case MotionEvent.ACTION_UP:
				flag = false;
				flag2 = false;
				break;
		}
	}

}

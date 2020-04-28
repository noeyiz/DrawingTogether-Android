package com.hansung.drawingtogether.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewTreeObserver;

import androidx.appcompat.widget.AppCompatImageView;

import com.hansung.drawingtogether.BitmapProcess;
import com.hansung.drawingtogether.data.remote.model.MQTTClient;
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
	private int touchX[] = new int[2];
	private int touchY[] = new int[2];
	private int touchUpX[] = new int[2];
	private int touchUpY[] = new int[2];
	private int limit;
	int pointerCount = 1;
	boolean flag = false;
	boolean flag2 = false;

	public WarpingControlView(Context context) {
		super(context);
	}

	public WarpingControlView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public void setImage(final Bitmap img) {
		getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				image = Bitmap.createScaledBitmap(img, getWidth(), getHeight(), true);
				invalidate();
				getViewTreeObserver().removeOnGlobalLayoutListener(this);
			}
		});
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
		warping(event);
		sendDrawMqttMessage(event.getAction(), new WarpingMessage(event));
		return true;
	}

	public void dispatchEvent(MotionEvent event) {
		warping(event);
	}

	public void warping(MotionEvent event) {
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
					image = BitmapProcess.warping(image, srcTriangle, dstTriangle);
					invalidate();
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
				break;
		}
	}
}

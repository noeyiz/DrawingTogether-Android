package com.hansung.drawingtogether.util;

import android.app.Activity;
import android.content.Context;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.Display;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.AbsoluteLayout;
import android.widget.ImageView;

import androidx.appcompat.widget.AppCompatButton;

import com.hansung.drawingtogether.R;

import java.util.Vector;


@SuppressWarnings("deprecation")
public class PointView extends AbsoluteLayout {
	private static final int STATEBAR_SIZE = 25;
	private static final int POINT_SIZE = 25;
	private static final int WASTBASKET_SIZE = 60;
	private static final int POINT_DISTANCE = 20;
	private Context parent;
	private int wastbasketX, wastbasketY, wastbasketW, wastbasketH;
	private int mode = 0;

	public static final int MODE_1 = 0;
	public static final int MODE_2 = 1;
	public boolean running = false;
	
	private Vector<PointButton> vector = new Vector<PointButton>();

	public PointView(Context parent, AttributeSet attrs) {
		super(parent, attrs);
		this.parent = parent;
		init();
	}

	public PointView(Context parent){
		super(parent);
		this.parent = parent;
		init();

	}
	public void init(){
		ImageView wastbasket = new ImageView(parent);

		Display display = ((WindowManager)parent.getSystemService("window")).getDefaultDisplay();
	
		wastbasketX = display.getWidth()-WASTBASKET_SIZE-5;
		wastbasketY = 5;
		wastbasketW = WASTBASKET_SIZE;
		wastbasketH = WASTBASKET_SIZE;
		wastbasket.setBackgroundResource(R.drawable.wastebasket);
		wastbasket.setLayoutParams(new LayoutParams(wastbasketW, wastbasketH, wastbasketX, wastbasketY));
		this.addView(wastbasket);
	}
	
	public Vector<PointButton> getVector(){
		vector.add(new PointButton(parent,0,0));
		vector.add(new PointButton(parent,this.getWidth()-1,0));
		vector.add(new PointButton(parent,this.getWidth()-1,this.getHeight()-1));
		vector.add(new PointButton(parent,0,this.getHeight()-1));
		
		return vector;
	}
	
	public void setMode(int m){
		if(mode==m){
			return;
		}
		mode = m;
		final int pointCount = vector.size();
		if(mode == MODE_1){
			new Thread(new Runnable(){
				public void run() {
					running = true;
					for(float j=0.1f; j<=1.0f; j+=0.025f){
						SystemClock.sleep(20);
						for(int i=0; i<pointCount; i++){
							final PointButton p = (PointButton)vector.elementAt(i);
							final int weightx = (int) ((p.dp.p1.x - p.dp.p2.x) *j);
							final int weighty = (int) ((p.dp.p1.y - p.dp.p2.y) *j);
							((Activity)parent).runOnUiThread(new Runnable(){
								public void run() {
									p.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT,
											LayoutParams.WRAP_CONTENT,(int) p.dp.p2.x + weightx - POINT_SIZE,(int) p.dp.p2.y + weighty - POINT_SIZE));
								}
							});
						}
					}
					running = false;
				}

			}).start();
		}
		else{
			new Thread(new Runnable(){
				public void run() {
					running = true;
					for(float j=0.1f; j<=1.0f; j+=0.025f){
						SystemClock.sleep(20);
						for(int i=0; i<pointCount; i++){
							final PointButton p = (PointButton)vector.elementAt(i);
							final int weightx = (int) ((p.dp.p2.x - p.dp.p1.x) *j);
							final int weighty = (int) ((p.dp.p2.y - p.dp.p1.y) *j);
							((Activity)parent).runOnUiThread(new Runnable(){
								public void run() {
									p.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT,
											LayoutParams.WRAP_CONTENT,(int) p.dp.p1.x + weightx - POINT_SIZE, (int) p.dp.p1.y + weighty - POINT_SIZE));
								}
							});

						}
					}
					running = false;
				}

			}).start();
		}
	}

	public boolean onTouchEvent(MotionEvent event) {
		if(event.getX() > 1 && event.getX() < this.getWidth()-2 ||
				event.getY() > 1 && event.getY() < this.getHeight()-2){
			int x = (int) event.getX();
			int y = (int) event.getY();
			PointButton p = new PointButton(parent,x,y);
			vector.add(p);
			p.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, x - POINT_SIZE, y - POINT_SIZE));
			this.addView(p);
		}
		return false;
	}
	
	public class PointButton extends AppCompatButton {
		public DoublePoint dp;
		
		public PointButton(Context context, int x, int y) {
			super(context);
			this.setBackgroundResource(R.drawable.point);
			dp = new DoublePoint(x,y);
		}
		public PointButton(int x, int y){
			super(null);
			dp = new DoublePoint(x,y);
		}

		public boolean onTouchEvent(MotionEvent event) {
			int action = event.getAction();
			LayoutParams l=null;
			switch(action){
			case MotionEvent.ACTION_DOWN :
				PointView.this.bringChildToFront(this);
				
				break;
			case MotionEvent.ACTION_UP :
				int x = 0;
				int y = 0;
				x = (int) event.getRawX();
				y = (int) event.getRawY() - STATEBAR_SIZE;
				if(x > wastbasketX && x < wastbasketX +wastbasketW &&
						y >wastbasketY && y < wastbasketY + wastbasketH ){
					PointView.this.removeView(this);
					vector.remove(this);
				}
				
			case MotionEvent.ACTION_MOVE :
				x = (int) event.getRawX();
				y = (int) event.getRawY() - STATEBAR_SIZE - POINT_DISTANCE;
				switch(mode){
				case MODE_1:
					if(x < 1)
						dp.p1.x = 1;
					else if(x > PointView.this.getWidth()-2)
						dp.p1.x = PointView.this.getWidth()-2;
					else
						dp.p1.x = x;
					
					if(y < 1)
						dp.p1.y = 1;
					else if(y > PointView.this.getHeight()-2)
						dp.p1.y = PointView.this.getHeight()-2;
					else
						dp.p1.y = y;
					
					l = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT
							,(int)dp.p1.x-POINT_SIZE,(int) dp.p1.y-POINT_SIZE);
					break;
				case MODE_2:
					if(x < 1)
						dp.p2.x = 1;
					else if(x > PointView.this.getWidth()-2)
						dp.p2.x = PointView.this.getWidth()-2;
					else
						dp.p2.x = x;
					
					if(y < 1)
						dp.p2.y = 1;
					else if(y > PointView.this.getHeight()-2)
						dp.p2.y = PointView.this.getHeight()-2;
					else
						dp.p2.y = y;
					
					l = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT
							,(int)dp.p2.x-POINT_SIZE, (int)dp.p2.y-POINT_SIZE);

				}
				PointButton.this.setLayoutParams(l);
			}
			
			return super.onTouchEvent(event);
		}
	}
}





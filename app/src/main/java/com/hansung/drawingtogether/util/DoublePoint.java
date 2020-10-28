package com.hansung.drawingtogether.util;

public class DoublePoint {
	public RealPoint p1,p2;
	
	public DoublePoint(int x,int y){
		p1 = new RealPoint(x,y);
		p2 = new RealPoint(x,y);
	}
	public DoublePoint(float x, float y){
		p1 = new RealPoint(x,y);
		p2 = new RealPoint(x,y);
	}

	public DoublePoint(int touchX, int touchY, int x, int y) {
		p1 = new RealPoint(touchX, touchY);
		p2 = new RealPoint(x,y);
	}
	
	public DoublePoint(float touchX, float touchY, float x, float y) {
		p1 = new RealPoint(touchX, touchY);
		p2 = new RealPoint(x,y);
	}
}

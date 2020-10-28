package com.hansung.drawingtogether.util;

public class RealPoint {
	public float x, y;
	
	public RealPoint() { x = y = 0.0f; }
	public RealPoint(float x, float y) { this.x = x; this.y = y; }
	public RealPoint(RealPoint p) { x = p.x; y = p.y; }
	
	public float distance(RealPoint p) {
		float dx, dy;

		dx = p.x - x;
		dy = p.y - y;
		return (float) Math.sqrt((double)(dx * dx + dy * dy));
	}

	public float distanceSq(RealPoint p) { 
		float dx, dy;

		dx = p.x - x;
		dy = p.y - y;
		return (float)(dx * dx + dy * dy);
	}
}
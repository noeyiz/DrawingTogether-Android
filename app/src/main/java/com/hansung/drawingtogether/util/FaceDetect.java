package com.hansung.drawingtogether.util;

import android.graphics.Bitmap;
import android.graphics.PointF;
import android.media.FaceDetector;

public class FaceDetect {
	private static final int NUM_FACES = 1; // �ִ� �� ã�� ���� Max=64
	private FaceDetector.Face[] getFaces;
	private FaceDetector.Face getFace = null;
	private FaceDetector detector;
	private float eyesDistance;
	private RealPoint leftEye,rightEye,nose,mouse,midPoint;
	


	public FaceDetect(Bitmap source) {

		getFaces = new FaceDetector.Face[NUM_FACES];
		detector = new FaceDetector(source.getWidth(), source.getHeight(), getFaces.length);
		
		// �� ã�� ����
		detector.findFaces(source, getFaces);
		
		
		for(int i=0; i<getFaces.length; i++) { 
			getFace = getFaces[i];
			
			if(getFace == null)
				return;
			
			PointF midPoint = new PointF();
			
			//���� �������� ��� ����
			getFace.getMidPoint(midPoint);            

			this.midPoint = new RealPoint(midPoint.x,midPoint.y);
			
			//���� ������ �Ÿ�
			eyesDistance = getFace.eyesDistance();

			// ���ʴ�
			leftEye = new RealPoint((midPoint.x-eyesDistance/2), midPoint.y);
			
			// ������ ��
			rightEye = new RealPoint(midPoint.x+eyesDistance/2,midPoint.y);
			// ��
			nose = new RealPoint(midPoint.x,(midPoint.y+eyesDistance/2));
			// ��
			mouse = new RealPoint(midPoint.x, (midPoint.y+eyesDistance));			
		}
	}
	
	public RealPoint getMidPoint(){
		return midPoint;
	}
	public RealPoint getLeftEye(){
		return leftEye;
	}
	public RealPoint getRightEye(){
		return rightEye;
	}
	public RealPoint getNose(){
		return nose;
	}
	public RealPoint getMouse(){
		return mouse;
	}
	public float getDistance(){
		return eyesDistance;
	}
}

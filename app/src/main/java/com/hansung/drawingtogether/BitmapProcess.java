package com.hansung.drawingtogether;

import android.graphics.Bitmap;
import android.util.Log;

import com.hansung.drawingtogether.util.Triangle;


public class BitmapProcess {

	/*
	 * opencv Library load
	 */
	static{
		try{
			Log.i("JNI","Trying to load lib opencv.so");
			System.loadLibrary("opencv_java3");
			System.loadLibrary("native-lib");
		}catch(UnsatisfiedLinkError e){
			Log.e("JNI","WARNING: Could not load lib opencv.so");
		}
	}

	/**
	 * source1¸¦ weightÀÇ °¡ÁßÄ¡ ¸¸Å­ source2¿Í overrapÇÑ´Ù. 
	 * @param source1 Ã¹¹øÂ° ÀÌ¹ÌÁö
	 * @param source2 µÎ¹øÂ° ÀÌ¹ÌÁö
	 * @param width »ý¼ºÇÒ ÀÌ¹ÌÁöÀÇ ³ÐÀÌ
	 * @param height »ý¼ºÇÒ ÀÌ¹ÌÁöÀÇ ³ôÀÌ
	 * @param weight °¡ÁßÄ¡
	 * @return overrapµÈ bitmap
	 */
//	public static Bitmap addWeighted(Bitmap src1, Bitmap src2, int width, int height, float weight){
//		int source1Data[]=null, source2Data[]=null, weightData[]=null;
//		int source1Width = src1.getWidth();
//		int source1Height = src1.getHeight();
//		int source2Width = src2.getWidth();
//		int source2Height = src2.getHeight();
//		
//		if(weight == 1){
//			if(source1Width == width && source1Height == height){
//				return src1;
//			}else{
//				source1Data = getData(src1);
//				weightData = cvResize(source1Data,source1Width, source1Height,width,height);
//			}
//		}else if(weight == 0){
//			if(source2Width == width && source2Height == height){
//				return src2;
//			}else{
//				source2Data = getData(src2);
//				weightData = cvResize(source2Data,source2Width, source2Height,width,height);
//			}
//		}else{
//			source1Data = getData(src1);
//			source2Data = getData(src2);
//			weightData = cvAddWeighted(source1Data, source1Width, source1Height, 
//					source2Data, source2Width, source2Height,
//					width, height, weight);
//		}
//		System.gc();
//		Bitmap weightedBitmap = Bitmap.createBitmap(weightData, width, height, Bitmap.Config.ARGB_8888);
//		
//		return weightedBitmap;
//	}
	
	/**
	 * source¿Í  dst¸¦ morphingÇÑ´Ù.
	 * @param source Ã¹¹øÂ° ÀÌ¹ÌÁö
	 * @param dst µÎ¹øÂ° ÀÌ¹ÌÁö
	 * @param srcTriangle ÃÊ±â »ï°¢Çü
	 * @param dstTriangle ¸ñÀû »ï°¢Çü
	 * @param weight °¡ÁßÄ¡
	 * @return morphingµÈ bitmap
	 */
//	public static Bitmap morphing(Bitmap src, Bitmap dst, Triangle srcT0,
//								  Triangle dstT0, Triangle srcT1, Triangle dstT1, float weight ){
//		int sourceData[]=null, dstData[]=null, morphData[] = null;
//
//		if(weight==1.0f)
//			return src;
//		else if(weight==0)
//			return dst;
//
//		int width = src.getWidth();
//		int height = src.getHeight();
//
//		sourceData = getData(src);
//		dstData = getData(dst);
//
//		System.gc();
//		morphData = cvMorph(sourceData,dstData,width,height,srcT0.ptr,dstT0.ptr,srcT1.ptr,dstT1.ptr,weight);
//
//		return Bitmap.createBitmap(morphData,width,height,Bitmap.Config.ARGB_8888);
//	}
	
	/**
	 * srcTriangle À» ±âº»À¸·Î dstTriangleÀ» ÂüÁ¶ÇÏ¿© source ¿öÇÎ
	 * @param source ¿øº» ÀÌ¹ÌÁö
	 * @param srcTriangle ÃÊ±â »ï°¢Çü
	 * @param dstTriangle ¸ñÇ¥ »ï°¢Çü
	 * @return wapingµÈ bitmap
	 */
	public static Bitmap warping(Bitmap src, Triangle srcT, Triangle dstT){
		int srcData[] = null, warpData[]= null;
		
		int width = src.getWidth();
		int height = src.getHeight();
		
		srcData = getData(src);

		System.gc();
		warpData = cvWarp(srcData,width,height,srcT.ptr,dstT.ptr);

		return Bitmap.createBitmap(warpData,width,height,Bitmap.Config.ARGB_8888);
	}
	
	///////////////////////////////////////////// private
	private static int[] getData(Bitmap src){
		int data[] = new int[src.getWidth()*src.getHeight()];
		
		src.getPixels(data, 0,src.getWidth(), 0, 0, src.getWidth(), src.getHeight());
		
		return data;
	}
	
	//////////////////////////////////////////// native method

//	public static native int[] cvAddWeighted(int[] src1, int srcWidth, int srcHeight, 
//			int[] src2, int srcWidth2, int srcHeight2, 
//			int width, int height, float weight);
	public static native int[] cvWarp(int[] source, int srcWidth, int srcHeight,
			int[] srcT, int[] dstT);


}

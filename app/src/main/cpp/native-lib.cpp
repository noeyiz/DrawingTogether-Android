#include <jni.h>
#include <opencv2/opencv.hpp>
#include <stdio.h>

#include <opencv2/core/types_c.h>
#include <opencv2/core/core_c.h>
#include <opencv2/imgproc/imgproc_c.h>

void getIplImageBuf(IplImage *,jint *,int,int);

extern "C"
JNIEXPORT jintArray JNICALL
Java_com_hansung_drawingtogether_BitmapProcess_cvWarp(JNIEnv *env, jclass cls, jintArray src_data, jint src_width, jint src_height,
                                                      jintArray src_triangle, jintArray dst_triangle) {
    int triangle_count = env->GetArrayLength(src_triangle)/6;

    jint *src_triangle_ptr = env->GetIntArrayElements(src_triangle,NULL);
    jint *dst_triangle_ptr = env->GetIntArrayElements(dst_triangle,NULL);

    IplImage *src  = cvCreateImage(cvSize(src_width, src_height), IPL_DEPTH_8U, 4);
    jint *src_ptr = env->GetIntArrayElements(src_data,NULL);
    src->imageData = (char *) src_ptr;
//    getIplImageBuf(&src,src_ptr,src_width,src_height);

    IplImage *warp = cvCreateImage(cvSize(src_width, src_height), IPL_DEPTH_8U, 4);
    jintArray warp_data = env->NewIntArray(src_width*src_height);
    jint *warp_ptr = env->GetIntArrayElements(warp_data,NULL);
    warp->imageData = (char *) warp_ptr;
//    getIplImageBuf(&warp,warp_ptr,src_width, src_height);


    IplImage *buf = cvCreateImage(cvGetSize(warp),IPL_DEPTH_8U,4);
    IplImage *buf2 = cvCreateImage(cvGetSize(warp),IPL_DEPTH_8U,4);

    CvPoint2D32f srcTri[3], dstTri[3];
    CvMat *warp_mat = cvCreateMat(2,3,CV_32FC1);

    CvPoint **pts;
    pts = (CvPoint **) malloc (sizeof (CvPoint *) * 2);
    pts[0] = (CvPoint *) malloc (sizeof (CvPoint) * 3);
    int npts[1] = {3};

    for(int i=0; i<triangle_count; i++){

        srcTri[0].x = src_triangle_ptr[i*6];
        srcTri[0].y = src_triangle_ptr[i*6+1];
        srcTri[1].x = src_triangle_ptr[i*6+2];
        srcTri[1].y = src_triangle_ptr[i*6+3];
        srcTri[2].x = src_triangle_ptr[i*6+4];
        srcTri[2].y = src_triangle_ptr[i*6+5];

        dstTri[0].x = pts[0][0].x = dst_triangle_ptr[i*6];
        dstTri[0].y = pts[0][0].y = dst_triangle_ptr[i*6+1];
        dstTri[1].x = pts[0][1].x = dst_triangle_ptr[i*6+2];
        dstTri[1].y = pts[0][1].y = dst_triangle_ptr[i*6+3];
        dstTri[2].x = pts[0][2].x = dst_triangle_ptr[i*6+4];
        dstTri[2].y = pts[0][2].y = dst_triangle_ptr[i*6+5];

        int x = (srcTri[0].x<dstTri[0].x)?srcTri[0].x:dstTri[0].x;
        int y = (srcTri[0].y<dstTri[0].y)?srcTri[0].y:dstTri[0].y;
        int w = (srcTri[0].x>dstTri[0].x)?srcTri[0].x:dstTri[0].x;
        int h = (srcTri[0].y>dstTri[0].y)?srcTri[0].y:dstTri[0].y;

        for(int k=1; k<3 ; k++){
            if(x > srcTri[k].x)
                x = srcTri[k].x;
            if(y > srcTri[k].y)
                y = srcTri[k].y;
            if(x > dstTri[k].x)
                x = dstTri[k].x;
            if(y > dstTri[k].y)
                y = dstTri[k].y;

            if(w < srcTri[k].x)
                w = srcTri[k].x;
            if(h < srcTri[k].y)
                h = srcTri[k].y;
            if(w < dstTri[k].x)
                w = dstTri[k].x;
            if(h < dstTri[k].y)
                h = dstTri[k].y;
        }

        srcTri[0].x = srcTri[0].x -x;
        srcTri[0].y = srcTri[0].y -y;
        srcTri[1].x = srcTri[1].x -x;
        srcTri[1].y = srcTri[1].y -y;
        srcTri[2].x = srcTri[2].x -x;
        srcTri[2].y = srcTri[2].y -y;

        dstTri[0].x = dstTri[0].x -x;
        dstTri[0].y = dstTri[0].y -y;
        dstTri[1].x = dstTri[1].x -x;
        dstTri[1].y = dstTri[1].y -y;
        dstTri[2].x = dstTri[2].x -x;
        dstTri[2].y = dstTri[2].y -y;

        pts[0][0].x = dstTri[0].x;
        pts[0][0].y = dstTri[0].y;
        pts[0][1].x = dstTri[1].x;
        pts[0][1].y = dstTri[1].y;
        pts[0][2].x = dstTri[2].x;
        pts[0][2].y = dstTri[2].y;

        cvSetImageROI(src,cvRect(x,y,w+1,h+1));
        cvSetImageROI(buf,cvRect(x,y,w+1,h+1));
        cvSetImageROI(buf2,cvRect(x,y,w+1,h+1));
        cvSetImageROI(warp,cvRect(x,y,w+1,h+1));

        cvGetAffineTransform(srcTri,dstTri,warp_mat);
        cvWarpAffine(src,buf,warp_mat);

        cvZero(buf2);
        cvFillPoly (buf2, pts, npts, 1, cvScalar(255,255,255,255),8,0);
        cvFillPoly(warp,pts,npts,1,cvScalar(0,0,0,0),8,0);

        cvAnd(buf2,buf,buf,NULL);
        cvOr(buf,warp,warp,NULL);
    }

    free(pts[0]);
    free(pts);
    cvReleaseImage(&buf);
    cvReleaseImage(&buf2);
    env->ReleaseIntArrayElements(warp_data, warp_ptr, JNI_ABORT);
    env->ReleaseIntArrayElements(src_triangle, src_triangle_ptr, JNI_ABORT);
    env->ReleaseIntArrayElements(dst_triangle, dst_triangle_ptr, JNI_ABORT);
    env->ReleaseIntArrayElements(src_data, src_ptr, JNI_ABORT);

    return warp_data;
}

void getIplImageBuf(IplImage *imagebuf,jint *data, int width, int height){
    imagebuf->nSize = 144;
    imagebuf->ID = 0;
    imagebuf->nChannels = 4;
    imagebuf->alphaChannel = 0;
    imagebuf->depth =8;
    imagebuf->dataOrder =0;
    imagebuf->align =4;
    imagebuf->width = width;
    imagebuf->height = height;
    imagebuf->roi = NULL;
    imagebuf->maskROI = NULL;
    imagebuf->imageId = NULL;
    imagebuf->widthStep = width *4;
    imagebuf->imageSize = height * imagebuf->widthStep;
    imagebuf->imageData = (char *) data;
    imagebuf->imageDataOrigin = NULL;

}
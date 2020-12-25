package com.hansung.drawingtogether.tester;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Toast;

import com.hansung.drawingtogether.R;
import com.hansung.drawingtogether.data.remote.model.MQTTClient;
import com.hansung.drawingtogether.view.drawing.DrawingComponent;
import com.hansung.drawingtogether.view.drawing.DrawingEditor;
import com.hansung.drawingtogether.view.drawing.DrawingFragment;
import com.hansung.drawingtogether.view.drawing.DrawingView;
import com.hansung.drawingtogether.view.drawing.Eraser;
import com.hansung.drawingtogether.view.drawing.Mode;

import java.util.Random;
import java.util.Vector;

import lombok.Getter;

@Getter
public enum DrawingTester {
    INSTANCE;

    private int background; // 화면에 그려질 스트로크 개수 (테스트 환경 구축)
    private int bSegment; // 화면에 그려질 스트로크가 이루는 세그먼트 개수

    private static int corRange = 50; // 랜덤 위치를 기준으로 좌표 범위

    private DrawingFragment dFragment;
    private Context context;

    private DrawingView dView;
    private DrawingEditor de;
    private Eraser eraser;
    private Vector<Integer> erasedId;



    /* 초기화 함수 */
    public void init(DrawingFragment dFragment) {
        this.dFragment = dFragment;
        this.context = dFragment.getContext();

        this.dView = dFragment.getBinding().drawingView;
        this.de = DrawingEditor.getInstance();
        this.eraser = new Eraser();
        this.erasedId = new Vector<Integer>();
    }

    /* 테스트 파라미터 초기화 함수 */
    public void set(int background, int bSegment) {
        this.background = background;
        this.bSegment = bSegment;
    }

    /* 테스트 환경 구축 함수 */
    // 일정 개수의 스트로크를 화면에 그려놓는 함수
    // 드로잉 코드 호출

    public void setEnv() {

        new Thread(new Runnable() {
            @Override
            public void run() {
                Random random = new Random();
                int rX, rY; // down random coordinate
                int rX2, rY2; // move, up random coordinate

                /* 테스트 상황 구축 부분 (화면에 스트로크 그려놓기) */
                for(int i=0; i<background; i++) {
                    // down - [start segment]
                    rX = random.nextInt((int)dView.getCanvasWidth()-(corRange * 4) + 1) + (corRange * 2); // (100) ~ (canvas width -100)
                    rY = random.nextInt((int)dView.getCanvasHeight()-(corRange * 4) + 1) + (corRange * 2); // (100) ~ (canvas height - 100)

                    de.setStrokeColor("#D3D3D3"); // 화면에 그려놓는 스트로크는 회색으로 고정
                    dView.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_DOWN, rX, rY, 0));

                    // move - [data segment]
                    for(int j=0; j<bSegment * dView.getMsgChunkSize(); j++) {
                        rX2 = random.nextInt(corRange * 2 + 1) + (rX-10);
                        rY2 = random.nextInt(corRange * 2 + 1) + (rY-10);

                        dView.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_MOVE, rX2, rY2, 0));

                    }

                    // up - [end segment]
                    rX2 = random.nextInt(corRange * 2 + 1) + (rX-10);
                    rY2 = random.nextInt(corRange * 2 + 1) + (rY-10);
                    dView.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, rX2, rY2, 0));

                    try { Thread.sleep(500); } // 2 strokes / 1s
                    catch(InterruptedException ie) { Log.e("tester", "draw background strokes loop"); }
                }

            }
        }).start();
    }

    public static DrawingTester getInstance() { return INSTANCE; }
}





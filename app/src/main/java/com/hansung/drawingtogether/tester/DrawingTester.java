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
    private int sending; // 보내는 스트로크 개수 (스트로크를 몇 번 반복해서 보낼 것인지 정하는 변수)
    private int sSegment; // 보내는 스트로크가 이루는 세그먼트 개수
    private int count; // 테스트를 반복할 횟수

    public static boolean tFinish = false; // 테스트 완료 플래그
    public static boolean measurement = false; // 성능 측정 플래그

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
    public void set(int background, int bSegment, int sending, int sSegment, int count) {
        this.background = background;
        this.bSegment = bSegment;
        this.sending = sending;
        this.sSegment = sSegment;
        this.count = count;
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

                tFinish = false;
                MQTTClient.msgMeasurement = true;

                ((Activity)context).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dFragment.getBinding().measureButton.setEnabled(true); // 측정 버튼 활성화
                    }
                });

            }
        }).start();
    }

    /* 테스트 시작 함수 */
    // 임의로 좌푯값을 생성해서 드로잉하는 함수
    // 드로잉 코드 호출
    public void measure() {
        tFinish = false;
        DrawingComponent dc; // 현재 그린 스트로크

        new Thread(new Runnable() {
            @Override
            public void run() {
                Random random = new Random();
                int rX, rY; // down random coordinate
                int rX2, rY2; // move, up random coordinate

                /* 토스트 메시지 출력 */
                ((Activity)context).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(dFragment.getContext(), R.string.msg_sending_start, Toast.LENGTH_LONG).show();
                    }
                });

                /* 성능 측정 부분 ("메시지 전송 시간"과 "화면에 그려지는 시간" 측정) */
                for(int i=0; i<count; i++) {
                    for(int j=0; j<sending; j++) {

                        // down - [start segment]
                        rX = random.nextInt((int)dView.getCanvasWidth()-(corRange * 4) + 1) + (corRange * 2); // (100) ~ (canvas width -100)
                        rY = random.nextInt((int)dView.getCanvasHeight()-(corRange * 4) + 1) + (corRange * 2); // (100) ~ (canvas height - 100)

                        de.setStrokeColor(de.generateRandomHexCode());
                        dView.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_DOWN, rX, rY, 0));

                        // down 후 바로 move 루프 들어가면 세그먼트를 너무 빨리 만들어내서
                        // start segment 보내고 바로 data segment 보내게 된다.
                        // 손으로 그릴 경우 start segment 와 data segment 메시지를 받는데 어느정도 시간차가 있음
                        // ( 발생된 터치 이벤트로 point msg chunk 채우는 시간 )
//                        try { Thread.sleep(100); } // wait for saving msg chunk
//                        catch(InterruptedException ie) { ie.printStackTrace(); }

                        // move - [data segment]
                        for(int k=0; k<sSegment * dView.getMsgChunkSize(); k++) {
                            rX2 = random.nextInt(corRange * 2 + 1) + (rX-10);
                            rY2 = random.nextInt(corRange * 2 + 1) + (rY-10);

                            dView.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_MOVE, rX2, rY2, 0));
//                            try { Thread.sleep(25); } // occur move event slowly
//                            catch(InterruptedException ie) { ie.printStackTrace(); }
                        }

                        // up - [end segment]
                        rX2 = random.nextInt(corRange * 2 + 1) + (rX-10);
                        rY2 = random.nextInt(corRange * 2 + 1) + (rY-10);
                        dView.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, rX2, rY2, 0));

                        // sleep 시간이 짧으면, 아이디 처리 이상 -> 선을 지우고 그리는 과정에서 문제 발생
                        try { Thread.sleep(1000); } // wait before erase
                        catch(InterruptedException ie) { ie.printStackTrace(); }

                        Log.e("tester", "before erase drawing components size = " + de.getDrawingComponents().size());
                        Log.e("tester", "before erase last drawing components id = " + de.getDrawingComponents().get(de.getDrawingComponents().size()-1).getId() + ", users component id = " + de.getDrawingComponents().get(de.getDrawingComponents().size()-1).getUsersComponentId());

                        // 방금 그린 선 지우기
                        de.setCurrentMode(Mode.ERASE);
                        erasedId.add(de.getDrawingComponents().get(de.getDrawingComponents().size()-1).getId());
                        eraser.erase(erasedId);
                        erasedId.clear();

                        Log.e("tester", "after erase drawing components size = " + de.getDrawingComponents().size());
                        Log.e("tester", "after erase last drawing components id = " + de.getDrawingComponents().get(de.getDrawingComponents().size()-1).getId() + ", users component id = " + de.getDrawingComponents().get(de.getDrawingComponents().size()-1).getUsersComponentId());

                        de.setCurrentMode(Mode.DRAW);

                        try { Thread.sleep(1000); } // wait after erase
                        catch (InterruptedException ie) { ie.printStackTrace(); }

                    }

                }

                tFinish = true;
                MQTTClient.msgMeasurement = false; // 측정 종료 (측정 객체 관리 종료)

                ((Activity)context).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dFragment.getBinding().measureButton.setEnabled(true); // 측정 버튼 활성화
                    }
                });

            }
        }).start();
    }

    public static DrawingTester getInstance() { return INSTANCE; }
}





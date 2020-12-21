package com.hansung.drawingtogether.view.drawing;

import android.util.Log;
import android.view.MotionEvent;

import com.hansung.drawingtogether.data.remote.model.MQTTClient;
import com.hansung.drawingtogether.data.remote.model.MyLog;
import com.hansung.drawingtogether.tester.PerformanceData;


import java.util.ConcurrentModificationException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import lombok.Getter;

@Getter
public class SendMqttMessage {    //consumer  //queue 가 비어있을때까지 publish 하는 thread
    public ExecutorService executor;

    private MQTTClient client = MQTTClient.getInstance();
    private final JSONParser parser = JSONParser.getInstance();

    private BlockingQueue<MqttMessageFormat> queue = new ArrayBlockingQueue<>(10000);    //Linked, Array 두개의 차이 알아보기
    //private final Object lock = new Object();
    //private SendMqttMessageThread sendMqttMessageThread;
    private boolean isWait = false;

    private int putCnt = 0;
    private int takeCnt = 0;

    private SendMqttMessage() {
        //this.sendMqttMessageThread = new SendMqttMessageThread();
        this.executor = Executors.newFixedThreadPool(10, new LowPriorityThreadFactory());
    }

    private static class LazyHolder {
        public static final SendMqttMessage INSTANCE = new SendMqttMessage();
    }

    public static SendMqttMessage getInstance() {
        return LazyHolder.INSTANCE;
    }

    public void putMqttMessage(MqttMessageFormat messageFormat) {    //producer  //queue 가 꽉차있으면 wait, 아니면 put 하는 thread
        try {
            queue.put(messageFormat);
            putCnt++;
            //MyLog.i("sendThread", "offer success " + putCnt + ", size() = " + queue.size());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void startThread() {
        MyLog.i("sendThread", "startThread");

        /*if(sendMqttMessageThread.isAlive()) {
            MyLog.i("sendThread", "isAlive");
            //sendMqttMessageThread.interrupt();
            //sendMqttMessageThread.start();
        } else {
            MyLog.i("sendThread", "isNotAlive | thread start");
            //sendMqttMessageThread.start();
        }*/

        MyLog.i("sendThread", "thread start");

        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    while (true) {
                        //MyLog.i("sendThread", "send thread is running");
                        //MyLog.i("sendThread", "topic data = " + client.getTopic_data());
                        try {
                            //MyLog.i("sendThread", "before publish");
                            MqttMessageFormat messageFormat = queue.take();
                            //MyLog.i("sendThread", "active thread count: " + Thread.activeCount() + ", current: " + Thread.currentThread().getName());

                            // fixme nayeon for performance
//                            if(client.isMaster()
//                                    &&  messageFormat.getMode().equals(Mode.DRAW) /* && messageFormat.getAction() != null
//                                && messageFormat.getAction() == MotionEvent.ACTION_MOVE
//                                && messageFormat.getType().equals(ComponentType.STROKE)*/) { // 자유 곡선, 사각형, 원 모두 포함 ( only DRAW mode )
//                                MQTTClient.receiveTimeList.add(new Velocity(System.currentTimeMillis()));
//                            }

                            // todo for performance
                            // todo [메시지 수신 시간 측정 시작] only for sender
                            // 보낸 스트로크에 대한 메시지 수신 시간 측정 (테스트 환경을 위한 지우기 메시지는 측정에서 제외)
                            long start = System.currentTimeMillis();
                            if(MQTTClient.msgMeasurement && messageFormat.getMode() == Mode.DRAW) {
                                switch (messageFormat.getAction()) {
                                    case MotionEvent.ACTION_DOWN:
                                        MQTTClient.receiveTimeList.add(new PerformanceData("ss", System.currentTimeMillis())); // start segment
                                        break;
                                    case MotionEvent.ACTION_MOVE:
                                        MQTTClient.receiveTimeList.add(new PerformanceData("ds", System.currentTimeMillis())); // data segment
                                        break;
                                    case MotionEvent.ACTION_UP:
                                        MQTTClient.receiveTimeList.add(new PerformanceData("es", System.currentTimeMillis())); // end segment
                                        break;
                                }
//                                Log.i("tester", "send mqtt thread check routine time (receive time) = " + (System.currentTimeMillis() - start)/1000.0);
                            }

                            client.publish(client.getTopic_data(), parser.jsonWrite(messageFormat));
                            //MyLog.i("segment", "publish | " + parser.jsonWrite(messageFormat));
                            takeCnt++;
                            //MyLog.i("sendThread", messageFormat.getUsersComponentId() + ", poll success " + takeCnt + ", size() = " + queue.size());
                        } catch (ConcurrentModificationException e) {
                            MyLog.i("sendThread", "*** ConcurrentModificationException ***");
                            e.printStackTrace();
                        }
                    }

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

    }

    /*class SendMqttMessageThread extends Thread {
        @Override
        public void run() {
            try {
                while (true) {
                    //MyLog.i("sendThread", "send thread is running");
                    //MyLog.i("sendThread", "topic data = " + client.getTopic_data());
                    try {
                        MyLog.i("sendThread", "before publish");
                        MqttMessageFormat messageFormat = queue.take();
                        //MyLog.i("sendThread", "active thread count: " + Thread.activeCount() + ", current: " + Thread.currentThread().getName());

                        // fixme nayeon for performance
                        if(client.isMaster()
                                &&  messageFormat.getMode().equals(Mode.DRAW) /* && messageFormat.getAction() != null
                                && messageFormat.getAction() == MotionEvent.ACTION_MOVE
                            && messageFormat.getType().equals(ComponentType.STROKE)) {
                            MQTTClient.receiveTimeList.add(new Velocity(System.currentTimeMillis()));
                        }


                        /*client.publish(client.getTopic_data(), parser.jsonWrite(messageFormat));
                        takeCnt++;
                        MyLog.i("sendThread", messageFormat.getUsersComponentId() + ", poll success " + takeCnt + ", size() = " + queue.size());
                    } catch (ConcurrentModificationException e) {
                        MyLog.i("sendThread", "*** ConcurrentModificationException ***");
                        e.printStackTrace();
                    }
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }*/



}


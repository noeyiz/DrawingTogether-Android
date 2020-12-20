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


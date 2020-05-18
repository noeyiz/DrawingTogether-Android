package com.hansung.drawingtogether.view.drawing;

import android.util.Log;
import android.view.MotionEvent;

import com.hansung.drawingtogether.data.remote.model.MQTTClient;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import lombok.Getter;

@Getter
public class SendMqttMessage {    //consumer  //queue 가 비어있을때까지 publish 하는 thread
    private MQTTClient client = MQTTClient.getInstance();
    //private String topicData;
    private final JSONParser parser = JSONParser.getInstance();

    private BlockingQueue<MqttMessageFormat> queue = new ArrayBlockingQueue<>(10000);    //Linked, Array 두개의 차이 알아보기
    //private final Object lock = new Object();
    private SendMqttMessageThread sendMqttMessageThread;
    private boolean isWait = false;
    private int cnt = 0;
    private int putCnt = 0;
    //private ArrayList<MqttMessageFormat> messages = new ArrayList<>();

    private SendMqttMessage() {
        sendMqttMessageThread = new SendMqttMessageThread();
    }

    private static class LazyHolder {
        public static final SendMqttMessage INSTANCE = new SendMqttMessage();
    }

    public static SendMqttMessage getInstance() {
        return LazyHolder.INSTANCE;
    }

    public void putMqttMessage(MqttMessageFormat messageFormat) {    //producer  //queue 가 꽉차있으면 wait, 아니면 put 하는 thread
        try {
            if(messageFormat.getAction() == MotionEvent.ACTION_DOWN) {
                client.publish(client.getTopic_data(), parser.jsonWrite(messageFormat));
            } else {
                queue.put(messageFormat);
                putCnt++;
                Log.i("sendThread", "offer success " + putCnt + ", size() = " + queue.size());
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void startThread() {
        Log.i("sendThread", "startThread");

        if(sendMqttMessageThread.isAlive()) {
            Log.i("sendThread", "isAlive");
            //sendMqttMessageThread.interrupt();
            //sendMqttMessageThread.start();
        } else {
            Log.i("sendThread", "isNotAlive | thread start");
            sendMqttMessageThread.start();
        }
    }

    class SendMqttMessageThread extends Thread {
        @Override
        public void run() {
            //topicData = client.getTopic_data();
            try {
                while (true) {
                    Log.i("sendThread", "topic data = " + client.getTopic_data());
                    //Log.i("sendThread", "draw touch count = " + drawCnt);
                    try {
                        Log.i("sendThread", "before publish");
                        MqttMessageFormat messageFormat = queue.take();
                        Log.i("sendThread", Thread.activeCount() + ", ");
                        client.publish(client.getTopic_data(), parser.jsonWrite(messageFormat));
                        //Thread.sleep(10);
                        cnt++;
                        Log.i("sendThread", messageFormat.getUsersComponentId() + ", poll success " + cnt + ", size() = " + queue.size());

                    } catch (ConcurrentModificationException e) {
                        Log.i("sendThread", "*** ConcurrentModificationException ***");
                        e.printStackTrace();
                    }
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }
}


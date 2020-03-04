package com.hansung.drawingtogether.data.remote.model;

import android.util.Log;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import lombok.Getter;

// fixme hyeyeon[6] - second초에 한번씩 topic_alive로 자신의 이름 publish
@Getter
public enum AliveThread implements Runnable {
    INSTANCE;

    private MQTTClient client = MQTTClient.getInstance();  // 참조
    private int second = 2000;
    private int count = -5;
    private ConcurrentHashMap<String, Integer> aliveCheckMap = client.getAliveCheckMap();  // 참조

    public static AliveThread getInstance() {
        return INSTANCE;
    }

    @Override
    public void run() {
        String topic_alive = client.getTopic_alive();

        while (true) {
            try {
                client.publish(topic_alive, client.getMyName().getBytes());
                Thread.sleep(second);

                synchronized (aliveCheckMap) {  // fixme hyeyeon[7]
                    Iterator<String> iterator = aliveCheckMap.keySet().iterator();
                    while (iterator.hasNext()) {
                        String key = iterator.next();
                        aliveCheckMap.put(key, aliveCheckMap.get(key) - 1);

                        if (aliveCheckMap.get(key) == count) {
                            iterator.remove();
                            Log.e("kkankkan", "removeUser 부르기 전");
                            client.removeUser(key);
                            Log.e("kkankkan", "removeUser 부르기 후");
                        }
                    }
                }

                Log.e("kkankkan", "[AliveThread] : " + aliveCheckMap.toString());
/*
                //aliveCheckMap = client.getAliveCheckMap();
                for (String key: aliveCheckMap.keySet()) {
                    aliveCheckMap.put(key, aliveCheckMap.get(key) - 1);

                    if (aliveCheckMap.get(key)  == -5) {
                        client.removeUser(key);  // todo : concurrentmodificationexception 해결
                    }

                    Log.e("kkankkan", key + " " + aliveCheckMap.get(key));
                }*/
            } catch (InterruptedException e) {
                Log.e("kkankkan", "alive thread is dead");
                break;
            }

        }
    }

    public void setSecond(int second) {
        this.second = second;
    }

    public void setCount(int count) {
        this.count = count;
    }

}

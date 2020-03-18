package com.hansung.drawingtogether.data.remote.model;

import android.util.Log;

import com.hansung.drawingtogether.view.drawing.DrawingViewModel;
import com.hansung.drawingtogether.view.drawing.JSONParser;
import com.hansung.drawingtogether.view.drawing.MqttMessageFormat;
import com.hansung.drawingtogether.view.main.AliveMessage;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import lombok.Getter;

// fixme hyeyeon-second초에 한번씩 topic_alive로 자신의 이름 publish
@Getter
public enum AliveThread implements Runnable {
    INSTANCE;

    private MQTTClient client = MQTTClient.getInstance();  // 참조
    private int second = 2000;
    private boolean kill;

    public void setKill(boolean kill) {
        this.kill = kill;
    }

    public static AliveThread getInstance() {
        return INSTANCE;
    }

    @Override
    public void run() {
        String topic_alive = client.getTopic_alive();  // 복사
        String myName = client.getMyName();  // 복사
        while (true) {
            try {
                AliveMessage aliveMessage = new AliveMessage(myName);
                MqttMessageFormat mqttMessageFormat = new MqttMessageFormat(aliveMessage);
                client.publish(topic_alive, JSONParser.getInstance().jsonWrite(mqttMessageFormat));
                Thread.sleep(second);

/*                  synchronized (userList) {
                    Iterator<User> iterator = userList.iterator();
                    while (iterator.hasNext()) {
                        User user = iterator.next();
                        user.setCount(user.getCount() - 1);
                        if (user.getCount() == count) {
                            iterator.remove();
                            drawingViewModel.setUserNum(userList.size());
                            drawingViewModel.setUserPrint(client.userPrint());
                            if (userList.get(0).getName().equals(myName) && !client.isMaster()) {
                                client.setMaster(true);
                                Log.e("kkankkan", "새로운 master는 나야! " + client.isMaster());
                            }

                            Log.e("kkankkan", user.getName() + " exit 후 [userList] : " + client.userPrint());
                            client.setToastMsg("[ " + user.getName() + " ] 님 접속이 끊겼습니다");
                        }
                        Log.e("kkankkan", "[" + user.getName() + ", " + user.getCount() + "]");
                    }
                }*/
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
                kill = true;
                Log.e("kkankkan", "alive thread is dead");
                break;
            }

        }
    }

    public void setSecond(int second) {
        this.second = second;
    }
}

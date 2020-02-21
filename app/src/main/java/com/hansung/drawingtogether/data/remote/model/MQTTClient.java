package com.hansung.drawingtogether.data.remote.model;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.hansung.drawingtogether.view.drawing.DrawingViewModel;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.ArrayList;
import java.util.List;

public class MQTTClient {

    private MqttClient client;
    private final String BROKER_ADDRESS = "tcp://" + "192.168.0.47" + ":1883";

    private FirebaseStorage storage;
    private StorageReference storageReference;

    private boolean master;
    private List<String> userList = new ArrayList<>();
    private String myName;

    private String topic;
    private String topic_join;
    private String topic_exit;
    private String topic_delete;
    private String topic_data = "data";
    // private String topic_load;

    private DrawingViewModel drawingViewModel;

    public MQTTClient(String topic, String name, boolean master, DrawingViewModel drawingViewModel) {
        try {
            client = new MqttClient(BROKER_ADDRESS,  MqttClient.generateClientId(), null);
            client.connect();

            Log.e("kkankkan", "mqtt connect success");
        } catch (MqttException e) {
            e.printStackTrace();
        }

        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        this.master = master;
        this.topic = topic;
        this.myName = name;
        userList.add(myName);
        topic_join = this.topic + "_join";
        topic_exit = this.topic + "_exit";
        topic_delete = this.topic + "_delete";

        this.drawingViewModel = drawingViewModel;
        this.drawingViewModel.setUserNum(userList.size());
    }

    public void subscribe(String newTopic) {
        try {
            client.subscribe(newTopic);
            Log.e("kkankkan", newTopic + " subscribe");
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void publish(String newTopic, byte[] payload) {
        try {
            MqttMessage message = new MqttMessage(payload);
            client.publish(newTopic, message);
            Log.e("kkankkan", newTopic + " publish");
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void setCallback() {
        client.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                Log.e("kkankkan", cause.toString());
            }

            @Override
            public void messageArrived(String newTopic, MqttMessage message) throws Exception {

                /*
                topic_join으로 오는 메시지 종류
                1. "name":"이름"
                2. "master":"이름"/"userList":"이름1,이름2,이름3"/"loadingData":"..."
                 */
                if (newTopic.equals(topic_join)) {
                    String data[] = message.toString().split(":");

                    if (data[0].equals("master")) {
                        String token[] = message.toString().split("/");
                        String users = token[1].split(":")[1];
                        String loadingData = token[2].split(":")[1];

                        Log.e("kkankkan", "master가 보낸 userList : " + users);

                        userList.removeAll(userList);
                        for (int i=0; i<users.split(",").length; i++) {
                            userList.add(users.split(",")[i]);
                        }

                        Log.e("kkankkan", userList.toString());

                        topic_data = loadingData;
                    }
                    else {  // other or self
                        String name = data[1];

                        if (!myName.equals(name)) {  // other
                            userList.add(name);

                            if (master) {
                                String users = "";
                                for(int i=0; i<userList.size(); i++) {
                                    users += userList.get(i);
                                    if (i == userList.size()-1)
                                        break;
                                    users += ",";
                                }

                                MqttMessage msg = new MqttMessage(("master:" + userList.get(0) + "/userList:" + users + "/loadingData:" + topic_data).getBytes());
                                client.publish(topic_join, msg);

                                Log.e("kkankkan", "master data -> " + msg);
                            }

                            Log.e("kkankkan", name + " join 후 : " + userList.toString());
                            //drawingViewModel.setUserNumTv(userList.size());
                        }
                    }
                    drawingViewModel.setUserNum(userList.size());
                }

                if (newTopic.equals(topic_exit)) {
                    if (myName.equals(message.toString())) {  // 내가 exit 하는 경우
                        if (master) {  // master==나
                            if (userList.size() == 1) {  // 나==마지막 사용자
                                // db에 drawview data 저장
                            }
                        }
                        try {
                            client.unsubscribe(topic_join);
                            client.unsubscribe(topic_exit);
                            client.unsubscribe(topic_delete);
                            client.unsubscribe(topic_data);

                            Log.e("kkankkan", "unsubscribe 완료");

                        } catch (MqttException e) {
                            e.printStackTrace();
                        }
                        // 나가기
                        drawingViewModel.back();
                    }
                    else {  // 다른 사람이 exit 하는 경우
                        userList.remove(message.toString());
                        if (myName.equals(userList.get(0))) {  // 확인 해봐야함
                            master = true;
                            Log.e("kkankkan", "새로운 master는 나야! " + master);
                        }

                        Log.e("kkankkan", message.toString() + " exit 후" + userList.toString());
                        drawingViewModel.setUserNum(userList.size());
                    }
                }

                if (newTopic.equals(topic_delete)) {
                    if (message.toString().equals("master")) {
                        try {
                            client.unsubscribe(topic_join);
                            client.unsubscribe(topic_exit);
                            client.unsubscribe(topic_delete);
                            client.unsubscribe(topic_data);

                            Log.e("kkankkan", "unsubscribe 완료");

                        } catch (MqttException e) {
                            e.printStackTrace();
                        }
                        // 나가기
                        drawingViewModel.back();
                    }
                    if (message.toString().equals(myName) && master) {
                        Log.e("kkankkan", "master delete");
                        // db에서 topic 삭제
                        storageReference.child(topic).delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Log.e("kkankkan", "topic delete success");
                                try {
                                    client.unsubscribe(topic_join);
                                    client.unsubscribe(topic_exit);
                                    client.unsubscribe(topic_delete);
                                    client.unsubscribe(topic_data);

                                    client.publish(topic_delete, new MqttMessage("master".getBytes()));
                                    Log.e("kkankkan", "unsubscribe 완료");

                                } catch (MqttException e) {
                                    e.printStackTrace();
                                }
                                // 나가기
                                drawingViewModel.back();

                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.e("kkankkan", e.toString());
                            }
                        });
                    }
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });
    }

}
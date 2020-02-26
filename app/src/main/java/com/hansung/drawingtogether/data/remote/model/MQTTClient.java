package com.hansung.drawingtogether.data.remote.model;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.view.MotionEvent;

import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import lombok.Getter;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.hansung.drawingtogether.databinding.FragmentDrawingBinding;
import com.hansung.drawingtogether.view.drawing.DrawingComponent;
import com.hansung.drawingtogether.view.drawing.DrawingEditor;
import com.hansung.drawingtogether.view.drawing.DrawingFragment;
import com.hansung.drawingtogether.view.drawing.DrawingItem;
import com.hansung.drawingtogether.view.drawing.DrawingView;
import com.hansung.drawingtogether.view.drawing.DrawingViewModel;
import com.hansung.drawingtogether.view.drawing.EraserTask;
import com.hansung.drawingtogether.view.drawing.JSONParser;
import com.hansung.drawingtogether.view.drawing.Mode;
import com.hansung.drawingtogether.view.drawing.MqttMessageFormat;
import com.hansung.drawingtogether.view.drawing.Text;
import com.hansung.drawingtogether.view.drawing.TextAttribute;
import com.hansung.drawingtogether.view.drawing.TextMode;
import com.hansung.drawingtogether.view.main.JoinMessage;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

@Getter
public enum MQTTClient {
    INSTANCE;

    private MqttClient client;
    private String BROKER_ADDRESS;

    private FirebaseStorage storage;
    private StorageReference storageReference;

    private FirebaseDatabase database;
    private DatabaseReference databaseReference;

    private boolean master;
    private List<String> userList = new ArrayList<>();
    private String myName;

    private String topic;
    private String topic_join;
    private String topic_exit;
    private String topic_delete;
    private String topic_data;
    // private String topic_load;

    private DrawingViewModel drawingViewModel;

    private int qos = 2;
    private DrawingEditor de = DrawingEditor.getInstance();
    private JSONParser parser = JSONParser.getInstance();
    private DrawingFragment drawingFragment;
    private FragmentDrawingBinding binding;
    private DrawingTask drawingTask;
    private DrawingView drawingView;

    public static MQTTClient getInstance() { return INSTANCE; }

    public void init(String topic, String name, boolean master, DrawingViewModel drawingViewModel, String ip, String port) {
        connect(ip, port);

        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        database = FirebaseDatabase.getInstance();
        databaseReference = database.getReference();

        this.master = master;
        this.topic = topic;
        this.myName = name;
        //userList.add(myName);
        topic_join = this.topic + "_join";
        topic_exit = this.topic + "_exit";
        topic_delete = this.topic + "_delete";
        topic_data = this.topic + "_data";

        this.drawingViewModel = drawingViewModel;
        this.drawingViewModel.setUserNum(userList.size());

        de.setMyUsername(name);
    }

    public void connect(String ip, String port) {
        try {
            BROKER_ADDRESS = "tcp://" + ip + ":" + port;
            client = new MqttClient(BROKER_ADDRESS, MqttClient.generateClientId(), new MemoryPersistence());

            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            connOpts.setKeepAliveInterval(1000);
            connOpts.setMaxInflight(5000);   //?

            client.connect(connOpts);

            Log.e("kkankkan", "mqtt connect success");
            Log.i("mqtt", "CONNECT");

            String currentClientId = client.getClientId();
            Log.i("mqtt", "Client ID: " + currentClientId);
        } catch(MqttException e) {
            e.printStackTrace();
        }
    }

    public void subscribe(String newTopic) {
        try {
            client.subscribe(newTopic, this.qos);
            //client.subscribe(newTopic);
            Log.e("kkankkan", newTopic + " subscribe");
            Log.i("mqtt", "SUBSCRIBE topic: " + newTopic);
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

    public void publish(String newTopic, String payload) {
        try {
            MqttMessage message = new MqttMessage(payload.getBytes());

            client.publish(newTopic, payload.getBytes(), this.qos, false);
            Log.i("mqtt", "PUBLISH topic: " + newTopic + ", msg: " + message);
        } catch(MqttException e) {
            e.printStackTrace();
        }
    }

    public void setCallback() {
        client.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                Log.e("kkankkan", cause.toString());
                Log.i("mqtt", cause.getCause().toString());
                Log.i("mqtt", "CONNECTION LOST");
            }

            @Override
            public void messageArrived(String newTopic, MqttMessage message) throws Exception {

                /*
                topic_join으로 오는 메시지 종류
                1. "name":"이름"
                2. "master":"이름"/"userList":"이름1,이름2,이름3"/"loadingData":"..."
                 */
              /*  if (newTopic.equals(topic_join)) {
                    String data[] = message.toString().split(":");

                    if (data[0].equals("master")) {
                        String token[] = message.toString().split("/");
                        String name = token[1].split(":")[1];
                        String users = token[2].split(":")[1];
                        String loadingData = token[3].split(":")[1];

                        if (name.equals(myName)) {
                            Log.e("kkankkan", "master가 보낸 userList : " + users);

                            userList.removeAll(userList);
                            for (int i=0; i<users.split(",").length; i++) {
                                userList.add(users.split(",")[i]);
                            }

                            Log.e("kkankkan", userList.toString());

                            topic_data = loadingData;
                        }

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

                                //MqttMessage msg = new MqttMessage(("master:" + userList.get(0) + "/userList:" + users + "/loadingData:" + topic_data).getBytes());
                                MqttMessage msg = new MqttMessage(("master:" + userList.get(0) + "/to:" + userList.get(userList.size()-1) + "/userList:" + users + "/loadingData:" + topic_data).getBytes());
                                client.publish(topic_join, msg);

                                Log.e("kkankkan", "master data -> " + msg);
                            }

                            Log.e("kkankkan", name + " join 후 : " + userList.toString());
                            //drawingViewModel.setUserNumTv(userList.size());
                        }
                        else {  // self
                            userList.add(name);
                        }

                    }
                    drawingViewModel.setUserNum(userList.size());
                }
                */

                // fixme nayeon [ 중간자 ]
                if (newTopic.equals(topic_join)) {
                    String msg = new String(message.getPayload());
                    MqttMessageFormat mqttMessageFormat = (MqttMessageFormat) parser.jsonReader(msg);
                    JoinMessage joinMessage = mqttMessageFormat.getJoinMessage();

                    String master = joinMessage.getMaster(); // null or not-null ( "master":"이름"/"userList":"이름1,이름2,이름3"/"loadingData":"..." )
                    String name = joinMessage.getName(); // null or not-null ( "name":"이름" )
                    List<String> users = joinMessage.getUserList(); // null or not-null
                    // String loadingData = joinMessage.getLoadingData(); // null or not-null

                    if (master != null) { // 메시지 형식이 "master":"이름"/"userList":"이름1,이름2,이름3"/"loadingData":"..."  일 경우
                        String to = joinMessage.getTo();

                        if (to.equals(myName)) { // 마스터가 중간자(to:" ") 에게 보낸 메시지 처리

                            /* 중간자만 처리하는 부분 */

                            userList = users; // 메시지로 전송받은 리스트 배열 세팅
                            //topic_data = loadingData;

                            Log.e("who I am", to);
                            Log.e("received message", "mid data -> " + msg);

                            // fixme nayeon - 드로잉에 필요한 구조체들 저장하는 부분
                            // 필요한 배열 리스트들과 배경 이미지 세팅
                            de.setDrawingComponents(mqttMessageFormat.getDrawingComponents());
                            de.setTexts(mqttMessageFormat.getTexts());
                            if(mqttMessageFormat.getBitmapByteArray() != null) { de.byteArrayToBitmap(mqttMessageFormat.getBitmapByteArray()); }

                            // 아이디 세팅 fixme nayeon - 동시성 문제
                            de.setComponentId(de.getDrawingComponents().size() - 1);
                            de.setTextId(de.getTexts().size() - 1);

                            Log.e("my name, drawingComponents size", myName + ", " + de.getDrawingComponents().size());
                            Log.e("my name, texts size", myName + ", " + de.getTexts().size());
                            Log.e("componentId variable value, last componentId", de.getComponentId() + ", " + de.getDrawingComponents().get(de.getDrawingComponents().size()-1).getId());
                            Log.e("textId variable value, last textId", de.getTextId() + ", " + de.getTexts().get(de.getTexts().size()-1).getTextAttribute().getId());

                            client.publish(topic_data, new MqttMessage(JSONParser.getInstance().jsonWrite(new MqttMessageFormat(myName, Mode.MID)).getBytes()));
                        }
                    }
                    else {  // other or self // 메시지 형식이 "name":"이름"  일 경우
                        if (!myName.equals(name)) {  // other
                            userList.add(name);

                            if (isMaster()) { //  todo nayeon - 마스터인 경우 자신의 드로잉 구조체들 전송하는 부분
                                //JoinMessage joinMsg = new JoinMessage(userList.get(0), userList.get(userList.size() - 1), userList);
                                JoinMessage joinMsg = new JoinMessage(userList.get(0), userList.get(userList.size() - 1), userList);

                                MqttMessageFormat messageFormat;
                                if(de.getBackgroundImage() == null) { messageFormat = new MqttMessageFormat(joinMsg, de.getDrawingComponents(), de.getTexts()); }
                                else {  messageFormat = new MqttMessageFormat(joinMsg, de.getDrawingComponents(), de.getTexts(), de.bitmapToByteArray(de.getBackgroundImage())); }
                                MqttMessage mqttMessage = new MqttMessage(parser.jsonWrite(messageFormat).getBytes());
                                client.publish(topic_join, mqttMessage);

                                Log.e("kkankkan", "master data -> " + mqttMessage);
                            }

                            Log.e("kkankkan", name + " join 후 : " + userList.toString());
                            //drawingViewModel.setUserNumTv(userList.size());
                        }
                        else {  // self
                            userList.add(name);
                        }
                    }
                    drawingViewModel.setUserNum(userList.size());
                }


                if (newTopic.equals(topic_exit)) {
                    if (myName.equals(message.toString())) {  // 내가 exit 하는 경우
                        if (userList.size() == 1) {  // 나==마지막 사용자, master==나
                            // db에 drawview data 저장
                            databaseReference.child(topic).child("master").setValue(false);
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
                        userList.removeAll(userList);
                        // 나가기
                        drawingViewModel.back();
                    }
                    else {  // 다른 사람이 exit 하는 경우
                        userList.remove(message.toString());
                        if (myName.equals(userList.get(0)) && !master) {  // 확인 해봐야함
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
                        userList.removeAll(userList);
                        // 나가기
                        drawingViewModel.back();
                    }
                    if (message.toString().equals(myName) && master) {
                        Log.e("kkankkan", "master delete");
                        // db에서 topic 삭제

                        databaseReference.child(topic).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
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
                                userList.removeAll(userList);
                                // 나가기
                                drawingViewModel.back();
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.e("kkankkan", e.toString());
                            }
                        });

                        /*storageReference.child(topic).delete().addOnSuccessListener(new OnSuccessListener<Void>() {
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
                        });*/
                    }
                }

                //drawing
                if (newTopic.equals(topic_data)) {
                    String msg = new String(message.getPayload());
                    MqttMessageFormat messageFormat = (MqttMessageFormat)parser.jsonReader(msg);

                    drawingTask = new DrawingTask();
                    drawingTask.execute(messageFormat);
                    //drawingTask.cancel(true);
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });


    }

    class DrawingTask extends AsyncTask<MqttMessageFormat, MqttMessageFormat, Void> {
        private int action;
        private Mode mode;
        private DrawingComponent dComponent;

        private Void draw() {
            float myCanvasWidth = drawingView.getCanvasWidth();
            float myCanvasHeight = drawingView.getCanvasHeight();
            dComponent.calculateRatio(myCanvasWidth, myCanvasHeight);

            switch(action) {
                case MotionEvent.ACTION_DOWN:
                    dComponent.setId(de.componentIdCounter());
                    de.addCurrentComponents(dComponent);
                    Log.i("drawing", "currentComponents.size() = " + de.getCurrentComponents().size());
                    dComponent.draw(de.getBackCanvas());
                    return null;

                case MotionEvent.ACTION_MOVE:
                    dComponent.draw(de.getBackCanvas());
                    return null;

                case MotionEvent.ACTION_UP:
                    dComponent.draw(de.getBackCanvas());
                    de.removeCurrentComponents(dComponent.getId());
                    //de.removeCurrentComponents(dComponent);

                /*if(de.isContainsDrawingComponents(dComponent.getId())) {
                    de.setCurrentMode(Mode.DRAWSYNC);

                    MqttMessageFormat messageFormat = new MqttMessageFormat(de.getMyUsername(), de.getCurrentMode(), dComponent.getId() + 1, dComponent);
                    publish(topic_data, parser.jsonWrite(messageFormat));

                    de.setCurrentMode(Mode.DRAW);
                    dComponent.setId(dComponent.getId() + 1);
                }*/
                /*String str = "dc up = ";
                for(DrawingComponent component: de.getDrawingComponents()) {
                    str += component.getId() + " ";
                }
                Log.i("drawing", str);*/

                    de.splitPoints(dComponent, myCanvasWidth, myCanvasHeight);
                    de.addDrawingComponents(dComponent);
                    de.addHistory(new DrawingItem(de.getCurrentMode(), dComponent/*, de.getDrawingBitmap()*/));
                    Log.i("drawing", "drawingComponents.size() = " + de.getDrawingComponents().size());
                    de.setLastDrawingBitmap(de.getDrawingBitmap().copy(de.getDrawingBitmap().getConfig(), true));
                    return null;
            }
            return null;
        }

        private Void changeText(MqttMessageFormat message) {
            TextMode textMode = message.getTextMode();
            TextAttribute textAttr = message.getTextAttr();

            Text text = null;

            // 텍스트 객체가 처음 생성되는 경우, 텍스트 배열에 저장된 정보 없음
            // 그 이후에 일어나는 텍스트에 대한 모든 행위들은
            // 텍스트 배열로부터 텍스트 객체를 찾아서 작업 가능
            if(!textMode.equals(TextMode.CREATE)) {
                text = de.findTextById(textAttr.getId());

                if(text == null) return null; // fixme nayeon - 중간자가 자신에게 MID 로 보낸 메시지보다, 마스터가 TEXT 로 보낸 메시지가 먼저 올 경우 (중간자가 자신의 처리를 다 했다는 플래그 필요?)

                text.setTextAttribute(textAttr); // MQTT 로 전송받은 텍스트 속성 지정해주기
            }

            switch (textMode) {
                case CREATE:
                    new Text(drawingFragment, textAttr).getTextAttribute().setTextInited(true); // fixme nayeon
                    publishProgress(message);
                    Log.e("texts size", Integer.toString(de.getTexts().size()));
                    return null;
                case DRAG_STARTED:
                case DRAG_LOCATION:
                case DROP:
                case DRAG_EXITED:
                    text.setTextViewLocation();
                    return null;
                case DONE:
                case DRAG_ENDED:
                    return null;
                case ERASE:
                case MODIFY:
                    publishProgress(message);
                    return null;
            }
            return null;
        }
        private void changeTextOnMainThread(MqttMessageFormat message) {
            TextMode textMode = message.getTextMode();
            TextAttribute textAttr = message.getTextAttr();

            Text text = de.findTextById(textAttr.getId());
            switch(textMode) {
                case CREATE:
                    textAttr.setId(de.textIdCounter());    //fixme minj componentIdCounter()
                    text.addTextViewToFrameLayout();
                    text.createGestureDetecter();
                    break;
                case DRAG_STARTED:
                case DRAG_LOCATION:
                case DRAG_ENDED:
                    break;
                case ERASE:
                    text.removeTextViewToFrameLayout();
                    de.removeTexts(text);

                    Log.e("texts size", Integer.toString(de.getTexts().size()));
                    break;
                case MODIFY:
                    text.modifyTextViewContent(textAttr.getText());
                    break;
            }
        }

        @Override
        protected Void doInBackground(MqttMessageFormat... messages) {
            MqttMessageFormat message = messages[0];

            String username = message.getUsername();
            this.mode = message.getMode();
            this.action = message.getAction();
            this.dComponent = message.getComponent();

            Log.e("my name, message name, mode", de.getMyUsername() + ", " + username + ", " + mode.toString());

            // fixme nayeon
            // 메시지를 보낸 사람의 이름이 나의 이름이고, 메시지에 MID 모드가 지정되어 있다면
            // 중간자 자기 자신에게 보낸 메시지
            if(de.getMyUsername().equals(username) && this.mode.equals(Mode.MID)) {
                // drawingComponents 그리기
                // 백그라운드 이미지 지정
                // texts 붙이기 등등

                de.drawAllDrawingComponents(); // drawingComponents 그리기 ( onPostExecute 에서 invalidate 호출 ) - fixme nayeon (단순 그려진 모습 보기위한 코드)

                publishProgress(message); // 텍스트 붙이기와 배경 이미지 붙이기는 메인 스레드에서 처리 필요
                return null;
            }

            // 중간자가 MID 모드의 메시지 보다 다른 모드(TEXT) 메시지 먼저 받는 경우 있음
            if(de.getMyUsername().equals(username)) return null;

            switch(mode) {
                case DRAW:
                    //fixme not myUsername --> curentComponents draw
                    Log.i("mqtt", "MESSAGE ARRIVED message: username=" + dComponent.getUsername() + ", mode=" + mode.toString() + ", id=" + dComponent.getId());
                    draw();
                    return null;
                case ERASE:
                    Log.i("mqtt", "MESSAGE ARRIVED message: username=" + username + ", mode=" + mode.toString() + ", id=" + message.getComponentIds().toString());
                    Vector<Integer> erasedComponentIds = message.getComponentIds();
                    new EraserTask(erasedComponentIds).doNotInBackground();
                    return null;
                case SELECT:
                case GROUP:
                    return null;
                case TEXT:
                    changeText(message);
                    return null;
               /* case BACKGROUND_IMAGE:
                    //de.setBackgroundImage(de.stringToBitmap(message.getBitmapString()));
                    de.setBackgroundImage(de.byteArrayToBitmap(message.getBitmapByteArray()));
                    publishProgress(message);
                    return null;*/
                case CLEAR:
                    Log.i("mqtt", "MESSAGE ARRIVED message: username=" + username + ", mode=" + mode.toString());
                    de.clearDrawingComponents();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(MqttMessageFormat... messages) { // fixme nayeon
            MqttMessageFormat message = messages[0];

            Mode mode = message.getMode();

            switch(mode) {
               /* case BACKGROUND_IMAGE:
                    ImageView imageView = new ImageView(drawingFragment.getContext());
                    imageView.setLayoutParams(new LinearLayout.LayoutParams(drawingFragment.getSize().x, ViewGroup.LayoutParams.MATCH_PARENT));
                    imageView.setImageBitmap(de.getBackgroundImage());

                    binding.backgroundView.addView(imageView);
                    break;*/
                case TEXT:
                    changeTextOnMainThread(message);
                    break;

                case MID:
                    Log.e("onProgressUpdate", de.getMyUsername());
                    de.addAllTextViewToFrameLayout();
                    break;
            }
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            if(mode == Mode.CLEAR) {
                de.clearTexts(); // 텍스트는 드로잉 컴포넌트들과 달리 Background 에서 처리 불가, 따라서 onPostExecute 에서 처리필요
            }

            drawingView.invalidate();
        }
    }

    public void setDrawingFragment(DrawingFragment drawingFragment) {
        this.drawingFragment = drawingFragment;
        this.binding = drawingFragment.getBinding();
        this.drawingView = this.binding.drawingView;
    }

}
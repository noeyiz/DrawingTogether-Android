package com.hansung.drawingtogether.data.remote.model;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.Canvas;

import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import lombok.Getter;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.hansung.drawingtogether.databinding.FragmentDrawingBinding;
import com.hansung.drawingtogether.view.drawing.ComponentType;
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
import com.hansung.drawingtogether.view.main.AliveMessage;
import com.hansung.drawingtogether.view.main.DeleteMessage;
import com.hansung.drawingtogether.view.main.ExitMessage;
import com.hansung.drawingtogether.view.main.JoinMessage;
import com.hansung.drawingtogether.view.main.MainActivity;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.net.UnknownServiceException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Vector;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

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
    private List<User> userList = new ArrayList<>();  // fixme hyeyeon-User객제 arrayList로 변경
    private String myName;

    private String topic;
    private String topic_join;
    private String topic_exit;
    private String topic_delete;
    private String topic_data;
    private String topic_mid;

    private String topic_alive; // fixme hyeyeon-토픽 추가
    // private String topic_load;

    private DrawingViewModel drawingViewModel;

    private int qos = 2;
    private JSONParser parser = JSONParser.getInstance();
    private DrawingEditor de = DrawingEditor.getInstance();
    private DrawingFragment drawingFragment;
    private FragmentDrawingBinding binding;
    private DrawingTask drawingTask;
    private DrawingView drawingView;
    private boolean isMid = true;
    private Map<String, Integer> usersActionMap;

    private ProgressDialog progressDialog;

    // fixme hyeyeon
    private boolean backPressed;
    private Thread th;
    //

    public static MQTTClient getInstance() { return INSTANCE; }

    public void init(String topic, String name, boolean master, DrawingViewModel drawingViewModel, String ip, String port) {
        connect(ip, port);

        userList.clear();  // fixme hyeyeon-exit,delete시 clear대신 init시 clear로 변경

        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        database = FirebaseDatabase.getInstance();
        databaseReference = database.getReference();

        this.master = master;
        this.topic = topic;
        this.myName = name;

        User user = new User(myName, 0);  // fixme hyeyeon
        userList.add(user); // 생성자에서 사용자 리스트에 내 이름 추가

        topic_join = this.topic + "_join";
        topic_exit = this.topic + "_exit";
        topic_delete = this.topic + "_delete";
        topic_data = this.topic + "_data";
        topic_mid = this.topic + "_mid";
        topic_alive = this.topic + "_alive"; // fixme hyeyeon

        this.drawingViewModel = drawingViewModel;
        this.drawingViewModel.setUserNum(userList.size());
        this.drawingViewModel.setUserPrint(userPrint());

        this.usersActionMap = new HashMap<>();
        de.setMyUsername(name);

        this.backPressed = false; // fixme hyeyeon
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
            showTimerAlertDialog("브로커 연결 실패", "메인 화면으로 이동합니다");
        }
    }

    public void subscribe(String newTopic) {
        try {
            client.subscribe(newTopic, this.qos);
            Log.e("kkankkan", newTopic + " subscribe");
            Log.i("mqtt", "SUBSCRIBE topic: " + newTopic);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

/*    public void publish(String newTopic, byte[] payload) {
        try {
            MqttMessage message = new MqttMessage(payload);
            client.publish(newTopic, message);
            Log.e("kkankkan", newTopic + " publish");
        } catch (MqttException e) {
            e.printStackTrace();
            showTimerAlertDialog("메시지 전송 실패", "메인 화면으로 이동합니다");
        }
    }*/

    public void publish(String newTopic, String payload) {
        try {
            MqttMessage message = new MqttMessage(payload.getBytes());
            client.publish(newTopic, payload.getBytes(), this.qos, false);
            //Log.i("mqtt", "PUBLISH topic: " + newTopic + ", msg: " + message); // fixme nayeon
            // Log.e("mqtt payload size", Integer.toString(message.getPayload().length)); // fixme nayeon
        } catch(MqttException e) {
            e.printStackTrace();
            showTimerAlertDialog("메시지 전송 실패", "메인 화면으로 이동합니다");
        }
    }

    public void unsubscribeAllTopics() {    //fixme minj - unsubscribe 할 topic 이 추가되어 따로 함수 생성
        try {
            client.unsubscribe(topic_join);
            client.unsubscribe(topic_exit);
            client.unsubscribe(topic_delete);
            client.unsubscribe(topic_data);
            client.unsubscribe(topic_mid);
            client.unsubscribe(topic_alive);  // fixme hyeyeon

            Log.e("kkankkan", "unsubscribe 완료");

        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    // fixme hyeyeon
    public String userPrint() {
        String names = "";
        for (User user: userList) {
            names += user.getName() + "\n";
        }
        return names;
    }

    public String userPrintForLog() {
        String names = "";
        for (User user: userList) {
            names += "[" + user.getName() + "," + user.getCount() + "] ";
        }
        return names;
    }

    public void setCallback() {
        client.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                showTimerAlertDialog("브로커 연결 유실", "메인 화면으로 이동합니다.");

                Log.e("kkankkan", cause.toString());
                Log.i("mqtt", cause.getCause().toString());
                Log.i("mqtt", "CONNECTION LOST");
            }

            @Override
            public void messageArrived(String newTopic, MqttMessage message) throws Exception {
                // [ 중간자 ]
                if (newTopic.equals(topic_join)) {
                    String msg = new String(message.getPayload());
                    MqttMessageFormat mqttMessageFormat = (MqttMessageFormat) parser.jsonReader(msg);
                    JoinMessage joinMessage = mqttMessageFormat.getJoinMessage();

                    String master = joinMessage.getMaster(); // null or not-null ( "master":"이름"/"userList":"이름1,이름2,이름3"/"loadingData":"..." )
                    String name = joinMessage.getName(); // null or not-null ( "name":"이름" )
                    List<User> users = joinMessage.getUserList(); // null or not-null  // fixme hyeyeon

                    if (master != null) { // 메시지 형식이 "master":"이름"/"userList":"이름1,이름2,이름3"/"loadingData":"..."  일 경우
                        //binding.drawingView.setIntercept(false);
                        de.setIntercept(false); // 중간에 누가 들어왔을 때 선을 그리는 중이 아닌 다른 사용자들 터치 이벤트 부모뷰에서 가로채도록 (모두가 처리하는 부분)
                        Log.i("drawing", "isMid = " + isMid());

                        String to = joinMessage.getTo();
                        Log.i("mqtt", "to = " + to + ", myname = " + myName);
                        if (to.equals(myName)) { // 마스터가 중간자(to:" ") 에게 보낸 메시지 처리

                            /* 중간자만 처리하는 부분 */

                            if (userList.size() > 1) {  // fixme hyeyeon-중간자가 마스터로부터 데이터를 받기 전, 다른 사람의 join 메시지를 받을 수 있음
                                userList.remove(0); // 내 이름을 지우고
                                for (User user : userList) { // 내 이름 다음의 이름들을 마스터로부터 전송받은 사용자 리스트에 추가
                                    users.add(user);
                                }
                            }
                            userList.clear(); // 중간자는 마스터에게 사용자 리스트를 받기 전에 userList.add() 했음 따라서 자신의 리스트를 지우고 마스터가 보내준 배열 저장
                            for (User user: users) {  // 메시지로 전송받은 리스트 배열 세팅 // fixme hyeyeon
                                user.setCount(0);
                                userList.add(user);
                            }
                            //topic_data = loadingData;

                            Log.e("who I am", to);
                            Log.e("received message", "mid data -> " + msg);

                            // 드로잉에 필요한 구조체들 저장하는 부분
                            // 필요한 배열 리스트들과 배경 이미지 세팅
                            de.setDrawingComponents(mqttMessageFormat.getDrawingComponents());
                            de.setHistory(mqttMessageFormat.getHistory());
                            de.setUndoArray(mqttMessageFormat.getUndoArray());
                            de.setRemovedComponentId(mqttMessageFormat.getRemovedComponentId());

                            de.setTexts(mqttMessageFormat.getTexts());
                            if (mqttMessageFormat.getBitmapByteArray() != null) {
                                de.byteArrayToBitmap(mqttMessageFormat.getBitmapByteArray());
                            }

                            // 아이디 세팅
                            de.setComponentId(mqttMessageFormat.getMaxComponentId());
                            // de.setTextId(mqttMessageFormat.getMaxTextId()); // fixme nayeon - 텍스트 아이디는 "사용자이름-textIdCount" 이므로 textIdCount 가 같아도 고유
                            Log.i("drawing", "component id = " + mqttMessageFormat.getMaxComponentId() + ", text id = " + mqttMessageFormat.getMaxTextId());

                            if(mqttMessageFormat.getBitmapByteArray() != null) {
                                de.setBackgroundImage(de.byteArrayToBitmap(mqttMessageFormat.getBitmapByteArray()));
                            }

                            publish(topic_mid, JSONParser.getInstance().jsonWrite(new MqttMessageFormat(myName, Mode.MID)));
                            //publish(topic_data, JSONParser.getInstance().jsonWrite(new MqttMessageFormat(myName, Mode.MID)));
                        }
                    } else {  // other or self // 메시지 형식이 "name":"이름"  일 경우
                        if (!myName.equals(name)) {  // other // 한 사람이 "name":"이름" 메시지 보냈을 경우 다른 사람들이 받아서 처리하는 부분 - '나'는 처리 안하는 부분
                            if (!isContainsUserList(name)) {
                                User user = new User(name, 0);  // fixme hyeyeon
                                userList.add(user); // 들어온 사람의 이름을 추가

                                // 다른 사용자가 들어왔다는 메시지를 받았을 경우
                                // 텍스트 비활성화를 위해 플래그 설정
                                de.setMidEntered(true); // fixme nayeon

                                if (de.getCurrentMode() == Mode.DRAW) {  // current mode 가 DRAW 이면, 그리기 중이던 component 까지만 그리고 touch intercept   // todo 다른 모드에서도 intercept 하도록 추가
                                    de.setIntercept(true);
                                    //binding.drawingView.InterceptTouchEventAndDoActionUp();
                                }
                                setToastMsg("[ " + userList.get(userList.size() - 1).getName() + " ] 님이 접속하셨습니다");
                            }

                            // 마스터이고, 모든 username 의 마지막 draw action 이 ACTION_UP 이면, 자신의 드로잉 구조체들 전송
                            if (isMaster()) {
                                if (isUsersActionUp() && isTextInUse()) { // fixme nayeon
                                    JoinMessage joinMsg = new JoinMessage(userList.get(0).getName(), userList.get(userList.size() - 1).getName(), userList);  // fixme hyeyeon

                                    MqttMessageFormat messageFormat;
                                    if (de.getBackgroundImage() == null) {
                                        messageFormat = new MqttMessageFormat(joinMsg, de.getDrawingComponents(), de.getTexts(), de.getHistory(), de.getUndoArray(), de.getRemovedComponentId(), de.getMaxComponentId(), de.getMaxTextId());
                                    } else {
                                        messageFormat = new MqttMessageFormat(joinMsg, de.getDrawingComponents(), de.getTexts(), de.getHistory(), de.getUndoArray(), de.getRemovedComponentId(), de.getMaxComponentId(), de.getMaxTextId(), de.bitmapToByteArray(de.getBackgroundImage()));
                                    }
                                    publish(topic_join, parser.jsonWrite(messageFormat));
                                    //binding.drawingView.setIntercept(false);

                                    setToastMsg("[ " + userList.get(userList.size() - 1).getName() + " ] 님에게 데이터 전송을 완료했습니다");
                                    Log.e("kkankkan", "master data -> " + parser.jsonWrite(messageFormat));
                                    Log.e("kkankkan", name + " join 후 : " + userList.toString());
                                    Log.i("drawing", "payload size (bytes) = " + parser.jsonWrite(messageFormat).getBytes().length);
                                } else {
                                    MqttMessageFormat messageFormat = new MqttMessageFormat(new JoinMessage(userList.get(userList.size() - 1).getName()));
                                    publish(topic_join, parser.jsonWrite(messageFormat));

                                    Log.e("master republish name", topic_join);
                                }
                            }
                            //drawingViewModel.setUserNumTv(userList.size());
                        }
                    }
                    drawingViewModel.setUserNum(userList.size());
                    drawingViewModel.setUserPrint(userPrint());
                    //Log.e("after topic_join process", Arrays.toString(userList.toArray()));
                }

                if (newTopic.equals(topic_exit)) {
                    String msg = new String(message.getPayload());
                    MqttMessageFormat mqttMessageFormat = (MqttMessageFormat) parser.jsonReader(msg);
                    ExitMessage exitMessage = mqttMessageFormat.getExitMessage();
                    String name = exitMessage.getName();
                   /* String exitMsg = exitMessage.getMessage();  // fixme hyeyeon-자신도 모르는 사이에 자신이 강제종료 되는 경우가 생긴다면 추가할 부분

                    if (myName.equals(name) && exitMessage != null) {
                        th.interrupt();
                        unsubscribeAllTopics();
                        isMid = true;
                        de.removeAllDrawingData();
                        usersActionMap.clear();
                        Log.i("drawing", "userActionMap = " + usersActionMap.toString());
                        drawingViewModel.back();
                    }*/

                    if (myName.equals(name)) {  // 내가 exit 하는 경우
                        if (userList.size() == 1 && isMaster()) {  // 나==마지막 사용자, master==나
                            // db에 drawview data 저장
                            databaseReference.child(topic).runTransaction(new Transaction.Handler() {  // fixme hyeyeon-DB접근 부분 트랜젝션 함수 사용하도록 변경함
                                @NonNull
                                @Override
                                public Transaction.Result doTransaction(@NonNull MutableData mutableData) {
                                    if (mutableData.getValue() == null) {
                                        Log.e("kkankkan", "mutabledata null");
                                    } else {
                                        mutableData.child("master").setValue(false);
                                    }
                                    return Transaction.success(mutableData);
                                }

                                @Override
                                public void onComplete(@Nullable DatabaseError databaseError, boolean b, @Nullable DataSnapshot dataSnapshot) {
                                    Log.e("kkankkan", "DB master value change success");
                                    Log.e("kkankkan", "transaction complete");

                                    th.interrupt(); // fixme hyeyeon
                                    unsubscribeAllTopics();
                                    isMid = true;
                                    de.removeAllDrawingData();
                                    usersActionMap.clear();
                                    Log.i("drawing", "userActionMap = " + usersActionMap.toString());
                                    //de.printDrawingData();
                                    //userList.clear();

                                    if (backPressed) {  // fixme hyeyeon
                                        drawingFragment.getActivity().finish();
                                        System.exit(0);
                                        android.os.Process.killProcess(android.os.Process.myPid());
                                    }
                                    ((MainActivity)drawingFragment.getActivity()).setOnKeyBackPressedListener(null);  // fixme hyeyeon
                                    drawingViewModel.back();
                                }
                            });
                        } else {  // master 아닌데 그냥 exit
                            th.interrupt(); // fixme hyeyeon
                            unsubscribeAllTopics();
                            isMid = true;
                            de.removeAllDrawingData();
                            usersActionMap.clear();
                            Log.i("drawing", "userActionMap = " + usersActionMap.toString());
                            //de.printDrawingData();
                            //userList.clear();

                            if (backPressed) {  // fixme hyeyeon
                                drawingFragment.getActivity().finish();
                                System.exit(0);
                                android.os.Process.killProcess(android.os.Process.myPid());
                            }
                            ((MainActivity)drawingFragment.getActivity()).setOnKeyBackPressedListener(null);  // fixme hyeyeon
                            drawingViewModel.back();
                        }
                    } else {  // 다른 사람이 exit 하는 경우
                        for (User user: userList) {
                            if (user.getName().equals(name)) {
                                usersActionMap.remove(user.getName());
                                Log.i("drawing", "userActionMap = " + usersActionMap.toString());
                                userList.remove(user); break;
                            }
                        }
                        if (myName.equals(userList.get(0).getName()) && !isMaster()) {
                            master = true;
                            Log.e("kkankkan", "새로운 master는 나야! " + isMaster());
                        }
                        drawingViewModel.setUserNum(userList.size());
                        drawingViewModel.setUserPrint(userPrint());

                        Log.e("kkankkan", name + " exit 후 " + userPrintForLog());
                    }
                }

                if (newTopic.equals(topic_delete)) {
                    String msg = new String(message.getPayload());
                    MqttMessageFormat mqttMessageFormat = (MqttMessageFormat) parser.jsonReader(msg);
                    DeleteMessage deleteMessage = mqttMessageFormat.getDeleteMessage();

                    String name = deleteMessage.getName();
                    String deleteMsg = deleteMessage.getMessage();

                    if (deleteMsg != null) {  // fixme hyeyeon-마스터가 topic을 delete한 경우
                        th.interrupt(); // fixme hyeyeon
                        unsubscribeAllTopics();
                        isMid = true;
                        de.removeAllDrawingData();
                        usersActionMap.clear();
                        Log.i("drawing", "userActionMap = " + usersActionMap.toString());
                        //de.printDrawingData();

                        //userList.clear();
                        ((MainActivity)drawingFragment.getActivity()).setOnKeyBackPressedListener(null);  // fixme hyeyeon
                        drawingViewModel.back();
                    }
                    if (name.equals(myName) && isMaster()) {
                        databaseReference.child(topic).runTransaction(new Transaction.Handler() {  // fixme hyeyeon
                            @NonNull
                            @Override
                            public Transaction.Result doTransaction(@NonNull MutableData mutableData) {
                                if (mutableData.getValue() == null) {
                                    Log.e("kkankkan", "mutabledata null");
                                } else {
                                    databaseReference.child(topic).removeValue();
                                }
                                return Transaction.success(mutableData);
                            }

                            @Override
                            public void onComplete(@Nullable DatabaseError databaseError, boolean b, @Nullable DataSnapshot dataSnapshot) {
                                Log.e("kkankkan", "topic delete success");
                                Log.e("kkankkan", "transaction complete");

                                th.interrupt(); // fixme hyeyeon
                                unsubscribeAllTopics();

                                DeleteMessage delMessage = new DeleteMessage(myName, "delete");
                                MqttMessageFormat messageFormat = new MqttMessageFormat(delMessage);
                                publish(topic_delete, JSONParser.getInstance().jsonWrite(messageFormat));  // fixme hyeyeon

                                isMid = true;
                                de.removeAllDrawingData();
                                usersActionMap.clear();
                                Log.i("drawing", "userActionMap = " + usersActionMap.toString());

                                //userList.clear();
                                ((MainActivity)drawingFragment.getActivity()).setOnKeyBackPressedListener(null);  // fixme hyeyeon
                                drawingViewModel.back();
                            }
                        });
                        /*
                        databaseReference.child(topic).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Log.e("kkankkan", "topic delete success");

                                unsubscribeAllTopics();

                                publish(topic_delete, "master:delete".getBytes());  // fixme hyeyeon [2]

                                isMid = true;
                                de.removeAllDrawingData();

                                userList.clear();
                                usersActionMap.clear();
                                Log.i("drawing", "userActionMap = " + usersActionMap.toString());

                                drawingViewModel.back();
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.e("kkankkan", e.toString());
                            }
                        });*/

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
                    // fixme hyeyeon - master가 아닌 사용자가 topic delete 누를 때 토스트를 출력해주기 위함
                    else if (name.equals(myName) && !isMaster()) {
                        setToastMsg("master만 topic을 삭제할 수 있습니다");
                    }
                }

                // fixme hyeyeon
                if (newTopic.equals(topic_alive)) {
                    String msg = new String(message.getPayload());
                    MqttMessageFormat mqttMessageFormat = (MqttMessageFormat) parser.jsonReader(msg);
                    AliveMessage aliveMessage = mqttMessageFormat.getAliveMessage();
                    String name = aliveMessage.getName();
                    // Log.e("kkankkan", name); // fixme nayeon 출력 주석

                    if (myName.equals(name)) {
                        // Log.e("kkankkan", "COUNT PLUS BEFORE" + userPrintForLog());
                        Iterator<User> iterator = userList.iterator();
                        while (iterator.hasNext()) {
                            User user = iterator.next();
                            if (!user.getName().equals(myName)) {
                                user.setCount(user.getCount() + 1);
                                if (user.getCount() == 5) {
                                /*ExitMessage exitMessage = new ExitMessage(user.getName(), "AbnormalExit");
                                MqttMessageFormat messageFormat = new MqttMessageFormat(exitMessage);
                                publish(topic_exit, JSONParser.getInstance().jsonWrite(messageFormat));*/
                                    iterator.remove();
                                    drawingViewModel.setUserNum(userList.size());
                                    drawingViewModel.setUserPrint(userPrint());
                                    if (userList.get(0).getName().equals(myName) && !isMaster()) {
                                        master = true;
                                        Log.e("kkankkan", "새로운 master는 나야! " + isMaster());
                                    }
                                    Log.e("kkankkan", user.getName() + " exit 후 [userList] : " + userPrintForLog());
                                    setToastMsg("[ " + user.getName() + " ] 님 접속이 끊겼습니다");
                                }
                            }
                        }
                        // Log.e("kkankkan", "COUNT PLUS AFTER" + userPrintForLog());
                    } else {
                        for (User user : userList) {
                            if (user.getName().equals(name)) {
                                user.setCount(0); break;
                            }
                        }
                    }
                }
                //

                //drawing
                if (newTopic.equals(topic_data) && de.getDrawingBitmap() != null) {
                    String msg = new String(message.getPayload());
                    MqttMessageFormat messageFormat = (MqttMessageFormat)parser.jsonReader(msg);

                    if(messageFormat.getMode() == Mode.TEXT) {  //TEXT 모드일 경우, username 이 다른 경우만 task 생성
                        if(!messageFormat.getUsername().equals(de.getMyUsername())) {
                            Log.i("drawing", "username = " + messageFormat.getUsername() + ", text id = " + messageFormat.getTextAttr().getId() + ", mode = " + messageFormat.getMode() + ", text mode = " + messageFormat.getTextMode());
                            new TextTask().execute(messageFormat);
                        }
                    }
                    else {  // todo - background image 도 따로 task 만들어 관리
                        drawingTask = new DrawingTask();
                        drawingTask.execute(messageFormat);
                    }
                }

                //mid data 처리
                if(newTopic.equals(topic_mid)) {
                    String msg = new String(message.getPayload());
                    MqttMessageFormat messageFormat = (MqttMessageFormat)parser.jsonReader(msg);

                    // fixme nayeon
                    // 모든 사용자가 topic_mid 로 메시지 전송받음
                    // 이 시점 중간자에게는 모든 데이터 저장 완료 후
                    de.setMidEntered(false);

                    Log.i("mqtt", "isMid=" + isMid());
                    if(isMid && messageFormat.getUsername().equals(de.getMyUsername())) {
                        isMid = false;
                        Log.i("mqtt", "mid username=" + messageFormat.getUsername());
                        new MidTask().execute();
                    }
                }
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
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });
    }

    public void setToastMsg(final String message) {
        Objects.requireNonNull(drawingFragment.getActivity()).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(Objects.requireNonNull(drawingFragment.getContext()).getApplicationContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public boolean isContainsUserList(String username) {
        for(int i=0; i<userList.size(); i++) {
            if(userList.get(i).getName().equals(username))
                return true;
        }
        return false;
    }

    public void putUsersActionMap(String username, int action) {
        usersActionMap.put(username, action);
    }

    public boolean isUsersActionUp() {
        /*if(!usersActionMap.containsValue(MotionEvent.ACTION_DOWN) && !usersActionMap.containsValue(MotionEvent.ACTION_MOVE))
            return true;
        else
            return false;*/

        for (String s : usersActionMap.keySet()) {
            try {
                if (usersActionMap.get(s) != MotionEvent.ACTION_UP)
                    return false;
            } catch (NullPointerException e) {
                e.printStackTrace();
                return false;
            }
        }
        Log.i("drawing", "userActionMap = " + usersActionMap.toString());
        return true;
    }

    // fixme nayeon
    public boolean isTextInUse() {
        for(Text t : de.getTexts()) {
            if(t.getTextAttribute().getUsername() != null)
                return false;
        }
        return true;
    }

    public void doInBack() {
        th.interrupt();
        unsubscribeAllTopics();
        ((MainActivity)drawingFragment.getActivity()).setOnKeyBackPressedListener(null);
        // fixme hyeyeon-back해서 홈화면으로 가는 다이얼로그라면 exit publish 필요
        ExitMessage exitMessage = new ExitMessage(myName);
        MqttMessageFormat messageFormat = new MqttMessageFormat(exitMessage);
        publish(topic_exit, JSONParser.getInstance().jsonWrite(messageFormat));
        //
        isMid = true;
        de.removeAllDrawingData();
        usersActionMap.clear();
        Log.i("drawing", "userActionMap = " + usersActionMap.toString());
        drawingViewModel.back();
    }

    public void showTimerAlertDialog(String title, String message) {
        AlertDialog dialog = new AlertDialog.Builder(de.getDrawingFragment().getActivity())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.i("drawing", "dialog onclick");
                        doInBack();
                        //dialog.cancel();
                        dialog.dismiss();
                    }
                })
                .create(); // UI 스레드가 아닌 일반 쓰레드에서 UI 작업을 위해 new Handler()를 통해

        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            private static final int AUTO_DISMISS_MILLIS = 6000;
            @Override
            public void onShow(final DialogInterface dialog) {
                final Button defaultButton = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
                final CharSequence negativeButtonText = defaultButton.getText();
                new CountDownTimer(AUTO_DISMISS_MILLIS, 100) {
                    @Override
                    public void onTick(long millisUntilFinished) {
                        defaultButton.setText(String.format(
                                Locale.getDefault(), "%s (%d)",
                                negativeButtonText,
                                TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) + 1 //add one so it never displays zero
                        ));
                    }
                    @Override
                    public void onFinish() {
                        if (((AlertDialog) dialog).isShowing()) {
                            doInBack();
                            dialog.dismiss();
                        }
                    }
                }.start();
            }
        });
        dialog.show();
    }

    //-----setter-----

    public void setDrawingFragment(DrawingFragment drawingFragment) {
        this.drawingFragment = drawingFragment;
        this.binding = drawingFragment.getBinding();
        this.drawingView = this.binding.drawingView;
    }

    public void setProgressDialog(ProgressDialog progressDialog) {
        this.progressDialog = progressDialog;
    }

    public void setBackPressed(boolean backPressed) {  // fixme hyeyeon
        this.backPressed = backPressed;
    }

    public void setThread(Thread th) {  // fixme hyeyeon
        this.th = th;
    }
}


class DrawingTask extends AsyncTask<MqttMessageFormat, MqttMessageFormat, Void> {   // todo AsyncTask Memory Leak --> WeakReference 만들어 관리
    //private WeakReference<DrawingFragment> weakReferencedFragment;
    private String username;
    private int action;
    private DrawingComponent dComponent;
    private MQTTClient client = MQTTClient.getInstance();
    private DrawingEditor de = DrawingEditor.getInstance();
    private float myCanvasWidth = client.getDrawingView().getCanvasWidth();
    private float myCanvasHeight = client.getDrawingView().getCanvasHeight();

        /*DrawingTask(DrawingFragment drawingFragment) {
            weakReferencedFragment = new WeakReference<>(drawingFragment);
        }*/

    private void draw(MqttMessageFormat message) {
        if(action == MotionEvent.ACTION_DOWN) {
            if (de.isContainsCurrentComponents(dComponent.getId())) {
                if(de.getUsername().equals(username)) {
                    dComponent.setId(de.getComponentId());
                    Log.i("drawing", "second id (self) = " + dComponent.getId());
                } else {
                    dComponent.setId(de.componentIdCounter());
                    Log.i("drawing", "second id (other) = " + dComponent.getId());
                }
            } else if(de.getMyUsername().equals(username)){
                Log.i("drawing", "first id (self) = " + dComponent.getId());
            } else {
                de.componentIdCounter();
                Log.i("drawing", "first id (other) = " + dComponent.getId());
            }
            de.addCurrentComponents(dComponent);
            Log.i("drawing", "currentComponents.size() = " + de.getCurrentComponents().size());
        }

        client.putUsersActionMap(username, action);

        if(de.getMyUsername().equals(username)) return;

        if(de.findCurrentComponent(dComponent.getUsersComponentId()) == null) return;   //중간자가 MidTask 수행 전에 그려진 component 는 return

        dComponent.calculateRatio(myCanvasWidth, myCanvasHeight);
        switch(action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                dComponent.draw(de.getBackCanvas());
                if(dComponent.getType() == ComponentType.STROKE) {
                    Canvas canvas = new Canvas(de.getLastDrawingBitmap());
                    dComponent.draw(canvas);
                }
                return;
            case MotionEvent.ACTION_UP:
                dComponent.draw(de.getBackCanvas());

                Log.i("drawing", "dComponent: id=" + dComponent.getId() + ", endPoint=" + dComponent.getEndPoint().toString());
                try {   // todo 중간자 들어올 때, 2명 이상이 그리는 경우 테스트 (2명 이상이 동시에 그리는 경우 테스트)
                    DrawingComponent upComponent = de.findCurrentComponent(dComponent.getUsersComponentId());
                    Log.i("drawing", "upComponent: id=" + upComponent.getId() + ", endPoint=" + upComponent.getEndPoint().toString());
                    dComponent.setId(upComponent.getId());
                } catch (NullPointerException e) {
                    //dComponent.setId(dComponent.getId());
                    //dComponent.drawComponent(de.getBackCanvas());
                    e.printStackTrace();
                }

                de.removeCurrentComponents(dComponent.getId());
                de.splitPoints(dComponent, myCanvasWidth, myCanvasHeight);
                de.addDrawingComponents(dComponent);
                Log.i("drawing", "drawingComponents.size() = " + de.getDrawingComponents().size());
                de.addHistory(new DrawingItem(Mode.DRAW, dComponent));
                Log.i("drawing", "history.size()=" + de.getHistory().size() + ", id=" + dComponent.getId());

                if(dComponent.getType() == ComponentType.STROKE) {
                    Canvas canvas = new Canvas(de.getLastDrawingBitmap());
                    dComponent.draw(canvas);
                } else {
                    de.setLastDrawingBitmap(de.getDrawingBitmap().copy(de.getDrawingBitmap().getConfig(), true));
                }
                publishProgress(message);
        }
    }

    @Override
    protected Void doInBackground(MqttMessageFormat... messages) {
        MqttMessageFormat message = messages[0];
        this.username = message.getUsername();
        this.action = message.getAction();
        this.dComponent = message.getComponent();
        Mode mode = message.getMode();

        de.setMyCanvasWidth(myCanvasWidth);
        de.setMyCanvasHeight(myCanvasHeight);

        if(de.getMyUsername().equals(username) && !mode.equals(Mode.DRAW)) { return null; }

        switch(mode) {
            case DRAW:
                try{
                    Log.i("mqtt", "MESSAGE ARRIVED message: username=" + dComponent.getUsername() + ", mode=" + mode.toString() + ", id=" + dComponent.getId());
                    draw(message);
                } catch(NullPointerException e) {
                    e.printStackTrace();
                }
                return null;
            case ERASE:
                Log.i("mqtt", "MESSAGE ARRIVED message: username=" + username + ", mode=" + mode.toString() + ", id=" + message.getComponentIds().toString());
                Vector<Integer> erasedComponentIds = message.getComponentIds();
                new EraserTask(erasedComponentIds).doNotInBackground();
                publishProgress(message);
                return null;
            case SELECT:
            case GROUP:
                return null;
            case BACKGROUND_IMAGE:
                de.setBackgroundImage(de.byteArrayToBitmap(message.getBitmapByteArray()));
                publishProgress(message);
                return null;
            case CLEAR:
                Log.i("mqtt", "MESSAGE ARRIVED message: username=" + username + ", mode=" + mode.toString());
                de.clearDrawingComponents();
                publishProgress(message);
                return null;
            case CLEAR_BACKGROUND_IMAGE:
                de.setBackgroundImage(null);
                publishProgress(message);
                return null;
            case UNDO:
            case REDO:
                Log.i("mqtt", "MESSAGE ARRIVED message: username=" + username + ", mode=" + mode.toString());
                publishProgress(message);
                return null;
        }
        return null;
    }

    @Override
    protected void onProgressUpdate(MqttMessageFormat... messages) {
        MqttMessageFormat message = messages[0];
        Mode mode = message.getMode();

        switch(mode) {
            case BACKGROUND_IMAGE:
                if(de.getBackgroundImage() != null) {
                    client.getBinding().backgroundView.removeAllViews();    //fixme minj - 우선 배경 이미지는 하나만
                }
                ImageView imageView = new ImageView(client.getDrawingFragment().getContext());
                imageView.setLayoutParams(new LinearLayout.LayoutParams(client.getDrawingFragment().getSize().x, ViewGroup.LayoutParams.MATCH_PARENT));
                imageView.setImageBitmap(de.getBackgroundImage());
                client.getBinding().backgroundView.addView(imageView);
                break;
            case DRAW:
                if(action == MotionEvent.ACTION_UP) {
                    de.clearUndoArray();
                    if(de.getHistory().size() == 1)
                        client.getBinding().undoBtn.setEnabled(true);
                }
                break;
            case ERASE:
                de.clearUndoArray();
                break;
            case CLEAR:
                de.clearTexts();
                client.getBinding().redoBtn.setEnabled(false);
                client.getBinding().undoBtn.setEnabled(false);
                break;
            case CLEAR_BACKGROUND_IMAGE:
                de.clearBackgroundImage();
                break;
            case UNDO:
                if(de.getHistory().size() == 0)
                    return;
                de.addUndoArray(de.popHistory());
                if(de.getUndoArray().size() == 1)
                    client.getBinding().redoBtn.setEnabled(true);
                if(de.getHistory().size() == 0) {
                    client.getBinding().undoBtn.setEnabled(false);
                    de.clearDrawingBitmap();
                    return;
                }
                Log.i("drawing", "history.size()=" + de.getHistory().size());
                break;
            case REDO:
                if(de.getUndoArray().size() == 0)
                    return;
                de.addHistory(de.popUndoArray());
                if(de.getHistory().size() == 1)
                    client.getBinding().undoBtn.setEnabled(true);
                if(de.getUndoArray().size() == 0)
                    client.getBinding().redoBtn.setEnabled(false);
                Log.i("drawing", "history.size()=" + de.getHistory().size());
                break;
        }
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        //DrawingFragment fragment = weakReferencedFragment.get();
            /*if (fragment == null || fragment.getActivity() == null || fragment.getActivity().isFinishing()) {
                return;
            }*/

        //fragment.getBinding().drawingView.invalidate();
        client.getDrawingView().invalidate();
    }
}

class TextTask extends AsyncTask<MqttMessageFormat, MqttMessageFormat, Void> {
    private MQTTClient client = MQTTClient.getInstance();
    private DrawingEditor de = DrawingEditor.getInstance();

    @Override
    protected Void doInBackground(MqttMessageFormat... messages) {  //changeText()
        MqttMessageFormat message = messages[0];

        TextMode textMode = message.getTextMode();
        TextAttribute textAttr = message.getTextAttr();

        Text text = null;

        // 텍스트 객체가 처음 생성되는 경우, 텍스트 배열에 저장된 정보 없음
        // 그 이후에 일어나는 텍스트에 대한 모든 행위들은
        // 텍스트 배열로부터 텍스트 객체를 찾아서 작업 가능
        if(!textMode.equals(TextMode.CREATE)) {
            text = de.findTextById(textAttr.getId());
            if(text == null) return null; // todo nayeon - 중간자가 자신에게 MID 로 보낸 메시지보다, 마스터가 TEXT 로 보낸 메시지가 먼저 올 경우 (중간자가 자신의 처리를 다 했다는 플래그 필요?)
            text.setTextAttribute(textAttr); // MQTT 로 전송받은 텍스트 속성 지정해주기
        }

        switch (textMode) {
            case CREATE:
               /* Text newText = new Text(client.getDrawingFragment(), textAttr);
                newText.getTextAttribute().setTextInited(true); // 만들어진 직후 상단 중앙에 놓이도록
                de.addTexts(newText);
                de.addHistory(new DrawingItem(TextMode.CREATE, textAttr));*/
                publishProgress(message);
                return null;
            case DRAG_LOCATION:
            case DRAG_EXITED:
                text.setTextViewLocation();
                return null;
            case DROP:
                de.addHistory(new DrawingItem(TextMode.DROP, textAttr));
                text.setTextViewLocation();
                publishProgress(message);
                return null;
            case DONE:
                if(textAttr.isModified()) {
                    de.addHistory(new DrawingItem(TextMode.MODIFY, textAttr));
                    Log.i("drawing", "isModified mqtt= " + textAttr.isModified());
                }
                publishProgress(message);
                return null;
            case DRAG_ENDED:
                return null;
            case ERASE:
                de.addHistory(new DrawingItem(TextMode.ERASE, textAttr));
                publishProgress(message);
                return null;
            case MODIFY_START:
                publishProgress(message);
                return null;
        }
        return null;
    }

    @Override
    protected void onProgressUpdate(MqttMessageFormat... messages) { // changeTextOnMainThread()
        MqttMessageFormat message = messages[0];

        TextMode textMode = message.getTextMode();
        TextAttribute textAttr = message.getTextAttr();

        Text text = de.findTextById(textAttr.getId());
        switch(textMode) {
            case CREATE:
                Text newText = new Text(client.getDrawingFragment(), textAttr);
                newText.getTextAttribute().setTextInited(true); // 만들어진 직후 상단 중앙에 놓이도록
                de.addTexts(newText);
                de.addHistory(new DrawingItem(TextMode.CREATE, textAttr));
                newText.setTextViewProperties();  // fixme nayeon
                newText.addTextViewToFrameLayout();
                newText.createGestureDetecter();
                de.clearUndoArray();
                break;
            case DRAG_STARTED:
            case DRAG_LOCATION:
            case DRAG_ENDED:
                break;
            case DROP:
                de.clearUndoArray();
                break;
            case DONE: // fixme nayeon
                text.getTextView().setBackground(null); // 테두리 설정 해제
                text.setTextViewAttribute();
                if(textAttr.isModified()) { de.clearUndoArray(); }
                break;
            case ERASE:
                text.removeTextViewToFrameLayout();
                de.removeTexts(text);
                de.clearUndoArray();
                //Log.e("texts size", Integer.toString(de.getTexts().size()));
                break;
            case MODIFY_START: // fixme nayeon
                text.getTextView().setBackground(de.getTextFocusBorderDrawable()); // 수정중일 때 텍스트 테두리 설정하여 수정중인 텍스트 표시
                //text.modifyTextViewContent(textAttr.getText());
                break;
        }

        if(de.getHistory().size() == 1) {
            client.getBinding().undoBtn.setEnabled(true);
        }
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
    }
}

class MidTask extends AsyncTask<Void, Void, Void> {
    private MQTTClient client = MQTTClient.getInstance();
    private DrawingEditor de = DrawingEditor.getInstance();

    @Override
    protected Void doInBackground(Void... values) {
        if(de.getBackgroundImage() != null) {
            publishProgress();
        }
        return null;
    }

    @Override
    protected void onProgressUpdate(Void... values) {
        super.onProgressUpdate(values);
        Log.i("mqtt", "mid onProgressUpdate()");
        ImageView imageView = new ImageView(client.getDrawingFragment().getContext());
        imageView.setLayoutParams(new LinearLayout.LayoutParams(client.getDrawingFragment().getSize().x, ViewGroup.LayoutParams.MATCH_PARENT));
        imageView.setImageBitmap(de.getBackgroundImage());
        client.getBinding().backgroundView.addView(imageView);
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        Log.i("mqtt", "mid onPostExecute()");
        if (de.getHistory().size() > 0)
            client.getBinding().undoBtn.setEnabled(true);
        if (de.getUndoArray().size() > 0)
            client.getBinding().redoBtn.setEnabled(true);

        de.drawAllDrawingComponentsForMid();
        de.setLastDrawingBitmap(de.getDrawingBitmap().copy(de.getDrawingBitmap().getConfig(), true));
        de.addAllTextViewToFrameLayoutForMid();
        client.getDrawingView().invalidate();

        Log.i("mqtt", "mid progressDialog dismiss");
        client.getProgressDialog().dismiss();
    }
}
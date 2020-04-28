package com.hansung.drawingtogether.data.remote.model;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.hansung.drawingtogether.databinding.FragmentDrawingBinding;
import com.hansung.drawingtogether.view.WarpingControlView;
import com.hansung.drawingtogether.view.drawing.AudioPlayThread;
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
import com.hansung.drawingtogether.view.main.AudioMessage;
import com.hansung.drawingtogether.view.main.DeleteMessage;
import com.hansung.drawingtogether.view.main.ExitMessage;
import com.hansung.drawingtogether.view.main.JoinMessage;
import com.hansung.drawingtogether.view.main.MainActivity;
import com.hansung.drawingtogether.view.main.WarpingMessage;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

import lombok.Getter;

@Getter
public enum MQTTClient {
    INSTANCE;

    private MqttClient client;
    private String BROKER_ADDRESS;

    private FirebaseDatabase database;
    private DatabaseReference databaseReference;

    private boolean master;
    private String masterName;  // fixme hyeyeon

    private List<User> userList = new ArrayList<>(100);  // fixme hyeyeon-User객제 arrayList로 변경
    private List<AudioPlayThread> audioPlayThreadList = new ArrayList<>(100); // fixme jiyeon
    private String myName;

    private String topic;
    private String topic_join;
    private String topic_exit;
    private String topic_delete;
    private String topic_data;
    private String topic_mid;
    private String topic_audio; // fixme jiyeon
    private String topic_alive;

    private int aliveCount = 3;  // fixme hyeyeon
    // private String topic_load;

    private DrawingViewModel drawingViewModel;
    private boolean audioPlaying = false; // fixme jiyeon

    private int qos = 2;
    private JSONParser parser = JSONParser.getInstance();
    private DrawingEditor de = DrawingEditor.getInstance();
    private Logger logger = Logger.getInstance(); // fixme nayeon
    private DrawingFragment drawingFragment;
    private FragmentDrawingBinding binding;
    private DrawingTask drawingTask;
    private DrawingView drawingView;
    private boolean isMid = true;
    private DrawingComponent drawingComponent;

    private ProgressDialog progressDialog;

    private Thread th;
    private boolean exitPublish;

    private int savedFileCount = 0; // fixme nayeon
    private boolean exitCompleteFlag = false; // fixme nayeon

    public static MQTTClient getInstance() {
        return INSTANCE;
    }

    public void init(String topic, String name, boolean master, DrawingViewModel drawingViewModel, String ip, String port, String masterName) {
        connect(ip, port);

        database = FirebaseDatabase.getInstance();
        databaseReference = database.getReference();

        this.master = master;
        this.topic = topic;
        this.myName = name;
        this.masterName = masterName;  // fixme hyeyeon

        if (!isMaster()) {  // fixme hyeyeon-마스터가 토픽을 삭제하지 못하고 종료해서 마스터 없는 토픽방이 되었을 때 join한 유저들을 토픽방에서 나가게 하기 위해
            User mUser = new User(masterName, 0, MotionEvent.ACTION_UP, false);
            userList.add(mUser);
        }
        User user = new User(myName, 0, MotionEvent.ACTION_UP, false);
        userList.add(user); // 생성자에서 사용자 리스트에 내 이름 추가
        // fixme jiyeon
        AudioPlayThread audioPlayThread = new AudioPlayThread();
        audioPlayThread.setName(myName);
        audioPlayThread.setBufferUnitSize(2);
        audioPlayThreadList.add(audioPlayThread);

        topic_join = this.topic + "_join";
        topic_exit = this.topic + "_exit";
        topic_delete = this.topic + "_delete";
        topic_data = this.topic + "_data";
        topic_mid = this.topic + "_mid";
        topic_alive = this.topic + "_alive";
        topic_audio = this.topic + "_audio";

        this.drawingViewModel = drawingViewModel;
        this.drawingViewModel.setUserNum(userList.size());
        this.drawingViewModel.setUserPrint(userPrint());

        //this.usersActionMap = new HashMap<>();
        de.setMyUsername(name);
        this.exitPublish = false;
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

            MyLog.e("kkankkan", "mqtt connect success");
            MyLog.i("mqtt", "CONNECT");

            String currentClientId = client.getClientId();
            MyLog.i("mqtt", "Client ID: " + currentClientId);
        } catch (MqttException e) {
            e.printStackTrace();
            showTimerAlertDialog("브로커 연결 실패", "메인 화면으로 이동합니다");
        }
    }

    public void subscribe(String newTopic) {
        try {
            client.subscribe(newTopic, this.qos);
            MyLog.e("kkankkan", newTopic + " subscribe");
            MyLog.i("mqtt", "SUBSCRIBE topic: " + newTopic);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void publish(String newTopic, String payload) {
        try {
            MqttMessage message = new MqttMessage(payload.getBytes());
            client.publish(newTopic, payload.getBytes(), this.qos, false);
            MyLog.i("mqtt", "PUBLISH topic: " + newTopic + ", msg: " + message);
            // Log.e("mqtt payload size", Integer.toString(message.getPayload().length)); // fixme nayeon
        } catch (MqttException e) {
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
            client.unsubscribe(topic_alive);
//            client.unsubscribe(topic_audio);

            MyLog.e("kkankkan", "unsubscribe 완료");

        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void exitTask() {
        try {
            MyLog.e("kkankkan", "exitTask 시작");
            th.interrupt();
            client.unsubscribe(topic_join);
            client.unsubscribe(topic_exit);
            client.unsubscribe(topic_delete);
            client.unsubscribe(topic_data);
            client.unsubscribe(topic_mid);
            client.unsubscribe(topic_alive);
            if (drawingViewModel.isAudioFlag()) { // fixme jiyeon - 오디오 처리
                drawingViewModel.getRecThread().setFlag(false);
                for (AudioPlayThread audioPlayThread : audioPlayThreadList) {
                    if (audioPlayThread.getName().equals(myName)) continue;

                    audioPlayThread.setFlag(false);
                    synchronized (audioPlayThread.getBuffer()) {
                        audioPlayThread.getBuffer().clear();
                        MyLog.e("2yeonz", audioPlayThread.getBuffer().size() + " : clear 후" + audioPlayThread.getName());
                    }
                }
                client.unsubscribe(topic_audio);
                audioPlaying = false;
            }
            audioPlayThreadList.clear();
            MyLog.e("2yeonz", audioPlayThreadList.size() + " : 모두 clear 후");

            isMid = true;
            /*
            fixme hyeyeon - drawingViewModel의 onCleared()에서 처리하도록 변경
            de.removeAllDrawingData();
            fixme hyeyeon - drawingFragment의 onDetach()에서 처리하도록 변경
            ((MainActivity)drawingFragment.getActivity()).setOnKeyBackPressedListener(null);
            fixme hyeyeon - exitTask()만 수행하고 back()하지 않아야 하는 경우가 있어서 exitTask()를 호출할 때 따로 콜 하도록 변경
            drawingViewModel.back();
            */

            MyLog.e("kkankkan", "exitTask 완료");

            exitCompleteFlag = true; // todo nayeon


        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public String userPrint() {
        String names = "";
        for (int i = 0; i < userList.size(); i++) {
            if (userList.get(i).getName().equals(myName) && isMaster())
                names += userList.get(i).getName() + " ★ (나)\n";
            else if (userList.get(i).getName().equals(masterName))
                names += userList.get(i).getName() + " ★\n";
            else if (userList.get(i).getName().equals(myName) && !isMaster())
                names += userList.get(i).getName() + " (나)\n";
            else
                names += userList.get(i).getName() + "\n";
        }
        return names;
    }

    public String userPrintForLog() {
        String names = "";
        for (User user : userList) {
            names += "[" + user.getName() + "," + user.getCount() + "] ";
        }
        return names;
    }

    public void setCallback() {
        client.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                showTimerAlertDialog("브로커 연결 유실", "메인 화면으로 이동합니다.");

                MyLog.e("kkankkan", cause.toString());
                MyLog.i("mqtt", cause.getCause().toString());
                MyLog.i("mqtt", "CONNECTION LOST");
            }

            @Override
            public void messageArrived(String newTopic, MqttMessage message) throws Exception {
                // Log.e("kkankkan", "message Arrived");

                // [ 중간자 ]
                if (newTopic.equals(topic_join)) {
                    String msg = new String(message.getPayload());
                    MqttMessageFormat mqttMessageFormat = (MqttMessageFormat) parser.jsonReader(msg);
                    JoinMessage joinMessage = mqttMessageFormat.getJoinMessage();

                    String master = joinMessage.getMaster(); // null or not-null ( "master":"이름"/"userList":"이름1,이름2,이름3"/"loadingData":"..." )
                    String name = joinMessage.getName(); // null or not-null ( "name":"이름" )
                    List<User> users = joinMessage.getUserList(); // null or not-null

                    if (master != null) { // 메시지 형식이 "master":"이름"/"userList":"이름1,이름2,이름3"/"loadingData":"..."  일 경우
                        //binding.drawingView.setIntercept(false);
                        de.setIntercept(false); // 중간에 누가 들어왔을 때 선을 그리는 중이 아닌 다른 사용자들 터치 이벤트 부모뷰에서 가로채도록 (모두가 처리하는 부분)

                        MyLog.i("drawing", "isMid = " + isMid());

                        String to = joinMessage.getTo();
                        MyLog.i("mqtt", "to = " + to + ", myname = " + myName);
                        if (to.equals(myName)) { // 마스터가 중간자(to:" ") 에게 보낸 메시지 처리

                            /* 중간자만 처리하는 부분 */

                            if (userList.size() > 2) {  // fixme hyeyeon-중간자가 마스터로부터 데이터를 받기 전, 다른 사람의 join 메시지를 받을 수 있음
                                userList.remove(0); // 마스터
                                userList.remove(1);  // 나
                                for (User user : userList) { // 내 이름 다음의 이름들을 마스터로부터 전송받은 사용자 리스트에 추가
                                    users.add(user);
                                }
                            }
                            userList.clear(); // 중간자는 마스터에게 사용자 리스트를 받기 전에 userList.add() 했음 따라서 자신의 리스트를 지우고 마스터가 보내준 배열 저장
                            audioPlayThreadList.clear(); // fixme hyeyeon

                            audioPlayThreadList.clear();

                            for (User user : users) {  // 메시지로 전송받은 리스트 배열 세팅
                                user.setCount(0);
                                userList.add(user);
                                // fixme jiyeon
                                AudioPlayThread audioPlayThread = new AudioPlayThread();
                                audioPlayThread.setName(user.getName());
                                audioPlayThread.setBufferUnitSize(2);
                                audioPlayThreadList.add(audioPlayThread);
                            }

                            // todo hyeyeon - master로부터 데이터 받기 전에 exit한 사용자들 remove
                            //topic_data = loadingData;

                            MyLog.e("who I am", to);
                            MyLog.e("received message", "mid data -> " + msg);

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
                            MyLog.i("drawing", "component id = " + mqttMessageFormat.getMaxComponentId() + ", text id = " + mqttMessageFormat.getMaxTextId());

                            if (mqttMessageFormat.getBitmapByteArray() != null) {
                                de.setBackgroundImage(de.byteArrayToBitmap(mqttMessageFormat.getBitmapByteArray()));
                            }

                            publish(topic_mid, JSONParser.getInstance().jsonWrite(new MqttMessageFormat(myName, Mode.MID)));
                            //publish(topic_data, JSONParser.getInstance().jsonWrite(new MqttMessageFormat(myName, Mode.MID)));
                        }
                    } else {  // other or self // 메시지 형식이 "name":"이름"  일 경우
                        if (!myName.equals(name)) {  // other // 한 사람이 "name":"이름" 메시지 보냈을 경우 다른 사람들이 받아서 처리하는 부분 - '나'는 처리 안하는 부분
                            if (!isContainsUserList(name)) {
                                User user = new User(name, 0, MotionEvent.ACTION_UP, false);  // fixme hyeyeon
                                userList.add(user); // 들어온 사람의 이름을 추가
                                // fixme jiyeon
                                AudioPlayThread audioPlayThread = new AudioPlayThread();
                                audioPlayThread.setName(name);
                                audioPlayThread.setBufferUnitSize(2);
                                audioPlayThreadList.add(audioPlayThread);
                                MyLog.e("2yeonz", audioPlayThreadList.size() + " : add 후");
                                if(drawingViewModel.isAudioFlag()) {
                                    audioPlayThread.setFlag(true);
                                    new Thread(audioPlayThread).start();
                                }

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
                                if (isUsersActionUp(userList.get(userList.size() - 1).getName()) && isTextInUse()) { // fixme nayeon
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
                                    MyLog.e("kkankkan", "master data -> " + parser.jsonWrite(messageFormat));
                                    MyLog.e("kkankkan", name + " join 후 : " + userList.toString());
                                    MyLog.i("drawing", "payload size (bytes) = " + parser.jsonWrite(messageFormat).getBytes().length);
                                } else {
                                    MqttMessageFormat messageFormat = new MqttMessageFormat(new JoinMessage(userList.get(userList.size() - 1).getName()));
                                    publish(topic_join, parser.jsonWrite(messageFormat));

                                    MyLog.e("master republish name", topic_join);
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

                    if (!myName.equals(name)) {  // 다른 사용자가 exit 하는 경우

                        // todo hyeyeon - master로부터 데이터 받기 전에 exit 하는 사용자들 기억해두기
                        for (int i=0; i<userList.size(); i++) {
                            if (userList.get(i).getName().equals(name)) {
                                userList.remove(i);
                                // fixme jiyeon
                                MyLog.e("2yeonz", audioPlayThreadList.size() + " : remove 전");
                                audioPlayThreadList.get(i).setFlag(false);
                                audioPlayThreadList.get(i).getBuffer().clear();
                                audioPlayThreadList.remove(i);
                                MyLog.e("2yeonz", audioPlayThreadList.size() + " : remove 후");
                                break;
                            }
                        }
                        drawingViewModel.setUserNum(userList.size());
                        drawingViewModel.setUserPrint(userPrint());

                        MyLog.e("kkankkan", name + " exit 후 " + userPrintForLog());
                    }
                }

                if (newTopic.equals(topic_delete)) {
                    String msg = new String(message.getPayload());
                    MqttMessageFormat mqttMessageFormat = (MqttMessageFormat) parser.jsonReader(msg);
                    DeleteMessage deleteMessage = mqttMessageFormat.getDeleteMessage();

                    String name = deleteMessage.getName();

                    if (!name.equals(myName)) {  // 마스터가 topic을 delete한 경우
                        showExitAlertDialog("마스터가 토픽을 종료하였습니다");
                    }
                }

                if (newTopic.equals(topic_alive)) {
                    String msg = new String(message.getPayload());
                    MqttMessageFormat mqttMessageFormat = (MqttMessageFormat) parser.jsonReader(msg);
                    AliveMessage aliveMessage = mqttMessageFormat.getAliveMessage();
                    String name = aliveMessage.getName();

                    if (myName.equals(name)) {
                        // Log.e("kkankkan", "COUNT PLUS BEFORE" + userPrintForLog());
                        Iterator<User> iterator = userList.iterator();
                        while (iterator.hasNext()) {
                            User user = iterator.next();
                            if (!user.getName().equals(myName)) {
                                user.setCount(user.getCount() + 1);
                                if (user.getCount() == aliveCount && user.getName().equals(masterName)) {
                                    showExitAlertDialog("마스터 접속이 끊겼습니다. 토픽을 종료합니다");  // todo topic을 삭제할것인가 말것인가?
                                }
                                else if (user.getCount() == aliveCount) {
                                    iterator.remove();
                                    drawingViewModel.setUserNum(userList.size());
                                    drawingViewModel.setUserPrint(userPrint());
                                    MyLog.e("kkankkan", user.getName() + " exit 후 [userList] : " + userPrintForLog());
                                    setToastMsg("[ " + user.getName() + " ] 님 접속이 끊겼습니다");
                                }
                            }
                        }
                        // Log.e("kkankkan", "COUNT PLUS AFTER" + userPrintForLog());

                    } else {
                        for (User user : userList) {
                            if (user.getName().equals(name)) {
                                user.setCount(0);
                                break;
                            }
                        }
                    }
                }


                //drawing
                if (newTopic.equals(topic_data) && de.getDrawingBitmap() != null) {
                    String msg = new String(message.getPayload());
                    MqttMessageFormat messageFormat = (MqttMessageFormat) parser.jsonReader(msg);

                    if (messageFormat.getMode() == Mode.TEXT) {  //TEXT 모드일 경우, username 이 다른 경우만 task 생성
                        if (!messageFormat.getUsername().equals(de.getMyUsername())) {
                            MyLog.i("drawing", "username = " + messageFormat.getUsername() + ", text id = " + messageFormat.getTextAttr().getId() + ", mode = " + messageFormat.getMode() + ", text mode = " + messageFormat.getTextMode());
                            new TextTask().execute(messageFormat);
                        }
                    } else {  // todo - background image 도 따로 task 만들어 관리
                        drawingTask = new DrawingTask();
                        drawingTask.execute(messageFormat);
                    }
                }

                //mid data 처리
                if (newTopic.equals(topic_mid)) {
                    String msg = new String(message.getPayload());
                    MqttMessageFormat messageFormat = (MqttMessageFormat) parser.jsonReader(msg);

                    // fixme nayeon
                    // 모든 사용자가 topic_mid 로 메시지 전송받음
                    // 이 시점 중간자에게는 모든 데이터 저장 완료 후
                    de.setMidEntered(false);

                    MyLog.i("mqtt", "isMid=" + isMid());
                    if (isMid && messageFormat.getUsername().equals(de.getMyUsername())) {
                        isMid = false;
                        MyLog.i("mqtt", "mid username=" + messageFormat.getUsername());
                        new MidTask().execute();
                    }
                }

                // fixme jiyeon
                if (newTopic.equals(topic_audio)) {
                    if (!audioPlaying && drawingViewModel.isAudioFlag()) { // 오디오 start
                        audioPlaying = true;
                        MyLog.e("2yeonz", "audioPlaying");
                        MyLog.e("2yeonz", audioPlayThreadList.size() + " : 오디오 start");
                        for (AudioPlayThread audioPlayThread : audioPlayThreadList) {
                            if (audioPlayThread.getName().equals(myName)) continue;

                            audioPlayThread.setFlag(true);
                            new Thread(audioPlayThread).start();
                        }
                    }

                    if (audioPlaying && !drawingViewModel.isAudioFlag()) { // 오디오 stop
                        MyLog.e("2yeonz", "audioPlaying NONO");
                        MyLog.e("2yeonz", audioPlayThreadList.size() + " : 오디오 stop");
                        for (AudioPlayThread audioPlayThread : audioPlayThreadList) {
                            if (audioPlayThread.getName().equals(myName)) continue;

                            audioPlayThread.setFlag(false);
                            synchronized (audioPlayThread.getBuffer()) {
                                audioPlayThread.getBuffer().clear();
                            }
                        }
                        audioPlaying = false;
                        client.unsubscribe(topic_audio);
                        return;
                    }

                    String msg = new String(message.getPayload());
                    MqttMessageFormat mqttMessageFormat = (MqttMessageFormat) parser.jsonReader(msg);

                    AudioMessage audioMessage = mqttMessageFormat.getAudioMessage();
                    String name = audioMessage.getName();

                    if (myName.equals(name)) return;

                    byte[] data = audioMessage.getData();


                    MyLog.e("2yeonz", audioPlayThreadList.size() + " : 데이터 add");
                    for (AudioPlayThread audioPlayThread : audioPlayThreadList) {
                        if (audioPlayThread.getName().equals(name)) {
                            synchronized (audioPlayThread.getBuffer()) {
                                audioPlayThread.getBuffer().add(data);
                            }
                            break;
                        }
                    }
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });
    }

    public boolean isContainsUserList(String username) {
        for (int i = 0; i < userList.size(); i++) {
            if (userList.get(i).getName().equals(username))
                return true;
        }
        return false;
    }

    public void updateUsersAction(String username, int action) {
        for(User user : userList) {
            if(user.getName().equals(username)) {
                user.setAction(action);
            }
        }
    }

    public boolean isUsersActionUp(String username) {
        /*if(!usersActionMap.containsValue(MotionEvent.ACTION_DOWN) && !usersActionMap.containsValue(MotionEvent.ACTION_MOVE))
            return true;
        else
            return false;*/

        for (User user : userList) {
            try {
                if (!user.getName().equals(username) && user.getAction() != MotionEvent.ACTION_UP)
                    return false;
            } catch (NullPointerException e) {
                e.printStackTrace();
                return false;
            }
        }
        String str = "";
        for(User user : userList) {
            if (!user.getName().equals(username)) {
                str += "[" + user.getName() + ", " + MotionEvent.actionToString(user.getAction()) + "] ";
            }
        }
        MyLog.i("drawing", "users action = " + str);

        return true;
    }

    // fixme nayeon
    public boolean isTextInUse() {
        for (Text t : de.getTexts()) {
            if (t.getTextAttribute().getUsername() != null)
                return false;
        }
        return true;
    }

    public void doInBack() {
        th.interrupt();
        isMid = true;
        de.removeAllDrawingData();
        drawingViewModel.back();
    }

    public void showTimerAlertDialog(final String title, final String message) {
        final MainActivity mainActivity = (MainActivity) MainActivity.context; // fixme hyeyeon
        Objects.requireNonNull(mainActivity).runOnUiThread(new Runnable() {
            @Override
            public void run() { // fixme hyeyeon
                AlertDialog dialog = new AlertDialog.Builder(mainActivity) // fixme hyeyeon
                        .setTitle(title)
                        .setMessage(message)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                MyLog.d("drawing", "dialog onclick");
                                MyLog.d("button", "timer dialog ok button click"); // fixme nayeon
                                doInBack();
                                //dialog.cancel();
                                dialog.dismiss();
                            }
                        })
                        .create();

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
                MyLog.i("mqtt", "timer dialog show"); // fixme nayeon
            }
        });
    }

    public void showExitAlertDialog(final String message) {

        final MainActivity mainActivity = (MainActivity)MainActivity.context; // fixme hyeyeon
        Objects.requireNonNull(mainActivity).runOnUiThread(new Runnable() {
            @Override
            public void run() { // fixme hyeyeon
                AlertDialog dialog = new AlertDialog.Builder(mainActivity) // fixme hyeyeon
                        .setTitle("토픽 종료")
                        .setMessage(message)
                        .setCancelable(false)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                MyLog.d("button", "exit dialog ok button click"); // fixme nayeon

                                exitTask();
                                if (progressDialog.isShowing())
                                    progressDialog.dismiss();  // todo 로딩하는 동안 터치 안먹히도록 수정해야함
                                drawingViewModel.back();
                            }
                        })
                        .setNeutralButton("저장 후 종료", new DialogInterface.OnClickListener() { // fixme nayeon
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if(!exitCompleteFlag) drawingViewModel.clickSave(); // fixme nayeon 저장
                                exitTask();
                                if (progressDialog.isShowing())
                                    progressDialog.dismiss();  // todo 로딩하는 동안 터치 안먹히도록 수정해야함
                                drawingViewModel.back();
                            }
                        })
                        .create();
                dialog.show();
                MyLog.i("mqtt", "exit dialog show"); // fixme nayeon
            }
        });
    }

    public void setToastMsg(final String message) {
        final MainActivity mainActivity = (MainActivity)MainActivity.context; // fixme hyeyeon
        Objects.requireNonNull(mainActivity).runOnUiThread(new Runnable() {
            @Override
            public void run() { // fixme hyeyeon
                Toast.makeText(Objects.requireNonNull(mainActivity).getApplicationContext(), message, Toast.LENGTH_SHORT).show(); // fixme hyeyeon

            }
        });
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

    public void setThread(Thread th) {  // fixme hyeyeon
        this.th = th;
    }

    //
    private int cnt = 0;
    public void setCnt(int cnt) {
        this.cnt = cnt;
    }

    public void setDrawingComponent(DrawingComponent drawingComponent) {
        this.drawingComponent = drawingComponent;
    }

    public void setExitPublish(boolean exitPublish) {  // fixme hyeyeon[4]
        this.exitPublish = exitPublish;
    }

    public void setAliveCount(int aliveCount) {
        this.aliveCount = aliveCount;
    }

    public int getSavedFileCount() { return ++savedFileCount; }
}


class DrawingTask extends AsyncTask<MqttMessageFormat, MqttMessageFormat, Void> {   // todo AsyncTask Memory Leak --> WeakReference 만들어 관리
    //private WeakReference<DrawingFragment> weakReferencedFragment;
    private String username;
    private int action;
    private DrawingComponent dComponent;
    private Point point;
    private MQTTClient client = MQTTClient.getInstance();
    private DrawingEditor de = DrawingEditor.getInstance();
    private float myCanvasWidth = client.getDrawingView().getCanvasWidth();
    private float myCanvasHeight = client.getDrawingView().getCanvasHeight();
    private WarpingMessage warpingMessage;

        /*DrawingTask(DrawingFragment drawingFragment) {
            weakReferencedFragment = new WeakReference<>(drawingFragment);
        }*/

    private void draw(MqttMessageFormat message) {

        if(action == MotionEvent.ACTION_DOWN) {
            dComponent = message.getComponent();
            MyLog.i("sendThread", "all down " + dComponent.getUsername() + ", " + dComponent.getId() + ", " + dComponent.getPoints().size());

            if (de.isContainsCurrentComponents(dComponent.getId())) {
                if(de.getUsername().equals(username)) {
                    dComponent.setId(de.getComponentId());
                    MyLog.i("drawing", "second id (self) = " + dComponent.getId());
                } else {
                    dComponent.setId(de.componentIdCounter());
                    MyLog.i("drawing", "second id (other) = " + dComponent.getId());
                }
            } else if(de.getMyUsername().equals(username)){
                MyLog.i("drawing", "first id (self) = " + dComponent.getId());
            } else {
                de.componentIdCounter();
                MyLog.i("drawing", "first id (other) = " + dComponent.getId());
            }
            de.addCurrentComponents(dComponent);
           /* if(dComponent.getType() != ComponentType.STROKE)
                de.addCurrentShapes(dComponent);*/
            MyLog.i("drawing", "currentComponents.size() = " + de.getCurrentComponents().size());
            //Log.i("drawing", "currentShapes.size() = " + de.getCurrentShapes().size());

        }

        client.updateUsersAction(username, action);

        if(de.getMyUsername().equals(username)) return;

        if(message.getComponent() == null) {
            //dComponent = de.getCurrentComponent(message.getId());
            dComponent = de.getCurrentComponent(message.getUsersComponentId());
        }

        if (dComponent != null && de.findCurrentComponent(dComponent.getUsersComponentId()) == null) return;   //중간자가 MidTask 수행 전에 그려진 component 는 return

        dComponent.calculateRatio(myCanvasWidth, myCanvasHeight);

        client.setCnt(client.getCnt() + 1);
        MyLog.i("minj", "message arrived " + client.getCnt());

        switch(action) {
            case MotionEvent.ACTION_DOWN:
                Point p = dComponent.getPoints().get(0);
                dComponent.clearPoints();   //fixme reference
                client.getDrawingView().addPointAndDraw(dComponent, p);
                MyLog.i("sendThread", "down " + dComponent.getUsername() + ", " + dComponent.getId() + ", " + dComponent.getPoints().size());
                break;
            case MotionEvent.ACTION_MOVE:   //DrawingView addPointAndDraw(DrawingComponent, Point)
                //Log.i("sendThread", "move " + dComponent.getUsername() + ", " + dComponent.getId());
                client.getDrawingView().addPointAndDraw(dComponent, point);
                return;
            case MotionEvent.ACTION_UP:
                MyLog.i("sendThread", "up " + dComponent.getUsername() + ", " + dComponent.getId());
                client.getDrawingView().addPointAndDraw(dComponent, point);

                MyLog.i("drawing", "dComponent: id=" + dComponent.getId() + ", endPoint=" + dComponent.getEndPoint().toString());
                try {   // todo 중간자 들어올 때, 2명 이상이 그리는 경우 테스트 (2명 이상이 동시에 그리는 경우 테스트)
                    DrawingComponent upComponent = de.findCurrentComponent(dComponent.getUsersComponentId());
                    MyLog.i("drawing", "upComponent: id=" + upComponent.getId() + ", endPoint=" + upComponent.getEndPoint().toString());
                    dComponent.setId(upComponent.getId());
                } catch (NullPointerException e) {
                    //dComponent.setId(dComponent.getId());
                    //dComponent.drawComponent(de.getBackCanvas());
                    e.printStackTrace();
                }

                //de.removeCurrentComponents(dComponent.getId());
                de.removeCurrentComponents(dComponent.getUsersComponentId());
                de.removeCurrentShapes(dComponent.getUsersComponentId());
                de.splitPoints(dComponent, myCanvasWidth, myCanvasHeight);
                de.addDrawingComponents(dComponent);
                MyLog.i("drawing", "drawingComponents.size() = " + de.getDrawingComponents().size());
                de.addHistory(new DrawingItem(Mode.DRAW, dComponent));
                MyLog.i("drawing", "history.size()=" + de.getHistory().size() + ", id=" + dComponent.getId());

                /*if(dComponent.getType() == ComponentType.STROKE) {
                    Canvas canvas = new Canvas(de.getLastDrawingBitmap());
                    dComponent.draw(canvas);
                } else {
                    //de.setLastDrawingBitmap(de.getDrawingBitmap().copy(de.getDrawingBitmap().getConfig(), true));
                }*/
                publishProgress(message);
        }

        /*switch(action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:   //DrawingView addPointAndDraw(DrawingComponent, Point)
                dComponent.draw(de.getBackCanvas());
                if(dComponent.getType() == ComponentType.STROKE) {
                    Canvas canvas = new Canvas(de.getLastDrawingBitmap());
                    dComponent.draw(canvas);
                } *//*else if(!de.isDrawingShape()) {
                    dComponent.draw(de.getBackCanvas());
                    de.updateCurrentShapes(dComponent);
                } else {
                    de.updateCurrentShapes(dComponent);
                }*//*
                return;
            case MotionEvent.ACTION_UP:
                if(dComponent.getType() == ComponentType.STROKE) {
                    dComponent.draw(de.getBackCanvas());
                } *//*else if(!de.isDrawingShape()) {
                    dComponent.draw(de.getBackCanvas());
                    de.updateCurrentShapes(dComponent);
                    de.setLastDrawingBitmap(de.getDrawingBitmap().copy(de.getDrawingBitmap().getConfig(), true));
                }*//*

                MyLog.i("drawing", "dComponent: id=" + dComponent.getId() + ", endPoint=" + dComponent.getEndPoint().toString());
                try {   // todo 중간자 들어올 때, 2명 이상이 그리는 경우 테스트 (2명 이상이 동시에 그리는 경우 테스트)
                    DrawingComponent upComponent = de.findCurrentComponent(dComponent.getUsersComponentId());
                    MyLog.i("drawing", "upComponent: id=" + upComponent.getId() + ", endPoint=" + upComponent.getEndPoint().toString());
                    dComponent.setId(upComponent.getId());
                } catch (NullPointerException e) {
                    //dComponent.setId(dComponent.getId());
                    //dComponent.drawComponent(de.getBackCanvas());
                    e.printStackTrace();
                }

                de.removeCurrentComponents(dComponent.getId());
                de.removeCurrentShapes(dComponent.getUsersComponentId());
                de.splitPoints(dComponent, myCanvasWidth, myCanvasHeight);
                de.addDrawingComponents(dComponent);
                MyLog.i("drawing", "drawingComponents.size() = " + de.getDrawingComponents().size());
                de.addHistory(new DrawingItem(Mode.DRAW, dComponent));
                MyLog.i("drawing", "history.size()=" + de.getHistory().size() + ", id=" + dComponent.getId());

                if(dComponent.getType() == ComponentType.STROKE) {
                    Canvas canvas = new Canvas(de.getLastDrawingBitmap());
                    dComponent.draw(canvas);
                } else {
                    //de.setLastDrawingBitmap(de.getDrawingBitmap().copy(de.getDrawingBitmap().getConfig(), true));
                }
                publishProgress(message);
        }*/
    }

    @Override
    protected Void doInBackground(MqttMessageFormat... messages) {
        MqttMessageFormat message = messages[0];
        this.username = message.getUsername();
        this.action = message.getAction();
        //this.dComponent = message.getComponent();
        this.point = message.getPoint();
        Mode mode = message.getMode();

        de.setMyCanvasWidth(myCanvasWidth);
        de.setMyCanvasHeight(myCanvasHeight);

        /*if(message.getComponent() != null) {
            client.setDrawingComponent(message.getComponent());
            this.dComponent = client.getDrawingComponent();
            Log.i("sendThread", "action down " + dComponent.getUsername() + " " + dComponent.getId());
        } else {
            this.dComponent = client.getDrawingComponent();
            Log.i("sendThread", "action move, up " + dComponent.getUsername() + " " + dComponent.getId());
        }*/

        // fixme jiyeon - 자기 자신의 background image도 콜백에서 처리
        if(de.getMyUsername().equals(username) && !mode.equals(Mode.DRAW) && !mode.equals(Mode.BACKGROUND_IMAGE)) { return null; }
//            if(de.getMyUsername().equals(username) && !mode.equals(Mode.DRAW)) { return null; }

        switch(mode) {
            case DRAW:
                try{
                    //Log.i("mqtt", "MESSAGE ARRIVED message: username=" + dComponent.getUsername() + ", mode=" + mode.toString() + ", id=" + dComponent.getId());
                    draw(message);
                    //client.setDrawingComponent(dComponent); //갱신된 dComponent로 client의 component 바꿔줌 (id, ratio 등)
                } catch(NullPointerException e) {
                    e.printStackTrace();
                }
                return null;
            case ERASE:
                MyLog.i("mqtt", "MESSAGE ARRIVED message: username=" + username + ", mode=" + mode.toString() + ", id=" + message.getComponentIds().toString());
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
                MyLog.i("mqtt", "MESSAGE ARRIVED message: username=" + username + ", mode=" + mode.toString());
                de.clearDrawingComponents();
                publishProgress(message);
                return null;
            case CLEAR_BACKGROUND_IMAGE:
                de.setBackgroundImage(null);
                publishProgress(message);
                return null;
            case UNDO:
            case REDO:
                MyLog.i("mqtt", "MESSAGE ARRIVED message: username=" + username + ", mode=" + mode.toString());
                publishProgress(message);
                return null;
            case WARP:
                this.warpingMessage = message.getWarpingMessage();
                publishProgress(message);
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
                WarpingControlView imageView = new WarpingControlView(client.getDrawingFragment().getContext());
                imageView.setLayoutParams(new LinearLayout.LayoutParams(client.getDrawingFragment().getSize().x, ViewGroup.LayoutParams.MATCH_PARENT));
                imageView.setImage(de.getBackgroundImage());
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
                MyLog.i("drawing", "history.size()=" + de.getHistory().size());
                break;
            case REDO:
                if(de.getUndoArray().size() == 0)
                    return;
                de.addHistory(de.popUndoArray());
                if(de.getHistory().size() == 1)
                    client.getBinding().undoBtn.setEnabled(true);
                if(de.getUndoArray().size() == 0)
                    client.getBinding().redoBtn.setEnabled(false);
                MyLog.i("drawing", "history.size()=" + de.getHistory().size());
                break;
            case WARP:
                MotionEvent event = warpingMessage.getEvent();
                ((WarpingControlView)client.getBinding().backgroundView.getChildAt(0)).dispatchEvent(event);
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
            case MODIFY_START: // fixme nayeon
            case START_COLOR_CHANGE:
            case FINISH_COLOR_CHANGE:
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
                    MyLog.i("drawing", "isModified mqtt= " + textAttr.isModified());
                }
                publishProgress(message);
                return null;
            case DRAG_ENDED:
                return null;
            case ERASE:
                de.addHistory(new DrawingItem(TextMode.ERASE, textAttr));
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
                newText.createGestureDetector();
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

            // fixme nayeon
            case START_COLOR_CHANGE:
                text.getTextView().setBackground(de.getTextHighlightBorderDrawble()); // 텍스트 색상 편집 시작 테두리 설정
                break;
            case FINISH_COLOR_CHANGE:
                text.getTextView().setBackground(null); // 텍스트 색상 편집 완료 후 테두리 제거
                text.setTextViewAttribute(); // 변경된 색상 적용
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
        MyLog.i("mqtt", "mid onProgressUpdate()");
        WarpingControlView imageView = new WarpingControlView(client.getDrawingFragment().getContext());
        imageView.setLayoutParams(new LinearLayout.LayoutParams(client.getDrawingFragment().getSize().x, ViewGroup.LayoutParams.MATCH_PARENT));
        imageView.setImage(de.getBackgroundImage());
        client.getBinding().backgroundView.addView(imageView);
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        MyLog.i("mqtt", "mid onPostExecute()");
        if (de.getHistory().size() > 0)
            client.getBinding().undoBtn.setEnabled(true);
        if (de.getUndoArray().size() > 0)
            client.getBinding().redoBtn.setEnabled(true);

        de.drawAllDrawingComponentsForMid();
        de.setLastDrawingBitmap(de.getDrawingBitmap().copy(de.getDrawingBitmap().getConfig(), true));
        de.addAllTextViewToFrameLayoutForMid();
        client.getDrawingView().invalidate();

        MyLog.i("mqtt", "mid progressDialog dismiss");
        client.getProgressDialog().dismiss();
    }
}
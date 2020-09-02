package com.hansung.drawingtogether.data.remote.model;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.Point;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.github.twocoffeesoneteam.glidetovectoryou.GlideToVectorYou;
import com.hansung.drawingtogether.databinding.FragmentDrawingBinding;
import com.hansung.drawingtogether.monitoring.ComponentCount;
import com.hansung.drawingtogether.view.WarpingControlView;
import com.hansung.drawingtogether.view.drawing.AudioPlayThread;
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
import com.hansung.drawingtogether.view.main.AutoDrawMessage;
import com.hansung.drawingtogether.view.main.CloseMessage;
import com.hansung.drawingtogether.view.main.ExitMessage;
import com.hansung.drawingtogether.view.main.JoinAckMessage;
import com.hansung.drawingtogether.view.main.JoinMessage;
import com.hansung.drawingtogether.view.main.MQTTSettingData;
import com.hansung.drawingtogether.view.main.MainActivity;
import com.hansung.drawingtogether.view.main.WarpingMessage;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.ArrayList;
import java.util.Arrays;
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
    private MqttClient client2;
    private String BROKER_ADDRESS;

    private boolean master;
    private String masterName;

    private List<User> userList = new ArrayList<>(100);
    private List<AudioPlayThread> audioPlayThreadList = new ArrayList<>(100); // 다른 참가자들의 PlayThread를 저장하는 리스
    private String myName;

    private float myCanvasWidth;
    private float myCanvasHeight;

    private String topic;
    private String topic_join;
    private String topic_exit;
    private String topic_close;
    private String topic_data;
    private String topic_mid;
    private String topic_audio;
    private String topic_image;
    private String topic_alive;
    private String topic_monitoring;

    private int aliveCount = 5;

    private DrawingViewModel drawingViewModel;

    private int qos = 2;
    private JSONParser parser = JSONParser.getInstance();
    private DrawingEditor de = DrawingEditor.getInstance();
    private Logger logger = Logger.getInstance();
    private DrawingFragment drawingFragment;
    private FragmentDrawingBinding binding;
    private DrawingView drawingView;
    private boolean isMid = true;
    private int totalMoveX = 0;
    private int totalMoveY = 0;

    private ProgressDialog progressDialog;

    private Thread th;
    private MQTTSettingData data = MQTTSettingData.getInstance();

    private int savedFileCount = 0;
    private boolean exitCompleteFlag = false;

    private ComponentCount componentCount;
    private Thread monitoringThread;

    private int networkTry = 0;

    private MqttConnectOptions connOpts;

    public static MQTTClient getInstance() {
        return INSTANCE;
    }

    public void init(String topic, String name, boolean master, DrawingViewModel drawingViewModel,
                     String ip, String port, String masterName) {
        connect(ip, port, topic, name);

        this.master = master;
        this.topic = topic;
        this.myName = name;
        this.masterName = masterName;

        userList.clear();
        audioPlayThreadList.clear();

        if (!isMaster()) {
            User mUser = new User(masterName, 0, MotionEvent.ACTION_UP, false);
            userList.add(mUser);
            // 마스터의 PlayThread 생성
            AudioPlayThread audioPlayThread = new AudioPlayThread();
            audioPlayThread.setUserName(masterName);
            audioPlayThread.setBufferUnitSize(2);
            audioPlayThread.start();
            audioPlayThreadList.add(audioPlayThread);
            MyLog.i("audio", masterName + " 추가 후 : " + audioPlayThreadList.size());
        }

        User user = new User(myName, 0, MotionEvent.ACTION_UP, false);
        userList.add(user); // 생성자에서 사용자 리스트에 내 이름 추가

        topic_join = this.topic + "_join";
        topic_exit = this.topic + "_exit";
        topic_close = this.topic + "_close";
        topic_data = this.topic + "_data";
        topic_mid = this.topic + "_mid";
        topic_alive = this.topic + "_alive";
        topic_audio = this.topic + "_audio";
        topic_image = this.topic + "_image";

        topic_monitoring = "monitoring";

        this.drawingViewModel = drawingViewModel;
        this.drawingViewModel.setUserNum(userList.size());
        this.drawingViewModel.setUserPrint(userPrint());
        MyLog.i("canvas size check", userPrintForLog());

        //this.usersActionMap = new HashMap<>();
        de.setMyUsername(name);

        networkTry = 0;
        isMid = true;
    }

    public void connect(String ip, String port, String topic, String name) { // client id = "*name_topic_android"
        try {
            BROKER_ADDRESS = "tcp://" + ip + ":" + port;

            // 드로잉 데이터를 전송하는 클라이언트일 경우
            // 브로커 로그에 표시되는 client id를 지정
            client = new MqttClient(BROKER_ADDRESS, ("*" + name + "_" + topic + "_Android"), new MemoryPersistence());
            client2 = new MqttClient(BROKER_ADDRESS, MqttClient.generateClientId(), new MemoryPersistence());

            connOpts = new MqttConnectOptions();

            connOpts.setCleanSession(true);
            connOpts.setKeepAliveInterval(1000);
            connOpts.setMaxInflight(5000);   //?

            // fixme - 클라이언트 연결이 끊겼을 때 자동으로 재연결 되도록 설정
            connOpts.setAutomaticReconnect(true);

            client.connect(connOpts);
            client2.connect(connOpts);
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
            MyLog.i("mqtt", "SUBSCRIBE topic: " + newTopic);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void publish(String newTopic, String payload) {
        try {
            client.publish(newTopic, new MqttMessage(payload.getBytes()));
//            networkTry = 0;
        } catch (MqttException e) {
            e.printStackTrace();
            showTimerAlertDialog("메시지 전송 실패", "메인 화면으로 이동합니다");

//            setToastMsg("네트워크 상태 불안정");
//            networkTry++;
//            if (networkTry == 5) {
//                showNetworkAlert("네트워크 상태 불안정", "네트워크 상태를 확인해 주세요.");
//            }
        }
    }

    public void publish(String newTopic, byte[] payload) {
        try {
            client.publish(newTopic, new MqttMessage(payload));
        } catch (MqttException e) {
            // 마이크 On 상태에서 Exit Or Close 할 경우 Record Thread Interrupt 처
            if (drawingViewModel.getRecThread().isAlive()) {
                drawingViewModel.getRecThread().interrupt();
            }
        }
    }

    public void subscribeAllTopics() {
        subscribe(topic_join);
        subscribe(topic_exit);
        subscribe(topic_close);
        subscribe(topic_data);
        subscribe(topic_mid);
        subscribe(topic_image);
        subscribe(topic_alive);
    }

    public void exitTask() {
        try {
            MyLog.i("exittask", "exitTask 시작");

            th.interrupt();

            if(isMaster())
                monitoringThread.interrupt();


            if (isMaster()) {
                CloseMessage closeMessage = new CloseMessage(myName);
                MqttMessageFormat messageFormat = new MqttMessageFormat(closeMessage);
                publish(topic_close, JSONParser.getInstance().jsonWrite(messageFormat));
                MyLog.i("exittask", "master close pub");
            } else {
                ExitMessage exitMessage = new ExitMessage(myName);
                MqttMessageFormat messageFormat = new MqttMessageFormat(exitMessage);
                publish(topic_exit, JSONParser.getInstance().jsonWrite(messageFormat));
                MyLog.i("exittask", "exit pub");
            }

            client.unsubscribe(topic_join);
            client.unsubscribe(topic_exit);
            client.unsubscribe(topic_close);
            client.unsubscribe(topic_data);
            client.unsubscribe(topic_image);
            client.unsubscribe(topic_mid);
            client.unsubscribe(topic_alive);

            isMid = true;

            MyLog.i("exittask", "exitTask 완료");

            exitCompleteFlag = true;

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
            names += "[" + user.getName() + "," + user.getCount() + ", " + user.getDrawnCanvasWidth() + "," + user.getDrawnCanvasHeight() + "]";
        }
        return names;
    }

    public void setDrawnCanvasSize(String username, float drawnCanvasWidth, float drawnCanvasHeight) {
        for(User user: userList) {
            if (user.getName().equals(username)) {
                user.setDrawnCanvasSize(drawnCanvasWidth, drawnCanvasHeight);
                break;
            }
        }
    }

    public void setCallback() {
        client.setCallback(new MqttCallbackExtended() {

            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                if (reconnect) {
                    MyLog.i("modified mqtt", "RECONNECT");
                    subscribeAllTopics();

                    if(isMaster())
                        monitoringThread.notify();

                    // 스피커 On(오디오 sub 중)이었다면 다시 sub
                    if (drawingViewModel.isSpeakerFlag())
                        subscribe(topic_audio);
                } else {
                    MyLog.i("modified mqtt", "CONNECT");
                }
            }

            @Override
            public void connectionLost(Throwable cause) {
                MyLog.i("modified mqtt", "CONNECTION LOST : " + cause.getCause().toString());
                try {
                    if(isMaster())
                        monitoringThread.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void messageArrived(String newTopic, MqttMessage message) throws Exception {

                // [ 중간자 ]
                if (newTopic.equals(topic_join)) {

                    String msg = new String(message.getPayload());
                    MqttMessageFormat mqttMessageFormat = (MqttMessageFormat) parser.jsonReader(msg);

                    JoinMessage joinMessage = mqttMessageFormat.getJoinMessage();
                    JoinAckMessage joinAckMessage = mqttMessageFormat.getJoinAckMessage();

                    if (joinMessage != null) {  // 중간 참여자가 보낸 메시지
                        MyLog.i("joinMessage", "joinMessage arrived");

                        float drawnCanvasWidth = joinMessage.getDrawnCanvasWidth();
                        float drawnCanvasHeight = joinMessage.getDrawnCanvasHeight();

                        String name = joinMessage.getName();

                        if (!name.equals(myName)) {
                            if (!isContainsUserList(name)) {  // master, 기존 참여자 수행
                                User user = new User(name, 0, MotionEvent.ACTION_UP, false, drawnCanvasWidth, drawnCanvasHeight);
                                userList.add(user); // 들어온 사람의 이름을 추가

                                if (!master) {
                                    JoinAckMessage joinAckMsg = new JoinAckMessage(myName, name, myCanvasWidth, myCanvasHeight);
                                    MqttMessageFormat msgFormat = new MqttMessageFormat(joinAckMsg);
                                    client2.publish(topic_join, new MqttMessage(parser.jsonWrite(msgFormat).getBytes()));
                                }

                                // 중간 참여자의 Play Thread 생성
                                AudioPlayThread audioPlayThread = new AudioPlayThread();
                                audioPlayThread.setUserName(name);
                                audioPlayThread.setBufferUnitSize(2);
                                audioPlayThread.start();
                                audioPlayThreadList.add(audioPlayThread);
                                MyLog.e("audio", name + " 추가 후 : " + audioPlayThreadList.size());

                                // 다른 사용자가 들어왔다는 메시지를 받았을 경우
                                // 텍스트 비활성화를 위해 플래그 설정
                                de.setMidEntered(true);

                                //if (de.getCurrentMode() == Mode.DRAW) {  // current mode 가 DRAW 이면, 그리기 중이던 component 까지만 그리고 touch intercept   // todo 다른 모드에서도 intercept 하도록 추가
                                    de.setIntercept(true);
                                    if(!getDrawingView().isMovable()) {
                                        getDrawingView().setIntercept(true);
                                    }
                                //}

                                setToastMsg("[ " + name + " ] 님이 접속하셨습니다");

                                drawingViewModel.setUserNum(userList.size());
                                drawingViewModel.setUserPrint(userPrint());
                                MyLog.i("canvas size check", userPrintForLog());
                            }
                            if (master) {  // master 수행
                                if (isUsersActionUp(name) && !isTextInUse()) {
                                    MyLog.i("text", "check text in use");
                                    JoinAckMessage joinAckMsgMaster = new JoinAckMessage(myName, name, myCanvasWidth, myCanvasHeight);

                                    // fixme - 드로잉 데이터는 MqttMessageFormat, 이미지 데이터는 binary로 publish
                                    MqttMessageFormat messageFormat = new MqttMessageFormat(joinAckMsgMaster, de.getDrawingComponents(), de.getTexts(), de.getHistory(), de.getUndoArray(), de.getRemovedComponentId(), de.getMaxComponentId(), de.getMaxTextId());
                                    String json = parser.jsonWrite(messageFormat);

                                    client2.publish(topic_join, new MqttMessage(json.getBytes()));

                                    if (de.getBackgroundImage() != null) {
                                        // 중간자에게 와핑된 이미지를 보내도록 변경
                                        byte[] backgroundImage = de.bitmapToByteArray(((WarpingControlView)MQTTClient.getInstance().getBinding().backgroundView).getImage());
                                        client2.publish(topic_image, new MqttMessage(backgroundImage));
                                    }

                                    for (int i = 0; i < de.getAutoDrawImageList().size(); i++) {
                                        String url = de.getAutoDrawImageList().get(i);
                                        ImageView view = de.getAutoDrawImageViewList().get(i);
                                        AutoDrawMessage autoDrawMessage = new AutoDrawMessage(data.getName(), url, view.getX(), view.getY());
                                        MqttMessageFormat messageFormat2 = new MqttMessageFormat(de.getMyUsername(), de.getCurrentMode(), de.getCurrentType(), autoDrawMessage);
                                        String json2 = parser.jsonWrite(messageFormat2);
                                        client2.publish(topic_data, new MqttMessage(json2.getBytes()));
                                    }

                                    setToastMsg("[ " + name + " ] 님에게 데이터 전송을 완료했습니다");

                                } else {
                                    MqttMessageFormat messageFormat = new MqttMessageFormat(new JoinMessage(name, drawnCanvasWidth, drawnCanvasHeight));
                                    client2.publish(topic_join, new MqttMessage(parser.jsonWrite(messageFormat).getBytes()));
                                    MyLog.i("master republish name", topic_join);
                                }
                            }
                        }
                        else {
                            setDrawnCanvasSize(name, drawnCanvasWidth, drawnCanvasHeight);
                            myCanvasWidth = drawnCanvasWidth;
                            myCanvasHeight = drawnCanvasHeight;
                        }

                    }
                    else if (joinAckMessage != null) {  // master or 기존 참여자가 보낸 메시지, 중간 참여자 수행
                        MyLog.i("joinAckMessage", "joinAckMessage arrived");
                        float drawnCanvasWidth = joinAckMessage.getDrawnCanvasWidth();
                        float drawnCanvasHeight = joinAckMessage.getDrawnCanvasHeight();

                        String name = joinAckMessage.getName();
                        String target = joinAckMessage.getTarget();

                        if (target.equals(myName)) {
                            if (name.equals(masterName)) {  // master가 보낸 메시지
                                // 드로잉에 필요한 구조체들 저장하는 부분
                                // 필요한 배열 리스트들과 배경 이미지 세팅
                                de.setDrawingComponents(mqttMessageFormat.getDrawingComponents());
                                de.setHistory(mqttMessageFormat.getHistory());
                                de.setUndoArray(mqttMessageFormat.getUndoArray());
                                de.setRemovedComponentId(mqttMessageFormat.getRemovedComponentId());
                                setDrawnCanvasSize(name, drawnCanvasWidth, drawnCanvasHeight);

                                de.setTexts(mqttMessageFormat.getTexts());
                                if (mqttMessageFormat.getBitmapByteArray() != null) {
                                    de.byteArrayToBitmap(mqttMessageFormat.getBitmapByteArray());
                                }

                                // 아이디 세팅
                                de.setMaxComponentId(mqttMessageFormat.getMaxComponentId());
                                // de.setTextId(mqttMessageFormat.getMaxTextId()); // fixme - 텍스트 아이디는 "사용자이름-textIdCount" 이므로 textIdCount 가 같아도 고유
                                MyLog.i("drawing", "component id = " + mqttMessageFormat.getMaxComponentId() + ", text id = " + mqttMessageFormat.getMaxTextId());

                                client2.publish(topic_mid, new MqttMessage(JSONParser.getInstance().jsonWrite(new MqttMessageFormat(myName, Mode.MID)).getBytes()));
                            }
                            else if (!isContainsUserList(name)) {  // 기존 참여자가 보낸 메시지
                                User user = new User(name, 0, MotionEvent.ACTION_UP, false, drawnCanvasWidth, drawnCanvasHeight);
                                userList.add(user); // 들어온 사람의 이름을 추가
                                setDrawnCanvasSize(name, drawnCanvasWidth, drawnCanvasHeight);

                                // 기존 참여자의 Play Thread 생성
                                AudioPlayThread audioPlayThread = new AudioPlayThread();
                                audioPlayThread.setUserName(name);
                                audioPlayThread.setBufferUnitSize(2);
                                audioPlayThread.start();
                                audioPlayThreadList.add(audioPlayThread);
                                MyLog.i("audio", name + " 추가 후 : " + audioPlayThreadList.size());

                                drawingViewModel.setUserNum(userList.size());
                                drawingViewModel.setUserPrint(userPrint());
                                MyLog.i("canvas size check", userPrintForLog());
                            }
                        }
                    }
                }

                if (newTopic.equals(topic_exit)) {
                    String msg = new String(message.getPayload());
                    MqttMessageFormat mqttMessageFormat = (MqttMessageFormat) parser.jsonReader(msg);
                    ExitMessage exitMessage = mqttMessageFormat.getExitMessage();
                    String name = exitMessage.getName();

                    if (!myName.equals(name)) {  // 다른 사용자가 exit 하는 경우

                        for (int i=0; i<userList.size(); i++) {
                            if (userList.get(i).getName().equals(name)) {
                                userList.remove(i);

                                drawingViewModel.setUserNum(userList.size());
                                drawingViewModel.setUserPrint(userPrint());

                                setToastMsg("[ " + name + " ] 님이 나가셨습니다");
                                MyLog.e("kkankkan", name + " exit 후 " + userPrintForLog());

                                break;
                            }
                        }

                        // Play Thread Wait -> AudioTrack Stop, Release -> Play Thread Interrupt -> Remove
                        for (int i=0; i<audioPlayThreadList.size(); i++) {
                            if (audioPlayThreadList.get(i).getUserName().equals(name)) {
                                audioPlayThreadList.get(i).setFlag(false);
                                audioPlayThreadList.get(i).getBuffer().clear();
                                audioPlayThreadList.get(i).stopPlaying();
                                audioPlayThreadList.get(i).interrupt();
                                MyLog.i("audio", name + " remove 전 : " + audioPlayThreadList.size());
                                audioPlayThreadList.remove(i);
                                MyLog.i("audio", name + " remove 후 : " + audioPlayThreadList.size());
                            }
                        }
                    }
                }

                if (newTopic.equals(topic_close)) {
                    String msg = new String(message.getPayload());
                    MqttMessageFormat mqttMessageFormat = (MqttMessageFormat) parser.jsonReader(msg);
                    CloseMessage closeMessage = mqttMessageFormat.getCloseMessage();

                    String name = closeMessage.getName();

                    if (!name.equals(myName)) {
                        showExitAlertDialog("마스터가 회의방을 종료하였습니다");
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

                            MyLog.i("alive", userPrintForLog());

                            if (!user.getName().equals(myName)) {
                                user.setCount(user.getCount() + 1);
                                if (user.getCount() == aliveCount && user.getName().equals(masterName)) {
                                    showExitAlertDialog("마스터 접속이 끊겼습니다. 회의방을 종료합니다.");
                                }
                                else if (user.getCount() == aliveCount) {
                                    iterator.remove();
                                    drawingViewModel.setUserNum(userList.size());
                                    drawingViewModel.setUserPrint(userPrint());

                                    MyLog.i("alive", user.getName() + " exit 후 [userList] : " + userPrintForLog());
                                    setToastMsg("[ " + user.getName() + " ] 님 접속이 끊겼습니다");
                                }
                            }
                        }
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
                    //MyLog.i("drawMsg", msg);
                    MqttMessageFormat messageFormat = (MqttMessageFormat) parser.jsonReader(msg);

                    // 중간 참여자가 입장했을 때 처리
                    if(de.isMidEntered() && (messageFormat.getAction() != null && messageFormat.getAction() != MotionEvent.ACTION_UP)) { // fixme - getAction == null
                        //MyLog.i("drawing", "mid entering");
                        if(/*getDrawingView().isIntercept() || */(de.isIntercept() && (messageFormat.getAction() != null && messageFormat.getAction() == MotionEvent.ACTION_DOWN)) || (de.getCurrentComponent(messageFormat.getUsersComponentId()) == null))
                            return;
                    }

                    MyLog.i("> monitoring", "before check component count");
                    MyLog.i("> monitoring", "mode = " + messageFormat.getMode() + ", type = " + messageFormat.getType()
                            + ", text mode = " + messageFormat.getTextMode());

                    // 컴포넌트 개수 저장
                    if( (messageFormat.getAction() != null && messageFormat.getAction() == MotionEvent.ACTION_DOWN)
                            || messageFormat.getMode() == Mode.TEXT || messageFormat.getMode() == Mode.ERASE) {
                        MyLog.i("< monitoring", "mode = " + messageFormat.getMode() + ", type = " + messageFormat.getType()
                        + ", text mode = " + messageFormat.getTextMode());
                        checkComponentCount(messageFormat.getMode(), messageFormat.getType(), messageFormat.getTextMode());
                    }

                    MyLog.i("< monitoring", "after check component count");

                    // 컴포넌트 처리
                    if (messageFormat.getMode() == Mode.TEXT) {  //TEXT 모드일 경우, username 이 다른 경우만 task 생성
                        if (!messageFormat.getUsername().equals(de.getMyUsername())) {
                            MyLog.i("drawing", "username = " + messageFormat.getUsername() + ", text id = " + messageFormat.getTextAttr().getId() + ", mode = " + messageFormat.getMode() + ", text mode = " + messageFormat.getTextMode());
                            new TextTask().execute(messageFormat);
                        }
                    } else {
                        new DrawingTask().execute(messageFormat);
                    }
                }

                //mid data 처리
                if (newTopic.equals(topic_mid)) {
                    String msg = new String(message.getPayload());
                    MqttMessageFormat messageFormat = (MqttMessageFormat) parser.jsonReader(msg);

                    MyLog.i("mqtt", "isMid=" + isMid() + ", " + de.getMyUsername());
                    if (isMid && messageFormat.getUsername().equals(de.getMyUsername())) {
                        isMid = false;
                        MyLog.i("mqtt", "mid username=" + messageFormat.getUsername());
                        new MidTask().execute();
                    }

                    de.setIntercept(false);
                    MyLog.i("mqtt", "set intercept false");

                    // 모든 사용자가 topic_mid 로 메시지 전송받음
                    // 이 시점 중간자에게는 모든 데이터 저장 완료 후
                    de.setMidEntered(false);
                }

                if (newTopic.equals(topic_audio)) {
                    byte[] audioMessage = message.getPayload();

                    byte[] nameByte = Arrays.copyOfRange(audioMessage, 5000, audioMessage.length);
                    String name = new String(nameByte);

                    if (myName.equals(name)) return; // 자신의 데이터는 받지 않음

                    byte[] audioData = Arrays.copyOfRange(audioMessage, 0, audioMessage.length - nameByte.length);

                    // Play Thread의 userName 검사하여 데이터 추가
                    for (AudioPlayThread audioPlayThread : audioPlayThreadList) {
                        if (audioPlayThread.getUserName().equals(name)) {
                            audioPlayThread.getBuffer().add(audioData);
                            break;
                        }
                    }
                }

                if (newTopic.equals(topic_image)) {
                    byte[] imageData = message.getPayload();
                    de.setBackgroundImage(de.byteArrayToBitmap(imageData));

                    // fixme - WarpingControlView(backgroundview) 고정, 비트맵만 갈아끼우도록 변경 (깜빡임 없애기 위해)
                    binding.backgroundView.setCancel(true);
                    binding.backgroundView.setImage(de.getBackgroundImage());
                    MyLog.i("image", "set image");

                    if(isMaster()) { // fixme - 전송한 이미지 개수 카운팅 (모니터링)
                        componentCount.increaseImage();
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
                if(action == MotionEvent.ACTION_UP)
                    MyLog.i("intercept", username + " UP");
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

    public boolean isTextInUse() {
        MyLog.i("text", "inTextInUse func");

        for (Text t : de.getTexts()) {
            if (t.getTextAttribute().getUsername() != null) {
                return true;
            }
        }
        return false;
    }

    public void checkComponentCount(Mode mode, ComponentType type, TextMode textMode) {
        Log.e("monitoring", "execute check component count func.");


        // 마스터만 컴포넌트 개수 카운팅
        if(!isMaster()) {
            Log.e("monitoring", "check component count func. i'am not master.");
            return;
        }

        if(mode == Mode.TEXT && textMode == TextMode.CREATE) {
            Log.e("monitoring", "check component count func. text count increase.");

            componentCount.increaseText();
            return;
        }

        if(mode != Mode.DRAW) {
            Log.e("monitoring", "check component count func. mode is not DRAW");
            return;
        }
        Log.e("monitoring", "check component count func. mode is DRAW");


        switch (type) {
            case STROKE:
                componentCount.increaseStroke();
                break;
            case RECT:
                componentCount.increaseRect();
                break;
            case OVAL:
                componentCount.increaseOval();
                break;
        }
    }

    public void doInBack() {

        th.interrupt();

        if(isMaster())
            monitoringThread.interrupt();


        isMid = true;
        de.removeAllDrawingData();
        drawingViewModel.back();
    }

    public void showNetworkAlert(final String title, final String message) {

        final MainActivity mainActivity = (MainActivity)MainActivity.context;
        Objects.requireNonNull(mainActivity).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog dialog = new AlertDialog.Builder(mainActivity)
                        .setTitle(title)
                        .setMessage(message)
                        .setCancelable(false)
                        .setPositiveButton("재시도", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                networkTry = 0;
                            }
                        })
                        .setNeutralButton("종료하기", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ((MainActivity) MainActivity.context).finish();
                                android.os.Process.killProcess(android.os.Process.myPid());
                                System.exit(10);
                                return;
                            }
                        })
                        .create();
                dialog.show();
            }
        });
    }

    public void showTimerAlertDialog(final String title, final String message) {
        final MainActivity mainActivity = (MainActivity) MainActivity.context;
        Objects.requireNonNull(mainActivity).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog dialog = new AlertDialog.Builder(mainActivity)
                        .setTitle(title)
                        .setMessage(message)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                MyLog.d("drawing", "dialog onclick");
                                MyLog.d("button", "timer dialog ok button click");
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
                MyLog.i("mqtt", "timer dialog show");
            }
        });
    }

    public void showExitAlertDialog(final String message) {

        final MainActivity mainActivity = (MainActivity)MainActivity.context;
        Objects.requireNonNull(mainActivity).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog dialog = new AlertDialog.Builder(mainActivity)
                        .setTitle("회의방 종료")
                        .setMessage(message)
                        .setCancelable(false)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                MyLog.d("button", "exit dialog ok button click");
                                if (client.isConnected()) {
                                    exitTask();
                                }
                                if (progressDialog.isShowing())
                                    progressDialog.dismiss();  // todo 로딩하는 동안 터치 안먹히도록 수정해야함
                                drawingViewModel.back();
                            }
                        })
                        .setNeutralButton("저장 후 종료", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if(!exitCompleteFlag) drawingViewModel.clickSave();
                                if (client.isConnected()) {
                                    exitTask();
                                }
                                if (progressDialog.isShowing())
                                    progressDialog.dismiss(); // todo 로딩하는 동안 터치 안먹히도록 수정해야함
                                drawingViewModel.back();
                            }
                        })
                        .create();
                dialog.show();
                MyLog.i("mqtt", "exit dialog show");
            }
        });
    }

    public void setToastMsg(final String message) {
        final MainActivity mainActivity = (MainActivity)MainActivity.context;
        Objects.requireNonNull(mainActivity).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(Objects.requireNonNull(mainActivity).getApplicationContext(), message, Toast.LENGTH_SHORT).show();

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

    public void setThread(Thread th) {
        this.th = th;
    }

    public void setMonitoringThread(Thread monitoringThread) {
        this.monitoringThread = monitoringThread;
    }

    public void setComponentCount(ComponentCount componentCount) { this.componentCount = componentCount; }

    public void setIsMid(boolean mid) {
        this.isMid = mid;
    }

    public void setAliveCount(int aliveCount) {
        this.aliveCount = aliveCount;
    }

    public int getSavedFileCount() { return ++savedFileCount; }


    public void setTotalMovePoint(int x, int y) {
        this.totalMoveX = x;
        this.totalMoveY = y;
    }

}


class DrawingTask extends AsyncTask<MqttMessageFormat, MqttMessageFormat, Void> {
    private String username;
    private int action;
    private DrawingComponent dComponent;
    private Point point;
    private MQTTClient client = MQTTClient.getInstance();
    private DrawingEditor de = DrawingEditor.getInstance();
    private float myCanvasWidth = client.getDrawingView().getCanvasWidth();
    private float myCanvasHeight = client.getDrawingView().getCanvasHeight();
    private WarpingMessage warpingMessage;

    @Override
    protected Void doInBackground(MqttMessageFormat... messages) {
        MqttMessageFormat message = messages[0];
        this.username = message.getUsername();
        Mode mode = message.getMode();

        if((message.getMode() == null) || (message.getUsername() == null)) {
            return null;
        }

        de.setMyCanvasWidth(myCanvasWidth);
        de.setMyCanvasHeight(myCanvasHeight);

        if(de.getMyUsername().equals(username) && !mode.equals(Mode.DRAW)) { return null; }

        publishProgress(message);

        return null;
    }

    @Override
    protected void onProgressUpdate(MqttMessageFormat... messages) {
        MqttMessageFormat message = messages[0];
        Mode mode = message.getMode();

        switch(mode) {
            case DRAW:
                try{
                    draw(message);
                } catch(NullPointerException e) {
                    e.printStackTrace();
                }

                /*
                // fixme - draw point monitoring
                if (client.isMaster() && message.getAction() == MotionEvent.ACTION_MOVE
                        && message.getType().equals(ComponentType.STROKE) && !message.getUsername().equals(de.getMyUsername())) { // // 다른 사람이 보낸 메시지일 경우 [마스터가 자신의 화면에 그리는 시간 측정]
                    Log.e("monitoring", "******************** start recording ********************");
                    (MQTTClient.displayTimeList.lastElement()).calcTime(System.currentTimeMillis());
                    client.printDisplayTimeList();
                }
                */

                break;
            case ERASE:
                MyLog.i("mqtt", "MESSAGE ARRIVED message: username=" + username + ", mode=" + mode.toString() + ", id=" + message.getComponentIds().toString());

                Vector<Integer> erasedComponentIds = message.getComponentIds();
                new EraserTask(erasedComponentIds).doNotInBackground();
                de.clearUndoArray();
                break;
            case SELECT:
                if(message.getAction() == null && (de.findDrawingComponentByUsersComponentId(message.getUsersComponentId()) != null)) {
                    de.setDrawingComponentSelected(message.getUsersComponentId(), message.getIsSelected());
                    //de.findDrawingComponentByUsersComponentId(message.getUsersComponentId()).setSelected(message.getIsSelected());
                }

                DrawingComponent selectedComponent = de.findDrawingComponentByUsersComponentId(message.getUsersComponentId());
                if(selectedComponent == null) return;

                if(message.getAction() == null) {
                    /*if(message.getIsSelected()) {
                        de.clearMyCurrentBitmap();
                        de.drawSelectedComponentBorder(selectedComponent, de.getSelectedBorderColor());
                    } else {
                        de.clearMyCurrentBitmap();
                    }*/
                } else {
                    switch(message.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            client.setTotalMovePoint(0, 0);
                            MyLog.i("drawing", "other selected true");
                            break;
                        case MotionEvent.ACTION_MOVE:
                            if(message.getMoveSelectPoints().size() == 0) break;
                            for(Point point : message.getMoveSelectPoints()) {
                                client.setTotalMovePoint(client.getTotalMoveX()+point.x, client.getTotalMoveY()+point.y);
                                de.moveSelectedComponent(selectedComponent, point.x, point.y);
                            }
                            //de.clearMyCurrentBitmap();
                            de.updateSelectedComponent(selectedComponent);
                            de.clearDrawingBitmap();
                            de.drawAllDrawingComponents();
                            break;
                        case MotionEvent.ACTION_UP:
                            //de.clearMyCurrentBitmap();
                            //de.drawSelectedComponentBorder(selectedComponent, de.getSelectedBorderColor());
                            de.splitPointsOfSelectedComponent(selectedComponent, myCanvasWidth, myCanvasHeight);
                            de.updateSelectedComponent(selectedComponent);
                            de.clearDrawingBitmap();
                            de.drawAllDrawingComponents();

                            if(selectedComponent.clone() != null) {
                                de.addHistory(new DrawingItem(Mode.SELECT, selectedComponent.clone(), new Point(client.getTotalMoveX(), client.getTotalMoveY())));
                                MyLog.i("drawing", "history.size()=" + de.getHistory().size() + "id=" + selectedComponent.getId());
                            }
                            de.clearUndoArray();

                            if(de.getCurrentMode() == Mode.SELECT && client.getDrawingView().isSelected()) {
                                de.setPreSelectedComponentsBitmap();
                                de.setPostSelectedComponentsBitmap();

                                de.clearMyCurrentBitmap();
                                de.drawUnselectedComponents();
                                de.getSelectedComponent().drawComponent(de.getMyCurrentCanvas());
                                de.drawSelectedComponentBorder(de.getSelectedComponent(), de.getMySelectedBorderColor());
                            }

                            MyLog.i("drawing", "other selected finish");
                            break;
                    }
                }
                break;
            case CLEAR:
                MyLog.i("mqtt", "MESSAGE ARRIVED message: username=" + username + ", mode=" + mode.toString());
                de.clearDrawingComponents();

                if(client.getBinding().drawingView.isSelected()) {
                    de.deselect(true);
                    //de.clearAllSelectedBitmap();
                }

                de.clearTexts();
                client.getBinding().redoBtn.setEnabled(false);
                client.getBinding().undoBtn.setEnabled(false);
                break;
            case CLEAR_BACKGROUND_IMAGE:
                de.setBackgroundImage(null);
                de.clearBackgroundImage();
                break;
            case UNDO:
                MyLog.i("mqtt", "MESSAGE ARRIVED message: username=" + username + ", mode=" + mode.toString());
                if(client.getBinding().drawingView.isSelected()) {
                    de.deselect(true);
                    //de.clearAllSelectedBitmap();
                }

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
                MyLog.i("mqtt", "MESSAGE ARRIVED message: username=" + username + ", mode=" + mode.toString());
                if(client.getBinding().drawingView.isSelected()) {
                    de.deselect(true);
                    //de.clearAllSelectedBitmap();
                }

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
                this.warpingMessage = message.getWarpingMessage();
                WarpData data = warpingMessage.getWarpData();
                ((WarpingControlView)client.getBinding().backgroundView).warping2(data.getAction(), data.getPoints());
                break;
            case AUTODRAW:
                AutoDrawMessage autoDrawMessage = message.getAutoDrawMessage();
                ImageView imageView = new ImageView(MainActivity.context);
                imageView.setLayoutParams(new LinearLayout.LayoutParams(300, 300));
                imageView.setX(autoDrawMessage.getX());
                imageView.setY(autoDrawMessage.getY());
                client.getBinding().drawingViewContainer.addView(imageView);
                GlideToVectorYou.init().with(MainActivity.context).load(Uri.parse(autoDrawMessage.getUrl()),imageView);
                de.addAutoDraw(autoDrawMessage.getUrl(), imageView);
                break;
        }
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);

        client.getDrawingView().invalidate();


    }

    private void draw(MqttMessageFormat message) {
        this.action = message.getAction();
        this.point = message.getPoint();

        if(message.getComponent() == null) {
            if(de.getCurrentComponent(message.getUsersComponentId()) == null) return;
            dComponent = de.getCurrentComponent(message.getUsersComponentId());
        } else {
            dComponent = message.getComponent();
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                dComponent.clearPoints();
                dComponent.setId(de.componentIdCounter());

                de.addCurrentComponents(dComponent);
                de.printCurrentComponents("down");

                client.updateUsersAction(username, action);

                break;
            case MotionEvent.ACTION_MOVE:
                if (de.getMyUsername().equals(username)) {
                    for (Point point : message.getMovePoints()) {
                        client.getDrawingView().addPoint(dComponent, point);
                    }
                } else {
                    dComponent.calculateRatio(myCanvasWidth, myCanvasHeight);

                    MyLog.i("sendThread", "points[] = " + message.getMovePoints().toString());
                    for(Point point : message.getMovePoints()) {
                         client.getDrawingView().addPointAndDraw(dComponent, point, de.getCurrentCanvas());
                    }
                }

                client.updateUsersAction(username, action);

                break;
            case MotionEvent.ACTION_UP:
                if(de.getCurrentMode() == Mode.SELECT) {
                    de.addPostSelectedComponent(dComponent);
                }

                if(de.getMyUsername().equals(username)) {
                    client.getDrawingView().addPoint(dComponent, point);
                    client.getDrawingView().doInDrawActionUp(dComponent, de.getMyCanvasWidth(), de.getMyCanvasHeight());
                    if(de.isIntercept()) {
                        client.getDrawingView().setIntercept(true);
                        MyLog.i("intercept", "drawingview true (MQTT Client)");
                    }
                } else {
                    MyLog.i("sendThread", "draw up");
                    client.getDrawingView().addPointAndDraw(dComponent, point, de.getCurrentCanvas());
                    //client.getDrawingView().redrawShape(dComponent);
                    client.getDrawingView().doInDrawActionUp(dComponent, myCanvasWidth, myCanvasHeight);

                    de.clearCurrentBitmap();
                    de.drawOthersCurrentComponent(dComponent.getUsername());
                    dComponent.drawComponent(de.getBackCanvas());
                }
                client.updateUsersAction(username, action);

                break;
        }

        //if(de.getMyUsername().equals(username)) return;

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
            case MODIFY_START:
            case START_COLOR_CHANGE:
            case FINISH_COLOR_CHANGE:
                publishProgress(message);
                return null;
            case DRAG_LOCATION:
            case DRAG_EXITED:
                text.setTextViewLocation();
                return null;
            case DROP:
                //de.addHistory(new DrawingItem(TextMode.DROP, textAttr));
                text.setTextViewLocation();
                publishProgress(message);
                return null;
            case DONE:
                if(textAttr.isModified()) {
                    //de.addHistory(new DrawingItem(TextMode.MODIFY, textAttr));
                    MyLog.i("drawing", "isModified mqtt= " + textAttr.isModified());
                }
                publishProgress(message);
                return null;
            case DRAG_ENDED:
                return null;
            case ERASE:
                //de.addHistory(new DrawingItem(TextMode.ERASE, textAttr));
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
                //de.addHistory(new DrawingItem(TextMode.CREATE, textAttr));
                newText.setTextViewProperties();
                newText.addTextViewToFrameLayout();
                newText.createGestureDetector();
                //de.clearUndoArray();
                break;
            case DRAG_STARTED:
            case DRAG_LOCATION:
            case DRAG_ENDED:
                break;
            case DROP:
                //de.clearUndoArray();
                break;
            case DONE:
                text.getTextView().setBackground(null); // 테두리 설정 해제
                text.setTextViewAttribute();
                if(textAttr.isModified()) { de.clearUndoArray(); }
                break;
            case ERASE:
                text.removeTextViewToFrameLayout();
                de.removeTexts(text);
                //de.clearUndoArray();
                //Log.e("texts size", Integer.toString(de.getTexts().size()));
                break;
            case MODIFY_START:
                text.getTextView().setBackground(de.getTextFocusBorderDrawable()); // 수정중일 때 텍스트 테두리 설정하여 수정중인 텍스트 표시
                //text.modifyTextViewContent(textAttr.getText());
                break;
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
            //de.setBackgroundImage(de.byteArrayToBitmap());
            publishProgress();
        }
        return null;
    }

    @Override
    protected void onProgressUpdate(Void... values) {
        super.onProgressUpdate(values);
        MyLog.i("mqtt", "mid onProgressUpdate()");

//        WarpingControlView imageView = new WarpingControlView(client.getDrawingFragment().getContext());
//        imageView.setLayoutParams(new LinearLayout.LayoutParams(client.getDrawingFragment().getSize().x, ViewGroup.LayoutParams.MATCH_PARENT));
//        imageView.setImage(de.getBackgroundImage());
//
//        client.getBinding().backgroundView.addView(imageView);
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
        de.addAllTextViewToFrameLayoutForMid();
        client.getDrawingView().invalidate();

        client.getProgressDialog().dismiss();
        MyLog.i("mqtt", "mid progressDialog dismiss");
    }
}
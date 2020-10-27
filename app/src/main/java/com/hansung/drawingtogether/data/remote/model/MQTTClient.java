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
import com.hansung.drawingtogether.monitoring.Velocity;
import com.hansung.drawingtogether.view.WarpingControlView;
import com.hansung.drawingtogether.view.drawing.AudioPlayThread;
import com.hansung.drawingtogether.view.drawing.AutoDraw;
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

import com.hansung.drawingtogether.monitoring.Velocity;

import lombok.Getter;

@Getter
public enum MQTTClient {
    INSTANCE;

    /* MQTT 관련 변수 */
    private MqttClient client;
    private MqttClient client2;  // messageArrived 콜백 함수에서 publish 하는 MqttClient 객체

    private String BROKER_ADDRESS;
    private int qos = 2;

    private MQTTSettingData data = MQTTSettingData.getInstance();
    private MqttConnectOptions connOpts;

    /* TOPIC */
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

    private boolean master;
    private boolean isMid = true;

    private String masterName;
    private String myName;

    private JSONParser parser = JSONParser.getInstance();
    private DrawingEditor de = DrawingEditor.getInstance();
    private Logger logger = Logger.getInstance();

    private DrawingViewModel drawingViewModel;
    private DrawingFragment drawingFragment;
    private FragmentDrawingBinding binding;
    private DrawingView drawingView;

    private List<User> userList = new ArrayList<>(100);  // Member List
    private List<AudioPlayThread> audioPlayThreadList = new ArrayList<>(100);

    private Thread th;
    private int aliveLimitCount = 5;

    private int totalMoveX = 0;
    private int totalMoveY = 0;

    private int savedFileCount = 0;

    private boolean exitCompleteFlag = false;

    private ProgressDialog progressDialog;

    // fixme nayeon for monitoring
    /* 모니터링 관련 변수 */
    private ComponentCount componentCount;
    private Thread monitoringThread;

    // [Key] UUID [Value] Velocity
//    public static Vector<Velocity> receiveTimeList = new Vector<Velocity>();  // 메시지를 수신하는데 걸린 속도 데이터
//    public static Vector<Velocity> displayTimeList = new Vector<Velocity>();  // 화면에 출력하는데 걸린 속도 데이터
//    public static Vector<Velocity> deliveryTimeList = new Vector<Velocity>(); // 중간 참여자에게 메시지를 전송하는데 걸린 속도 데이터

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

            /* 마스터의 PlayThread 생성 */
            AudioPlayThread audioPlayThread = new AudioPlayThread();
            audioPlayThread.setUserName(masterName);
            audioPlayThread.setBufferUnitSize(2);
            audioPlayThread.start();
            audioPlayThreadList.add(audioPlayThread);
            MyLog.i("Audio", masterName + " 추가 후 : " + audioPlayThreadList.size());
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

        //this.usersActionMap = new HashMap<>();
        de.setMyUsername(name);
        isMid = true;
    }

    public void connect(String ip, String port, String topic, String name) { // client id = "*name_topic_android"
        try {
            BROKER_ADDRESS = "tcp://" + ip + ":" + port;

            /* 드로잉 데이터를 전송하는 클라이언트일 경우 */
            /* 브로커 로그에 표시되는 client id를 지정 */
            client = new MqttClient(BROKER_ADDRESS, ("*" + name + "_" + topic + "_Android"), new MemoryPersistence());
            client2 = new MqttClient(BROKER_ADDRESS, MqttClient.generateClientId(), new MemoryPersistence());

            connOpts = new MqttConnectOptions();

            connOpts.setCleanSession(true);
            connOpts.setKeepAliveInterval(1000);
            connOpts.setMaxInflight(5000);

            /* 클라이언트 연결이 끊겼을  자동으로 재연결 되도록 설정 */
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

    // fixme nayeon for performance
    /*
    public void monitoringClientSetting(MqttClient client, String topic) {
        try {

            // 드로잉 데이터를 전송하는 클라이언트일 경우
            // 브로커 로그에 표시되는 client id를 지정

            MqttConnectOptions connOpts = new MqttConnectOptions();

            connOpts.setCleanSession(true);
            connOpts.setKeepAliveInterval(1000);
            connOpts.setMaxInflight(5000);   //?

            connOpts.setAutomaticReconnect(true);

            client.connect(connOpts);

            // subscribeAllTopics(client);

            // clients.add(client);

            MyLog.e("kkankkan", topic + " subscribe");
            MyLog.i("mqtt", "SUBSCRIBE topic: " + topic);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
     */

    // fixme nayeon for performance
    /*
    public void subscribeAllTopics(MqttClient client) {
        try {
            client.subscribe(topic_join);
            client.subscribe(topic_exit);
            client.subscribe(topic_close);
            client.subscribe(topic_data);
            client.subscribe(topic_mid);
            client.subscribe(topic_image);
            client.subscribe(topic_alive);
        }catch(MqttException e) { e.printStackTrace(); }

    }
     */

    public void publish(String newTopic, String payload) {
        try {
            client.publish(newTopic, new MqttMessage(payload.getBytes()));
        } catch (MqttException e) {
            e.printStackTrace();
            showTimerAlertDialog("메시지 전송 실패", "메인 화면으로 이동합니다");
        }
    }

    public void publish(String newTopic, byte[] payload) {
        try {
            client.publish(newTopic, new MqttMessage(payload));
        } catch (MqttException e) {
            e.printStackTrace();
            /* 마이크 On 상태에서 Exit Or Close할 경우 Record Thread Interrupt 처리 */
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

    /* 회의방 퇴장 또는 종료 시 수행 */
    public void exitTask() {
        try {
            MyLog.i("ExitTask", "ExitTask 시작");

            th.interrupt(); // Alive Thread Interrupt

            // fixme nayeon for monitoring
            if(isMaster())
                monitoringThread.interrupt();

            if (isMaster()) {
                CloseMessage closeMessage = new CloseMessage(myName);
                MqttMessageFormat messageFormat = new MqttMessageFormat(closeMessage);
                publish(topic_close, JSONParser.getInstance().jsonWrite(messageFormat));

                MyLog.i("ExitTask", "Master Close Publish");
            } else {
                ExitMessage exitMessage = new ExitMessage(myName);
                MqttMessageFormat messageFormat = new MqttMessageFormat(exitMessage);
                publish(topic_exit, JSONParser.getInstance().jsonWrite(messageFormat));

                MyLog.i("ExitTask", "Participant Exit Publish");
            }

            /* UNSUBSCRIBE */
            client.unsubscribe(topic_join);
            client.unsubscribe(topic_exit);
            client.unsubscribe(topic_close);
            client.unsubscribe(topic_data);
            client.unsubscribe(topic_image);
            client.unsubscribe(topic_mid);
            client.unsubscribe(topic_alive);

            isMid = true;
            exitCompleteFlag = true;

            MyLog.i("ExitTask", "ExitTask 완료");
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    /* 멤버 리스트 출력 */
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

    public void setCallback() {
        client.setCallback(new MqttCallbackExtended() {

            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                if (reconnect) {
                    MyLog.i("modified mqtt", "RECONNECT");
                    subscribeAllTopics();

                    // fixme nayeon for monitoring
                    if(isMaster())
                        monitoringThread.notify();


                    /* 스피커 On(오디오 subscribe 중)이었다면 다시 subscribe */
                    if (drawingViewModel.isSpeakerFlag())
                        subscribe(topic_audio);
                } else {
                    MyLog.i("modified mqtt", "CONNECT");
                }
            }

            @Override
            public void connectionLost(Throwable cause) {
                MyLog.i("modified mqtt", "CONNECTION LOST : " + cause.getCause().toString());
                cause.printStackTrace();

                // fixme nayeon for monitoring
                try {
                    if(isMaster())
                        monitoringThread.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void messageArrived(String newTopic, MqttMessage message) throws Exception {

                // fixme nayeon for performance
//                if(!newTopic.equals(topic_image)) {
//                    MqttMessageFormat mmf = (MqttMessageFormat) parser.jsonReader(new String(message.getPayload()));
//
//                    if(isMaster()) {
//                        Log.e("performance", "component count = " + de.getDrawingComponents().size());
//                    }
//
//
//                    if (isMaster() && mmf.getAction() != null && mmf.getAction() == MotionEvent.ACTION_MOVE
//                            && mmf.getType().equals(ComponentType.STROKE)) { // 마스터가 STROKE 의 MOVE 이벤트에 대한 메시지를 받았을 경우
//                        if (mmf.getUsername().equals(myName)) { // 자기 자신이 보낸 메시지일 경우 [메시지를 받는데 걸린 시간 측정]
//                            System.out.println("here");
//                            (receiveTimeList.lastElement()).calcTime(System.currentTimeMillis(), message.getPayload().length);
//                            // printReceiveTimeList();
//                        }
//                        else if (!mmf.getUsername().equals(myName)) { // 다른 사람이 보낸 메시지일 경우 [화면에 그리는 시간 측정]
//                            displayTimeList.add(new Velocity(System.currentTimeMillis(), de.getDrawingComponents().size(), message.getPayload().length));
//                        }
//                    }
//
//                }

                /* TOPIC_JOIN */
                if (newTopic.equals(topic_join)) {

                    String msg = new String(message.getPayload());
                    MqttMessageFormat mqttMessageFormat = (MqttMessageFormat) parser.jsonReader(msg);

                    JoinMessage joinMessage = mqttMessageFormat.getJoinMessage();
                    JoinAckMessage joinAckMessage = mqttMessageFormat.getJoinAckMessage();

                    /* 새 참가자가 보낸 메시지인 경우 */
                    if (joinMessage != null) {
                        Log.i("JoinMessage", "JoinMessage Arrived");

                        String name = joinMessage.getName();

                        if (!name.equals(myName)) {
                            if (!isContainsUserList(name)) {

                                /* 새 참가자 제외 모든 멤버 수행 */
                                /* 새 참가자의 이름을 멤버 리스트에 추가 */
                                User user = new User(name, 0, MotionEvent.ACTION_UP, false);
                                userList.add(user);

                                /* 기존 멤버 수행 */
                                /* JoinAckMessage 전송 */
                                if (!master) {
                                    JoinAckMessage joinAckMsg = new JoinAckMessage(myName, name);
                                    MqttMessageFormat msgFormat = new MqttMessageFormat(joinAckMsg);
                                    client2.publish(topic_join, new MqttMessage(parser.jsonWrite(msgFormat).getBytes()));
                                }

                                /* 중간 참여자의 Play Thread 생성 */
                                AudioPlayThread audioPlayThread = new AudioPlayThread();
                                audioPlayThread.setUserName(name);
                                audioPlayThread.setBufferUnitSize(2);
                                audioPlayThread.start();
                                audioPlayThreadList.add(audioPlayThread);
                                MyLog.i("Audio", name + " 추가 후 : " + audioPlayThreadList.size());

                                /* 다른 사용자가 들어왔다는 메시지를 받았을 경우 */
                                /* 텍스트 비활성화를 위해 플래그 설정 */
                                Log.e("mid", "before mid enter true");
                                de.setMidEntered(true);
                                Log.e("mid", "after mid enter true, isMidEntered = " + de.isMidEntered());


                                //if (de.getCurrentMode() == Mode.DRAW) {  // current mode 가 DRAW 이면, 그리기 중이던 component 까지만 그리고 touch intercept   // todo 다른 모드에서도 intercept 하도록 추가
                                    de.setIntercept(true);
                                    if(!getDrawingView().isMovable()) {
                                        getDrawingView().setIntercept(true);
                                    }
                                //}

                                setToastMsg("[ " + name + " ] 님이 접속하셨습니다");

                                /* 멤버 리스트 출력 */
                                drawingViewModel.setUserNum(userList.size());
                                drawingViewModel.setUserPrint(userPrint());
                            }
                            /* master 수행 */
                            if (master) {
                                if (isUsersActionUp(name) && !isTextInUse()) {
                                    MyLog.i("text", "check text in use");
                                    JoinAckMessage joinAckMsgMaster = new JoinAckMessage(myName, name);

                                    /* 현재까지 공유된 컴포넌트 리스트 전송 */
                                    /* 드로잉 데이터는 MqttMessageFormat, 이미지 데이터는 binary로 publish */
                                    MqttMessageFormat messageFormat = new MqttMessageFormat(joinAckMsgMaster, de.getDrawingComponents(), de.getTexts(), de.getHistory(), de.getUndoArray(), de.getRemovedComponentId(), de.getMaxComponentId(), de.getMaxTextId(), de.getAutoDrawList());
                                    String json = parser.jsonWrite(messageFormat);

                                    // fixme nayeon for performance
                                    // deliveryTimeList.add(new Velocity(System.currentTimeMillis(), name, json.getBytes().length, de.getDrawingComponents().size()));

                                    client2.publish(topic_join, new MqttMessage(json.getBytes()));

                                    if (de.getBackgroundImage() != null) {
                                        /* 중간 참자에게 와핑된 이미지를 보내도록 변경 */
                                        byte[] backgroundImage = de.bitmapToByteArray(((WarpingControlView)MQTTClient.getInstance().getBinding().backgroundView).getImage());
                                        client2.publish(topic_image, new MqttMessage(backgroundImage));
                                    }

//                                    for (int i = 0; i < de.getAutoDrawImageList().size(); i++) {
//                                        String url = de.getAutoDrawImageList().get(i);
//                                        ImageView view = de.getAutoDrawImageViewList().get(i);
//                                        AutoDrawMessage autoDrawMessage = new AutoDrawMessage(data.getName(), url, view.getX(), view.getY(), de.getMyCanvasWidth(), de.getMyCanvasHeight());
//                                        MqttMessageFormat messageFormat2 = new MqttMessageFormat(de.getMyUsername(), de.getCurrentMode(), de.getCurrentType(), autoDrawMessage);
//                                        String json2 = parser.jsonWrite(messageFormat2);
////                                        client2.publish(topic_data, new MqttMessage(json2.getBytes()));
//                                    }

                                    setToastMsg("[ " + name + " ] 님에게 데이터 전송을 완료했습니다");

                                } else {
                                    MqttMessageFormat messageFormat = new MqttMessageFormat(new JoinMessage(name));
                                    client2.publish(topic_join, new MqttMessage(parser.jsonWrite(messageFormat).getBytes()));
                                    MyLog.i("master republish name", topic_join);
                                }
                            }
                        }

                    }
                    /* master 또는 기존 멤버가 보낸 메시지인 경우 */
                    else if (joinAckMessage != null) {

                        /* 새 참여자 수행 */
                        Log.e("joinAckMessage", "JoinAckMessage Arrived");

                        String name = joinAckMessage.getName();
                        String target = joinAckMessage.getTarget();

                        if (target.equals(myName)) {
                            if (name.equals(masterName)) {
                                /* master가 보낸 메시지인 경우 */

                                /* 드로잉에 필요한 구조체들 저장하는 부분 */
                                /* 필요한 배열 리스트들과 배경 이미지 세팅 */
                                de.setDrawingComponents(mqttMessageFormat.getDrawingComponents());
                                de.setHistory(mqttMessageFormat.getHistory());
                                de.setUndoArray(mqttMessageFormat.getUndoArray());
                                de.setRemovedComponentId(mqttMessageFormat.getRemovedComponentId());
                                de.setAutoDrawList(mqttMessageFormat.getAutoDrawList());

                                de.setTexts(mqttMessageFormat.getTexts());
                                if (mqttMessageFormat.getBitmapByteArray() != null) {
                                    de.byteArrayToBitmap(mqttMessageFormat.getBitmapByteArray());
                                }

                                // 아이디 세팅
                                de.setMaxComponentId(mqttMessageFormat.getMaxComponentId());
                                // de.setTextId(mqttMessageFormat.getMaxTextId()); // 텍스트 아이디는 "사용자이름-textIdCount" 이므로 textIdCount 가 같아도 고유
                                MyLog.i("drawing", "component id = " + mqttMessageFormat.getMaxComponentId() + ", text id = " + mqttMessageFormat.getMaxTextId());

                                client2.publish(topic_mid, new MqttMessage(JSONParser.getInstance().jsonWrite(new MqttMessageFormat(myName, Mode.MID)).getBytes()));
                            }
                            else if (!isContainsUserList(name)) {
                                /* 기존 멤버가 보낸 메시지인 경우 */
                                /* 멤버의 이름을 멤버 리스트에 추가 */
                                User user = new User(name, 0, MotionEvent.ACTION_UP, false);
                                userList.add(user);

                                /* 기존 참여자의 Play Thread 생성 */
                                AudioPlayThread audioPlayThread = new AudioPlayThread();
                                audioPlayThread.setUserName(name);
                                audioPlayThread.setBufferUnitSize(2);
                                audioPlayThread.start();
                                audioPlayThreadList.add(audioPlayThread);
                                MyLog.i("audio", name + " 추가 후 : " + audioPlayThreadList.size());

                                /* 멤버 리스트 출력 */
                                drawingViewModel.setUserNum(userList.size());
                                drawingViewModel.setUserPrint(userPrint());
                            }
                        }
                    }
                }

                /* TOPIC_EXIT */
                if (newTopic.equals(topic_exit)) {

                    String msg = new String(message.getPayload());
                    MqttMessageFormat mqttMessageFormat = (MqttMessageFormat) parser.jsonReader(msg);
                    ExitMessage exitMessage = mqttMessageFormat.getExitMessage();
                    String name = exitMessage.getName();

                    if (!myName.equals(name)) {

                        /* 멤버 리스트에서 멤버 제거 */
                        for (int i=0; i<userList.size(); i++) {
                            if (userList.get(i).getName().equals(name)) {
                                userList.remove(i);

                                /* 멤버 리스트 출력 */
                                drawingViewModel.setUserNum(userList.size());
                                drawingViewModel.setUserPrint(userPrint());

                                setToastMsg("[ " + name + " ] 님이 나가셨습니다");
                                break;
                            }
                        }

                        /* 해당 멤버의 Play Thread Interrupt, PlayThreadList에서 제거 */
                        for (int i=0; i<audioPlayThreadList.size(); i++) {
                            if (audioPlayThreadList.get(i).getUserName().equals(name)) {
                                audioPlayThreadList.get(i).setFlag(false);
                                audioPlayThreadList.get(i).getBuffer().clear();
                                audioPlayThreadList.get(i).stopPlaying();
                                audioPlayThreadList.get(i).interrupt();
                                MyLog.i("Audio", name + " remove 전 : " + audioPlayThreadList.size());
                                audioPlayThreadList.remove(i);
                                MyLog.i("Audio", name + " remove 후 : " + audioPlayThreadList.size());
                            }
                        }
                    }
                }

                /* TOPIC_CLOSE */
                if (newTopic.equals(topic_close)) {

                    String msg = new String(message.getPayload());
                    MqttMessageFormat mqttMessageFormat = (MqttMessageFormat) parser.jsonReader(msg);
                    CloseMessage closeMessage = mqttMessageFormat.getCloseMessage();

                    String name = closeMessage.getName();

                    /* 회의방 종료 */
                    if (!name.equals(myName)) {
                        showExitAlertDialog("마스터가 회의방을 종료하였습니다");
                    }
                }

                /* TOPIC_ALIVE */
                if (newTopic.equals(topic_alive)) {

                    String msg = new String(message.getPayload());
                    MqttMessageFormat mqttMessageFormat = (MqttMessageFormat) parser.jsonReader(msg);
                    AliveMessage aliveMessage = mqttMessageFormat.getAliveMessage();
                    String name = aliveMessage.getName();

                    if (myName.equals(name)) {
                        Iterator<User> iterator = userList.iterator();
                        while (iterator.hasNext()) {
                            User user = iterator.next();

                            if (!user.getName().equals(myName)) {
                                user.setCount(user.getCount() + 1);

                                if (user.getCount() == aliveLimitCount && user.getName().equals(masterName)) {
                                    showExitAlertDialog("마스터 접속이 끊겼습니다. 회의방을 종료합니다.");
                                }
                                else if (user.getCount() == aliveLimitCount) {
                                    iterator.remove();
                                    drawingViewModel.setUserNum(userList.size());
                                    drawingViewModel.setUserPrint(userPrint());
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

                /* TOPIC_DATA */
                if (newTopic.equals(topic_data) && de.getMainBitmap() != null) {
                    String msg = new String(message.getPayload());

                    //MyLog.i("drawMsg", msg);

                    MqttMessageFormat messageFormat = (MqttMessageFormat) parser.jsonReader(msg);

                    /* 중간 참여자가 입장했을 때 처리 */
                    if(de.isMidEntered() && (messageFormat.getAction() != null && messageFormat.getAction() != MotionEvent.ACTION_UP)) { // getAction == null
                        if(/*getDrawingView().isIntercept() || */(de.isIntercept() && (messageFormat.getAction() != null && messageFormat.getAction() == MotionEvent.ACTION_DOWN)) || (de.getCurrentComponent(messageFormat.getUsersComponentId()) == null))
                            return;
                    }


                    // fixme nayeon for monitoring
                    /* 모니터링 코드 (컴포넌트 개수 카운트) Only Master */
                    if(isMaster()) {
                        MyLog.i("> monitoring", "before check component count");
                        MyLog.i("> monitoring", "mode = " + messageFormat.getMode() + ", type = " + messageFormat.getType()
                                + ", text mode = " + messageFormat.getTextMode());

                        /* 컴포넌트 개수 저장 */
                        if ((messageFormat.getAction() != null && messageFormat.getAction() == MotionEvent.ACTION_DOWN)
                                || messageFormat.getMode() == Mode.TEXT || messageFormat.getMode() == Mode.ERASE) {
                            MyLog.i("< monitoring", "mode = " + messageFormat.getMode() + ", type = " + messageFormat.getType()
                                    + ", text mode = " + messageFormat.getTextMode());
                            checkComponentCount(messageFormat.getMode(), messageFormat.getType(), messageFormat.getTextMode());
                        }

                        MyLog.i("< monitoring", "after check component count");
                    }

                    /* 컴포넌트 처리 */
                    if (messageFormat.getMode() == Mode.TEXT) {  // TEXT 모드일 경우, username이 다른 경우만 task 생성
                        if (!messageFormat.getUsername().equals(de.getMyUsername())) {
//                            MyLog.i("drawing", "username = " + messageFormat.getUsername() + ", text id = " + messageFormat.getTextAttr().getId() + ", mode = " + messageFormat.getMode() + ", text mode = " + messageFormat.getTextMode());
                            new TextTask().execute(messageFormat);
                        }
                    } else {
                        new DrawingTask().execute(messageFormat);
                    }
                }

                /* TOPIC_MID */
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

                    /* 모든 사용자가 topic_mid 로 메시지 전송받음 */
                    /* 이 시점 중간자에게는 모든 데이터 저장 완료 후 */
                    de.setMidEntered(false);
                }

                /* TOPIC_AUDIO */
                if (newTopic.equals(topic_audio)) {
                    byte[] audioMessage = message.getPayload();

                    byte[] nameByte = Arrays.copyOfRange(audioMessage, 5000, audioMessage.length);
                    String name = new String(nameByte);

                    if (myName.equals(name)) return; // 자신의 데이터는 받지 않음

                    byte[] audioData = Arrays.copyOfRange(audioMessage, 0, audioMessage.length - nameByte.length);

                    /* username을 검사하여 해당 PlayThread의 오디오 큐에 오디오 데이터 삽입 */
                    for (AudioPlayThread audioPlayThread : audioPlayThreadList) {
                        if (audioPlayThread.getUserName().equals(name)) {
                            audioPlayThread.getBuffer().add(audioData);
                            break;
                        }
                    }
                }

                /* TOPIC_IMAGE */
                if (newTopic.equals(topic_image)) {
                    byte[] imageData = message.getPayload();
                    de.setBackgroundImage(de.byteArrayToBitmap(imageData));

                    /* 이미지 변경 시 깜빡임을 없애기 위해 WarpingControlView를 backgroundview에 고정, 비트맵만 갈아끼우도록 변경 */
                    binding.backgroundView.setCancel(true);
                    binding.backgroundView.setImage(de.getBackgroundImage());
                    MyLog.i("Image", "set image");

                    // fixme nayeon for monitoring
                    if(isMaster()) { // 전송한 이미지 개수 카운팅 (모니터링)
                        componentCount.increaseImage();
                    }

                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });
    }

    /* 멤버 리스트에 존재하는 이름인지 검사 */
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
            if (/*t.getTextAttribute().getUsername() != null*/ t.getTextAttribute().isDragging()) {
                Log.e("text", "text in use id = " + t.getTextAttribute().getId());
                return true;
            }
        }
        return false;
    }

    // fixme nayeon for monitoring
    public void checkComponentCount(Mode mode, ComponentType type, TextMode textMode) {
        Log.i("monitoring", "execute check component count func.");

        if(mode == Mode.TEXT && textMode == TextMode.CREATE) {
            //Log.e("monitoring", "check component count func. text count increase.");

            componentCount.increaseText();
            return;
        }

        if(mode != Mode.DRAW) {
            //Log.e("monitoring", "check component count func. mode is not DRAW");
            return;
        }
        //Log.e("monitoring", "check component count func. mode is DRAW");


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

        // fixme nayeon for monitoring
        if(isMaster())
            monitoringThread.interrupt();


        isMid = true;
        de.removeAllDrawingData();
        drawingViewModel.back();
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

                                    // fixme nayeon for performance
//                                    if(isMaster()) {
//                                        MonitoringDataWriter.getInstance().write();
//                                    }

                                }
                                if (progressDialog.isShowing())
                                    progressDialog.dismiss();
                                drawingViewModel.back();
                            }
                        })
                        .setNeutralButton("저장 후 종료", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if(!exitCompleteFlag) drawingViewModel.clickSave();
                                if (client.isConnected()) {
                                    exitTask();

                                    // fixme nayeon for performance
//                                    if(isMaster()) {
//                                        MonitoringDataWriter.getInstance().write();
//                                    }

                                }
                                if (progressDialog.isShowing())
                                    progressDialog.dismiss();
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

    /* SETTER */

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


    public void setIsMid(boolean mid) {
        this.isMid = mid;
    }

    public void setAliveLimitCount(int aliveLimitCount) {
        this.aliveLimitCount = aliveLimitCount;
    }

    public int getSavedFileCount() { return ++savedFileCount; }

    public void setTotalMovePoint(int x, int y) {
        this.totalMoveX = x;
        this.totalMoveY = y;
    }

    /* monitoring variable setting function */
    // fixme nayeon for monitoring
    public void setMonitoringThread(Thread monitoringThread) {
        this.monitoringThread = monitoringThread;
    }

    // fixme nayeon for monitoring
    public void setComponentCount(ComponentCount componentCount) { this.componentCount = componentCount; }

    // fixme nayeon for performance
    /* monitoring print function */
    /*
    public void printReceiveTimeList() {
        System.out.println("-------------------- Receive Time List --------------------");

        for(int i=0; i<receiveTimeList.size(); i++)
            System.out.println(i + ". " + receiveTimeList.get(i).toString());
    }

    public void printDisplayTimeList() {
        System.out.println("-------------------- Display Time List --------------------");

        for(int i=0; i<displayTimeList.size(); i++)
            System.out.println(i + ". " + displayTimeList.get(i).toString());
    }

    public void printDeliveryTimeList() {
        System.out.println("-------------------- Delivery Time List --------------------");

        for(int i=0; i<deliveryTimeList.size(); i++)
            System.out.println(i + ". " + deliveryTimeList.get(i).toString());
    }
     */

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


                // fixme nayeon for performance ( draw point )
//                if (client.isMaster() && /*message.getAction() == MotionEvent.ACTION_MOVE
//                        && messageFormat.getType().equals(ComponentType.STROKE)*/ message.getMode().equals(Mode.DRAW)
//                        && !message.getUsername().equals(de.getMyUsername())) { // 다른 사람이 보낸 메시지일 경우 [마스터가 자신의 화면에 그리는 시간 측정]
//
//                    (MQTTClient.displayTimeList.lastElement()).calcTime(System.currentTimeMillis());
//                    //client.printDisplayTimeList();
//                }



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
                                de.getSelectedComponent().drawComponent(de.getCurrentCanvas());
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
//                WarpData data = warpingMessage.getWarpData();
                ((WarpingControlView)client.getBinding().backgroundView).warping2(warpingMessage);
                break;
            case AUTODRAW:
                AutoDrawMessage autoDrawMessage = message.getAutoDrawMessage();
                ImageView imageView = new ImageView(MainActivity.context);
                imageView.setLayoutParams(new LinearLayout.LayoutParams(300, 300));

                Point point = new Point();
                point.x = (int)(autoDrawMessage.getX() * myCanvasWidth / autoDrawMessage.getWidth());
                point.y = (int)(autoDrawMessage.getY() * myCanvasHeight / autoDrawMessage.getHeight());

                imageView.setX(point.x);
                imageView.setY(point.y);

                AutoDraw autoDraw = new AutoDraw(myCanvasWidth, myCanvasHeight, point, autoDrawMessage.getUrl());
                de.addAutoDraw(autoDraw);
                de.addAutoDrawImageView(imageView);

                String url = autoDrawMessage.getUrl();
                client.getBinding().drawingViewContainer.addView(imageView);
                GlideToVectorYou.init().with(MainActivity.context).load(Uri.parse(url), imageView);
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
                         client.getDrawingView().addPointAndDraw(dComponent, point, de.getReceiveCanvas());
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
                    client.getDrawingView().addPointAndDraw(dComponent, point, de.getReceiveCanvas());
                    //client.getDrawingView().redrawShape(dComponent);
                    client.getDrawingView().doInDrawActionUp(dComponent, myCanvasWidth, myCanvasHeight);

                    de.clearCurrentBitmap();
                    de.drawOthersCurrentComponent(dComponent.getUsername());
                    dComponent.drawComponent(de.getMainCanvas());
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

        /* 텍스트 객체가 처음 생성되는 경우, 텍스트 배열에 저장된 정보 없음 */
        /* 그 이후에 일어나는 텍스트에 대한 모든 행위들은 */
        /* 텍스트 배열로부터 텍스트 객체를 찾아서 작업 가능 */
        if(!textMode.equals(TextMode.CREATE)) {
            text = de.findTextById(textAttr.getId());
            if(text == null) return null; // 중간자가 자신에게 MID 로 보낸 메시지보다, 마스터가 TEXT 로 보낸 메시지가 먼저 올 경우 (중간자가 자신의 처리를 다 했다는 플래그 필요?)
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

        for (int i = 0; i < de.getAutoDrawList().size(); i++) {
            ImageView imageView = new ImageView(MainActivity.context);
            imageView.setLayoutParams(new LinearLayout.LayoutParams(300, 300));
            AutoDraw autoDraw = de.getAutoDrawList().get(i);

            int x = (int)(autoDraw.getPoint().x * client.getDrawingView().getCanvasWidth() / autoDraw.getWidth());
            int y = (int)(autoDraw.getPoint().y * client.getDrawingView().getCanvasHeight() / autoDraw.getHeight());

            imageView.setX(x);
            imageView.setY(y);
            client.getBinding().drawingViewContainer.addView(imageView);
            GlideToVectorYou.init().with(MainActivity.context).load(Uri.parse(autoDraw.getUrl()), imageView);
            de.addAutoDrawImageView(imageView);
        }

        client.getProgressDialog().dismiss();
        MyLog.i("mqtt", "mid progressDialog dismiss");
    }
}
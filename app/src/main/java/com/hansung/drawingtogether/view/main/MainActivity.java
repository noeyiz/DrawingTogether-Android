package com.hansung.drawingtogether.view.main;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.hansung.drawingtogether.R;
import com.hansung.drawingtogether.data.remote.model.Logger;
import com.hansung.drawingtogether.data.remote.model.MQTTClient;
import com.hansung.drawingtogether.data.remote.model.MyLog;
import com.hansung.drawingtogether.view.drawing.DrawingEditor;
import com.hansung.drawingtogether.view.drawing.SendMqttMessage;

import org.eclipse.paho.client.mqttv3.MqttException;

public class MainActivity extends AppCompatActivity {
    private String topicPassword="";
    private Toolbar toolbar;
    private TextView title;

    public static Context context; // fixme nayeon

    private Logger logger = Logger.getInstance(); // fixme nayeon ☆☆☆☆☆ 1. Log 기록에 사용할 클래스 참조

    private DrawingEditor de = DrawingEditor.getInstance();
    private long lastTimeBackPressed;  // fixme hyeyon[3]

    public interface OnBackListener {
        public void onBack();
    }

    private OnBackListener onBackListener;


    // fixme hyeyeon
    private onKeyBackPressedListener mOnKeyBackPressedListener;  // 하단의 백버튼 리스너

    public interface onKeyBackPressedListener {
        void onBackKey();
    }
    //

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MyLog.i("lifeCycle", "MainActivity onCreate()");

        setContentView(R.layout.activity_main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); // 앱 실행 중 화면 사라지지 않도록

        SendMqttMessage sendMqttMessage = SendMqttMessage.getInstance();
        sendMqttMessage.startThread();

        context = this; // fixme nayeon


        toolbar = (Toolbar)findViewById(R.id.toolbar);
        title = (TextView)findViewById(R.id.toolbar_title);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false); // 기본 title 안 보이게

        Intent kakaoIntent = getIntent();
        if (kakaoIntent == null)
            return;

        Uri uri = kakaoIntent.getData();
        if (uri == null)
            return;

        String topic = uri.getQueryParameter("topic");
        String password = uri.getQueryParameter("password");

        MyLog.e("kkankkan", topic + "/" + password);

        if (!(topic == null) && !(password == null)) {
            topicPassword = topic;
            topicPassword += "/" + password;
        }
    }

    @Override
    public void onBackPressed() {

        if (mOnKeyBackPressedListener != null) {
            mOnKeyBackPressedListener.onBackKey();
        }
        else {
            MyLog.e("kkankkan", "메인엑티비티 onbackpressed");
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setMessage("앱을 종료하시겠습니까?")
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            MyLog.d("button", "back press ok button click"); // fixme nayeon

                            MainActivity.super.onBackPressed();
                            return;
                        }
                    })
                    /*.setNeutralButton("save and exit", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            MainActivity.super.onBackPressed();
                            return;
                        }
                    })*/ // todo nayeon - mOnKeyBackPressedListener 가 null 일 경우가 언제?
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            MyLog.d("button", "back press cancel button click"); // fixme nayeon

                            return;
                        }
                    })
                    .create();
            dialog.show();


        }
    }

    public void setOnKeyBackPressedListener(onKeyBackPressedListener listener) {
        this.mOnKeyBackPressedListener = listener;
    }
    //

    // title 설정
    public void setToolbarTitle(String title) {
        this.title.setText(title);
    }

    // fixme nayeon - ToolBar Menu Item Visibility
    public void setVisibilityToolbarMenus(boolean visibility) {
        for( int i=0; i<toolbar.getMenu().size(); i++ ) {
            toolbar.getMenu().getItem(i).setVisible(visibility);
        }
    }


    public String getTopicPassword() {
        return topicPassword;
    }

    public void setTopicPassword(String topicPassword) {
        this.topicPassword = topicPassword;
    }

    public void setOnBackListener(OnBackListener onBackListener) {
        this.onBackListener = onBackListener;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home)
            onBackListener.onBack();

        return super.onOptionsItemSelected(item);
    }

    // fixme hyeyeon[1]
    @Override
    protected void onRestart() {
        super.onRestart();
        MyLog.i("lifeCycle", "MainActivity onRestart()");
    }

    @Override
    protected void onStart() {
        super.onStart();
        MyLog.i("lifeCycle", "MainActivity onStart()");
    }

    @Override
    protected void onResume() {
        super.onResume();
        MyLog.i("lifeCycle", "MainActivity onResume()");
    }

    @Override
    protected void onPause() {
        super.onPause();
        MyLog.i("lifeCycle", "MainActivity onPause()");
    }

    @Override
    protected void onStop() {
        super.onStop();
        MyLog.i("lifeCycle", "MainActivity onStop()");
    }

    @Override
    protected void onDestroy() {  // todo
        super.onDestroy();
        MyLog.i("lifeCycle", "MainActivity onDestroy()");
        if (isFinishing()) {  // 앱 종료 시
            MyLog.i("lifeCycle", "isFinishing() " + isFinishing());

            // todo database 접근하는 코드 추가할지 말지 고민중
            MQTTClient client = MQTTClient.getInstance();
            if (client != null && client.getClient().isConnected()) {
                try {
                    client.getClient().disconnect();
                    client.getClient().close();
                    MyLog.i("lifeCycle", "mqttClient closed");
                } catch (MqttException e) {
                    e.printStackTrace();
                }
                client = null;
            }
        }
        else {  // 화면 회전 시
            MyLog.i("lifeCycle", "isFinishing() " + isFinishing());
        }
    }
}

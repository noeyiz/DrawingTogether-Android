package com.hansung.drawingtogether.view.main;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static com.kakao.util.helper.Utility.getPackageInfo;

public class MainActivity extends AppCompatActivity {
    private String topic;
    private String password;
    private Toolbar toolbar;
    private TextView title;

    public static Context context;

    private DrawingEditor de = DrawingEditor.getInstance();
    private Logger logger = Logger.getInstance(); // fixme - ☆☆☆☆☆ 1. Log 기록에 사용할 클래스 참조

    public interface OnLeftTopBackListener {  // 좌측 상단의 백버튼 리스너
        void onLeftTopBackPressed();
    }
    private OnLeftTopBackListener onLeftTopBackListener;

    public interface OnRightBottomBackListener {  // 디바이스의 오른쪽 하단의 백버튼 리스너
        void onRightBottomBackPressed();
    }
    private OnRightBottomBackListener onRightBottomBackListener;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MyLog.i("lifeCycle", "MainActivity onCreate()");

        setContentView(R.layout.activity_main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); // 앱 실행 중 화면 사라지지 않도록

        SendMqttMessage sendMqttMessage = SendMqttMessage.getInstance();
        sendMqttMessage.startThread();

        // kakaolink
        String kakaoTopic = getIntent().getStringExtra("kakaoTopic");
        String kakaoPassword = getIntent().getStringExtra("kakaoPassword");

        if (!(kakaoTopic == null) && !(kakaoPassword == null)) {
            topic = kakaoTopic;
            password = kakaoPassword;
        }
        //

        context = this;

        Log.i("kakao", "[key hash] " + getKeyHash(context));

        toolbar = (Toolbar)findViewById(R.id.toolbar);
        title = (TextView)findViewById(R.id.toolbar_title);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false); // 기본 title 안 보이게

    }

    public String getKeyHash(final Context context) {
        PackageInfo info = getPackageInfo(context, PackageManager.GET_SIGNATURES);
        if (info == null) {
            return null;
        }

        for (Signature signature: info.signatures) {
            try {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                return Base64.encodeToString(md.digest(), Base64.NO_WRAP);
            } catch (NoSuchAlgorithmException e) {
                Log.e("kkankkan", "Unable to get MessageDigest, sugnature = " + signature);
            }
        }

        return null;
    }

    @Override
    public void onBackPressed() {

        if (onRightBottomBackListener != null) {
            onRightBottomBackListener.onRightBottomBackPressed();
        }
        else {
            MyLog.e("kkankkan", "메인엑티비티 onbackpressed");
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setMessage("앱을 종료하시겠습니까?")
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            MyLog.d("button", "back press ok button click");

                            MainActivity.super.onBackPressed();
                            android.os.Process.killProcess(android.os.Process.myPid());
                            System.exit(10);
                            return;
                        }
                    })
                    /*.setNeutralButton("save and exit", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            MainActivity.super.onBackPressed();
                            return;
                        }
                    })*/ // todo - mOnKeyBackPressedListener 가 null 일 경우가 언제?
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            MyLog.d("button", "back press cancel button click");

                            return;
                        }
                    })
                    .create();
            dialog.show();


        }
    }

    public void setOnRightBottomBackListener(OnRightBottomBackListener listener) {
        this.onRightBottomBackListener = listener;
    }

    public void setOnLeftTopBackListener(OnLeftTopBackListener listener) {
        this.onLeftTopBackListener = listener;
    }

    public void setToolbarVisible() {
        toolbar.setVisibility(View.VISIBLE);
    }

    public void setToolbarInvisible() {
        toolbar.setVisibility(View.GONE);
    }

    // title 설정
    public void setToolbarTitle(String title) {
        this.title.setText(title);
    }

    // fixme - ToolBar Menu Item Visibility
    public void setVisibilityToolbarMenus(boolean visibility) {
        for( int i=0; i<toolbar.getMenu().size(); i++ ) {
            toolbar.getMenu().getItem(i).setVisible(visibility);
        }
    }

    public String getTopic() {
        return topic;
    }

    public String getPassword() {
        return password;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home)
            onLeftTopBackListener.onLeftTopBackPressed();

        return super.onOptionsItemSelected(item);
    }

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
        }
        else {  // 화면 회전 시
            MyLog.i("lifeCycle", "isFinishing() " + isFinishing());
        }
    }
}

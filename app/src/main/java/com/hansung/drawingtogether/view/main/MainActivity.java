package com.hansung.drawingtogether.view.main;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
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
import com.hansung.drawingtogether.data.remote.model.MyLog;
import com.hansung.drawingtogether.view.drawing.DrawingEditor;
import com.hansung.drawingtogether.view.drawing.SendMqttMessage;

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
    private Logger logger = Logger.getInstance();

    /* 좌측 상단의 백버튼 리스너 */
    public interface OnLeftTopBackListener { void onLeftTopBackPressed();}
    private OnLeftTopBackListener onLeftTopBackListener;

    /* 오른쪽 하단의 백버튼 리스너 */
    public interface OnRightBottomBackListener { void onRightBottomBackPressed();}
    private OnRightBottomBackListener onRightBottomBackListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MyLog.i("LifeCycle", "MainActivity onCreate()");

        setContentView(R.layout.activity_main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); // 앱 실행 중 화면이 꺼지지 않도록 설정

        SendMqttMessage sendMqttMessage = SendMqttMessage.getInstance();
        sendMqttMessage.startThread();

        /* SplashActivity로부터 전달 받은 topic, password 저장 */
        String kakaoTopic = getIntent().getStringExtra("kakaoTopic");
        String kakaoPassword = getIntent().getStringExtra("kakaoPassword");

        if (kakaoTopic != null && kakaoPassword != null) {
            topic = kakaoTopic;
            password = kakaoPassword;
        }

        context = this;

        /* Kakao Debug Key Hash 값 출력 */
        Log.i("KakaoLink", "[key hash] " + getKeyHash(context));

        toolbar = (Toolbar)findViewById(R.id.toolbar);
        title = (TextView)findViewById(R.id.toolbar_title);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false); // 기본 title이 안 보이도록 설정

    }

    /* Kakao Debug Key Hash 값 구하기 */
    public String getKeyHash(final Context context) {

        PackageInfo info = getPackageInfo(context, PackageManager.GET_SIGNATURES);
        if (info == null) { return null; }

        for (Signature signature: info.signatures) {
            try {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                return Base64.encodeToString(md.digest(), Base64.NO_WRAP);
            } catch (NoSuchAlgorithmException e) {
                Log.e("KakaoLink", "Unable to get MessageDigest, sugnature = " + signature);
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
            MyLog.i("Back Button", "MainActivity onBackPressed()");

            /* 메인 화면에서 오른쪽 하단의 백버튼 클릭 시 */
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setMessage("앱을 종료하시겠습니까?")
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            /* 앱 종료 */
                            MainActivity.super.onBackPressed();
                            android.os.Process.killProcess(android.os.Process.myPid());
                            System.exit(10);
                            return;
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) { }
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

    /* title 설정 */
    public void setToolbarTitle(String title) {
        this.title.setText(title);
    }

    /* ToolBar Menu Item Visibility */
    public void setVisibilityToolbarMenus(boolean visibility) {
        for( int i=0; i<toolbar.getMenu().size(); i++ ) {
            toolbar.getMenu().getItem(i).setVisible(visibility);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home)
            onLeftTopBackListener.onLeftTopBackPressed();

        return super.onOptionsItemSelected(item);
    }

    /* GETTER */
    public String getTopic() { return topic; }

    public String getPassword() { return password; }


    /* SETTER */
    public void setTopic(String topic) { this.topic = topic; }

    public void setPassword(String password) { this.password = password; }

    @Override
    protected void onRestart() {
        super.onRestart();
        MyLog.i("LifeCycle", "MainActivity onRestart()");
    }

    @Override
    protected void onStart() {
        super.onStart();
        MyLog.i("LifeCycle", "MainActivity onStart()");
    }

    @Override
    protected void onResume() {
        super.onResume();
        MyLog.i("LifeCycle", "MainActivity onResume()");
    }

    @Override
    protected void onPause() {
        super.onPause();
        MyLog.i("LifeCycle", "MainActivity onPause()");
    }

    @Override
    protected void onStop() {
        super.onStop();
        MyLog.i("LifeCycle", "MainActivity onStop()");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MyLog.i("LifeCycle", "MainActivity onDestroy()");
    }
}

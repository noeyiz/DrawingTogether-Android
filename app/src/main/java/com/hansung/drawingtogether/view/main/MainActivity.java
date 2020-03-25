package com.hansung.drawingtogether.view.main;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.hansung.drawingtogether.R;


public class MainActivity extends AppCompatActivity {
    private String topicPassword="";
    private Toolbar toolbar;
    private TextView title;

    public static Context context; // fixme nayeon


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
        Log.e("activity", "onCreate()");
        setContentView(R.layout.activity_main);

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

        Log.e("kkankkan", topic + "/" + password);

        if (!(topic == null) && !(password == null)) {
            topicPassword = topic;
            topicPassword += "/" + password;
        }






    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.e("activity", "onStart()");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.e("activity", "onRestart()");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.e("activity", "onResume()");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.e("activity", "onResume()");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.e("activity", "onStop()");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.e("activity", "onDestroy");
    }



    // fixme hyeyeon
    @Override
    public void onBackPressed() {

        if (mOnKeyBackPressedListener != null) {
            mOnKeyBackPressedListener.onBackKey();
        }
        else {
            Log.e("kkankkan", "메인엑티비티 onbackpressed");
            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            builder.setMessage("앱을 종료하시겠습니까?");
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                    System.exit(0);
                    android.os.Process.killProcess(android.os.Process.myPid());
                }
            });

            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                }
            });

            AlertDialog alertDialog = builder.create();
            alertDialog.show();
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
}

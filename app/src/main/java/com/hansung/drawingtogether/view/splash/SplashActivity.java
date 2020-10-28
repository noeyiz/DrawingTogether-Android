package com.hansung.drawingtogether.view.splash;

import android.animation.Animator;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.airbnb.lottie.LottieAnimationView;
import com.hansung.drawingtogether.R;
import com.hansung.drawingtogether.data.remote.model.MyLog;
import com.hansung.drawingtogether.data.remote.model.TaskService;
import com.hansung.drawingtogether.view.main.MainActivity;


public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        /* 앱 중복 실행 검사 */
        if (MainActivity.context != null) {
            Log.e("스플래쉬 액티비티", "context exist");
            showRunningAlert("앱 중복 실행", "이미 실행 중인 앱이 존재합니다.\n" +
                    "최근에 사용한 앱 리스트에 앱이 존재 하는지 확인해 주세요.");
            return;
        }

        /* Task Service 실행 */
        startService(new Intent(this, TaskService.class));

        /* Splash Animation */

//        LottieAnimationView animationView = findViewById(R.id.intro);
//        animationView.addAnimatorListener(new Animator.AnimatorListener() {
//            @Override
//            public void onAnimationStart(Animator animation) {
//
//            }
//
//            @Override
//            public void onAnimationEnd(Animator animation) {
//
//                /* 카카오 링크를 통해 앱이 실행되었는지 검사 */
//                Intent kakaoIntent = getIntent();
//                Uri uri = null;
//                String kakaoTopic = null, kakaoPassword = null;
//                if (kakaoIntent != null) {
//                    uri = kakaoIntent.getData();
//
//                    if (uri != null) {
//                        kakaoTopic = uri.getQueryParameter("topic");
//                        kakaoPassword = uri.getQueryParameter("password");
//                    }
//                }
//
//                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
//                intent.putExtra("state", "launch");
//
//                /* 카카오 링크를 통해 앱을 실행한 경우 */
//                if (kakaoTopic != null && kakaoPassword != null) {
//                    intent.putExtra("kakaoTopic", kakaoTopic);
//                    intent.putExtra("kakaoPassword", kakaoPassword);
//
//                    MyLog.i("KakaoLink", "SplashActivity " + kakaoTopic + "/" + kakaoPassword);
//                }
//
//                /* MainActivity로 topic, password 전달 */
//                startActivity(intent);
//                finish();
//            }
//
//            @Override
//            public void onAnimationCancel(Animator animation) {
//
//            }
//
//            @Override
//            public void onAnimationRepeat(Animator animation) {
//
//            }
//        });

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(3000);
                    /* 카카오 링크를 통해 앱이 실행되었는지 검사 */
                    Intent kakaoIntent = getIntent();
                    Uri uri = null;
                    String kakaoTopic = null, kakaoPassword = null;
                    if (kakaoIntent != null) {
                        uri = kakaoIntent.getData();

                        if (uri != null) {
                            kakaoTopic = uri.getQueryParameter("topic");
                            kakaoPassword = uri.getQueryParameter("password");
                        }
                    }

                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    intent.putExtra("state", "launch");

                    /* 카카오 링크를 통해 앱을 실행한 경우 */
                    if (kakaoTopic != null && kakaoPassword != null) {
                        intent.putExtra("kakaoTopic", kakaoTopic);
                        intent.putExtra("kakaoPassword", kakaoPassword);

                        MyLog.i("KakaoLink", "SplashActivity " + kakaoTopic + "/" + kakaoPassword);
                    }

                    /* MainActivity로 topic, password 전달 */
                    startActivity(intent);
                    finish();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void showRunningAlert(String title, String message) {

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .create();

        dialog.show();

    }
}

package com.hansung.drawingtogether.view.splash;

<<<<<<< HEAD
=======
import android.animation.Animator;
>>>>>>> 3e197793e3f110e598dfb21e492cdc5cb8e3ae19
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

<<<<<<< HEAD
=======
import com.airbnb.lottie.LottieAnimationView;
import com.hansung.drawingtogether.R;
import com.hansung.drawingtogether.data.remote.model.MyLog;
>>>>>>> 3e197793e3f110e598dfb21e492cdc5cb8e3ae19
import com.hansung.drawingtogether.data.remote.model.TaskService;
import com.hansung.drawingtogether.view.main.MainActivity;


public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        startService(new Intent(this, TaskService.class));

        LottieAnimationView animationView = findViewById(R.id.intro);
        animationView.addAnimatorListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
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

                MyLog.e("kakao", "스플레시 " + kakaoTopic + "/" + kakaoPassword);

                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                intent.putExtra("state", "launch");
                if (!(kakaoTopic == null) && !(kakaoPassword == null)) {
                    intent.putExtra("kakaoTopic", kakaoTopic);
                    intent.putExtra("kakaoPassword", kakaoPassword);
                }
                startActivity(intent);
                finish();
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
    }
}

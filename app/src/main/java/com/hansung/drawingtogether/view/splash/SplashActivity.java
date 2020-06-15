package com.hansung.drawingtogether.view.splash;

import android.animation.Animator;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.airbnb.lottie.LottieAnimationView;
import com.hansung.drawingtogether.R;
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
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                intent.putExtra("state", "launch");
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

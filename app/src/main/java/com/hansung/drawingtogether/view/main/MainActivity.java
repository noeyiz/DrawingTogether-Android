package com.hansung.drawingtogether.view.main;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.MenuItem;
import android.view.Window;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.hansung.drawingtogether.R;
import com.hansung.drawingtogether.view.drawing.DrawingEditor;
import com.hansung.drawingtogether.view.drawing.Mode;
import com.hansung.drawingtogether.view.drawing.Text;
import com.kakao.util.helper.Utility;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static com.kakao.util.maps.helper.Utility.getPackageInfo;

public class MainActivity extends AppCompatActivity {

    private String topicPassword="";
    private Toolbar toolbar;
    private TextView title;

    //private DrawingEditor de = DrawingEditor.getInstance();

    public interface OnBackListener {
        public void onBack();
    }

    private OnBackListener onBackListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
    public void onBackPressed() {
       /* // fixme nayeon [ 뒤로가기 이벤트 != 키보드 내리기 이벤트 ]
        if(de.getCurrentMode().equals(Mode.TEXT)) {
            Text text = de.getCurrentText();
            text.changeEditTextToTextView();
        }*/

    }

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

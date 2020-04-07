package com.hansung.drawingtogether.data.remote.model;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Looper;
import android.provider.ContactsContract;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.hansung.drawingtogether.view.drawing.DrawingEditor;
import com.hansung.drawingtogether.view.drawing.JSONParser;
import com.hansung.drawingtogether.view.drawing.MqttMessageFormat;
import com.hansung.drawingtogether.view.main.DeleteMessage;
import com.hansung.drawingtogether.view.main.ExitMessage;
import com.hansung.drawingtogether.view.main.MainActivity;

import org.eclipse.paho.client.mqttv3.MqttException;

import java.io.File;

// 비 정상 종료 시 실행될 핸들러
public class AbnormalTerminationHandler
        implements Thread.UncaughtExceptionHandler {

    private Logger logger = Logger.getInstance();
    private DrawingEditor de = DrawingEditor.getInstance();
    private MQTTClient client = MQTTClient.getInstance();

    private FirebaseStorage storage = FirebaseStorage.getInstance();
    private StorageReference storageRef = storage.getReference();

    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private DatabaseReference databaseRef = database.getReference();

    private ProgressDialog progressDialog;

    @Override
    public void uncaughtException(Thread thread, Throwable e) {
        Log.e("terminate", "Abnormal Termination Handler");
        Log.e("why", "abnormalTermiation");

        databaseRef.child(client.getTopic()).runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData mutableData) {
                if (mutableData.getValue() != null && client.isMaster()) {
                    mutableData.setValue(null);
                }
                if (mutableData.getValue() != null && !client.isMaster()) {
                    mutableData.child("username").child(client.getMyName()).setValue(null);
                }
                Log.e("transaction", "transaction success");
                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError databaseError, boolean b, @Nullable DataSnapshot dataSnapshot) {
                Log.e("transaction", "transaction complete");
                if (databaseError != null) {
                    Log.e("transaction", databaseError.getDetails());
                    return;
                }
                if (client.isMaster()) {
                    DeleteMessage deleteMessage = new DeleteMessage(client.getMyName());
                    MqttMessageFormat messageFormat = new MqttMessageFormat(deleteMessage);
                    client.publish(client.getTopic() + "_delete", JSONParser.getInstance().jsonWrite(messageFormat)); // fixme hyeyeon
                    client.setExitPublish(true);
                    client.exitTask();
                } else {
                    ExitMessage exitMessage = new ExitMessage(client.getMyName());
                    MqttMessageFormat messageFormat = new MqttMessageFormat(exitMessage);
                    client.publish(client.getTopic() + "_exit", JSONParser.getInstance().jsonWrite(messageFormat));
                    client.setExitPublish(true);
                    client.exitTask();
                }
            }
        });

/*        try {
            client.getClient().unsubscribe(client.getTopic_data()); // data topic 구독 취소
        } catch (MqttException me) { me.printStackTrace(); }*/

        logger.loggingUncaughtException(thread, e.getStackTrace()); // 발생한 오류에 대한 메시지 로그에 기록

        String filename = client.getTopic() + File.separator + de.getMyUsername() + ".log";

        File logFile = logger.createLogFile();
        Uri fileUri = Uri.fromFile(logFile); // File to Uri
        StorageReference logRef = storageRef.child("log/" + filename); // 스토리지 업로드 경로 지정

        Log.e("storage", "file path = " + "log/" + filename);

        UploadTask uploadTask = logRef.putFile(fileUri); // 파이어베이스 스토리지에 파일 업로드

        setProgressDialog(); // 다이얼로그 설정
        new UploadProgressDialogShowThread(progressDialog).start(); // 다이얼로그 보이기

        while(!uploadTask.isSuccessful()); // 파이어베이스에 업로드 될 때까지 기다림

        logger.deleteLogFile(logFile); // 스토리지 업로드 위해 파일 Uri 생성 후 외부 저장소에 저장된 파일 삭제하기

        new UploadProgressDialogDismissThread(progressDialog).start(); // 다이얼로그 끝내기

        new ErrorAlertDialogThread().start(); // 오류 메시지를 보여주는 알림창 띄우기
    }

    private void setProgressDialog() {
        progressDialog = new ProgressDialog(MainActivity.context);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setTitle("오류 발생");
        progressDialog.setMessage("로그 파일 업로드 중");
        progressDialog.setCancelable(false);
    }
}

class UploadProgressDialogShowThread extends Thread {

    private ProgressDialog progressDialog;

    public UploadProgressDialogShowThread(ProgressDialog progressDialog) {
        this.progressDialog = progressDialog;
    }

    @Override
    public void run() {
        Looper.prepare();

        progressDialog.show();

        Looper.loop();
    }
}

class UploadProgressDialogDismissThread extends Thread {

    private ProgressDialog progressDialog;

    public UploadProgressDialogDismissThread(ProgressDialog progressDialog) {
        this.progressDialog = progressDialog;
    }

    @Override
    public void run() {
        Looper.prepare();

        progressDialog.dismiss();

        Looper.loop();
    }

}

class ErrorAlertDialogThread extends Thread {

    private Logger logger = Logger.getInstance();

    @Override
    public void run() {
        Looper.prepare();

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.context);
        builder.setTitle("오류");
        builder.setMessage(logger.getUncaughtException());
        builder.setCancelable(false); // 다른 영역 터치 시 다이얼로그가 사라지지 않도록

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(10);
            }
        });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();

        Looper.loop();
    }
}
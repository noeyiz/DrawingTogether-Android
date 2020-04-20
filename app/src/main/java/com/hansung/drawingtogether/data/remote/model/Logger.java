package com.hansung.drawingtogether.data.remote.model;


import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Environment;
import android.os.Looper;
import android.util.Log;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.hansung.drawingtogether.view.drawing.DrawingEditor;
import com.hansung.drawingtogether.view.main.MainActivity;

import org.eclipse.paho.client.mqttv3.MqttException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import lombok.Getter;

// Logger Class
@Getter
public enum Logger {
    INSTANCE;

    private DrawingEditor de = DrawingEditor.getInstance();
    private MQTTClient client = MQTTClient.getInstance();

    private FirebaseStorage storage = FirebaseStorage.getInstance();
    private StorageReference storageRef = storage.getReference();

    private ProgressDialog progressDialog;

    private String log = "";
    private String uncaughtException = "";

    private boolean isAbnormalTerminated = false;


    public static Logger getInstance() { return INSTANCE; }

    public void info(String tag, String message) { log += "[ INFO " + tag  + " ] : " + new LogContent(message).toString() + "\n"; }

    public void warn(String tag, String message) { log += "[ WARN " + tag + " ] : " + new LogContent(message).toString() + "\n"; }

    public void error(String tag, String message) { log += "[ ERROR " + tag + " ] : " + new LogContent(message).toString() + "\n"; }

    public void loggingUncaughtException(Thread thread, StackTraceElement[] ste) {

        uncaughtException += "\n\n" + "[ Thread Name ]\n" + thread.getName() + "\n\n";

        uncaughtException += "[ Print Stack Trace ]\n";
        for(int i=0; i< ste.length; i++)
            uncaughtException += ste[i].toString() + "\n";

    }


    public File createLogFile() {

        File logFile = new File(Environment.getExternalStorageDirectory() + File.separator + de.getMyUsername() + ".log");
        Log.e("file", Environment.getExternalStorageDirectory() + File.separator + de.getMyUsername() + ".log");

        try {
           FileWriter fw = new FileWriter(logFile);
           fw.write(log + uncaughtException);

           fw.close();

           return logFile;

        } catch (IOException io) { io.printStackTrace(); }

        return null;
    }

    public void deleteLogFile(File file) {

        if(file.exists()) file.delete(); // 클라우드에 업로드 후, 외부 저장소에 남기지 않기 위해

    }

    // Upload Log File To Firebase Storage
    public void uploadLogFile(ExitType exitType) {
        try {
            client.getClient().unsubscribe(client.getTopic_data()); // data topic 구독 취소
        } catch (MqttException me) { me.printStackTrace(); }


        String filename = client.getTopic() + File.separator + de.getMyUsername() + ".log";

        File logFile = createLogFile();
        Uri fileUri = Uri.fromFile(logFile); // File to Uri
        StorageReference logRef = storageRef.child("log/" + filename); // 스토리지 업로드 경로 지정

        Log.e("storage", "file path = " + "log/" + filename);

        UploadTask uploadTask = logRef.putFile(fileUri); // 파이어베이스 스토리지에 파일 업로드

        setProgressDialog(); // 다이얼로그 설정
        // new UploadProgressDialogShowThread(progressDialog).start(); // 다이얼로그 보이기
        // progressDialog.show();


        while(!uploadTask.isSuccessful()) { System.out.println("."); } // 파이어베이스에 업로드 될 때까지 기다림

        deleteLogFile(logFile); // 스토리지 업로드 위해 파일 Uri 생성 후 외부 저장소에 저장된 파일 삭제하기

        //progressDialog.dismiss();


        // new UploadProgressDialogDismissThread(progressDialog).start(); // 다이얼로그 끝내기

        if(exitType == ExitType.ABNORMAL) {
            new ErrorAlertDialogThread().start(); // 오류 메시지를 보여주는 알림창 띄우기
        }

        Log.e("logger", "***********************************");
    }

    private void setProgressDialog() {
        progressDialog = new ProgressDialog(MainActivity.context);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setTitle("오류 발생");
        progressDialog.setMessage("로그 파일 업로드 중");
        progressDialog.setCancelable(false);
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

            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    logger.info("button", "error dialog ok button click"); // fixme nayeon

                    android.os.Process.killProcess(android.os.Process.myPid());
                    System.exit(10);
                }
            });

            AlertDialog alertDialog = builder.create();
            alertDialog.show();
            logger.info("uncaught exception", "error dialog show"); // fixme nayeon

            Looper.loop();
        }
    }

}


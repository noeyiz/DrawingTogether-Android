package com.hansung.drawingtogether.view.drawing;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;

import com.google.firebase.database.DatabaseError;
import com.hansung.drawingtogether.R;
import com.hansung.drawingtogether.data.remote.model.MQTTClient;
import com.hansung.drawingtogether.data.remote.model.MyLog;
import com.hansung.drawingtogether.view.main.DatabaseTransaction;
import com.hansung.drawingtogether.view.main.MQTTSettingData;
import com.hansung.drawingtogether.view.main.MainActivity;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter

/* 회의방 종료, 퇴장시 수행 */
public class ExitOnClickListener implements DialogInterface.OnClickListener {

    private MainActivity mainActivity = (MainActivity) MainActivity.context;
    private MQTTClient client = MQTTClient.getInstance();
    private MQTTSettingData data = MQTTSettingData.getInstance();

    private DrawingViewModel drawingViewModel;
    private boolean rightBottomBackPressed;
    private ProgressDialog progressDialog;

    @Override
    /* 백버튼 - 확인 클릭 시 */
    public void onClick(DialogInterface dialog, int which) {

        showExitProgressDialog();

        /* 네트워크 연결 상태 확인 */
        ConnectivityManager cm = (ConnectivityManager) MainActivity.context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm.getActiveNetwork() == null) {
            MyLog.i("네트워크", "network disconnected");

            if (rightBottomBackPressed) {
                /* 앱 종료 */
                mainActivity.finish();
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(10);
                return;
            }
            else {
                /* 메인 화면으로 이동 */
                drawingViewModel.back();
                return;
            }
        }
//        else if (cm.getActiveNetwork() != null && client.getClient().isConnected()) {
//            logger.uploadLogFile(ExitType.NORMAL);
//        }

        String mode = "";
        if (data.isMaster()) {
            mode = "masterMode";
        }
        else {
            mode = "joinMode";
        }
        /* Firebase Realtime Database Transaction 수행 */
        DatabaseTransaction dt = new DatabaseTransaction() {
            @Override
            public void completeLogin(DatabaseError error, String masterName, boolean topicError, boolean passwordError, boolean nameError) {  }

            @Override
            public void completeExit(DatabaseError error) {

                if (error != null) {
                    progressDialog.dismiss();
                    showDatabaseErrorAlert("데이터베이스 오류 발생", error.getMessage());
                    MyLog.i("Database transaction", error.getDetails());
                    return;
                }

                if (client.getClient().isConnected()) {
                    client.exitTask();
                }
                if (rightBottomBackPressed) {
                    /* 앱 종료 */
                    mainActivity.finish();
                    android.os.Process.killProcess(android.os.Process.myPid());
                    System.exit(10);
                    return;
                }
                else {
                    /* 메인 화면으로 이동 */
                    drawingViewModel.back();
                    return;
                }
            }
        };
        dt.runTransactionExit(data.getTopic(), data.getName(), mode);
    }

    public void showExitProgressDialog() {
        progressDialog = new ProgressDialog(MainActivity.context, R.style.MyProgressDialogStyle);
        progressDialog.setMessage("Loading...");
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.show();
    }


    /* Firebase Realtime Database Transaction 수행 중 오류 발생 알림 */
    public void showDatabaseErrorAlert(String title, String message) {

        AlertDialog dialog = new AlertDialog.Builder(MainActivity.context)
                .setTitle(title)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .create();

        dialog.show();

    }
}



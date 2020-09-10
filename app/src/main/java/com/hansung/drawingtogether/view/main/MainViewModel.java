
package com.hansung.drawingtogether.view.main;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.util.Log;

import android.view.View;

import androidx.lifecycle.MutableLiveData;

import com.google.firebase.database.DatabaseError;

import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.TedPermission;
import com.hansung.drawingtogether.R;
import com.hansung.drawingtogether.data.remote.model.MQTTClient;
import com.hansung.drawingtogether.data.remote.model.MyLog;
import com.hansung.drawingtogether.view.BaseViewModel;

import java.util.List;

public class MainViewModel extends BaseViewModel {

    private MutableLiveData<String> ip = new MutableLiveData<>();
    private MutableLiveData<String> port = new MutableLiveData<>();

    /* 사용자로부터 입력 받은 데이터를 저장 하기 위한 변수 */
    private MutableLiveData<String> topic = new MutableLiveData<>();
    private MutableLiveData<String> password = new MutableLiveData<>();
    private MutableLiveData<String> name = new MutableLiveData<>();

    private MutableLiveData<String> ipError = new MutableLiveData<>();
    private MutableLiveData<String> portError = new MutableLiveData<>();

    /* 에러 메시지를 출력 하기 위한 변수 */
    private MutableLiveData<String> topicError = new MutableLiveData<>();
    private MutableLiveData<String> passwordError = new MutableLiveData<>();
    private MutableLiveData<String> nameError = new MutableLiveData<>();

    /* 특수 문자, 공백을 포함하는지 검사하기 위한 변수 */
    private boolean hasSpecialCharacterAndBlank;

    /* MQTTClient init시 필요한 데이터를 저장하는 객체 */
    private MQTTSettingData data = MQTTSettingData.getInstance();

    /* MQTTClient 객체 */
    private MQTTClient client = MQTTClient.getInstance();

    private ProgressDialog progressDialog;

    public MainViewModel() {

        MyLog.i("LifeCycle", "MainViewModel init()");

        /* 변수 초기화 */
        ip.setValue("54.180.154.63"); // 클라우드
//      ip.setValue("192.168.0.36"); // 모니터링
        port.postValue("1883");
        setTopic("");
        setPassword("");
        setName("");

        setIpError("");
        setTopicError("");
        setPasswordError("");
        setNameError("");

        hasSpecialCharacterAndBlank = false;
    }

    /* 특수 문자, 공백을 포함 검사 */
    public void hasSpecialCharacterAndBlank() {

        if (ip.getValue().equals("")) {
            setIpError("아이피 주소를 입력해주세요");
            hasSpecialCharacterAndBlank = true;
        }

        if (port.getValue().equals("")) {
            setIpError("포트 번호를 입력해주세요");
            hasSpecialCharacterAndBlank = true;
        }

        if (topic.getValue().equals("")) {
            setTopicError("빈칸을 채워주세요");
            hasSpecialCharacterAndBlank = true;
        }

        if (password.getValue().equals("")) {
            setPasswordError("빈칸을 채워주세요");
            hasSpecialCharacterAndBlank = true;
        }

        if (name.getValue().equals("")) {
            setNameError("빈칸을 채워주세요");
            hasSpecialCharacterAndBlank = true;
        }

        if (!topic.getValue().matches("[0-9|a-z|A-Z|ㄱ-ㅎ|ㅏ-ㅣ|가-힝]*")) {
            setTopicError("특수문자를 포함하면 안됩니다");
            hasSpecialCharacterAndBlank = true;
        }

        if (!password.getValue().matches("[0-9|a-z|A-Z|ㄱ-ㅎ|ㅏ-ㅣ|가-힝]*")) {
            setPasswordError("특수문자를 포함하면 안됩니다");
            hasSpecialCharacterAndBlank = true;
        }

        if (!name.getValue().matches("[0-9|a-z|A-Z|ㄱ-ㅎ|ㅏ-ㅣ|가-힝]*")) {
            setNameError("특수문자를 포함하면 안됩니다");
            hasSpecialCharacterAndBlank = true;
        }

        for (int i=0; i<topic.getValue().length(); i++) {
            if (topic.getValue().charAt(i) == ' ') {
                setTopicError("공백을 포함하면 안됩니다");
                hasSpecialCharacterAndBlank = true;
            }
        }

        for (int i=0; i<password.getValue().length(); i++) {
            if (password.getValue().charAt(i) == ' ') {
                setPasswordError("공백을 포함하면 안됩니다");
                hasSpecialCharacterAndBlank = true;
            }
        }

        for (int i=0; i<name.getValue().length(); i++) {
            if (name.getValue().charAt(i) == ' ') {
                setNameError("공백을 포함하면 안됩니다");
                hasSpecialCharacterAndBlank = true;
            }
        }
    }

    /* 마스터 로그인 버튼 클릭 시 */
    public void masterLoginClicked(final View view) {

        setIpError("");
        setPortError("");
        setTopicError("");
        setPasswordError("");
        setNameError("");
        hasSpecialCharacterAndBlank = false;

        hasSpecialCharacterAndBlank();

        MyLog.i("Input Data", topic.getValue() + " / " + password.getValue() + " / " + name.getValue());

        if (!hasSpecialCharacterAndBlank) {

            /* 마스터가 맞는지 확인하는 AlertDialog */
            AlertDialog dialog = new AlertDialog.Builder(MainActivity.context)
                    .setTitle("마스터 체크")
                    .setMessage("마스터가 맞습니까?")
                    .setCancelable(false)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            afterMasterCheck(view);
                        }
                    })
                    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    })
                    .create();

            dialog.show();
        }
    }

    /* 마스터인 경우 수행하는 함수 */
    public void afterMasterCheck(View view) {

        /* 네트워크 연결 상태 확인 */
        ConnectivityManager cm = (ConnectivityManager) MainActivity.context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm.getActiveNetwork() == null) {
            Log.e("네트워크", "network disconnected");
            showNetworkAlert("네트워크 연결 오류", "네트워크가 연결되어 있는지 확인해주세요.");
            return;
        }

        progressDialog = new ProgressDialog(view.getContext(), R.style.MyProgressDialogStyle);
        progressDialog.setMessage("Loading...");
        progressDialog.setCanceledOnTouchOutside(true);
        client.setProgressDialog(progressDialog);
        progressDialog.show();

        /* Firebase Realtime Database Transaction 수행 */
        DatabaseTransaction dt = new DatabaseTransaction() {
            @Override
            public void completeExit(DatabaseError error) { }

            @Override
            public void completeLogin(DatabaseError error, String masterName, boolean topicError, boolean passwordError, boolean nameError) {

                Log.i("Database Transaction", "Transaction Complete");

                if (error != null) {
                    progressDialog.dismiss();
                    Log.e("Database Transaction", error.getMessage());
                    showDatabaseErrorAlert("데이터베이스 오류 발생", error.getMessage());
                    return;
                }

                if (topicError) {
                    progressDialog.dismiss();
                    setTopicError("이미 존재하는 회의명입니다.");
                }
                else {
                    data.setIp(ip.getValue());
                    data.setPort(port.getValue());
                    data.setTopic(topic.getValue());
                    data.setPassword(password.getValue());
                    data.setName(name.getValue());
                    data.setMasterName(masterName);
                    data.setMaster(true);

                    clearData();

                    navigate(R.id.action_mainFragment_to_drawingFragment);
                }
            }
        };
        dt.runTransactionLogin(topic.getValue(), password.getValue(), name.getValue(), "masterMode");
    }

    /* 조인 버튼 클릭 시 */
    public void joinClicked(View view) {

        setIpError("");
        setPortError("");
        setTopicError("");
        setPasswordError("");
        setNameError("");
        hasSpecialCharacterAndBlank = false;

        hasSpecialCharacterAndBlank();

        MyLog.i("Input Data", topic.getValue() + " / " + password.getValue() + " / " + name.getValue());

        if (!hasSpecialCharacterAndBlank) {

            /* 네트워크 연결 상태 확인 */
            ConnectivityManager cm = (ConnectivityManager) MainActivity.context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm.getActiveNetwork() == null) {
                Log.e("네트워크", "network disconnected");
                showNetworkAlert("네트워크 연결 오류", "네트워크가 연결되어 있는지 확인해주세요.");
                return;
            }

            progressDialog = new ProgressDialog(view.getContext(), R.style.MyProgressDialogStyle);
            progressDialog.setMessage("Loading...");
            progressDialog.setCanceledOnTouchOutside(true); // fixme - master 없는 topic 의 경우 빠져나오지를 못해서 잠시 cancel 가능하게 수정
            client.setProgressDialog(progressDialog);
            progressDialog.show();

            /* Firebase Realtime Database Transaction 수행 */
            DatabaseTransaction dt = new DatabaseTransaction() {
                @Override
                public void completeExit(DatabaseError error) { }

                @Override
                public void completeLogin(DatabaseError error, String masterName, boolean topicError, boolean passwordError, boolean nameError) {

                    Log.i("Database Transaction", "Transaction Complete");

                    if (error != null) {
                        progressDialog.dismiss();
                        Log.e("Database Transaction", error.getMessage());
                        showDatabaseErrorAlert("데이터베이스 오류 발생", error.getMessage());
                        return;
                    }

                    if (passwordError) {
                        progressDialog.dismiss();
                        setPasswordError("비밀번호가 일치하지 않습니다.");
                        return;
                    }
                    if (nameError) {
                        progressDialog.dismiss();
                        setNameError("이미 사용중인 이름입니다.");
                        return;
                    }
                    if (topicError) {
                        data.setIp(ip.getValue());
                        data.setPort(port.getValue());
                        data.setTopic(topic.getValue());
                        data.setPassword(password.getValue());
                        data.setName(name.getValue());
                        data.setMasterName(masterName);
                        data.setMaster(false);

                        clearData();

                        navigate(R.id.action_mainFragment_to_drawingFragment);
                    }
                    else {
                        progressDialog.dismiss();
                        setTopicError("존재하지 않는 회의명입니다");
                    }
                }
            };
            dt.runTransactionLogin(topic.getValue(), password.getValue(), name.getValue(), "joinMode");
        }
    }

    /* 네트워크 연결 상태 불안정 알림 */
    public void showNetworkAlert(String title, String message) {

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

    /* 입력 데이터, 에러 출력 메시지 초기화 */
    public void clearData() {
        setTopic("");
        setPassword("");
        setName("");

        setTopicError("");
        setPasswordError("");
        setNameError("");
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

    /* 마이크, 저장 공간, 카메라 권한 체크 */
    /* 권한 거부 시 앱 종료 */
    public void checkPermission(final Context context) {
        PermissionListener permissionListener = new PermissionListener() {
            @Override
            public void onPermissionGranted() { }
            @Override
            public void onPermissionDenied(List<String> deniedPermissions) {
                if (deniedPermissions.size() > 0) {
                    ((MainActivity)MainActivity.context).finish();
                    android.os.Process.killProcess(android.os.Process.myPid());
                    System.exit(10);
                }
            }
        };

        TedPermission.with(context)
                .setPermissionListener(permissionListener)
                .setDeniedMessage(context.getResources().getString(R.string.permission))
                .setPermissions(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO)
                .check();
    }

    public void showLocalHistory(View view) {
        navigate(R.id.action_mainFragment_to_historyFragment);
    }

    /* GETTER */
    public MutableLiveData<String> getTopic() { return topic; }

    public MutableLiveData<String> getPassword() { return password; }

    public MutableLiveData<String> getName() { return name; }

    public MutableLiveData<String> getTopicError() { return topicError; }

    public MutableLiveData<String> getPasswordError() { return passwordError; }

    public MutableLiveData<String> getNameError() { return nameError; }

    /* SETTER */
    public void setTopic(String text) { this.topic.postValue(text); }

    public void setPassword(String text) { this.password.postValue(text); }

    public void setName(String text) { this.name.postValue(text); }

    public void setIpError(String ip) { this.ipError.postValue(ip); }

    public void setPortError(String port) { this.portError.postValue(port); }

    public void setTopicError(String text) { this.topicError.postValue(text); }

    public void setPasswordError(String text) { this.passwordError.postValue(text); }

    public void setNameError(String text) { this.nameError.postValue(text); }

    @Override
    public void onCleared() {
        super.onCleared();
        Log.i("LifeCycle", "MainViewModel onCleared()");
    }

}
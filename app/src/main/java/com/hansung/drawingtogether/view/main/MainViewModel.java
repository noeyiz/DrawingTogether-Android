
package com.hansung.drawingtogether.view.main;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.util.Log;

import android.view.View;

import androidx.lifecycle.MutableLiveData;

import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import com.hansung.drawingtogether.R;
import com.hansung.drawingtogether.data.remote.model.MQTTClient;
import com.hansung.drawingtogether.data.remote.model.MyLog;
import com.hansung.drawingtogether.view.BaseViewModel;

public class MainViewModel extends BaseViewModel {

    private MutableLiveData<String> topic = new MutableLiveData<>();
    private MutableLiveData<String> password = new MutableLiveData<>();
    private MutableLiveData<String> name = new MutableLiveData<>();

    private MutableLiveData<String> ipError = new MutableLiveData<>();
    private MutableLiveData<String> portError = new MutableLiveData<>();
    private MutableLiveData<String> topicError = new MutableLiveData<>();
    private MutableLiveData<String> passwordError = new MutableLiveData<>();
    private MutableLiveData<String> nameError = new MutableLiveData<>();

    public final MutableLiveData<String> ip = new MutableLiveData<>();
    public final MutableLiveData<String> port = new MutableLiveData<>();

    private MQTTSettingData data = MQTTSettingData.getInstance();

    private boolean hasSpecialCharacterAndBlank;

    private MQTTClient client = MQTTClient.getInstance();
    private ProgressDialog progressDialog;

    public MainViewModel() {

        MyLog.e("kkankkan", "메인뷰모델 생성자");

        ip.setValue("54.180.154.63");
        port.postValue("1883");
        setTopic("");
        setPassword("");
        setName("");

        setIpError("");
        setTopicError("");
        setPasswordError("");
        setNameError("");

        MyLog.e("kkankkan", "메인뷰모델 초기화 완료");

        hasSpecialCharacterAndBlank = false;
    }

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

    public void masterLoginClicked(final View view) {

        setIpError("");
        setPortError("");
        setTopicError("");
        setPasswordError("");
        setNameError("");
        hasSpecialCharacterAndBlank = false;

        hasSpecialCharacterAndBlank();

        MyLog.e("kkankkan", topic.getValue() + " / " + password.getValue() + " / " + name.getValue());

        if (!hasSpecialCharacterAndBlank) {

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

    public void afterMasterCheck(View view) {

        ConnectivityManager cm = (ConnectivityManager) MainActivity.context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm.getActiveNetwork() == null) {
            Log.e("네트워크", "network disconnected");
            showNetworkAlert("네트워크 연결 오류", "네트워크가 연결되어 있는지 확인해주세요.");
            return;
        }

        progressDialog = new ProgressDialog(view.getContext(), R.style.MyProgressDialogStyle);
        progressDialog.setMessage("Loading...");
        progressDialog.setCanceledOnTouchOutside(true);    //fixme minj - master 없는 topic 의 경우 빠져나오지를 못해서 잠시 cancel 가능하게 수정
        client.setProgressDialog(progressDialog);
        progressDialog.show();

        DatabaseTransaction dt = new DatabaseTransaction() {
            @Override
            public void completeExit(DatabaseError error) { }

            @Override
            public void completeLogin(DatabaseError error, String masterName, boolean topicError, boolean passwordError, boolean nameError) {
                //progressDialog.dismiss();

                Log.e("dt", "interface complete");

                if (error != null) {
                    progressDialog.dismiss();
                    Log.e("dt", "error");
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
                    data.setMasterName(masterName);  // fixme hyeyeon
                    data.setMaster(true);

                    clearData();

                    navigate(R.id.action_mainFragment_to_drawingFragment);
                }
            }
        };
        dt.runTransactionLogin(topic.getValue(), password.getValue(), name.getValue(), "masterMode");
    }

    public void joinClicked(View view) {

        setIpError("");
        setPortError("");
        setTopicError("");
        setPasswordError("");
        setNameError("");
        hasSpecialCharacterAndBlank = false;

        hasSpecialCharacterAndBlank();

        MyLog.e("kkankkan", topic.getValue() + " / " + password.getValue() + " / " + name.getValue());

        if (!hasSpecialCharacterAndBlank) {

            ConnectivityManager cm = (ConnectivityManager) MainActivity.context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm.getActiveNetwork() == null) {
                Log.e("네트워크", "network disconnected");
                showNetworkAlert("네트워크 연결 오류", "네트워크가 연결되어 있는지 확인해주세요.");
                return;
            }

            progressDialog = new ProgressDialog(view.getContext(), R.style.MyProgressDialogStyle);
            progressDialog.setMessage("Loading...");
            progressDialog.setCanceledOnTouchOutside(true);    //fixme minj - master 없는 topic 의 경우 빠져나오지를 못해서 잠시 cancel 가능하게 수정
            client.setProgressDialog(progressDialog);
            progressDialog.show();

            DatabaseTransaction dt = new DatabaseTransaction() {
                @Override
                public void completeExit(DatabaseError error) { }

                @Override
                public void completeLogin(DatabaseError error, String masterName, boolean topicError, boolean passwordError, boolean nameError) {
                    //progressDialog.dismiss();

                    Log.e("dt", "interface complete");

                    if (error != null) {
                        progressDialog.dismiss();
                        Log.e("dt", "error");
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
                        data.setMasterName(masterName);  // fixme hyeyeon
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

    public void clearData() {
        setTopic("");
        setPassword("");
        setName("");

        setIpError("");
        setTopicError("");
        setPasswordError("");
        setNameError("");
    }

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

    public void showLocalHistory(View view) {
        navigate(R.id.action_mainFragment_to_historyFragment);
    }

    public MutableLiveData<String> getTopic() {
        return topic;
    }

    public MutableLiveData<String> getPassword() {
        return password;
    }

    public MutableLiveData<String> getName() {
        return name;
    }

    public MutableLiveData<String> getIpError() {
        return ipError;
    }

    public MutableLiveData<String> getPortError() {
        return portError;
    }

    public MutableLiveData<String> getTopicError() {
        return topicError;
    }

    public MutableLiveData<String> getPasswordError() {
        return passwordError;
    }

    public MutableLiveData<String> getNameError() {
        return nameError;
    }

    public void setTopic(String text) {
        this.topic.postValue(text);
    }

    public void setPassword(String text) {
        this.password.postValue(text);
    }

    public void setName(String text) {
        this.name.postValue(text);
    }

    public void setIpError(String ip) {
        this.ipError.postValue(ip);
    }

    public void setPortError(String port) {
        this.portError.postValue(port);
    }

    public void setTopicError(String text) {
        this.topicError.postValue(text);
    }

    public void setPasswordError(String text) {
        this.passwordError.postValue(text);
    }

    public void setNameError(String text) {
        this.nameError.postValue(text);
    }

    // fixme hyeyeon[1]
    @Override
    public void onCleared() {  // todo
        super.onCleared();
        Log.i("lifeCycle", "MainViewModel onCleared()");
    }
}
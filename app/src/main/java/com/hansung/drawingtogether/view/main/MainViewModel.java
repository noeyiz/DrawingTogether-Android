
package com.hansung.drawingtogether.view.main;

import android.app.ProgressDialog;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;

import com.hansung.drawingtogether.R;
import com.hansung.drawingtogether.data.remote.model.MQTTClient;
import com.hansung.drawingtogether.data.remote.model.Log; // fixme nayeon
import com.hansung.drawingtogether.view.BaseViewModel;


import lombok.Setter;

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

    private FirebaseDatabase database;
    private DatabaseReference databaseReference;

    private MQTTSettingData data = MQTTSettingData.getInstance();

    private boolean hasSpecialCharacterAndBlank;

    private MQTTClient client = MQTTClient.getInstance();
    private ProgressDialog progressDialog;

    private TranscationHandler transcationHandler;

    private String masterName;  // fixme hyeyeon

    // fixme hyeyeon[1]
    @Override
    public void onCleared() {  // todo
        super.onCleared();
        Log.i("lifeCycle", "MainViewModel onCleared()");

        if (database != null) {
            database = null;
        }
        if (databaseReference != null) {
            databaseReference = null;
        }
        if (data != null) {
            data = null;
        }

    }
    //

    public MainViewModel() {

        Log.e("kkankkan", "메인뷰모델 생성자");

        ip.setValue("54.180.154.63");
        port.postValue("1883");
        setTopic("");
        setPassword("");
        setName("");

        setIpError("");
        setTopicError("");
        setPasswordError("");
        setNameError("");

        Log.e("kkankkan", "메인뷰모델 초기화 완료");

        hasSpecialCharacterAndBlank = false;

        database = FirebaseDatabase.getInstance();
        databaseReference = database.getReference();

        transcationHandler = new TranscationHandler();

        masterName = "";  // fixme hyeyeon
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

    public void startDrawing(View view) {
        setIpError("");
        setPortError("");
        setTopicError("");
        setPasswordError("");
        setNameError("");
        hasSpecialCharacterAndBlank = false;

        hasSpecialCharacterAndBlank();

        Log.e("kkankkan", topic.getValue() + " / " + password.getValue() + " / " + name.getValue());

        if (!hasSpecialCharacterAndBlank) {
            progressDialog = new ProgressDialog(view.getContext());
            progressDialog.setMessage("Loding...");
            progressDialog.setCanceledOnTouchOutside(true);    //fixme minj - master 없는 topic 의 경우 빠져나오지를 못해서 잠시 cancel 가능하게 수정
            client.setProgressDialog(progressDialog);
            progressDialog.show();

            switch (view.getId()) {
                case R.id.master_login:
                    transcationHandler.setMode("masterMode");
                    databaseReference.child(topic.getValue()).runTransaction(transcationHandler);
                    break;
                case R.id.join:
                    transcationHandler.setMode("joinMode");
                    databaseReference.child(topic.getValue()).runTransaction(transcationHandler);
                    break;
            }
        }
    }

    @Setter
    class TranscationHandler implements Transaction.Handler {
        private String mode = "";
        private String topicErrorMsg = "";
        private String pwdErrorMsg = "";
        private String nameErrorMsg = "";

        @NonNull
        @Override
        public Transaction.Result doTransaction(@NonNull MutableData mutableData) {
            topicErrorMsg = "";
            pwdErrorMsg = "";
            nameErrorMsg = "";
            if (mutableData.getValue() != null) {
                Log.e("transaction", "exist topic " + mutableData.getValue());
                switch (mode) {
                    case "masterMode":
                        topicErrorMsg = "이미 존재하는 토픽입니다";
                        break;
                    case "joinMode":
                        if (!mutableData.child("password").getValue().equals(password.getValue())) {
                            pwdErrorMsg = "비밀번호가 일치하지 않습니다";
                            break;
                        }
                        if (mutableData.child("username").hasChild(name.getValue())) {
                            nameErrorMsg = "이미 사용중인 이름입니다";
                            break;
                        }
                        else {
                            mutableData.child("username").child(name.getValue()).setValue(name.getValue());
                            masterName = mutableData.child("master").getValue().toString();  // fixme hyeyeon
                            Log.i("login", "masterName: " + masterName);
                            break;
                        }
                }
                Log.e("transaction", "transaction success");
                return Transaction.success(mutableData);
            }
            Log.e("transaction", "new topic " + mutableData.getChildrenCount());
            switch (mode) {
                case "masterMode":
                    mutableData.child("password").setValue(password.getValue());
                    mutableData.child("username").child(name.getValue()).setValue(name.getValue());
                    mutableData.child("master").setValue(name.getValue());
                    masterName = name.getValue();  // fixme hyeyeon
                    Log.i("login", "masterName: " + masterName);
                    break;
                case "joinMode":
                    topicErrorMsg = "존재하지 않는 토픽입니다";
                    break;
            }
            Log.e("transaction", "transaction success");
            return Transaction.success(mutableData);
        }

        @Override
        public void onComplete(@Nullable DatabaseError databaseError, boolean b, @Nullable DataSnapshot dataSnapshot) {
            Log.e("transaction", "transaction complete");
            if (databaseError != null) {
                Log.e("transaction", databaseError.getDetails());
                progressDialog.dismiss();
                return;
            }
            switch (mode) {
                case "masterMode":
                    if (!topicErrorMsg.equals("")) {
                        setTopicError(topicErrorMsg);
                        progressDialog.dismiss();
                        return;
                    }
                    break;
                case "joinMode":
                    if (!topicErrorMsg.equals("")) {
                        setTopicError(topicErrorMsg);
                        progressDialog.dismiss();
                        return;
                    }
                    if (!pwdErrorMsg.equals("")) {
                        setPasswordError(pwdErrorMsg);
                        progressDialog.dismiss();
                        return;
                    }
                    if (!nameErrorMsg.equals("")) {
                        setNameError(nameErrorMsg);
                        progressDialog.dismiss();
                        return;
                    }
                    break;
            }

            data.setIp(ip.getValue());
            data.setPort(port.getValue());
            data.setTopic(topic.getValue());
            data.setPassword(password.getValue());
            data.setName(name.getValue());
            data.setMasterName(masterName);  // fixme hyeyeon

            switch (mode) {
                case "masterMode":
                    data.setMaster(true);
                    break;
                case "joinMode":
                    data.setMaster(false);
                    break;
            }

            setTopic("");
            setPassword("");
            setName("");

            setIpError("");
            setTopicError("");
            setPasswordError("");
            setNameError("");

            navigate(R.id.action_mainFragment_to_drawingFragment);
        }
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

}
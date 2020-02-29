package com.hansung.drawingtogether.view.main;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.BindingAdapter;
import androidx.databinding.InverseBindingAdapter;
import androidx.databinding.InverseBindingMethod;
import androidx.databinding.InverseBindingMethods;
import androidx.lifecycle.MutableLiveData;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.hansung.drawingtogether.R;
import com.hansung.drawingtogether.data.remote.model.MQTTClient;
import com.hansung.drawingtogether.view.BaseViewModel;
import com.hansung.drawingtogether.view.drawing.MqttMessageFormat;
import com.hansung.drawingtogether.view.drawing.TextMode;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

public class MainViewModel extends BaseViewModel {

    private MutableLiveData<String> topic = new MutableLiveData<>();
    private MutableLiveData<String> password = new MutableLiveData<>();
    private MutableLiveData<String> name = new MutableLiveData<>();
    private MutableLiveData<Boolean> masterCheck = new MutableLiveData<>();

    private MutableLiveData<String> ipError = new MutableLiveData<>();
    private MutableLiveData<String> portError = new MutableLiveData<>();
    private MutableLiveData<String> topicError = new MutableLiveData<>();
    private MutableLiveData<String> passwordError = new MutableLiveData<>();
    private MutableLiveData<String> nameError = new MutableLiveData<>();

    public final MutableLiveData<String> ip = new MutableLiveData<>();
    public final MutableLiveData<String> port = new MutableLiveData<>();

    private FirebaseStorage storage;
    private StorageReference storageReference;

    private FirebaseDatabase database;
    private DatabaseReference databaseReference;
    private boolean existTopic;
    private boolean newName;
    private boolean newMaster;

    private MQTTSettingData data = MQTTSettingData.getInstance();

    private boolean hasSpecialCharacterAndBlank;

    private MQTTClient client = MQTTClient.getInstance();
    private ProgressDialog progressDialog;

    public MainViewModel() {

        Log.e("kkankkan", "메인뷰모델 생성자");

        ip.setValue("54.180.154.63");
        port.postValue("1883");
        setTopic("");
        setPassword("");
        setName("");
        setMasterCheck(false);

        setIpError("");
        setTopicError("");
        setPasswordError("");
        setNameError("");

        Log.e("kkankkan", "메인뷰모델 초기화 완료");

        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        hasSpecialCharacterAndBlank = false;

        database = FirebaseDatabase.getInstance();
        databaseReference = database.getReference();
        //Log.e("kkankkan", storage.toString());
        //Log.e("kkankkan", storageReference.toString());

        newName = false;
        newMaster = false;
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

        // fixme hyeyeon - getter로 값 가져오기 -> 직접 값 가져오기 (복잡해보여서)
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

        Log.e("kkankkan", hasSpecialCharacterAndBlank + "");

        Log.e("kkankkan", topic.getValue() + " / " + password.getValue() + " / " + name.getValue());

        if (!hasSpecialCharacterAndBlank) {
            progressDialog = new ProgressDialog(view.getContext());
            progressDialog.setMessage("Loding...");
            progressDialog.setCanceledOnTouchOutside(true);    //fixme minj - master 없는 topic 의 경우 빠져나오지를 못해서 잠시 cancel 가능하게 수정
            client.setProgressDialog(progressDialog);
            progressDialog.show();

            databaseReference.child(getTopic().getValue()).runTransaction(new Transaction.Handler() {
                @NonNull
                @Override
                public Transaction.Result doTransaction(@NonNull MutableData mutableData) {
                    existTopic = false;

                    if (mutableData.child("master").getValue() == null) {  // 새 토픽
                        mutableData.child("master").setValue(true);
                        mutableData.child("password").setValue(password.getValue());
                        mutableData.child("username").child(name.getValue()).setValue(name.getValue());

                        Log.e("kkankkan", "새 토픽 올림!, 마스터는 나야");  // 왜 계속 불려???? 진짜 이상한데 잘됨 ..0
                    }
                    else {  // 기존 토픽
                        existTopic = true;
                        if (mutableData.child("password").getValue().equals(password.getValue())) {
                            if (mutableData.child("username").hasChild(name.getValue())) {
                                setNameError("이미 사용중인 이름입니다");
                                Log.e("kkankkan", "이미 사용중인 이름");
                                progressDialog.dismiss();
                            }
                            else {
                                newName = true;
                                if (mutableData.child("master").getValue().equals(true)) {
                                    mutableData.child("username").child(name.getValue()).setValue(name.getValue());
                                    Log.e("kkankkan", "마스터 이미 있음");
                                }
                                else {
                                    newMaster = true;
                                    mutableData.child("master").setValue(true);
                                    mutableData.child("username").child(name.getValue()).setValue(name.getValue());
                                    Log.e("kkankkan", "마스터는 나야");

                                }
                            }
                        }
                        else {
                            setPasswordError("비밀번호가 일치하지 않습니다");
                            Log.e("kkankkan", "비밀번호 틀림");
                            progressDialog.dismiss();
                        }

                    }
                    return Transaction.success(mutableData);
                }

                @Override
                public void onComplete(@Nullable DatabaseError databaseError, boolean b, @Nullable DataSnapshot dataSnapshot) {
                    Log.e("kkankkan", "transaction complete");
                    if (!existTopic) {
                        //setMasterCheck(true);

                        // fixme hyeyeon
                        data.setIp(ip.getValue());
                        data.setPort(port.getValue());
                        data.setTopic(topic.getValue());
                        data.setPassword(password.getValue());
                        data.setName(name.getValue());
                        data.setMaster(true);

                        /*Bundle bundle = new Bundle();
                        bundle.putString("topic", getTopic().getValue());
                        bundle.putString("name", getName().getValue());
                        bundle.putString("password", getPassword().getValue());
                        bundle.putString("master", Boolean.toString(true));
                        bundle.putString("ip", ip.getValue());
                        bundle.putString("port", port.getValue());*/

                        setTopic("");
                        setPassword("");
                        setName("");
                        //setMasterCheck(false);
                        Log.e("kkankkan", "메인뷰모델 초기화");

                        navigate(R.id.action_mainFragment_to_drawingFragment);
                        //navigate(R.id.action_mainFragment_to_drawingFragment, bundle);
                    }
                    else if (newName) {
                        if (newMaster) {
                            //setMasterCheck(true);
                            data.setIp(ip.getValue());
                            data.setPort(port.getValue());
                            data.setTopic(topic.getValue());
                            data.setPassword(password.getValue());
                            data.setName(name.getValue());
                            data.setMaster(true);

                            /*Bundle bundle = new Bundle();
                            bundle.putString("topic", getTopic().getValue());
                            bundle.putString("name", getName().getValue());
                            bundle.putString("password", getPassword().getValue());
                            bundle.putString("master", Boolean.toString(true));
                            bundle.putString("ip", ip.getValue());
                            bundle.putString("port", port.getValue());*/

                            setTopic("");
                            setPassword("");
                            setName("");
                            //setMasterCheck(false);
                            Log.e("kkankkan", "메인뷰모델 초기화");

                            navigate(R.id.action_mainFragment_to_drawingFragment);
                            // navigate(R.id.action_mainFragment_to_drawingFragment, bundle);
                        }
                        else {
                            data.setIp(ip.getValue());
                            data.setPort(port.getValue());
                            data.setTopic(topic.getValue());
                            data.setPassword(password.getValue());
                            data.setName(name.getValue());
                            data.setMaster(false);

                            /*Bundle bundle = new Bundle();
                            bundle.putString("topic", getTopic().getValue());
                            bundle.putString("name", getName().getValue());
                            bundle.putString("password", getPassword().getValue());
                            bundle.putString("master", Boolean.toString(false));
                            bundle.putString("ip", ip.getValue());
                            bundle.putString("port", port.getValue());*/

                            setTopic("");
                            setPassword("");
                            setName("");
                            //setMasterCheck(false);
                            Log.e("kkankkan", "메인뷰모델 초기화");

                            navigate(R.id.action_mainFragment_to_drawingFragment);
                            //navigate(R.id.action_mainFragment_to_drawingFragment, bundle);
                        }
                    }
                    newName = false;
                    newMaster = false;

                    //progressDialog.dismiss();
                }
            });
        }
    }


    /*public void storageUpload() {
        storageReference.child(getTopic().getValue()).getBytes(Long.MAX_VALUE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
            @Override
            public void onSuccess(byte[] bytes) {
                try {
                    JSONObject object = new JSONObject(new String(bytes));
                    Log.e("kkankkan", "토픽 가져오기 : " + object.toString());
                    String pw = object.getString("password");

                    if (getPassword().getValue().equals(pw)) {
                        JSONArray array = object.getJSONArray("names");
                        boolean isExistName = false;

                        for (int i=0; i<array.length(); i++) {
                            JSONObject obj =(JSONObject) array.get(i);
                            String name = obj.getString("name");
                            if (name.equals(getName().getValue())) {
                                isExistName = true;
                                break;
                            }
                        }

<<<<<<< HEAD
                                Bundle bundle = new Bundle();
                                bundle.putString("topic", getTopic().getValue());
                                bundle.putString("name", getName().getValue());
                                bundle.putString("password", getPassword().getValue());
                                bundle.putString("master", getMasterCheck().getValue().toString());
                                bundle.putString("ip", ip.getValue());
                                bundle.putString("port", port.getValue());
=======
                        if (!isExistName) {
                            JSONObject obj = new JSONObject();
                            obj.put("name", getName().getValue());
                            array.put(obj);

                            object.put("names", array);
>>>>>>> origin/login

                            Log.e("kkankkan", "기존 토픽에 새로운 사용자 추가 : " + object.toString());

<<<<<<< HEAD
                                setIpError("");
                                setPortError("");
                                setTopic("");
                                setPassword("");
                                setName("");
                                setMasterCheck();
                                Log.e("kkankkan", "메인뷰모델 초기화");
=======
                            UploadTask uploadTask = storageReference.child(getTopic().getValue()).putBytes(object.toString().getBytes());
                            uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                @Override
                                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                    Bundle bundle = new Bundle();
                                    bundle.putString("topic", getTopic().getValue());
                                    bundle.putString("name", getName().getValue());
                                    bundle.putString("password", getPassword().getValue());
                                    bundle.putString("master", getMasterCheck().getValue().toString());
                                    bundle.putString("ip", ip.getValue());
                                    bundle.putString("port", port.getValue());
>>>>>>> origin/login

                                    Log.e("kkankkan", getMasterCheck().getValue().toString());
                                    Log.e("kkankkan", "기존 토픽에 새로운 사용자 upload success !!");


                                    //navigate(R.id.action_topicFragment_to_drawingFragment);

                                    setIpError("");
                                    setPortError("");
                                    setTopic("");
                                    setPassword("");
                                    setName("");
                                    setMasterCheck(false);
                                    Log.e("kkankkan", "메인뷰모델 초기화");

                                    navigate(R.id.action_mainFragment_to_drawingFragment, bundle);
                                }
                            });

                        }
                        else  {
                            Log.e("kkankkan", "기존 토픽에 있는 사용자");

                            Bundle bundle = new Bundle();
                            bundle.putString("topic", getTopic().getValue());
                            bundle.putString("name", getName().getValue());
                            bundle.putString("password", getPassword().getValue());
                            bundle.putString("master", getMasterCheck().getValue().toString());
                            bundle.putString("ip", ip.getValue());
                            bundle.putString("port", port.getValue());

                            //navigate(R.id.action_topicFragment_to_drawingFragment);

                            setIpError("");
                            setPortError("");
                            setTopic("");
                            setPassword("");
                            setName("");
                            setMasterCheck(false);
                            Log.e("kkankkan", "메인뷰모델 초기화");

                            navigate(R.id.action_mainFragment_to_drawingFragment, bundle);
                        }

                    }
<<<<<<< HEAD
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {  // 새로운 topic
                    try {
                        JSONObject object = new JSONObject();
                        object.put("topic", getTopic().getValue());
                        object.put("password", getPassword().getValue());

                        JSONArray array = new JSONArray();
                        JSONObject name = new JSONObject();
                        name.put("name", getName().getValue());
                        array.put(name);
                        object.put("names", array);

                        Log.e("kkankkan", "새로운 토픽 : "  + object.toString());

                        UploadTask uploadTask = storageReference.child(getTopic().getValue()).putBytes(object.toString().getBytes());
                        uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                Bundle bundle = new Bundle();
                                bundle.putString("topic", getTopic().getValue());
                                bundle.putString("name", getName().getValue());
                                bundle.putString("password", getPassword().getValue());
                                bundle.putString("master", getMasterCheck().getValue().toString());
                                bundle.putString("ip", ip.getValue());
                                bundle.putString("port", port.getValue());

                                Log.e("kkankkan", "새로운 토픽 upload success !!");

                                setIpError("");
                                setPortError("");
                                setTopic("");
                                setPassword("");
                                setName("");
                                setMasterCheck();
                                Log.e("kkankkan", "메인뷰모델 초기화");
                                // navigate(R.id.action_topicFragment_to_drawingFragment);

                                navigate(R.id.action_mainFragment_to_drawingFragment, bundle);
=======
                    else {
                        Log.e("kkankkan", "password가 일치하지 않습니다");
                    }
>>>>>>> origin/login

                }catch (JSONException ex) {

                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {  // 새로운 topic
                try {
                    JSONObject object = new JSONObject();
                    object.put("topic", getTopic().getValue());
                    object.put("password", getPassword().getValue());

                    JSONArray array = new JSONArray();
                    JSONObject name = new JSONObject();
                    name.put("name", getName().getValue());
                    array.put(name);
                    object.put("names", array);

                    Log.e("kkankkan", "새로운 토픽 : "  + object.toString());

                    UploadTask uploadTask = storageReference.child(getTopic().getValue()).putBytes(object.toString().getBytes());
                    uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            Bundle bundle = new Bundle();
                            bundle.putString("topic", getTopic().getValue());
                            bundle.putString("name", getName().getValue());
                            bundle.putString("password", getPassword().getValue());
                            bundle.putString("master", getMasterCheck().getValue().toString());
                            bundle.putString("ip", ip.getValue());
                            bundle.putString("port", port.getValue());

                            Log.e("kkankkan", "새로운 토픽 upload success !!");

                            setIpError("");
                            setPortError("");
                            setTopic("");
                            setPassword("");
                            setName("");
                            setMasterCheck(false);
                            Log.e("kkankkan", "메인뷰모델 초기화");
                            // navigate(R.id.action_topicFragment_to_drawingFragment);

                            navigate(R.id.action_mainFragment_to_drawingFragment, bundle);

                        }
                    });

                } catch (JSONException exception) {
                    Log.e("TAG", "Fail to create JSONObject", exception);
                }
            }
        });
    }*/

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

    public MutableLiveData<Boolean> getMasterCheck() {
        return masterCheck;
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

    public void setMasterCheck(boolean ckeck) {
        this.masterCheck.postValue(ckeck);
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

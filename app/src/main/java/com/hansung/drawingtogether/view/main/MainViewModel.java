package com.hansung.drawingtogether.view.main;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.hansung.drawingtogether.R;
import com.hansung.drawingtogether.view.BaseViewModel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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

    private boolean hasSpecialCharacterAndBlank;

    public MainViewModel() {

        Log.e("kkankkan", "메인뷰모델 생성자");

        ip.setValue("54.180.154.63");
        port.postValue("1883");
        setTopic("");
        setPassword("");
        setName("");
        setMasterCheck();

        setIpError("");
        setTopicError("");
        setPasswordError("");
        setNameError("");

        Log.e("kkankkan", "메인뷰모델 초기화 완료");

        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        hasSpecialCharacterAndBlank = false;

        Log.e("kkankkan", storage.toString());
        Log.e("kkankkan", storageReference.toString());
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

        if (getTopic().getValue().equals("")) {
            setTopicError("빈칸을 채워주세요");
            hasSpecialCharacterAndBlank = true;
        }

        if (getPassword().getValue().equals("")) {
            setPasswordError("빈칸을 채워주세요");
            hasSpecialCharacterAndBlank = true;
        }

        if (getName().getValue().equals("")) {
            setNameError("빈칸을 채워주세요");
            hasSpecialCharacterAndBlank = true;
        }

        if (!getTopic().getValue().matches("[0-9|a-z|A-Z|ㄱ-ㅎ|ㅏ-ㅣ|가-힝]*")) {
            setTopicError("특수문자를 포함하면 안됩니다");
            hasSpecialCharacterAndBlank = true;
        }

        if (!getPassword().getValue().matches("[0-9|a-z|A-Z|ㄱ-ㅎ|ㅏ-ㅣ|가-힝]*")) {
            setPasswordError("특수문자를 포함하면 안됩니다");
            hasSpecialCharacterAndBlank = true;
        }

        if (!getName().getValue().matches("[0-9|a-z|A-Z|ㄱ-ㅎ|ㅏ-ㅣ|가-힝]*")) {
            setNameError("특수문자를 포함하면 안됩니다");
            hasSpecialCharacterAndBlank = true;
        }

        for (int i=0; i<getTopic().getValue().length(); i++) {
            if (getTopic().getValue().charAt(i) == ' ') {
                setTopicError("공백을 포함하면 안됩니다");
                hasSpecialCharacterAndBlank = true;
            }
        }

        for (int i=0; i<getPassword().getValue().length(); i++) {
            if (getPassword().getValue().charAt(i) == ' ') {
                setPasswordError("공백을 포함하면 안됩니다");
                hasSpecialCharacterAndBlank = true;
            }
        }

        for (int i=0; i<getName().getValue().length(); i++) {
            if (getName().getValue().charAt(i) == ' ') {
                setNameError("공백을 포함하면 안됩니다");
                hasSpecialCharacterAndBlank = true;
            }
        }
    }

    public void startDrawing(View view) {
        setTopicError("");
        setPasswordError("");
        setNameError("");
        hasSpecialCharacterAndBlank = false;

        hasSpecialCharacterAndBlank();

        Log.e("kkankkan", hasSpecialCharacterAndBlank + "");

        Log.e("kkankkan", getTopic().getValue() + " / " + getPassword().getValue() + " / " + getName().getValue() + " / " + getMasterCheck().getValue().toString());

        if (!hasSpecialCharacterAndBlank) {

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

                            if (!isExistName) {
                                JSONObject obj = new JSONObject();
                                obj.put("name", getName().getValue());
                                array.put(obj);

                                object.put("names", array);

                                Log.e("kkankkan", "기존 토픽에 새로운 사용자 추가 : " + object.toString());

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

                                        Log.e("kkankkan", getMasterCheck().getValue().toString());
                                        Log.e("kkankkan", "기존 토픽에 새로운 사용자 upload success !!");


                                        //navigate(R.id.action_topicFragment_to_drawingFragment);

                                        setIpError("");
                                        setPortError("");
                                        setTopic("");
                                        setPassword("");
                                        setName("");
                                        setMasterCheck();
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
                                setMasterCheck();
                                Log.e("kkankkan", "메인뷰모델 초기화");

                                navigate(R.id.action_mainFragment_to_drawingFragment, bundle);
                            }

                        }
                        else {
                            Log.e("kkankkan", "password가 일치하지 않습니다");
                        }

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
                                setMasterCheck();
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
        }
        else {
            Log.e("kkankkan", "특수문자나 공백을 포함하면 안됩니다.");
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

    public MutableLiveData<String> getNameError
            () {
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

    public void setMasterCheck() {
        this.masterCheck.postValue(false);
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

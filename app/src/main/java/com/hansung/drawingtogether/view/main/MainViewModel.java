package com.hansung.drawingtogether.view.main;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

    private FirebaseDatabase database;
    private DatabaseReference databaseReference;
    private boolean existTopic;
    private boolean newName;
    private boolean newMaster;

    private boolean hasSpecialCharacterAndBlank;

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

            databaseReference.child(getTopic().getValue()).runTransaction(new Transaction.Handler() {
                @NonNull
                @Override
                public Transaction.Result doTransaction(@NonNull MutableData mutableData) {
                    existTopic = false;


                    if (mutableData.child("master").getValue() == null) {  // 새 토픽
                        mutableData.child("master").setValue(true);
                        mutableData.child("password").setValue(getPassword().getValue());
                        mutableData.child("username").child(getName().getValue()).setValue(getName().getValue());

                        Log.e("kkankkan", "새 토픽 올림!, 마스터는 나야");  // 왜 계속 불려???? 진짜 이상한데 잘됨 ..0
                    }
                    else {  // 기존 토픽
                        existTopic = true;
                        if (mutableData.child("password").getValue().equals(getPassword().getValue())) {
                            if (mutableData.child("username").hasChild(getName().getValue())) {
                                setNameError("이미 사용중인 이름입니다");
                                Log.e("kkankkan", "이미 사용중인 이름");

                            }
                            else {
                                newName = true;
                                if (mutableData.child("master").getValue().equals(true)) {
                                    mutableData.child("username").child(getName().getValue()).setValue(getName().getValue());
                                    Log.e("kkankkan", "마스터 이미 있음");
                                }
                                else {
                                    newMaster = true;
                                    mutableData.child("master").setValue(true);
                                    mutableData.child("username").child(getName().getValue()).setValue(getName().getValue());
                                    Log.e("kkankkan", "마스터는 나야");

                                }
                            }
                        }
                        else {
                            setPasswordError("비밀번호가 일치하지 않습니다");
                            Log.e("kkankkan", "비밀번호 틀림");

                        }

                    }
                    return Transaction.success(mutableData);
                }

<<<<<<< HEAD
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
=======
                @Override
                public void onComplete(@Nullable DatabaseError databaseError, boolean b, @Nullable DataSnapshot dataSnapshot) {
                    Log.e("kkankkan", "transaction complete");
                    if (!existTopic) {
                        //setMasterCheck(true);
                        Bundle bundle = new Bundle();
                        bundle.putString("topic", getTopic().getValue());
                        bundle.putString("name", getName().getValue());
                        bundle.putString("password", getPassword().getValue());
                        bundle.putString("master", Boolean.toString(true));
                        bundle.putString("ip", ip.getValue());
                        bundle.putString("port", port.getValue());

                        setIpError("");
                        setPortError("");
                        setTopic("");
                        setPassword("");
                        setName("");
                        //setMasterCheck(false);
                        Log.e("kkankkan", "메인뷰모델 초기화");

                        navigate(R.id.action_mainFragment_to_drawingFragment, bundle);
                    }
                    else if (newName) {
                        if (newMaster) {
                            //setMasterCheck(true);

                            Bundle bundle = new Bundle();
                            bundle.putString("topic", getTopic().getValue());
                            bundle.putString("name", getName().getValue());
                            bundle.putString("password", getPassword().getValue());
                            bundle.putString("master", Boolean.toString(true));
                            bundle.putString("ip", ip.getValue());
                            bundle.putString("port", port.getValue());

                            setIpError("");
                            setPortError("");
                            setTopic("");
                            setPassword("");
                            setName("");
                            //setMasterCheck(false);
                            Log.e("kkankkan", "메인뷰모델 초기화");

                            navigate(R.id.action_mainFragment_to_drawingFragment, bundle);
                        }
                        else {
                            Bundle bundle = new Bundle();
                            bundle.putString("topic", getTopic().getValue());
                            bundle.putString("name", getName().getValue());
                            bundle.putString("password", getPassword().getValue());
                            bundle.putString("master", Boolean.toString(false));
                            bundle.putString("ip", ip.getValue());
                            bundle.putString("port", port.getValue());

                            setIpError("");
                            setPortError("");
                            setTopic("");
                            setPassword("");
                            setName("");
                            //setMasterCheck(false);
                            Log.e("kkankkan", "메인뷰모델 초기화");

                            navigate(R.id.action_mainFragment_to_drawingFragment, bundle);
                        }
>>>>>>> origin/login

                    }

                    newName = false;
                    newMaster = false;
                }
            });

        }

<<<<<<< HEAD
                                        setIpError("");
                                        setPortError("");
                                        setTopic("");
                                        setPassword("");
                                        setName("");
                                        setMasterCheck();
                                        Log.e("kkankkan", "메인뷰모델 초기화");
=======
    }
>>>>>>> origin/login


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

    public void setMasterCheck(boolean ckeck) {
        this.masterCheck.postValue(ckeck);
    }

    public void setIpError(String ip) {
        this.ipError.postValue(ip);
    }

    public void setPortError(String port) {
        this.portError.postValue(port);
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

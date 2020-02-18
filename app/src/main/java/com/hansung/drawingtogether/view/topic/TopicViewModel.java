package com.hansung.drawingtogether.view.topic;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EdgeEffect;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.databinding.Bindable;
import androidx.databinding.ObservableField;
import androidx.lifecycle.MutableLiveData;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.hansung.drawingtogether.view.BaseViewModel;
import com.hansung.drawingtogether.R;
import com.hansung.drawingtogether.view.drawing.DrawingFragment;
import com.hansung.drawingtogether.view.main.MainActivity;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TopicViewModel extends BaseViewModel {

    private MutableLiveData<String> topicEditText = new MutableLiveData<>();
    private MutableLiveData<String> passwordEditText = new MutableLiveData<>();
    private MutableLiveData<String> nameEditText = new MutableLiveData<>();
    private MutableLiveData<Boolean> masterCheck = new MutableLiveData<>();

    private FirebaseStorage storage;
    private StorageReference storageReference;

    public TopicViewModel() {

        setTopicEditText(topicEditText);
        setPasswordEditText(passwordEditText);
        setNameEditText(nameEditText);
        setMasterCheck(masterCheck);

        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        Log.e("kkankkan", "토픽뷰모델 생성자");

    }

    public boolean hasSpecialCharacterAndBlank() {
        if (getTopicEditText().getValue().matches("[0-9|a-z|A-Z|ㄱ-ㅎ|ㅏ-ㅣ|가-힝]*") && getPasswordEditText().getValue().matches("[0-9|a-z|A-Z|ㄱ-ㅎ|ㅏ-ㅣ|가-힝]*") && getNameEditText().getValue().matches("[0-9|a-z|A-Z|ㄱ-ㅎ|ㅏ-ㅣ|가-힝]*")) {
            for (int i=0; i<getTopicEditText().getValue().length(); i++) {
                if (getTopicEditText().getValue().charAt(i) == ' ')
                    return true;
            }
            for (int i=0; i<getPasswordEditText().getValue().length(); i++) {
                if (getPasswordEditText().getValue().charAt(i) == ' ')
                    return true;
            }
            for (int i=0; i<getNameEditText().getValue().length(); i++) {
                if (getTopicEditText().getValue().charAt(i) == ' ')
                    return true;
            }
            return false;
        }
        else {
            return true;
        }
    }

    public void startDrawing(View view) {
        Log.e("kkankkan", getTopicEditText().getValue() + " / " + getPasswordEditText().getValue() + " / " + getNameEditText().getValue() + " / " + getMasterCheck().getValue().toString());

        if (!getTopicEditText().getValue().equals("") && !getPasswordEditText().getValue().equals("") && !getNameEditText().getValue().equals("")) {

            if (!hasSpecialCharacterAndBlank()) {

                storageReference.child(getTopicEditText().getValue()).getBytes(Long.MAX_VALUE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                    @Override
                    public void onSuccess(byte[] bytes) {
                        try {
                            JSONObject object = new JSONObject(new String(bytes));
                            Log.e("kkankkan", "토픽 가져오기 : " + object.toString());
                            String pw = object.getString("password");

                            if (getPasswordEditText().getValue().equals(pw)) {
                                JSONArray array = object.getJSONArray("names");
                                boolean isExistName = false;

                                for (int i=0; i<array.length(); i++) {
                                    JSONObject obj =(JSONObject) array.get(i);
                                    String name = obj.getString("name");
                                    if (name.equals(getNameEditText().getValue())) {
                                        isExistName = true;
                                        break;
                                    }
                                }

                                if (!isExistName) {
                                    JSONObject obj = new JSONObject();
                                    obj.put("name", getNameEditText().getValue());
                                    array.put(obj);

                                    object.put("names", array);

                                    Log.e("kkankkan", "기존 토픽에 새로운 사용자 추가 : " + object.toString());

                                    UploadTask uploadTask = storageReference.child(getTopicEditText().getValue()).putBytes(object.toString().getBytes());
                                    uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                        @Override
                                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                            Bundle bundle = new Bundle();
                                            bundle.putString("topic", getTopicEditText().getValue());
                                            bundle.putString("name", getNameEditText().getValue());
                                            bundle.putString("password", getPasswordEditText().getValue());
                                            bundle.putString("master", getMasterCheck().getValue().toString());

                                            Log.e("kkankkan", getMasterCheck().getValue().toString());
                                            Log.e("kkankkan", "기존 토픽에 새로운 사용자 upload success !!");

                                            navigate(R.id.action_topicFragment_to_drawingFragment, bundle);
                                            //navigate(R.id.action_topicFragment_to_drawingFragment);

                                            setTopicEditText(topicEditText);
                                            setPasswordEditText(passwordEditText);
                                            setNameEditText(nameEditText);
                                            setMasterCheck(masterCheck);
                                            Log.e("kkankkan", "토픽뷰모델 초기화");
                                        }
                                    });

                                }
                                else  {
                                    Log.e("kkankkan", "기존 토픽에 있는 사용자");

                                    Bundle bundle = new Bundle();
                                    bundle.putString("topic", getTopicEditText().getValue());
                                    bundle.putString("name", getNameEditText().getValue());
                                    bundle.putString("password", getPasswordEditText().getValue());
                                    bundle.putString("master", getMasterCheck().getValue().toString());

                                    navigate(R.id.action_topicFragment_to_drawingFragment, bundle);
                                    //navigate(R.id.action_topicFragment_to_drawingFragment);

                                    setTopicEditText(topicEditText);
                                    setPasswordEditText(passwordEditText);
                                    setNameEditText(nameEditText);
                                    setMasterCheck(masterCheck);
                                    Log.e("kkankkan", "토픽뷰모델 초기화");
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
                            object.put("topic", getTopicEditText().getValue());
                            object.put("password", getPasswordEditText().getValue());

                            JSONArray array = new JSONArray();
                            JSONObject name = new JSONObject();
                            name.put("name", getNameEditText().getValue());
                            array.put(name);
                            object.put("names", array);

                            Log.e("kkankkan", "새로운 토픽 : "  + object.toString());

                            UploadTask uploadTask = storageReference.child(getTopicEditText().getValue()).putBytes(object.toString().getBytes());
                            uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                @Override
                                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                    Bundle bundle = new Bundle();
                                    bundle.putString("topic", getTopicEditText().getValue());
                                    bundle.putString("name", getNameEditText().getValue());
                                    bundle.putString("password", getPasswordEditText().getValue());
                                    bundle.putString("master", getMasterCheck().getValue().toString());

                                    Log.e("kkankkan", "새로운 토픽 upload success !!");
                                    navigate(R.id.action_topicFragment_to_drawingFragment, bundle);

                                    setTopicEditText(topicEditText);
                                    setPasswordEditText(passwordEditText);
                                    setNameEditText(nameEditText);
                                    setMasterCheck(masterCheck);
                                    Log.e("kkankkan", "토픽뷰모델 초기화");
                                    // navigate(R.id.action_topicFragment_to_drawingFragment);

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
        else {
            Log.e("kkankkan", "빈칸 없이 모두 입력하세요");
        }

    }

    public MutableLiveData<String> getTopicEditText() {
        return topicEditText;
    }

    public MutableLiveData<String> getPasswordEditText() {
        return passwordEditText;
    }

    public MutableLiveData<String> getNameEditText() {
        return nameEditText;
    }

    public MutableLiveData<Boolean> getMasterCheck() {
        return masterCheck;
    }

    public void setTopicEditText(MutableLiveData<String> topicEditText) {
        this.topicEditText = topicEditText;
        this.topicEditText.setValue("");
    }

    public void setPasswordEditText(MutableLiveData<String> passwordEditText) {
        this.passwordEditText = passwordEditText;
        this.passwordEditText.setValue("");
    }

    public void setNameEditText(MutableLiveData<String> nameEditText) {
        this.nameEditText = nameEditText;
        this.nameEditText.setValue("");
    }

    public void setMasterCheck(MutableLiveData<Boolean> masterCheck) {
        this.masterCheck = masterCheck;
        this.masterCheck.setValue(false);
    }
}

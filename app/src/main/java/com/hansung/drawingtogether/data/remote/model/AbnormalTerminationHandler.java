package com.hansung.drawingtogether.data.remote.model;

import android.app.ProgressDialog;

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
import com.hansung.drawingtogether.view.drawing.DrawingEditor;

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
        MyLog.e("exception", "UncaughtException");

//        if (databaseRef != null && client.getClient().isConnected()) {
//            client.exitTask();
//
//            databaseRef.child(client.getTopic()).runTransaction(new Transaction.Handler() {
//                @NonNull
//                @Override
//                public Transaction.Result doTransaction(@NonNull MutableData mutableData) {
//                    if (mutableData.getValue() != null && client.isMaster()) {
//                        mutableData.setValue(null);
//                    }
//                    if (mutableData.getValue() != null && !client.isMaster()) {
//                        mutableData.child("username").child(client.getMyName()).setValue(null);
//                    }
//                    MyLog.e("transaction", "transaction success");
//                    return Transaction.success(mutableData);
//                }
//
//                @Override
//                public void onComplete(@Nullable DatabaseError databaseError, boolean b, @Nullable DataSnapshot dataSnapshot) {
//                    MyLog.e("transaction", "transaction complete");
//
//                    if (databaseError != null) {
//                        MyLog.e("transaction", databaseError.getDetails());
//                        return;
//                    }
//                    client.exitTask();
//                }
//            });
//        }

        logger.loggingUncaughtException(thread, e.getStackTrace()); // 발생한 오류에 대한 메시지 로그에 기록
        logger.uploadLogFile(ExitType.ABNORMAL);

    }
}

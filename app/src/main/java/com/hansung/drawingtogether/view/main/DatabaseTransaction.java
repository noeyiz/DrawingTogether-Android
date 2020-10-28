package com.hansung.drawingtogether.view.main;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.hansung.drawingtogether.data.remote.model.MyLog;

/* Firebase Realtime Database Transaction 수행 후 불리는 Completion Handler */
interface completionHandler {
    void completeLogin(DatabaseError error, String masterName, boolean topicError, boolean passwordError, boolean nameError);
    void completeExit(DatabaseError error);
}

/* Firebase Realtime Database Transaction 수행 */
public abstract class DatabaseTransaction implements completionHandler {

    private DatabaseReference ref;

    private String masterName;

    private boolean topicError;
    private boolean passwordError;
    private boolean nameError;

    public DatabaseTransaction() {
        ref = FirebaseDatabase.getInstance().getReference();
        Log.i("Database Transaction", "Database Transaction init()");
    }

    /* 회의방 참가 시 */
    public void runTransactionLogin(String topic, final String password, final String name, final String mode) {

        ref.child(topic).runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData mutableData) {
                Log.i("Database Transaction", "doTransaction()");

                topicError = false;
                passwordError = false;
                nameError = false;

                /* 회의명을 노드로 하는 JSON 트리가 존재하는 경우*/
                if (mutableData.getValue() != null) {
                    MyLog.i("Database Transaction", "Exist Topic " + mutableData.getValue());
                    topicError = true;

                    switch (mode) {
                        case "masterMode":
                            masterName = "";
                            break;
                        case "joinMode":
                            if (!mutableData.child("password").getValue().equals(password)) {
                                passwordError = true;
                                break;
                            }
                            if (mutableData.child("username").hasChild(name)) {
                                nameError = true;
                                break;
                            }
                            else {
                                mutableData.child("username").child(name).setValue(name);
                                masterName = mutableData.child("master").getValue().toString();
                                break;
                            }
                    }
                    MyLog.i("Database Transaction", "Transaction Success");
                    return Transaction.success(mutableData);
                }

                /* 회의명을 노드로 하는 JSON 트리가 존재하지 않는 경우*/
                MyLog.i("Database Transaction", "New Topic");
                switch (mode) {
                    case "masterMode":
                        mutableData.child("password").setValue(password);
                        mutableData.child("username").child(name).setValue(name);
                        mutableData.child("master").setValue(name);
                        masterName = name;
                        break;
                    case "joinMode":
                        masterName = "";
                        break;
                }
                MyLog.i("Database Transaction", "Transaction Success");
                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError databaseError, boolean b, @Nullable DataSnapshot dataSnapshot) {
                Log.i("Database Transaction", "Transaction Complete");
                completeLogin(databaseError, masterName, topicError, passwordError, nameError);
            }
        });
    }

    /* 회의방 퇴장 또는 종료 시 */
    public void runTransactionExit(String topic, final String name, final String mode) {

        ref.child(topic).runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData mutableData) {
                if (mutableData.getValue() != null && mode.equals("masterMode")) {
                    Log.i("Database Transaction", "Master Topic Close");
                    mutableData.setValue(null);
                }
                if (mutableData.getValue() != null && mode.equals("joinMode")) {
                    Log.i("Database Transaction", "Participant Name Delete");
                    mutableData.child("username").child(name).setValue(null);
                }
                MyLog.i("Database Transaction", "Transaction Success");
                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError databaseError, boolean b, @Nullable DataSnapshot dataSnapshot) {
                completeExit(databaseError);
            }
        });
    }

}

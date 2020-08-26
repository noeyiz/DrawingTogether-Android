package com.hansung.drawingtogether.view.main;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.FirebaseException;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.hansung.drawingtogether.data.remote.model.MyLog;

interface completionHandler {
    void completeLogin(DatabaseError error, String masterName, boolean topicError, boolean passwordError, boolean nameError);
    void completeExit(DatabaseError error);
}

public abstract class DatabaseTransaction implements completionHandler {

    private DatabaseReference ref;

    private String masterName;

    private boolean topicError;
    private boolean passwordError;
    private boolean nameError;

    public DatabaseTransaction() {
        ref = FirebaseDatabase.getInstance().getReference();
        Log.e("dt", "init");
    }

    public void runTransactionLogin(String topic, final String password, final String name, final String mode) {

        ref.child(topic).runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData mutableData) {
                Log.e("dt", "doTransaction");

                topicError = false;
                passwordError = false;
                nameError = false;

                if (mutableData.getValue() != null) {
                    MyLog.e("transaction", "exist topic " + mutableData.getValue());
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
//                                mutableData.child("access time").setValue(System.currentTimeMillis()); // fixme nayeon

                                masterName = mutableData.child("master").getValue().toString();  // fixme hyeyeon
                                MyLog.i("login", "masterName: " + masterName);
                                break;
                            }
                    }
                    MyLog.e("transaction", "transaction success");
                    return Transaction.success(mutableData);
                }

                MyLog.e("transaction", "new topic " + mutableData.getChildrenCount());
                switch (mode) {
                    case "masterMode":
                        mutableData.child("password").setValue(password);
                        mutableData.child("username").child(name).setValue(name);
                        mutableData.child("master").setValue(name);
//                        mutableData.child("access time").setValue(System.currentTimeMillis()); // fixme nayeon

                        masterName = name;  // fixme hyeyeon
                        MyLog.i("login", "masterName: " + masterName);
                        break;
                    case "joinMode":
                        masterName = "";
                        break;
                }
                MyLog.e("transaction", "transaction success");
                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError databaseError, boolean b, @Nullable DataSnapshot dataSnapshot) {
                Log.e("dt", "database complete");
                completeLogin(databaseError, masterName, topicError, passwordError, nameError);
            }
        });
    }

    public void runTransactionExit(String topic, final String name, final String mode) {

        ref.child(topic).runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData mutableData) {
                if (mutableData.getValue() != null && mode.equals("masterMode")) {
                    Log.e("dt", "master delete");
                    mutableData.setValue(null);
                }
                if (mutableData.getValue() != null && mode.equals("joinMode")) {
                    Log.e("dt", "join delete");
                    mutableData.child("username").child(name).setValue(null);
                }
                MyLog.e("transaction", "transaction success");
                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError databaseError, boolean b, @Nullable DataSnapshot dataSnapshot) {
                completeExit(databaseError);
            }
        });
    }

}

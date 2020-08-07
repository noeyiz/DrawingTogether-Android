package com.hansung.drawingtogether.view.main;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

public class ShowAlertDialog {

    public void showAlert(Context context, String title, String message) {

        AlertDialog dialog = new AlertDialog.Builder(context)
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
}

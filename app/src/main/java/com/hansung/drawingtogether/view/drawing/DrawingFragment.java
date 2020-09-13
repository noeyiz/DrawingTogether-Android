package com.hansung.drawingtogether.view.drawing;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Point;
import android.media.AudioManager;
import android.media.ExifInterface;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.navigation.fragment.NavHostFragment;
import lombok.Getter;
import lombok.Setter;

import com.google.firebase.database.DatabaseError;
import com.hansung.drawingtogether.R;

import com.hansung.drawingtogether.data.remote.model.AliveThread;
import com.hansung.drawingtogether.monitoring.ComponentCount;
import com.hansung.drawingtogether.data.remote.model.ExitType;
import com.hansung.drawingtogether.data.remote.model.Logger;
import com.hansung.drawingtogether.data.remote.model.MQTTClient;
import com.hansung.drawingtogether.monitoring.MonitoringDataWriter;
import com.hansung.drawingtogether.monitoring.MonitoringRunnable;
import com.hansung.drawingtogether.data.remote.model.MyLog;
import com.hansung.drawingtogether.databinding.FragmentDrawingBinding;
import com.hansung.drawingtogether.monitoring.Velocity;
import com.hansung.drawingtogether.view.NavigationCommand;
import com.hansung.drawingtogether.view.main.DatabaseTransaction;
import com.hansung.drawingtogether.view.main.JoinMessage;

import com.hansung.drawingtogether.view.main.MQTTSettingData;
import com.hansung.drawingtogether.view.main.MainActivity;


import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;



@Getter
public class DrawingFragment extends Fragment implements MainActivity.OnRightBottomBackListener {  // fixme hyeyeon

    private final int PICK_FROM_GALLERY = 0;
    private final int PICK_FROM_CAMERA = 1;

    private Point size;

    private MQTTClient client = MQTTClient.getInstance();
    private MQTTSettingData data = MQTTSettingData.getInstance();  // fixme hyeyeon

    private DrawingEditor de = DrawingEditor.getInstance();
    private AttributeManager am = AttributeManager.getInstance();
    private Logger logger = Logger.getInstance(); // fixme nayeon

    private FragmentDrawingBinding binding;
    private DrawingViewModel drawingViewModel;
    private InputMethodManager inputMethodManager;

    private ExitOnClickListener exitOnClickListener;

    private AliveThread aliveTh = AliveThread.getInstance();

    private ProgressDialog progressDialog;

    private Toolbar toolbar;
    private TextView title;

    private MonitoringRunnable monitoringRunnable = MonitoringRunnable.getInstance();

    //private LinearLayout topToolLayout;
    //private Button doneBtn;

    private ProgressDialog exitProgressDialog;

    private boolean ok = false;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        ((MainActivity)context).setOnRightBottomBackListener(this);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        MyLog.i("lifeCycle", "DrawingFragment onCreateView()");

        exitOnClickListener = new ExitOnClickListener();
        exitOnClickListener.setRightBottomBackPressed(false);

        binding = FragmentDrawingBinding.inflate(inflater, container, false);

        JSONParser.getInstance().initJsonParser(this); // fixme nayeon ☆☆☆ JSON Parser 초기화 (toss DrawingFragmenet)
        Log.e("monitoring", "check parser init");

        drawingViewModel = ViewModelProviders.of(this).get(DrawingViewModel.class);

        client.setDrawingFragment(this);
        de.setDrawingFragment(this);

        de.setTextMoveBorderDrawable(getResources().getDrawable(R.drawable.text_move_border)); // fixme nayeon 텍스트 테두리 설정
        de.setTextFocusBorderDrawable(getResources().getDrawable(R.drawable.text_focus_border));
        de.setTextHighlightBorderDrawable(getResources().getDrawable(R.drawable.text_highlight_border)); // fixme nayeon

        am.setBinding(binding); // Palette Manager 의 FragmentDrawingBinding 변수 초기화
        am.setListener(); // 리스너 초기화
        am.showCurrentColor(Color.parseColor(de.getStrokeColor())); // 현재 색상 보여주기

        binding.drawBtn1.setBackgroundColor(Color.rgb(233, 233, 233)); // 초기 얇은 펜으로 설정
        binding.drawingViewContainer.setOnDragListener(new FrameLayoutDragListener());
        inputMethodManager = (InputMethodManager) Objects.requireNonNull(getActivity()).getSystemService(Context.INPUT_METHOD_SERVICE);

        // 디바이스 화면 size 구하기
        Display display = getActivity().getWindowManager().getDefaultDisplay();
        size = new Point();
        display.getSize(size);
        Log.e("drawing view size in fragment", size.x + ", " + size.y * 0.83);

        // 디바이스 화면 넓이의 3배 = 드로잉뷰 넓이
        //ViewGroup.LayoutParams layoutParams = binding.drawingView.getLayoutParams();
        //layoutParams.width = size.x*3;
        //layoutParams.width = size.x;
        //layoutParams.height = size.y; //****
        //binding.drawingView.setLayoutParams(layoutParams);

        //undo, redo 버튼 초기화
        if(de.getHistory().size() == 0)
            binding.undoBtn.setEnabled(false);
        if(de.getUndoArray().size() == 0)
            binding.redoBtn.setEnabled(false);


        if(de.getTexts().size() != 0) { //text 다시 붙이기
            for(Text t: de.getTexts()) {
                t.removeTextViewToFrameLayout();
                t.setDrawingFragment(this);
                t.addTextViewToFrameLayout();
            }
        }

        if(de.getBackgroundImage() != null) {   //backgroundImage 다시 붙이기
            // fixme jiyeon[0825]
            binding.backgroundView.setImage(de.getBackgroundImage());
        }
        Log.e("pre pub join message", this.getSize().x + ", " + this.getSize().y);

        Log.e("pre pub join message", this.getSize().x + ", " + this.getSize().y);

        if(de.getDrawingBitmap() == null) { // join 메시지 publish

            JoinMessage joinMessage = new JoinMessage(data.getName(), this.getSize().x, this.getSize().y);
            MqttMessageFormat messageFormat = new MqttMessageFormat(joinMessage);
            client.publish(data.getTopic() + "_join", JSONParser.getInstance().jsonWrite(messageFormat));
            MyLog.e("login", data.getName() + " join pub");

            aliveTh.setSecond(10000);
            aliveTh.setCount(0);
            Thread th = new Thread(aliveTh);
            th.start();
            client.setThread(th);

            if(client.isMaster()) {
                Log.e("monitoring", "mqtt client class init func. check master. i'am master.");
                client.setComponentCount(new ComponentCount(client.getTopic()));
                Thread monitoringThread = new Thread(monitoringRunnable);
                monitoringThread.start();
                client.setMonitoringThread(monitoringThread);
            }

//            intent = new Intent(MainActivity.context, AliveBackgroundService.class);
//            MainActivity.context.startService(intent);

            /*if (data.isAliveThreadMode() && !data.isAliveBackground()) {
                MyLog.e("alive", "DrawingFragment: " + data.isAliveThreadMode());
                // fixme hyeyeon
                aliveTh.setSecond(2000);
                aliveTh.setCount(0);
                Thread th = new Thread(aliveTh);
                th.start();
                client.setThread(th);
            }
            else if (data.isAliveThreadMode() && data.isAliveBackground()) {
                intent = new Intent(MainActivity.context, AliveBackgroundService.class);
                MainActivity.context.startService(intent);
            }
            else {
                MyLog.e("alive", "alive publish 안함");
            }
            MyLog.e("alive", "DrawingFragment aliveBackground: " + data.isAliveBackground());*/


        }

        drawingViewModel.drawingCommands.observe(getViewLifecycleOwner(), new Observer<DrawingCommand>() {
            @Override
            public void onChanged(DrawingCommand drawingCommand) {
                if (drawingCommand instanceof DrawingCommand.PenMode) {
                    showPopup(((DrawingCommand.PenMode) drawingCommand).getView(), R.layout.popup_pen_mode);
                }
                if (drawingCommand instanceof DrawingCommand.EraserMode) {
                    showPopup(((DrawingCommand.EraserMode) drawingCommand).getView(), R.layout.popup_eraser_mode);
                }
                if (drawingCommand instanceof DrawingCommand.TextMode) {
                    showPopup(((DrawingCommand.TextMode) drawingCommand).getView(), R.layout.popup_text_mode);
                }
                if (drawingCommand instanceof DrawingCommand.ShapeMode) {
                    showPopup(((DrawingCommand.ShapeMode) drawingCommand).getView(), R.layout.popup_shape_mode);
                }
            }
        });

        drawingViewModel.navigationCommands.observe(getViewLifecycleOwner(), new Observer<NavigationCommand>() {
            @Override
            public void onChanged(NavigationCommand navigationCommand) {
                if (navigationCommand instanceof NavigationCommand.To) {
                    NavHostFragment.findNavController(DrawingFragment.this)
                            .navigate(((NavigationCommand.To) navigationCommand).getDestinationId());
                }
                if (navigationCommand instanceof  NavigationCommand.Back) {
                    NavHostFragment.findNavController(DrawingFragment.this).popBackStack();
                }
            }
        });

        binding.userInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyLog.i("button", "user info"); // fixme nayeon

                if (binding.userPrint.getVisibility() == View.VISIBLE)
                    binding.userPrint.setVisibility(View.INVISIBLE);
                else
                    binding.userPrint.setVisibility(View.VISIBLE);
            }
        });
        binding.setVm(drawingViewModel);
        binding.setLifecycleOwner(this);

        setHasOptionsMenu(true);

        ((MainActivity)getActivity()).setOnLeftTopBackListener(new MainActivity.OnLeftTopBackListener() {
            @Override
            public void onLeftTopBackPressed() {
                exit();
            }
        });

        return binding.getRoot();
    }

    public void showPopup(View view, int layout) {
        View penSettingPopup = getLayoutInflater().inflate(layout, null);
        penSettingPopup.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        PopupWindow popupWindow = new PopupWindow(penSettingPopup, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        popupWindow.setFocusable(true);
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        popupWindow.showAtLocation(penSettingPopup, Gravity.NO_GRAVITY, location[0], location[1] - penSettingPopup.getMeasuredHeight());
        popupWindow.setElevation(20);

        if(layout == R.layout.popup_eraser_mode) {
            setEraserPopupClickListener(penSettingPopup, popupWindow);
        }
        else if(layout == R.layout.popup_shape_mode) {
            setShapePopupClickListener(penSettingPopup, popupWindow);
        }
    }

    private void setEraserPopupClickListener(View penSettingPopup, final PopupWindow popupWindow) {
        final Button backgroundEraserBtn = penSettingPopup.findViewById(R.id.backgroundEraserBtn);
        if(client.isMaster()) {
            backgroundEraserBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    MyLog.d("button", "background eraser button click"); // fixme nayeon

                    binding.drawingView.clearBackgroundImage();
                    popupWindow.dismiss();
                }
            });
        }
        else {
            backgroundEraserBtn.setTextColor(Color.LTGRAY);
            backgroundEraserBtn.setEnabled(false);
        }

        final Button clearBtn = penSettingPopup.findViewById(R.id.clearBtn);
        if(client.isMaster()) {
            clearBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    MyLog.d("button", "drawing clear button click"); // fixme nayeon

                    binding.drawingView.clear();
                    popupWindow.dismiss();
                }
            });
        }
        else {
            clearBtn.setTextColor(Color.LTGRAY);
            clearBtn.setEnabled(false);
        }
    }

    private void setShapePopupClickListener(View penSettingPopup, final PopupWindow popupWindow) {
        final Button rectBtn = penSettingPopup.findViewById(R.id.rectBtn);
        rectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MyLog.d("button", "rect shape button click"); // fixme nayeon

                de.setCurrentMode(Mode.DRAW);
                de.setCurrentType(ComponentType.RECT);
                MyLog.d("drawing", "mode = " + de.getCurrentMode().toString() + ", type = " + de.getCurrentType().toString());
                popupWindow.dismiss();
            }
        });

        final Button ovalBtn = penSettingPopup.findViewById(R.id.ovalBtn);
        ovalBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MyLog.d("button", "oval shape button click"); // fixme nayeon

                de.setCurrentMode(Mode.DRAW);
                de.setCurrentType(ComponentType.OVAL);
                MyLog.i("drawing", "mode = " + de.getCurrentMode().toString() + ", type = " + de.getCurrentType().toString());
                popupWindow.dismiss();
            }
        });
    }

    public void exit() { // 좌측 상단 뒤로가기 버튼
        MyLog.e("back", "left top back pressed");

        exitOnClickListener.setRightBottomBackPressed(false);

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.context);
        if (client.isMaster()) {
            builder.setMessage("회의방을 종료하시겠습니까?");
        } else {
            builder.setMessage("회의방을 나가시겠습니까?");
        }
        builder.setPositiveButton(android.R.string.ok, exitOnClickListener);
        builder.setNeutralButton("저장 후 종료", new DialogInterface.OnClickListener() { // fixme nayeon
            @Override
            public void onClick(DialogInterface dialog, int which) {
                drawingViewModel.clickSave();
                exitOnClickListener.onClick(dialog, which);
            }
        }); // fixme nayeon
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                MyLog.d("button", "exit dialog cancel button click"); // fixme nayeon

                return;
            }
        });
        builder.create().show();
    }

    @Override
    public void onRightBottomBackPressed() {  // 우측 하단 뒤로가기 버튼
        MyLog.e("back", "right bottom back pressed");

        exitOnClickListener.setRightBottomBackPressed(true);

        AlertDialog dialog = new AlertDialog.Builder(MainActivity.context)
                .setMessage("앱을 종료하시겠습니까?")
                .setPositiveButton(android.R.string.ok, exitOnClickListener)
                .setNeutralButton("저장 후 종료", new DialogInterface.OnClickListener() { // fixme nayeon
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        drawingViewModel.clickSave();
                        exitOnClickListener.onClick(dialog, which);
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        return;
                    }
                })
                .create();
        dialog.show();
    }

    @Setter
    class ExitOnClickListener implements DialogInterface.OnClickListener {

        private boolean rightBottomBackPressed;

        @Override
        public void onClick(DialogInterface dialog, int which) {

            showExitProgressDialog();

            ConnectivityManager cm = (ConnectivityManager) MainActivity.context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm.getActiveNetwork() == null) {
                Log.e("네트워크", "network disconnected");

                if (rightBottomBackPressed) {
                    getActivity().finish();
                    android.os.Process.killProcess(android.os.Process.myPid());
                    System.exit(10);
                    return;
                }
                else {
                    drawingViewModel.back();
                    return;
                }
            }
            else if (cm.getActiveNetwork() != null && client.getClient().isConnected()){  // fixme hyen[0825]
                logger.uploadLogFile(ExitType.NORMAL); // fixme nayeon
            }

            String mode = "";
            if (data.isMaster()) {
                mode = "masterMode";
            }
            else {
                mode = "joinMode";
            }
            DatabaseTransaction dt = new DatabaseTransaction() {
                @Override
                public void completeLogin(DatabaseError error, String masterName, boolean topicError, boolean passwordError, boolean nameError) {  }

                @Override
                public void completeExit(DatabaseError error) {

                    if (error != null) {
                        exitProgressDialog.dismiss();

                        showDatabaseErrorAlert("데이터베이스 오류 발생", error.getMessage(), rightBottomBackPressed);
                        MyLog.e("transaction", error.getDetails());
                        return;
                    }

                    if (client.getClient().isConnected()) {
                        client.exitTask();
                        MonitoringDataWriter.getInstance().write();
                    }
                    if (rightBottomBackPressed) {
                        getActivity().finish();
                        android.os.Process.killProcess(android.os.Process.myPid());
                        System.exit(10);
                        return;
                    }
                    else {
                        drawingViewModel.back();
                        return;
                    }
                }
            };
            dt.runTransactionExit(data.getTopic(), data.getName(), mode);
        }
    }

    public void showDatabaseErrorAlert(String title, String message, final boolean rightBottomBackPressed) {

        AlertDialog dialog = new AlertDialog.Builder(MainActivity.context)
                .setTitle(title)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (client.getClient().isConnected()) {
                            client.exitTask();
                            MonitoringDataWriter.getInstance().write();
                        }
                        if (rightBottomBackPressed) {
                            getActivity().finish();
                            android.os.Process.killProcess(android.os.Process.myPid());
                            System.exit(10);
                            return;
                        }
                        else {
                            drawingViewModel.back();
                            return;
                        }
                    }
                })
                .create();

        dialog.show();
    }

    private void setProgressDialog() {
        progressDialog = new ProgressDialog(MainActivity.context);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setTitle("오류 발생");
        progressDialog.setMessage("로그 파일 업로드 중");
        progressDialog.setCancelable(false);
    }

    public void showExitProgressDialog() {
        exitProgressDialog = new ProgressDialog(MainActivity.context, R.style.MyProgressDialogStyle);
        exitProgressDialog.setMessage("Loading...");
        exitProgressDialog.setCanceledOnTouchOutside(false);
        exitProgressDialog.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        MqttMessageFormat messageFormat;
        /*if(de.getBackgroundImage() != null) { //fixme minj - 우선 배경 이미지는 하나만
            binding.backgroundView.removeAllViews();
        }*/ // fixme nayeon MQTT CALLBACK

        Bitmap imageBitmap = null;

        // fixme jiyeon[0813] - 이미지는 바이너리 데이터 자체를 보내도록 변경
        switch (requestCode) {
            case PICK_FROM_GALLERY:
                if (data == null) {
                    return;
                }

                try {
                    Uri uri = data.getData();
                    imageBitmap = MediaStore.Images.Media.getBitmap(getContext().getContentResolver(), uri);
                    String filePath = getRealPathFromURI(uri);
                    MyLog.e("Image", "Before(Gallery) : " + new File(getRealPathFromURI(uri)).length() + " Bytes");

                    imageBitmap = rotateBitmap(imageBitmap, filePath);

//                    imageBitmap = decodeSampledBitmapFromBitmap(de.bitmapToByteArray(imageBitmap));
//                    imageBitmap = rotateBitmap(imageBitmap, filePath);
//                    imageBitmap = decodeSampledBitmapFromBitmap(de.bitmapToByteArray(imageBitmap));
//                    // todo nayeon : check image file size
//                    MyLog.e("gallery", "Gallery Image File Size = " + new File(getRealPathFromURI(uri)).length() + " Bytes");
//                    MyLog.e("gallery", "Gallery Bitmap Byte Count = " + imageBitmap.getRowBytes() * imageBitmap.getHeight());
                } catch(IOException e) {
                    e.printStackTrace();
                }
                break;
            case PICK_FROM_CAMERA:
                try {
                    // fixme jiyeon
                    File file = new File(drawingViewModel.getPhotoPath());
                    MyLog.e("Image", "Before(Camera) : " + file.length() + " Bytes");

                    imageBitmap = MediaStore.Images.Media.getBitmap(getContext().getContentResolver(), Uri.fromFile(file));
                    imageBitmap = rotateBitmap(imageBitmap, drawingViewModel.getPhotoPath());

//                    // todo nayeon : check image file size
//                    if(file.exists()) { MyLog.e("camera", "Camera File Size = " + file.length() + " Bytes"); }
//                    imageBitmap = MediaStore.Images.Media.getBitmap(getContext().getContentResolver(), Uri.fromFile(file));
//                    imageBitmap = decodeSampledBitmapFromBitmap(de.bitmapToByteArray(imageBitmap));
//                    imageBitmap = rotateBitmap(imageBitmap, drawingViewModel.getPhotoPath());
//                    imageBitmap = decodeSampledBitmapFromBitmap(de.bitmapToByteArray(imageBitmap));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
        }

        if(imageBitmap == null) {
            Toast.makeText(getContext(), "이미지 로딩을 실패했습니다", Toast.LENGTH_LONG).show();
            return;
        }

        byte[] mqttImageMessage = de.bitmapToByteArray(imageBitmap);
        client.publish(client.getTopic_image(), mqttImageMessage);
        MyLog.e("Image", "After : " + mqttImageMessage.length + " Bytes");
        //

//        setBackgroundImage(imageBitmap); // fixme nayeon
//        messageFormat = new MqttMessageFormat(de.getMyUsername(), Mode.BACKGROUND_IMAGE, de.bitmapToByteArray(imageBitmap));
//        client.publish(client.getTopic_data(), JSONParser.getInstance().jsonWrite(messageFormat));
    }

//    private void setBackgroundImage(Bitmap imageBitmap) {
//        binding.backgroundView.removeAllViews(); // fixme nayeon 배경이미지 하나
//
//        de.setBackgroundImage(imageBitmap);
//
//        WarpingControlView imageView = new WarpingControlView(getContext());
//        imageView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
//        imageView.setImage(de.getBackgroundImage()); // invalidate
//        client.getBinding().backgroundView.addView(imageView);
//    }

    @Override
    public void onResume() {
        super.onResume();
        ((MainActivity)getActivity()).setToolbarVisible();
        ((MainActivity)getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.application_menu, menu);
    }

    // fixme jiyeon
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {

            // fixme jiyeon[0428]
//            case R.id.drawing_mic:
//                boolean click = drawingViewModel.clickMic();
//                if (click) {
//                    item.setIcon(R.drawable.mic);
//                } else {
//                    item.setIcon(R.drawable.mic_slash);
//                }
//                break;
//            case R.id.drawing_speaker:
//                int mode = drawingViewModel.clickSpeaker();
//                if (mode == 0) { // speaker off
//                    item.setIcon(R.drawable.speakerslash);
//                } else if (mode == 1) { // speaker on
//                    item.setIcon(R.drawable.speaker1);
//                } else if (mode == 2) { // speaker loud
//                    item.setIcon(R.drawable.speaker3);
//                }
//                break;
            //
            case R.id.gallery:
                drawingViewModel.getImageFromGallery(DrawingFragment.this);
                break;
            case R.id.camera:
                drawingViewModel.getImageFromCamera(DrawingFragment.this);
                break;
//            case R.id.drawing_search:
//                drawingViewModel.clickSearch(getView());
//                break;
            case R.id.drawing_invite:
                drawingViewModel.clickInvite();
                break;
            case R.id.drawing_save:
                drawingViewModel.clickSave();

                break;
        }

        return super.onOptionsItemSelected(item);
    }

    // fixme jiyeon
    private Bitmap rotateBitmap(Bitmap bitmap, String path) {
        MyLog.e("Image", "rotate bitmap start");
        ExifInterface exif = null;
        try {
            exif = new ExifInterface(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
        int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);

        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.setRotate(90);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.setRotate(180);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.setRotate(-90);
                break;
            default:
                MyLog.e("Image", "rotate bitmap end 1");
                return bitmap;
        }
        try {
            MyLog.e("Image", "rotate bitmap end 2");
            Bitmap bmRotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            bitmap.recycle();
            return bmRotated;
        }
        catch (OutOfMemoryError e) {
            e.printStackTrace();
            return null;
        }
    }

    // fixme jiyeon
    private String getRealPathFromURI(Uri contentURI) {
        String result; Cursor cursor = getContext().getContentResolver().query(contentURI, null, null, null, null);
        if (cursor == null) {
            result = contentURI.getPath();
        } else {
            cursor.moveToFirst();
            int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            result = cursor.getString(idx); cursor.close();
        }
        return result;
    }


    // fixme nayeon
    public static int calculateInSampleSize(BitmapFactory.Options options) {
        MyLog.e("image", "calculate image size start");

        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;

        final int reqWidth = 800;
        final int reqHeight = 1000;

        MyLog.e("image", "option width = " + width + ", option height = " + height);

        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        MyLog.e("image", "inSampleSize = " + inSampleSize);

        MyLog.e("image", "calculate image size end");
        return inSampleSize;
    }

    public Bitmap decodeSampledBitmapFromBitmap(byte[] bitmapArray/*, int reqWidth, int reqHeight*/) {

        MyLog.e("image", "resize bitmap start");

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(bitmapArray, 0, bitmapArray.length, options);

        options.inSampleSize = calculateInSampleSize(options);
        // options.inSampleSize = 2;

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;

        MyLog.e("image", "resize bitmap end");
        return BitmapFactory.decodeByteArray(bitmapArray, 0, bitmapArray.length, options);
    }

    @Override
    public void onStart() {
        super.onStart();
        MyLog.i("lifeCycle", "DrawingFragment onStart()");
    }

    @Override
    public void onPause() {
        super.onPause();
        MyLog.i("lifeCycle", "DrawingFragment onPause()");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        MyLog.i("lifeCycle", "DrawingFragment onDestroyView()");
//        if (exitOnClickListener != null) {
//            exitOnClickListener = null;
//        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        MyLog.i("lifeCycle", "DrawingFragment onDestroy()");

        // 꼭 여기서 처리 해줘야 하는 부분
        client.getDe().removeAllDrawingData();
        client.getUserList().clear();
        client.getTh().interrupt();
        client.setIsMid(true);

        // fixme jiyeon[0826] - 오디오 처리
        if (drawingViewModel.isMicFlag()) {
            drawingViewModel.getRecThread().setFlag(false);
        }
//        drawingViewModel.getRecThread().stopRecording();
//        drawingViewModel.getRecThread().interrupt();

        try {
            if (client.getClient().isConnected()) {
                client.getClient().unsubscribe(client.getTopic_audio());
            }
        } catch (MqttException e) {
            e.printStackTrace();
        }

        for (AudioPlayThread audioPlayThread : client.getAudioPlayThreadList()) {
            if (drawingViewModel.isSpeakerFlag()) {
                audioPlayThread.setFlag(false);
                AudioManager audioManager = (AudioManager) MainActivity.context.getSystemService(Service.AUDIO_SERVICE);
                audioManager.setSpeakerphoneOn(false);
            }

//            audioPlayThread.stopPlaying();
            synchronized (audioPlayThread.getBuffer()) {
                audioPlayThread.getBuffer().clear();
            }
//            audioPlayThread.interrupt();
        }
        client.getAudioPlayThreadList().clear();
        //

        if (client.getClient().isConnected()) {
            if (!client.isExitCompleteFlag()) {
                MyLog.e("exit", "비정상 종료");
                client.exitTask();
                MonitoringDataWriter.getInstance().write();
            }
            try {
                client.getClient().disconnect();
                client.getClient().close();
                client.getClient2().disconnect();
                client.getClient2().close();
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }

        if (exitProgressDialog != null && exitProgressDialog.isShowing()) {
            exitProgressDialog.dismiss();
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        MyLog.i("lifeCycle", "DrawingFragment onDetach()");
        ((MainActivity)getContext()).setOnRightBottomBackListener(null);
    }
}
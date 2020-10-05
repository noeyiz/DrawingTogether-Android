package com.hansung.drawingtogether.view.drawing;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Point;
import android.media.AudioManager;
import android.media.ExifInterface;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.navigation.fragment.NavHostFragment;

import com.github.twocoffeesoneteam.glidetovectoryou.GlideToVectorYou;
import com.google.firebase.database.DatabaseError;
import com.hansung.drawingtogether.R;
import com.hansung.drawingtogether.data.remote.model.AliveThread;
import com.hansung.drawingtogether.data.remote.model.ExitType;
import com.hansung.drawingtogether.data.remote.model.Logger;
import com.hansung.drawingtogether.data.remote.model.MQTTClient;
import com.hansung.drawingtogether.data.remote.model.MyLog;
import com.hansung.drawingtogether.databinding.FragmentDrawingBinding;

import com.hansung.drawingtogether.monitoring.MonitoringDataWriter;
import com.hansung.drawingtogether.monitoring.Velocity;
import com.hansung.drawingtogether.monitoring.ComponentCount;
import com.hansung.drawingtogether.monitoring.MonitoringRunnable;

import com.hansung.drawingtogether.view.main.AutoDrawMessage;
import com.hansung.drawingtogether.view.main.DatabaseTransaction;
import com.hansung.drawingtogether.view.main.JoinMessage;
import com.hansung.drawingtogether.view.main.MQTTSettingData;
import com.hansung.drawingtogether.view.main.MainActivity;

import com.hansung.drawingtogether.view.NavigationCommand;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

import lombok.Getter;
import lombok.Setter;


@Getter
public class DrawingFragment extends Fragment implements MainActivity.OnRightBottomBackListener {

    private final int PICK_FROM_GALLERY = 0;
    private final int PICK_FROM_CAMERA = 1;

    private Point size;

    private MQTTClient client = MQTTClient.getInstance();
    private MQTTSettingData data = MQTTSettingData.getInstance();

    private DrawingEditor de = DrawingEditor.getInstance();
    private AttributeManager am = AttributeManager.getInstance();
    private Logger logger = Logger.getInstance();

    private FragmentDrawingBinding binding;
    private DrawingViewModel drawingViewModel;
    private InputMethodManager inputMethodManager;

    private ExitOnClickListener exitOnClickListener;

    private AliveThread aliveTh = AliveThread.getInstance();

    private ProgressDialog progressDialog;

    private MonitoringRunnable monitoringRunnable = MonitoringRunnable.getInstance();

    private ProgressDialog exitProgressDialog;

    float dX, dY;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        ((MainActivity)context).setOnRightBottomBackListener(this);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        MyLog.i("LifeCycle", "DrawingFragment onCreateView()");

        exitOnClickListener = new ExitOnClickListener();
        exitOnClickListener.setRightBottomBackPressed(false);

        binding = FragmentDrawingBinding.inflate(inflater, container, false);

        JSONParser.getInstance().initJsonParser(this); // JSON Parser 초기화 (toss DrawingFragmenet)
        MyLog.i("monitoring", "check parser init");

        drawingViewModel = ViewModelProviders.of(this).get(DrawingViewModel.class);

        client.setDrawingFragment(this);
        de.setDrawingFragment(this);

        de.setTextMoveBorderDrawable(getResources().getDrawable(R.drawable.text_move_border)); // 텍스트 테두리 설정
        de.setTextFocusBorderDrawable(getResources().getDrawable(R.drawable.text_focus_border));
        de.setTextHighlightBorderDrawable(getResources().getDrawable(R.drawable.text_highlight_border));

        am.setBinding(binding); // Palette Manager 의 FragmentDrawingBinding 변수 초기화
        am.setListener(); // 리스너 초기화
        am.showCurrentColor(Color.parseColor(de.getStrokeColor())); // 현재 색상 보여주기

        binding.drawBtn.setBackgroundColor(Color.rgb(233, 233, 233)); // 초기 얇은 펜으로 설정
        binding.drawingViewContainer.setOnDragListener(new FrameLayoutDragListener());
        inputMethodManager = (InputMethodManager) Objects.requireNonNull(getActivity()).getSystemService(Context.INPUT_METHOD_SERVICE);

        // 디바이스 화면 size 구하기
        Display display = getActivity().getWindowManager().getDefaultDisplay();
        size = new Point();
        display.getSize(size);
        MyLog.i("drawing view size in fragment", size.x + ", " + size.y * 0.83);

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

        if(de.getBackgroundImage() != null) { // backgroundImage 다시 붙이기
            binding.backgroundView.setImage(de.getBackgroundImage());
        }
        MyLog.i("pre pub join message", this.getSize().x + ", " + this.getSize().y);

        if(de.getMainBitmap() == null) {

            /* Join Message Publish */
            JoinMessage joinMessage = new JoinMessage(data.getName());
            MqttMessageFormat messageFormat = new MqttMessageFormat(joinMessage);
            client.publish(data.getTopic() + "_join", JSONParser.getInstance().jsonWrite(messageFormat));
            MyLog.i("Login", data.getName() + " Join Message Publish");

            /* Alive Thread Start */
            /* T초(예: 10초)에 한번씩 Alive Message Publish */
            aliveTh.setSecond(10000);
            Thread th = new Thread(aliveTh);
            th.start();
            client.setThread(th);

            if(client.isMaster()) {
                MyLog.i("monitoring", "mqtt client class init func. check master. i'am master.");
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
            MyLog.e("alive", "DrawingFragment aliveBackground: " + data.isAliveBackground());
            */

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

        drawingViewModel.getAutoDrawImage().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(final String url) {
                Toast.makeText(getContext(), "이미지를 원하는 위치로 드래그해주세요.", Toast.LENGTH_SHORT).show();
                final ImageView imageView = new ImageView(getContext());
                imageView.setLayoutParams(new LinearLayout.LayoutParams(300, 300));
                binding.drawingViewContainer.addView(imageView);
                GlideToVectorYou.init().with(getContext()).load(Uri.parse(url),imageView);
                imageView.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View view, MotionEvent event) {
                        switch (event.getAction()) {
                            case MotionEvent.ACTION_DOWN:
                                dX = view.getX() - event.getRawX();
                                dY = view.getY() - event.getRawY();
                                break;
                            case MotionEvent.ACTION_MOVE:
                                view.animate()
                                        .x(event.getRawX() + dX - (view.getWidth() / 2))
                                        .y(event.getRawY() + dY - (view.getHeight() / 2))
                                        .setDuration(0)
                                        .start();
                                break;
                            case MotionEvent.ACTION_UP:
                                imageView.setOnTouchListener(null);
                                AutoDrawMessage autoDrawMessage = new AutoDrawMessage(data.getName(), url, view.getX(), view.getY());
                                MqttMessageFormat messageFormat = new MqttMessageFormat(de.getMyUsername(), de.getCurrentMode(), de.getCurrentType(), autoDrawMessage);
                                client.publish(client.getTopic_data(), JSONParser.getInstance().jsonWrite(messageFormat));
                                de.addAutoDraw(autoDrawMessage.getUrl(), imageView);
                                break;
                            default:
                                return false;
                        }
                        return true;
                    }
                });
            }
        });

        binding.userInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyLog.i("button", "user info");

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

        if (layout == R.layout.popup_pen_mode) {
            setPenPopupClickListener(penSettingPopup, popupWindow);
        }
        else if(layout == R.layout.popup_eraser_mode) {
            setEraserPopupClickListener(penSettingPopup, popupWindow);
        }
        else if(layout == R.layout.popup_shape_mode) {
            setShapePopupClickListener(penSettingPopup, popupWindow);
        }
    }


    private void setPenPopupClickListener(View penSettingPopup, final PopupWindow popupWindow) {
        final Button drawBtn1 = penSettingPopup.findViewById(R.id.drawBtn1);
        drawBtn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MyLog.d("button", "draw1 pen button click");
                de.setStrokeWidth(10);
                popupWindow.dismiss();
            }
        });
        final Button drawBtn2 = penSettingPopup.findViewById(R.id.drawBtn2);
        drawBtn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyLog.d("button", "draw2 pen button click");
                popupWindow.dismiss();de.setStrokeWidth(20);
            }
        });
        final Button drawBtn3 = penSettingPopup.findViewById(R.id.drawBtn3);
        drawBtn3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyLog.d("button", "draw3 pen button click");
                popupWindow.dismiss();
                de.setStrokeWidth(30);
            }
        });
    }


    private void setEraserPopupClickListener(View penSettingPopup, final PopupWindow popupWindow) {
        final Button clearBtn = penSettingPopup.findViewById(R.id.clearBtn);
        if(client.isMaster()) {
            clearBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    MyLog.d("button", "clear button click");

                    binding.drawingView.clear();
                    popupWindow.dismiss();
                }
            });
        }
        else {
            clearBtn.setTextColor(Color.LTGRAY);
            clearBtn.setEnabled(false);
        }

        final Button backgroundEraserBtn = penSettingPopup.findViewById(R.id.backgroundEraserBtn);
        if(client.isMaster()) {
            backgroundEraserBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    MyLog.d("button", "background eraser button click");

                    binding.drawingView.clearBackgroundImage();
                    popupWindow.dismiss();
                }
            });
        }
        else {
            backgroundEraserBtn.setTextColor(Color.LTGRAY);
            backgroundEraserBtn.setEnabled(false);
        }

        final Button viewEraserBtn = penSettingPopup.findViewById(R.id.viewEraserBtn);
        if(client.isMaster()) {
            viewEraserBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    MyLog.d("button", "drawing clear button click");

                    binding.drawingView.clearDrawingView();
                    popupWindow.dismiss();
                }
            });
        }
        else {
            viewEraserBtn.setTextColor(Color.LTGRAY);
            viewEraserBtn.setEnabled(false);
        }
    }

    private void setShapePopupClickListener(View penSettingPopup, final PopupWindow popupWindow) {
        final Button rectBtn = penSettingPopup.findViewById(R.id.rectBtn);
        rectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MyLog.d("button", "rect shape button click");

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
                MyLog.d("button", "oval shape button click");

                de.setCurrentMode(Mode.DRAW);
                de.setCurrentType(ComponentType.OVAL);
                MyLog.i("drawing", "mode = " + de.getCurrentMode().toString() + ", type = " + de.getCurrentType().toString());
                popupWindow.dismiss();
            }
        });
    }

    /* 좌측 상단 백버튼 클릭 시 수행 */
    public void exit() {

        MyLog.i("Back Button", "Left Top Back Button Pressed");

        exitOnClickListener.setRightBottomBackPressed(false);

        /* 마스터 - 회의방 종료 */
        /* 마스터 제외 참가자 - 회의방 퇴장 */
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.context);
        if (client.isMaster()) {
            builder.setMessage("회의방을 종료하시겠습니까?");
        } else {
            builder.setMessage("회의방을 나가시겠습니까?");
        }
        builder.setPositiveButton(android.R.string.ok, exitOnClickListener);
        builder.setNeutralButton("저장 후 종료", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                drawingViewModel.clickSave();
                exitOnClickListener.onClick(dialog, which);
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                MyLog.d("button", "exit dialog cancel button click");
                return;
            }
        });
        builder.create().show();
    }

    @Override
    /* 우측 하단 백버튼 클릭 시 수행 */
    public void onRightBottomBackPressed() {

        MyLog.i("Back Button", "Right Bottom Back Button Pressed");

        exitOnClickListener.setRightBottomBackPressed(true);

        /* 앱 종료 */
        AlertDialog dialog = new AlertDialog.Builder(MainActivity.context)
                .setMessage("앱을 종료하시겠습니까?")
                .setPositiveButton(android.R.string.ok, exitOnClickListener)
                .setNeutralButton("저장 후 종료", new DialogInterface.OnClickListener() {
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
        /* 백버튼 - 확인 클릭 시 */
        public void onClick(DialogInterface dialog, int which) {

            showExitProgressDialog();

            /* 네트워크 연결 상태 확인 */
            ConnectivityManager cm = (ConnectivityManager) MainActivity.context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm.getActiveNetwork() == null) {
                MyLog.i("네트워크", "network disconnected");

                if (rightBottomBackPressed) {
                    /* 앱 종료 */
                    getActivity().finish();
                    android.os.Process.killProcess(android.os.Process.myPid());
                    System.exit(10);
                    return;
                }
                else {
                    /* 메인 화면으로 이동 */
                    drawingViewModel.back();
                    return;
                }
            }
            else if (cm.getActiveNetwork() != null && client.getClient().isConnected()) {
                logger.uploadLogFile(ExitType.NORMAL); // 정상종료일때 로그 올리기
            }

            String mode = "";
            if (data.isMaster()) {
                mode = "masterMode";
            }
            else {
                mode = "joinMode";
            }
            /* Firebase Realtime Database Transaction 수행 */
            DatabaseTransaction dt = new DatabaseTransaction() {
                @Override
                public void completeLogin(DatabaseError error, String masterName, boolean topicError, boolean passwordError, boolean nameError) {  }

                @Override
                public void completeExit(DatabaseError error) {

                    if (error != null) {
                        exitProgressDialog.dismiss();
                        showDatabaseErrorAlert("데이터베이스 오류 발생", error.getMessage());
                        MyLog.i("Database transaction", error.getDetails());
                        return;
                    }

                    if (client.getClient().isConnected()) {
                        client.exitTask();
                        if(client.isMaster()) {
                            MonitoringDataWriter.getInstance().write();
                        }
                    }
                    if (rightBottomBackPressed) {
                        /* 앱 종료 */
                        getActivity().finish();
                        android.os.Process.killProcess(android.os.Process.myPid());
                        System.exit(10);
                        return;
                    }
                    else {
                        /* 메인 화면으로 이동 */
                        drawingViewModel.back();
                        return;
                    }
                }
            };
            dt.runTransactionExit(data.getTopic(), data.getName(), mode);
        }
    }

    /* Firebase Realtime Database Transaction 수행 중 오류 발생 알림 */
    public void showDatabaseErrorAlert(String title, String message) {

        AlertDialog dialog = new AlertDialog.Builder(MainActivity.context)
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
        Bitmap imageBitmap = null;

        // 이미지는 바이너리 데이터 자체를 보내도록 변경
        switch (requestCode) {
            case PICK_FROM_GALLERY:
                if (data == null) {
                    return;
                }

                try {
                    Uri uri = data.getData();
                    imageBitmap = MediaStore.Images.Media.getBitmap(getContext().getContentResolver(), uri);
                    String filePath = getRealPathFromURI(uri);
                    MyLog.i("Image", "Before(Gallery) : " + new File(getRealPathFromURI(uri)).length() + " Bytes");

                    imageBitmap = rotateBitmap(imageBitmap, filePath);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case PICK_FROM_CAMERA:
                try {
                    File file = new File(drawingViewModel.getPhotoPath());
                    MyLog.i("Image", "Before(Camera) : " + file.length() + " Bytes");

                    imageBitmap = MediaStore.Images.Media.getBitmap(getContext().getContentResolver(), Uri.fromFile(file));
                    imageBitmap = rotateBitmap(imageBitmap, drawingViewModel.getPhotoPath());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
        }

        if (imageBitmap == null) {
            Toast.makeText(getContext(), "이미지 로딩을 실패했습니다", Toast.LENGTH_LONG).show();
            return;
        }

        byte[] mqttImageMessage = de.bitmapToByteArray(imageBitmap);
        client.publish(client.getTopic_image(), mqttImageMessage);
        MyLog.i("Image", "After : " + mqttImageMessage.length + " Bytes");
        //
    }

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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {

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
            case R.id.gallery:
                drawingViewModel.getImageFromGallery(DrawingFragment.this);
                break;
            case R.id.camera:
                drawingViewModel.getImageFromCamera(DrawingFragment.this);
                break;
            case R.id.drawing_invite:
                drawingViewModel.clickInvite();
                break;
            case R.id.drawing_save:
                drawingViewModel.clickSave();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    /* Bitmap의 Orientation 정보에 따라 알맞게 회전 */
    private Bitmap rotateBitmap(Bitmap bitmap, String path) {
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
                return bitmap;
        }
        try {
            Bitmap bmRotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            bitmap.recycle();
            return bmRotated;
        }
        catch (OutOfMemoryError e) {
            e.printStackTrace();
            return null;
        }
    }

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

    @Override
    public void onStart() {
        super.onStart();
        MyLog.i("LifeCycle", "DrawingFragment onStart()");
    }

    @Override
    public void onPause() {
        super.onPause();
        MyLog.i("LifeCycle", "DrawingFragment onPause()");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        MyLog.i("LifeCycle", "DrawingFragment onDestroyView()");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        MyLog.i("LifeCycle", "DrawingFragment onDestroy()");

        /* 데이터 초기화 */
        client.getDe().removeAllDrawingData();
        client.getUserList().clear();
        client.getTh().interrupt();
        client.setIsMid(true);
        client.getConnOpts().setAutomaticReconnect(false);

//        /* 오디오 처리 */
//        /* Record Thread와 PlayThreadList에 있는 모든 Play Thread Interrupt */
//        if (drawingViewModel.isMicFlag()) {
//            drawingViewModel.getRecThread().setFlag(false);
//        }
//
//        drawingViewModel.getRecThread().stopRecording();
//        drawingViewModel.getRecThread().interrupt();
//
//        try {
//            if (client.getClient().isConnected()) {
//                client.getClient().unsubscribe(client.getTopic_audio());
//            }
//        } catch (MqttException e) {
//            e.printStackTrace();
//        }
//
//        for (AudioPlayThread audioPlayThread : client.getAudioPlayThreadList()) {
//            if (drawingViewModel.isSpeakerFlag()) {
//                audioPlayThread.setFlag(false);
//                AudioManager audioManager = (AudioManager) MainActivity.context.getSystemService(Service.AUDIO_SERVICE);
//                audioManager.setSpeakerphoneOn(false);
//            }
//
//            audioPlayThread.stopPlaying();
//            synchronized (audioPlayThread.getBuffer()) {
//                audioPlayThread.getBuffer().clear();
//            }
//            audioPlayThread.interrupt();
//        }
//        client.getAudioPlayThreadList().clear();

        /* MQTT 클라이언트 연결 해제 */
        if (client.getClient().isConnected()) {
            if (!client.isExitCompleteFlag()) {
                MyLog.i("Exit", "비정상 종료");
                client.exitTask();
                if(client.isMaster()) {
                    MonitoringDataWriter.getInstance().write();
                }
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
        MyLog.i("LifeCycle", "DrawingFragment onDetach()");
        ((MainActivity)getContext()).setOnRightBottomBackListener(null);
    }
}
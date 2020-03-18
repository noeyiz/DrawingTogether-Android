package com.hansung.drawingtogether.view.drawing;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Point;
import android.media.ExifInterface;
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
import lombok.Getter;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.hansung.drawingtogether.R;
import com.hansung.drawingtogether.data.remote.model.AliveThread;
import com.hansung.drawingtogether.data.remote.model.MQTTClient;
import com.hansung.drawingtogether.data.remote.model.User;
import com.hansung.drawingtogether.databinding.FragmentDrawingBinding;
import com.hansung.drawingtogether.view.NavigationCommand;
import com.hansung.drawingtogether.view.main.DeleteMessage;
import com.hansung.drawingtogether.view.main.ExitMessage;
import com.hansung.drawingtogether.view.main.JoinMessage;

import com.hansung.drawingtogether.view.main.MQTTSettingData;
import com.hansung.drawingtogether.view.main.MainActivity;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

@Getter
public class DrawingFragment extends Fragment implements MainActivity.onKeyBackPressedListener{  // fixme hyeyeon

    private final int PICK_FROM_GALLERY = 0;
    private final int PICK_FROM_CAMERA = 1;

    Point size;

    private MQTTClient client = MQTTClient.getInstance();
    private MQTTSettingData data = MQTTSettingData.getInstance();  // fixme hyeyeon

    private DrawingEditor de = DrawingEditor.getInstance();
    private PaletteManager pm = PaletteManager.getInstance(); // fixme nayeon
    private FragmentDrawingBinding binding;
    private DrawingViewModel drawingViewModel;
    private InputMethodManager inputMethodManager;

    // fixme hyeyeon[2]
    private DatabaseReference databaseReference;
    private ExitOnClickListener exitOnClickListener;
    //

    private long lastTimeBackPressed;  // fixme hyeyeon[3]

    //private LinearLayout topToolLayout;
    //private Button doneBtn;

    // fixme hyeyeon
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        ((MainActivity)context).setOnKeyBackPressedListener(this);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.e("DrawingFragment", "onCreateView");

        // fixme hyeyeon[2]
        databaseReference = FirebaseDatabase.getInstance().getReference();
        exitOnClickListener = new ExitOnClickListener();
        //

        binding = FragmentDrawingBinding.inflate(inflater, container, false);

        JSONParser.getInstance().initJsonParser(this); // fixme nayeon ☆☆☆ JSON Parser 초기화 (toss DrawingFragmenet)

        drawingViewModel = ViewModelProviders.of(this).get(DrawingViewModel.class);

        client.setDrawingFragment(this);
        de.setDrawingFragment(this);

        de.setTextMoveBorderDrawable(getResources().getDrawable(R.drawable.text_move_border)); // fixme nayeon 텍스트 테두리 설정
        de.setTextFocusBorderDrawable(getResources().getDrawable(R.drawable.text_focus_border));

        // fixme nayeon
        pm.setBinding(binding); // Palette Manager 의 FragmentDrawingBinding 변수 초기화
        pm.setListener(); // 리스너 초기화
        pm.setPaletteButtonListener(); // 색상 버튼들의 리스너 세팅
        pm.showCurrentColor(de.getStrokeColor()); // 현재 색상 보여주기

        binding.drawBtn1.setBackgroundColor(Color.rgb(233, 233, 233)); // 초기 얇은 펜으로 설정
        binding.drawingViewContainer.setOnDragListener(new FrameLayoutDragListener());
        inputMethodManager = (InputMethodManager) Objects.requireNonNull(getActivity()).getSystemService(Context.INPUT_METHOD_SERVICE);

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
            binding.backgroundView.removeAllViews();
            ImageView imageView = new ImageView(this.getContext());
            imageView.setLayoutParams(new LinearLayout.LayoutParams(this.getSize().x, ViewGroup.LayoutParams.MATCH_PARENT));
            imageView.setImageBitmap(de.getBackgroundImage());
            binding.backgroundView.addView(imageView);
        }

        if(de.getDrawingBitmap() == null) { // join 메시지 publish
            JoinMessage joinMessage = new JoinMessage(drawingViewModel.getName());
            MqttMessageFormat messageFormat = new MqttMessageFormat(joinMessage);
            client.publish(drawingViewModel.getTopic() + "_join", JSONParser.getInstance().jsonWrite(messageFormat));

            // fixme hyeyeon
            AliveThread aliveTh = AliveThread.getInstance();
            aliveTh.setKill(false);
            aliveTh.setSecond(2000);
            Thread th = new Thread(aliveTh);
            th.start();
            client.setThread(th);
        }

        // 디바이스 화면 size 구하기
        Display display = getActivity().getWindowManager().getDefaultDisplay();
        size = new Point();
        display.getSize(size);

        // 디바이스 화면 넓이의 3배 = 드로잉뷰 넓이
        ViewGroup.LayoutParams layoutParams = binding.drawingView.getLayoutParams();
        //layoutParams.width = size.x*3;
        layoutParams.width = size.x;
        binding.drawingView.setLayoutParams(layoutParams);

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

/*  // fixme hyeyeon
        JSONParser.getInstance().initJsonParser(this); // fixme nayeon ☆☆☆ JSON Parser 초기화 (toss DrawingFragmenet)

        client.init(topic, name, master, drawingViewModel, ip, port);
        client.setDrawingFragment(this);
        client.setCallback();
        client.subscribe(topic + "_join");
        client.subscribe(topic + "_exit");
        client.subscribe(topic + "_delete");
        client.subscribe(topic + "_data");
        client.subscribe(topic + "_mid");

        // client.publish(topic_data ~~);

        // fixme nayeon 중간자 join 메시지 보내기 (메시지 형식 변경)
        JoinMessage joinMessage = new JoinMessage(name);
        MqttMessageFormat messageFormat = new MqttMessageFormat(joinMessage);
        client.publish(topic + "_join", JSONParser.getInstance().jsonWrite(messageFormat));
        //
*/

        binding.userInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (binding.userPrint.getVisibility() == View.VISIBLE)
                    binding.userPrint.setVisibility(View.INVISIBLE);
                else
                    binding.userPrint.setVisibility(View.VISIBLE);
            }
        });
        binding.setVm(drawingViewModel);
        binding.setLifecycleOwner(this);

        setHasOptionsMenu(true);

        ((MainActivity)getActivity()).setOnBackListener(new MainActivity.OnBackListener() {
            @Override
            public void onBack() {
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
                    binding.drawingView.clearBackgroundImage();
                    popupWindow.dismiss();
                }
            });
        }
        else {
            backgroundEraserBtn.setEnabled(false);
        }

        final Button clearBtn = penSettingPopup.findViewById(R.id.clearBtn);
        if(client.isMaster()) {
            clearBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    binding.drawingView.clear();
                    popupWindow.dismiss();
                }
            });
        }
        else {
            clearBtn.setEnabled(false);
        }
    }

    private void setShapePopupClickListener(View penSettingPopup, final PopupWindow popupWindow) {
        final Button rectBtn = penSettingPopup.findViewById(R.id.rectBtn);
        rectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                de.setCurrentMode(Mode.DRAW);
                de.setCurrentType(ComponentType.RECT);
                Log.i("drawing", "mode = " + de.getCurrentMode().toString() + ", type = " + de.getCurrentType().toString());
                popupWindow.dismiss();
            }
        });

        final Button ovalBtn = penSettingPopup.findViewById(R.id.ovalBtn);
        ovalBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                de.setCurrentMode(Mode.DRAW);
                de.setCurrentType(ComponentType.OVAL);
                Log.i("drawing", "mode = " + de.getCurrentMode().toString() + ", type = " + de.getCurrentType().toString());
                popupWindow.dismiss();
            }
        });
    }

    // fixme hyeyeon[2]-messageArrived 콜백에서 처리 -> 나가기 버튼 누른 후 바로 처리하도록 변경
    public void exit() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setItems(R.array.exit, exitOnClickListener);
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }
    //

    // fixme hyeyeon[3] - 2초 안에 2번 누르면 종료 하도록 변경
    @Override
    public void onBackKey() {
        Log.e("kkankkan", "드로잉프레그먼트 onbackpressed");

        if (System.currentTimeMillis() - lastTimeBackPressed < 2000) {
            ExitMessage exitMessage = new ExitMessage(client.getMyName());
            MqttMessageFormat messageFormat = new MqttMessageFormat(exitMessage);
            client.publish(client.getTopic() + "_exit", JSONParser.getInstance().jsonWrite(messageFormat));
            client.setExitPublish(true);
            Log.e("kkankkan", "exit publish");
            client.exitTask();
            getActivity().finish();
            return;
        }
        lastTimeBackPressed = System.currentTimeMillis();
        Toast.makeText(getContext(), "뒤로 버튼을 한 번 더 누르면 종료됩니다", Toast.LENGTH_SHORT).show();
    }
    //

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        MqttMessageFormat messageFormat;
        if(de.getBackgroundImage() != null) { //fixme minj - 우선 배경 이미지는 하나만
            binding.backgroundView.removeAllViews();
        }

        switch (requestCode) {
            case PICK_FROM_GALLERY:
                if (data == null) {
                    return;
                }
                // fixme jiyeon
                try {
                    Uri uri = data.getData();
                    Bitmap galleryBitmap = MediaStore.Images.Media.getBitmap(getContext().getContentResolver(), uri);
                    String filePath = getRealPathFromURI(uri);
                    galleryBitmap = rotateBitmap(galleryBitmap, filePath);

                    messageFormat = new MqttMessageFormat(de.getMyUsername(), Mode.BACKGROUND_IMAGE, de.bitmapToByteArray(galleryBitmap));
                    client.publish(client.getTopic_data(), JSONParser.getInstance().jsonWrite(messageFormat));

//                Uri uri = data.getData();
//                Bitmap galleryBitmap = null;
//                try {
//                    galleryBitmap = rotateBitmap(MediaStore.Images.Media.getBitmap(getContext().getContentResolver(), uri), PICK_FROM_GALLERY);
//                    galleryBitmap = resizeBitmap(galleryBitmap);
//                    ImageView imageView = new ImageView(getContext());
//                    imageView.setLayoutParams(new LinearLayout.LayoutParams(size.x, ViewGroup.LayoutParams.MATCH_PARENT));
//                    imageView.setImageBitmap(galleryBitmap);
//                    de.setBackgroundImage(galleryBitmap);
//                    binding.backgroundView.addView(imageView);
//
//                    messageFormat = new MqttMessageFormat(de.getUsername(), Mode.BACKGROUND_IMAGE, de.bitmapToByteArray(galleryBitmap));
//                    client.publish(client.getTopic_data(), JSONParser.getInstance().jsonWrite(messageFormat));

                } catch(IOException e) {
                    e.printStackTrace();
                }
                break;
            case PICK_FROM_CAMERA:
                try {
                    // fixme jiyeon
                    File file = new File(drawingViewModel.getPhotoPath());
                    Bitmap cameraBitmap = MediaStore.Images.Media.getBitmap(getContext().getContentResolver(), Uri.fromFile(file));
                    cameraBitmap = rotateBitmap(cameraBitmap, drawingViewModel.getPhotoPath());

                    messageFormat = new MqttMessageFormat(de.getMyUsername(), Mode.BACKGROUND_IMAGE, de.bitmapToByteArray(cameraBitmap));
                    client.publish(client.getTopic_data(), JSONParser.getInstance().jsonWrite(messageFormat));

//                    File file = new File(drawingViewModel.getPhotoPath());
//                    Bitmap cameraBitmap = rotateBitmap(MediaStore.Images.Media.getBitmap(getContext().getContentResolver(), Uri.fromFile(file)), PICK_FROM_CAMERA);
//                    cameraBitmap = resizeBitmap(cameraBitmap);
//                    if (cameraBitmap != null) {
//                        ImageView imageView = new ImageView(getContext());
//                        imageView.setLayoutParams(new LinearLayout.LayoutParams(size.x, ViewGroup.LayoutParams.MATCH_PARENT));
//                        imageView.setImageBitmap(cameraBitmap);
//                        de.setBackgroundImage(cameraBitmap);
//                        binding.backgroundView.addView(imageView);
//
//                        messageFormat = new MqttMessageFormat(de.getMyUsername(), Mode.BACKGROUND_IMAGE, de.bitmapToByteArray(cameraBitmap));
//                        client.publish(client.getTopic_data(), JSONParser.getInstance().jsonWrite(messageFormat));
//                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        ((MainActivity)getActivity()).setToolbarTitle("Drawing");
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
            case R.id.drawing_search:
                drawingViewModel.clickSearch(getView());
                break;
            case R.id.gallery:
                drawingViewModel.getImageFromGallery(DrawingFragment.this);
                break;
            case R.id.camera:
                drawingViewModel.getImageFromCamera(DrawingFragment.this);
                break;
            case R.id.drawing_plus:
                drawingViewModel.plusUser(DrawingFragment.this, data.getTopic(), data.getPassword());  // fixme hyeyeon
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    // fixme jiyeon
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

//    private Bitmap resizeBitmap(Bitmap bitmap) {
//        int resizeWidth = size.x/2;
//
//        double aspectRatio = (double) bitmap.getHeight() / (double) bitmap.getWidth();
//        int resizeHeight = (int) (resizeWidth * aspectRatio);
//        Bitmap resizeBitmap = Bitmap.createScaledBitmap(bitmap, resizeWidth, resizeHeight, true);
//        if (resizeBitmap != bitmap)
//            bitmap.recycle();
//
//        return resizeBitmap;
//    }

    // fixme hyeyeon[2]
    class ExitOnClickListener implements DialogInterface.OnClickListener {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (which == 0) {
                ExitMessage exitMessage = new ExitMessage(client.getMyName());
                MqttMessageFormat messageFormat = new MqttMessageFormat(exitMessage);
                client.publish(client.getTopic() + "_exit", JSONParser.getInstance().jsonWrite(messageFormat));
                client.setExitPublish(true);
                Log.e("kkankkan", "exit publish");

                if (client.getUserList().size() == 1 && client.isMaster()) {
                    databaseReference.child(client.getTopic()).runTransaction(new Transaction.Handler() {
                        @NonNull
                        @Override
                        public Transaction.Result doTransaction(@NonNull MutableData mutableData) {
                            if (mutableData.getValue() == null) {
                                Log.e("kkankkan", "mutabledata null");
                            } else {
                                mutableData.child("master").setValue(false);
                            }
                            return Transaction.success(mutableData);
                        }

                        @Override
                        public void onComplete(@Nullable DatabaseError databaseError, boolean b, @Nullable DataSnapshot dataSnapshot) {
                            Log.e("kkankkan", "DB master value change success");
                            Log.e("kkankkan", "transaction complete");

                            client.exitTask();
                            drawingViewModel.back();
                        }
                    });
                }
                else {
                    client.exitTask();
                    drawingViewModel.back();
                }
            }
            else if (which == 1){
                if (client.isMaster()) {
                    DeleteMessage deleteMessage = new DeleteMessage(client.getMyName());
                    MqttMessageFormat messageFormat = new MqttMessageFormat(deleteMessage);
                    client.publish(client.getTopic() + "_delete", JSONParser.getInstance().jsonWrite(messageFormat)); // fixme hyeyeon
                    client.setExitPublish(true);
                    databaseReference.child(client.getTopic()).runTransaction(new Transaction.Handler() {  // fixme hyeyeon
                        @NonNull
                        @Override
                        public Transaction.Result doTransaction(@NonNull MutableData mutableData) {
                            if (mutableData.getValue() == null) {
                                Log.e("kkankkan", "mutabledata null");
                            } else {
                                databaseReference.child(client.getTopic()).removeValue();
                            }
                            return Transaction.success(mutableData);
                        }

                        @Override
                        public void onComplete(@Nullable DatabaseError databaseError, boolean b, @Nullable DataSnapshot dataSnapshot) {
                            Log.e("kkankkan", "topic delete success");
                            Log.e("kkankkan", "transaction complete");

                            client.exitTask();
                            drawingViewModel.back();
                        }
                    });
                }
                else {
                    client.setToastMsg("master만 topic을 삭제할 수 있습니다");
                }
            }
        }
    }

    // fixme hyeyeon[1]
    @Override
    public void onPause() {  // todo
        super.onPause();
        Log.i("lifeCycle", "DrawingFragment onPause()");
    }

    @Override
    public void onDestroyView() {  // todo
        super.onDestroyView();
        Log.i("lifeCycle", "DrawingFragment onDestroyView()");
        if (exitOnClickListener != null) {
            exitOnClickListener = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i("lifeCycle", "DrawingFragment onDestroy()");
    }

    @Override
    public void onDetach() {  // todo
        super.onDetach();
        Log.i("lifeCycle", "DrawingFragment onDetach()");
        ((MainActivity)getContext()).setOnKeyBackPressedListener(null);
    }
}

package com.hansung.drawingtogether.view.drawing;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Point;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.navigation.fragment.NavHostFragment;
import lombok.Getter;
import lombok.Setter;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.hansung.drawingtogether.R;
import com.hansung.drawingtogether.data.remote.model.AliveThread;
import com.hansung.drawingtogether.data.remote.model.ExitType;
import com.hansung.drawingtogether.data.remote.model.Logger;
import com.hansung.drawingtogether.data.remote.model.MQTTClient;
import com.hansung.drawingtogether.data.remote.model.MyLog;
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
    private AttributeManager am = AttributeManager.getInstance();
    private Logger logger = Logger.getInstance(); // fixme nayeon

    private FragmentDrawingBinding binding;
    private DrawingViewModel drawingViewModel;
    private InputMethodManager inputMethodManager;

    private Menu menu; // fixme jiyeon

    // fixme hyeyeon[2]
    private DatabaseReference databaseReference;
    private ExitOnClickListener exitOnClickListener;
    //

    private ProgressDialog progressDialog;

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
        MyLog.e("DrawingFragment", "onCreateView");

        databaseReference = FirebaseDatabase.getInstance().getReference();
        exitOnClickListener = new ExitOnClickListener();
        exitOnClickListener.setBackKeyPressed(false);

        binding = FragmentDrawingBinding.inflate(inflater, container, false);

        JSONParser.getInstance().initJsonParser(this); // fixme nayeon ☆☆☆ JSON Parser 초기화 (toss DrawingFragmenet)

        drawingViewModel = ViewModelProviders.of(this).get(DrawingViewModel.class);

        client.setDrawingFragment(this);
        de.setDrawingFragment(this);

        de.setTextMoveBorderDrawable(getResources().getDrawable(R.drawable.text_move_border)); // fixme nayeon 텍스트 테두리 설정
        de.setTextFocusBorderDrawable(getResources().getDrawable(R.drawable.text_focus_border));
        de.setTextHighlightBorderDrawable(getResources().getDrawable(R.drawable.text_highlight_border)); // fixme nayeon

        am.setBinding(binding); // Palette Manager 의 FragmentDrawingBinding 변수 초기화
        am.setListener(); // 리스너 초기화
        am.showCurrentColor(de.getStrokeColor()); // 현재 색상 보여주기

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
            aliveTh.setSecond(2000);
            Thread th = new Thread(aliveTh);
            th.start();
            client.setThread(th);
        }

        SendMqttMessage sendMqttMessage = SendMqttMessage.getInstance();
        sendMqttMessage.startThread();

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
        drawingViewModel.checkPermission(getContext());

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
                    MyLog.d("button", "background eraser button click"); // fixme nayeon

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
                    MyLog.d("button", "drawing clear button click"); // fixme nayeon

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

    // fixme hyeyeon[2]-messageArrived 콜백에서 처리 -> 나가기 버튼 누른 후 바로 처리하도록 변경
    public void exit() { // 우측 상단 뒤로가기 버튼
        MyLog.e("why", "exit");
        exitOnClickListener.setBackKeyPressed(false);
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        if (client.isMaster()) {
            builder.setMessage(R.string.master_exit);
        } else {
            builder.setMessage(R.string.joiner_exit);
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

    private void setProgressDialog() {
        progressDialog = new ProgressDialog(MainActivity.context);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setTitle("오류 발생");
        progressDialog.setMessage("로그 파일 업로드 중");
        progressDialog.setCancelable(false);
    }

    @Override
    public void onBackKey() { // 디바이스 자체 뒤로가기 버튼
        MyLog.e("why", "onBackKey");
        exitOnClickListener.setBackKeyPressed(true);
        MyLog.e("kkankkan", "드로잉프레그먼트 onbackpressed");
        AlertDialog dialog = new AlertDialog.Builder(getContext())
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
        private boolean backKeyPressed;

        @Override
        public void onClick(DialogInterface dialog, int which) {
            logger.uploadLogFile(ExitType.NORMAL); // fixme nayeon

            MyLog.e("why", "exitOnClickListener : " + backKeyPressed);

            databaseReference.child(client.getTopic()).runTransaction(new Transaction.Handler() {
                @NonNull
                @Override
                public Transaction.Result doTransaction(@NonNull MutableData mutableData) {
                    if (mutableData.getValue() != null && client.isMaster()) {
                        mutableData.setValue(null);
                    }
                    if (mutableData.getValue() != null && !client.isMaster()) {
                        mutableData.child("username").child(client.getMyName()).setValue(null);
                    }
                    MyLog.e("transaction", "transaction success");
                    return Transaction.success(mutableData);
                }

                @Override
                public void onComplete(@Nullable DatabaseError databaseError, boolean b, @Nullable DataSnapshot dataSnapshot) {
                    MyLog.e("transaction", "transaction complete");


                    if (databaseError != null) {
                        MyLog.e("transaction", databaseError.getDetails());
                        return;
                    }
                    if (client.isMaster()) {
                        DeleteMessage deleteMessage = new DeleteMessage(client.getMyName());
                        MqttMessageFormat messageFormat = new MqttMessageFormat(deleteMessage);
                        client.publish(client.getTopic() + "_delete", JSONParser.getInstance().jsonWrite(messageFormat)); // fixme hyeyeon
                        client.setExitPublish(true);
                        client.exitTask();
                        if (backKeyPressed) {
                            getActivity().finish();
                            return;
                        }
                        else {
                            drawingViewModel.back();
                            return;
                        }
                    } else {
                        ExitMessage exitMessage = new ExitMessage(client.getMyName());
                        MqttMessageFormat messageFormat = new MqttMessageFormat(exitMessage);
                        client.publish(client.getTopic() + "_exit", JSONParser.getInstance().jsonWrite(messageFormat));
                        client.setExitPublish(true);
                        client.exitTask();
                        if (backKeyPressed) {
                            getActivity().finish();
                            return;
                        }
                        else {
                            drawingViewModel.back();
                            return;
                        }
                    }
                }
            });
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        MqttMessageFormat messageFormat;
        /*if(de.getBackgroundImage() != null) { //fixme minj - 우선 배경 이미지는 하나만
            binding.backgroundView.removeAllViews();
        }*/ // fixme nayeon MQTT CALLBACK

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

                    galleryBitmap = decodeSampledBitmapFromBitmap(de.bitmapToByteArray(galleryBitmap));

                    galleryBitmap = rotateBitmap(galleryBitmap, filePath);


                    // todo nayeon : check image file size
                    MyLog.e("gallery", "Gallery Image File Size = " + new File(getRealPathFromURI(uri)).length() + " Bytes");
                    MyLog.e("gallery", "Gallery Bitmap Byte Count = " + galleryBitmap.getRowBytes() * galleryBitmap.getHeight());

                    messageFormat = new MqttMessageFormat(de.getMyUsername(), Mode.BACKGROUND_IMAGE, de.bitmapToByteArray(galleryBitmap));
                    client.publish(client.getTopic_data(), JSONParser.getInstance().jsonWrite(messageFormat));
                } catch(IOException e) {
                    e.printStackTrace();
                }
                break;
            case PICK_FROM_CAMERA:
                try {
                    // fixme jiyeon
                    File file = new File(drawingViewModel.getPhotoPath());

                    // todo nayeon : check image file size
                    if(file.exists()) { MyLog.e("camera", "Camera File Size = " + file.length() + " Bytes"); }

                    Bitmap cameraBitmap = MediaStore.Images.Media.getBitmap(getContext().getContentResolver(), Uri.fromFile(file));

                    cameraBitmap = decodeSampledBitmapFromBitmap(de.bitmapToByteArray(cameraBitmap));
                    cameraBitmap = rotateBitmap(cameraBitmap, drawingViewModel.getPhotoPath());

                    messageFormat = new MqttMessageFormat(de.getMyUsername(), Mode.BACKGROUND_IMAGE, de.bitmapToByteArray(cameraBitmap));
                    client.publish(client.getTopic_data(), JSONParser.getInstance().jsonWrite(messageFormat));
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

        this.menu = menu;
    }

    // fixme jiyeon
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            // fixme jiyeon
            case R.id.drawing_voice:
                boolean click = drawingViewModel.clickVoice();
                if (click) {
                    item.setIcon(R.drawable.voice);
                    menu.findItem(R.id.drawing_speaker).setVisible(true);
                } else {
                    item.setIcon(R.drawable.voiceno);
                    menu.findItem(R.id.drawing_speaker).setVisible(false);
                    menu.findItem(R.id.drawing_speaker).setIcon(R.drawable.speaker);
                }
                break;
            case R.id.drawing_speaker:
                boolean mode = drawingViewModel.changeSpeakerMode();
                if (mode) {
                    item.setIcon(R.drawable.speaker_loud);
                } else {
                    item.setIcon(R.drawable.speaker);
                }
                break;
            //
            case R.id.gallery:
                drawingViewModel.getImageFromGallery(DrawingFragment.this);
                break;
            case R.id.camera:
                drawingViewModel.getImageFromCamera(DrawingFragment.this);
                break;
            case R.id.drawing_search:
                drawingViewModel.clickSearch(getView());
                break;
            case R.id.drawing_plus:
                drawingViewModel.plusUser(DrawingFragment.this, data.getTopic(), data.getPassword());  // fixme hyeyeon
                break;
            case R.id.drawing_save:
                drawingViewModel.clickSave();
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

    // fixme nayeon
    public static int calculateInSampleSize(BitmapFactory.Options options) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;

        final int reqWidth = 1000;
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

        return inSampleSize;
    }

    public Bitmap decodeSampledBitmapFromBitmap(byte[] bitmapArray/*, int reqWidth, int reqHeight*/) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        // options.inJustDecodeBounds = true;
        // BitmapFactory.decodeByteArray(bitmapArray, 0, bitmapArray.length, options);

        // options.inSampleSize = calculateInSampleSize(options);
        options.inSampleSize = 2;

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;

        return BitmapFactory.decodeByteArray(bitmapArray, 0, bitmapArray.length, options);
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

    // fixme hyeyeon[1]
    @Override
    public void onPause() {  // todo
        super.onPause();
        MyLog.i("lifeCycle", "DrawingFragment onPause()");
    }

    @Override
    public void onDestroyView() {  // todo
        super.onDestroyView();
        MyLog.i("lifeCycle", "DrawingFragment onDestroyView()");
        if (exitOnClickListener != null) {
            exitOnClickListener = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        MyLog.i("lifeCycle", "DrawingFragment onDestroy()");
    }

    @Override
    public void onDetach() {  // todo
        super.onDetach();
        MyLog.i("lifeCycle", "DrawingFragment onDetach()");
        ((MainActivity)getContext()).setOnKeyBackPressedListener(null);
    }
}
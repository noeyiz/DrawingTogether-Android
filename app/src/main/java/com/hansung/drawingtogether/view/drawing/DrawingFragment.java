package com.hansung.drawingtogether.view.drawing;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
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

import com.hansung.drawingtogether.R;
import com.hansung.drawingtogether.data.remote.model.AliveThread;
import com.hansung.drawingtogether.data.remote.model.MQTTClient;
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

    // fixme hyeyeon
/*  private String ip;
    private String port;
    private String topic;
    private String name;
    private String password;
    private boolean master;*/

    private MQTTClient client = MQTTClient.getInstance();
    private MQTTSettingData data = MQTTSettingData.getInstance();  // fixme hyeyeon

    private DrawingEditor de = DrawingEditor.getInstance();
    private AttributeManager am = AttributeManager.getInstance(); // fixme nayeon
    private FragmentDrawingBinding binding;
    private DrawingViewModel drawingViewModel;
    private InputMethodManager inputMethodManager;

    //private LinearLayout topToolLayout;
    //private Button doneBtn;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.e("DrawingFragment", "onCreateView");

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


        /*ip = getArguments().getString("ip");
        port = getArguments().getString("port");
        topic = getArguments().getString("topic");
        name = getArguments().getString("name");
        password = getArguments().getString("password");
        master = Boolean.parseBoolean(getArguments().getString("master"));

        Log.e("kkankkan", "MainViewModel로부터 전달 받은 Data : "  + topic + " / " + password + " / " + name + " / " + master);
*/

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

    public void exit() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setItems(R.array.exit, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) {
                    ExitMessage exitMessage = new ExitMessage(client.getMyName());
                    MqttMessageFormat messageFormat = new MqttMessageFormat(exitMessage);
                    client.publish(data.getTopic() + "_exit", JSONParser.getInstance().jsonWrite(messageFormat)); // fixme hyeyeon
                }
                else if (which == 1){
                    DeleteMessage deleteMessage = new DeleteMessage(client.getMyName());
                    MqttMessageFormat messageFormat = new MqttMessageFormat(deleteMessage);
                    client.publish(data.getTopic() + "_delete", JSONParser.getInstance().jsonWrite(messageFormat)); // fixme hyeyeon
                }
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    // fixme hyeyeon
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        ((MainActivity)context).setOnKeyBackPressedListener(this);
    }

    @Override
    public void onBackKey() {
        Log.e("kkankkan", "드로잉프레그먼트 onbackpressed");

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setMessage("앱을 종료하시겠습니까?");
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                MQTTClient client = MQTTClient.getInstance();
                client.setBackPressed(true);
                ExitMessage exitMessage = new ExitMessage(client.getMyName());
                MqttMessageFormat messageFormat = new MqttMessageFormat(exitMessage);
                client.publish(data.getTopic() + "_exit", JSONParser.getInstance().jsonWrite(messageFormat)); // fixme hyeyeon
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
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
                Uri uri = data.getData();
                Bitmap galleryBitmap = null;
                try {
                    galleryBitmap = rotateBitmap(MediaStore.Images.Media.getBitmap(getContext().getContentResolver(), uri), PICK_FROM_GALLERY);
                    galleryBitmap = resizeBitmap(galleryBitmap);
                    ImageView imageView = new ImageView(getContext());
                    imageView.setLayoutParams(new LinearLayout.LayoutParams(size.x, ViewGroup.LayoutParams.MATCH_PARENT));
                    imageView.setImageBitmap(galleryBitmap);
                    de.setBackgroundImage(galleryBitmap);
                    binding.backgroundView.addView(imageView);

                    messageFormat = new MqttMessageFormat(de.getUsername(), Mode.BACKGROUND_IMAGE, de.bitmapToByteArray(galleryBitmap));
                    client.publish(client.getTopic_data(), JSONParser.getInstance().jsonWrite(messageFormat));

                } catch(IOException e) { e.printStackTrace(); }

                break;
            case PICK_FROM_CAMERA:
                try {
                    File file = new File(drawingViewModel.getPhotoPath());
                    Bitmap cameraBitmap = rotateBitmap(MediaStore.Images.Media.getBitmap(getContext().getContentResolver(), Uri.fromFile(file)), PICK_FROM_CAMERA);
                    cameraBitmap = resizeBitmap(cameraBitmap);
                    if (cameraBitmap != null) {
                        ImageView imageView = new ImageView(getContext());
                        imageView.setLayoutParams(new LinearLayout.LayoutParams(size.x, ViewGroup.LayoutParams.MATCH_PARENT));
                        imageView.setImageBitmap(cameraBitmap);
                        de.setBackgroundImage(cameraBitmap);
                        binding.backgroundView.addView(imageView);

                        messageFormat = new MqttMessageFormat(de.getMyUsername(), Mode.BACKGROUND_IMAGE, de.bitmapToByteArray(cameraBitmap));
                        client.publish(client.getTopic_data(), JSONParser.getInstance().jsonWrite(messageFormat));
                    }
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

    private Bitmap rotateBitmap(Bitmap bitmap, int mode) {
        ExifInterface exif = null;
        int orientation = 0;

        if (mode == PICK_FROM_GALLERY) {
            //orientation = ExifInterface.ORIENTATION_ROTATE_90;
            orientation = ExifInterface.ORIENTATION_NORMAL; //fixme minj
        } else if (mode == PICK_FROM_CAMERA) {
            try {
                exif = new ExifInterface(drawingViewModel.getPhotoPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
            orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_UNDEFINED);
        }

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

    private Bitmap resizeBitmap(Bitmap bitmap) {
        int resizeWidth = size.x/2;

        double aspectRatio = (double) bitmap.getHeight() / (double) bitmap.getWidth();
        int resizeHeight = (int) (resizeWidth * aspectRatio);
        Bitmap resizeBitmap = Bitmap.createScaledBitmap(bitmap, resizeWidth, resizeHeight, true);
        if (resizeBitmap != bitmap)
            bitmap.recycle();

        return resizeBitmap;
    }
}

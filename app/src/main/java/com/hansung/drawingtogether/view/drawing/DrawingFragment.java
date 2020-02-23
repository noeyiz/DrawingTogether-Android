package com.hansung.drawingtogether.view.drawing;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
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

import com.google.gson.JsonSyntaxException;
import com.hansung.drawingtogether.R;
import com.hansung.drawingtogether.data.remote.model.MQTTClient;
import com.hansung.drawingtogether.databinding.FragmentDrawingBinding;
import com.hansung.drawingtogether.view.NavigationCommand;
import com.hansung.drawingtogether.view.main.MainActivity;
import com.kakao.kakaolink.v2.KakaoLinkResponse;
import com.kakao.kakaolink.v2.KakaoLinkService;
import com.kakao.message.template.LinkObject;
import com.kakao.message.template.TextTemplate;
import com.kakao.network.ErrorResult;
import com.kakao.network.callback.ResponseCallback;

import java.io.File;
import java.io.IOException;

@Getter
public class DrawingFragment extends Fragment {

    private final int PICK_FROM_GALLERY = 0;
    private final int PICK_FROM_CAMERA = 1;

    Point size;

    private String topic;
    private String name;
    private String password;
    private boolean master;

    private MQTTClient client = MQTTClient.getInstance();

    private DrawingEditor de = DrawingEditor.getInstance();
    private FragmentDrawingBinding binding;
    private DrawingViewModel drawingViewModel;
    private InputMethodManager inputMethodManager;
    private LinearLayout drawingMenuLayout;
    private Button doneBtn;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentDrawingBinding.inflate(inflater, container, false);
        drawingViewModel = ViewModelProviders.of(this).get(DrawingViewModel.class);

        inputMethodManager = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        binding.drawingViewContainer.setOnDragListener(new FrameLayoutDragListener());

        //setting done Btn
        doneBtn = new Button(this.getActivity()); //버튼 동적 생성
        initDoneButton();

        topic = getArguments().getString("topic");
        name = getArguments().getString("name");
        password = getArguments().getString("password");
        master = Boolean.parseBoolean(getArguments().getString("master"));

        // 디바이스 화면 size 구하기
        Display display = getActivity().getWindowManager().getDefaultDisplay();
        size = new Point();
        display.getSize(size);

        // 디바이스 화면 넓이의 3배 = 드로잉뷰 넓이
        ViewGroup.LayoutParams layoutParams = binding.drawingView.getLayoutParams();
        layoutParams.width = size.x*3;
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

        client.setDrawingFragment(this);
        client.setBinding(binding);
        client.init(topic, name, master, drawingViewModel);
        client.setCallback();
        client.subscribe(topic + "_join");
        client.subscribe(topic + "_exit");
        client.subscribe(topic + "_delete");
        client.subscribe(topic + "_data");
        client.publish(topic + "_join", ("name:" + name).getBytes());

        binding.setVm(drawingViewModel);
        binding.setLifecycleOwner(this);

        setHasOptionsMenu(true);

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
    }

    public void exit() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setItems(R.array.exit, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) {
                    client.publish(topic + "_exit", name.getBytes());
                }
                else if (which == 1){
                    client.publish(topic + "_delete", name.getBytes());
                }
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (requestCode) {
            case PICK_FROM_GALLERY:
                if (data == null) {
                    return;
                }
                Uri uri = data.getData();
                ImageView imageView = new ImageView(getContext());
                imageView.setLayoutParams(new LinearLayout.LayoutParams(size.x, ViewGroup.LayoutParams.MATCH_PARENT));
                imageView.setImageURI(uri);
                binding.backgroundView.addView(imageView);
                break;
            case PICK_FROM_CAMERA:
                try {
                    File file = new File(drawingViewModel.getPhotoPath());
                    Bitmap bitmap = rotateBitmap(MediaStore.Images.Media.getBitmap(getContext().getContentResolver(), Uri.fromFile(file)));

                    if (bitmap != null) {
                        imageView = new ImageView(getContext());
                        imageView.setLayoutParams(new LinearLayout.LayoutParams(size.x, ViewGroup.LayoutParams.MATCH_PARENT));
                        imageView.setImageBitmap(bitmap);
                        binding.backgroundView.addView(imageView);
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
                drawingViewModel.plusUser(DrawingFragment.this, topic, password);
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    public Bitmap rotateBitmap(Bitmap bitmap) {
        ExifInterface exif = null;
        try {
            exif = new ExifInterface(drawingViewModel.getPhotoPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
        int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED);

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

    private void setBasicButtons() {
        //textLayout.removeAllViews();
        //textLayout.addView(textBtn);
    }

    public void setDoneButton() {
        //textLayout.removeAllViews();
        //textLayout.addView(doneBtn);
    }

    private void initDoneButton() {
        doneBtn.setText("done");

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.gravity = Gravity.CENTER;
        layoutParams.weight = 1;
        doneBtn.setLayoutParams(layoutParams);

        doneBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Text text = de.getCurrentText();
                text.changeEditTextToTextView();

                // todo nayeon currentText 의 필요성 생각해보기
                de.setCurrentText(null);
                text.processFocusOut(); // 키보드 내리기
                setBasicButtons(); // 텍스트 조작이 끝나면 기본 버튼들 세팅

                de.setCurrentMode(Mode.DRAW);
            }
        });
    }

}

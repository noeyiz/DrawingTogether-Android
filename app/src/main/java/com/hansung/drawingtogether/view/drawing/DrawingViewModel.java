package com.hansung.drawingtogether.view.drawing;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;

import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.TedPermission;
import com.hansung.drawingtogether.R;
import com.hansung.drawingtogether.view.BaseViewModel;
import com.hansung.drawingtogether.view.SingleLiveEvent;
import com.kakao.kakaolink.v2.KakaoLinkResponse;
import com.kakao.kakaolink.v2.KakaoLinkService;
import com.kakao.message.template.LinkObject;
import com.kakao.message.template.TextTemplate;
import com.kakao.network.ErrorResult;
import com.kakao.network.callback.ResponseCallback;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static java.security.AccessController.getContext;

public class DrawingViewModel extends BaseViewModel {
    public final SingleLiveEvent<DrawingCommand> drawingCommands = new SingleLiveEvent<>();
    private MutableLiveData<String> userNum = new MutableLiveData<>();

    private final int PICK_FROM_GALLERY = 0;
    private final int PICK_FROM_CAMERA = 1;
    private String photoPath;

    private DrawingEditor de = DrawingEditor.getInstance();

    public DrawingViewModel() {
        setUserNum(0);
    }

    public void clickPen(View view) {
        de.setCurrentMode(Mode.DRAW);
        de.setCurrentType(ComponentType.STROKE);
        Log.i("drawing", "mode = " + de.getCurrentMode().toString() + ", type = " + de.getCurrentType().toString());
        //drawingCommands.postValue(new DrawingCommand.PenMode(view));      //fixme minj color picker
    }

    public void clickEraser(View view) {
        if(de.getCurrentMode() == Mode.ERASE)
            drawingCommands.postValue(new DrawingCommand.EraserMode(view));     //fixme minj add pixel eraser
        de.setCurrentMode(Mode.ERASE);
        Log.i("drawing", "mode = " + de.getCurrentMode().toString());
    }

    public void clickText(View view) {
        de.setCurrentMode(Mode.TEXT);
        FrameLayout frameLayout = de.getDrawingFragment().getBinding().drawingViewContainer;

        // 텍스트 속성 설정 ( 기본 도구에서 설정할 것인지 텍스트 도구에서 설정할 것인지? )
        TextAttribute textAttribute = new TextAttribute(de.setTextStringId(), de.getMyUsername(), //fixme nayeon - Text ID 초기값 NULL
                "Input Text", 20, Color.BLACK, Color.TRANSPARENT,
                View.TEXT_ALIGNMENT_CENTER, Typeface.BOLD,
                frameLayout.getWidth(), frameLayout.getHeight());


        Text text = new Text(de.getDrawingFragment(), textAttribute); // Context 넘겨주기
        text.createGestureDetecter(); // Set Gesture ( Single Tap Up )
        text.addEditTextToFrameLayout(); // 처음 텍스트 생성 시 EditText 부착
        text.activeEditText(); // EditText 커서와 키보드 활성화
        //drawingCommands.postValue(new DrawingCommand.TextMode(view));
    }

    public void clickShape(View view) {
        drawingCommands.postValue(new DrawingCommand.ShapeMode(view));
    }

    public void clickSearch(View view) {
        navigate(R.id.action_drawingFragment_to_searchFragment);
    }

    public void getImageFromGallery(Fragment fragment) {
        checkPermission(fragment.getContext());

        Intent galleryIntent = new Intent(Intent.ACTION_PICK);
        galleryIntent.setType(MediaStore.Images.Media.CONTENT_TYPE);
        fragment.startActivityForResult(galleryIntent, PICK_FROM_GALLERY);
    }

    public void getImageFromCamera(Fragment fragment) {
        checkPermission(fragment.getContext());

        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(fragment.getContext().getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile(fragment);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (photoFile != null) {
                Uri uri = FileProvider.getUriForFile(fragment.getContext(), "com.hansung.drawingtogether.fileprovider", photoFile);
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
                fragment.startActivityForResult(cameraIntent, PICK_FROM_CAMERA);
            }
        }
    }

    public void plusUser(Fragment fragment, String topic, String password) {
        TextTemplate params = TextTemplate.newBuilder("DrawingTogether!",
                LinkObject.newBuilder()
                        .setAndroidExecutionParams("topic=" + topic + "&password=" + password)
                        .build())
                .setButtonTitle("앱으로 이동").build();

        KakaoLinkService.getInstance().sendDefault(fragment.getContext(), params, new ResponseCallback<KakaoLinkResponse>() {
            @Override
            public void onFailure(ErrorResult errorResult) {
            }

            @Override
            public void onSuccess(KakaoLinkResponse result) {
            }
        });
    }

    public File createImageFile(Fragment fragment) throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = new File(Environment.getExternalStorageDirectory() + "/Pictures", "drawingtogether");
        if (!storageDir.exists()) storageDir.mkdirs();
        File  image = File.createTempFile(imageFileName, ".jpg", storageDir);
        photoPath = image.getAbsolutePath();
        Log.e("kkankkan", photoPath);
        return image;
    }

    private void checkPermission(Context context) {
        PermissionListener permissionListener = new PermissionListener() {
            @Override
            public void onPermissionGranted() {
                //
            }
            @Override
            public void onPermissionDenied(List<String> deniedPermissions) {
                //
            }
        };

        TedPermission.with(context)
                .setPermissionListener(permissionListener)
                .setDeniedMessage(context.getResources().getString(R.string.permission_camera))
                .setPermissions(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)
                .check();
    }

    public String getPhotoPath() {
        return photoPath;
    }

    public MutableLiveData<String> getUserNum() {
        return userNum;
    }

    public void setUserNum(int num) {
        userNum.postValue(num + "명");
    }

}

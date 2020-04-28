package com.hansung.drawingtogether.view.drawing;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;

import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.TedPermission;
import com.hansung.drawingtogether.R;
import com.hansung.drawingtogether.data.remote.model.Logger;
import com.hansung.drawingtogether.data.remote.model.MQTTClient;
import com.hansung.drawingtogether.data.remote.model.MyLog;
import com.hansung.drawingtogether.view.BaseViewModel;
import com.hansung.drawingtogether.view.SingleLiveEvent;
import com.hansung.drawingtogether.view.main.ExitMessage;
import com.hansung.drawingtogether.view.main.MQTTSettingData;
import com.hansung.drawingtogether.view.main.MainActivity;
import com.kakao.kakaolink.v2.KakaoLinkResponse;
import com.kakao.kakaolink.v2.KakaoLinkService;
import com.kakao.message.template.LinkObject;
import com.kakao.message.template.TextTemplate;
import com.kakao.network.ErrorResult;
import com.kakao.network.callback.ResponseCallback;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class DrawingViewModel extends BaseViewModel {
    public final SingleLiveEvent<DrawingCommand> drawingCommands = new SingleLiveEvent<>();
    private MutableLiveData<String> userNum = new MutableLiveData<>();
    private MutableLiveData<String> userPrint = new MutableLiveData<>();  // fixme hyeyeon

    private Logger logger = Logger.getInstance();

    private final int PICK_FROM_GALLERY = 0;
    private final int PICK_FROM_CAMERA = 1;
    private String photoPath;

    private DrawingEditor de = DrawingEditor.getInstance();

    // fixme hyeyeon
    private String ip;
    private String port;
    private String topic;
    private String name;
    private String password;
    private boolean master;
    private String masterName;  // fixme hyeyeon

    private MQTTClient client = MQTTClient.getInstance();
    private MQTTSettingData data = MQTTSettingData.getInstance();

    // fixme jiyeon
    private boolean audioFlag = false;
    private RecordThread recThread;
    private AudioManager audioManager = (AudioManager) ((MainActivity)MainActivity.context).getSystemService(Service.AUDIO_SERVICE); // fixme jiyeon

    private Button preMenuButton;

    // fixme hyeyeon[1]
    @Override
    public void onCleared() {  // todo
        super.onCleared();
        MyLog.i("lifeCycle", "DrawingViewModel onCleared()");

        if (client != null && client.getClient().isConnected()) {
            // 꼭 여기서 처리 해줘야 하는 부분
            client.getDe().removeAllDrawingData();
            client.getUserList().clear();

            // fixme hyeyeon[4] 강제 종료 시 불릴 경우 검사 후 해제, exit publish
            if (!client.isExitPublish()) {
                // fixme jiyeon
                for (AudioPlayThread audioPlayThread : client.getAudioPlayThreadList()) {
                    audioPlayThread.getBuffer().clear();
                }
                // fixme jiyeon
                audioManager.setSpeakerphoneOn(false);

                ExitMessage exitMessage = new ExitMessage(client.getMyName());
                MqttMessageFormat messageFormat = new MqttMessageFormat(exitMessage);
                client.publish(client.getTopic() + "_exit", JSONParser.getInstance().jsonWrite(messageFormat));
                client.setExitPublish(true);
            }
            if (!(client.getTh().getState() == Thread.State.TERMINATED)) {  // todo isInterruped() false 문제 해결 -> Thead의 state 검사
                client.getTh().interrupt();
                client.unsubscribeAllTopics();
            }

            // todo isMid = true;
            //
        }

        /*th.interrupt();
        unsubscribeAllTopics();
        isMid = true;
        usersActionMap.clear();;*/

    }
    public DrawingViewModel() {
        setUserNum(0);
        setUserPrint("");  // fixme hyeyeon

        // fixme hyeyeon
        ip = data.getIp();
        port = data.getPort();
        topic = data.getTopic();
        name = data.getName();
        password = data.getPassword();
        master = data.isMaster();
        masterName = data.getMasterName();  // fixme hyeyeon

        MyLog.e("kkankkan", "MQTTSettingData : "  + topic + " / " + password + " / " + name + " / " + master + "/" + masterName);

        client.init(topic, name, master, this, ip, port, masterName);
        client.setAliveCount(3);
        client.setCallback();
        client.subscribe(topic + "_join");
        client.subscribe(topic + "_exit");
        client.subscribe(topic + "_delete");
        client.subscribe(topic + "_data");
        client.subscribe(topic + "_mid");
        client.subscribe(topic + "_alive"); // fixme hyeyeon

        de.setCurrentType(ComponentType.STROKE);    //fixme minj
        de.setCurrentMode(Mode.DRAW);

//        client.subscribe(topic + "_audio"); // fixme jiyeon

    }

    public void clickUndo(View view) {
        MyLog.d("button", "undo button click");

        de.getDrawingFragment().getBinding().drawingView.undo();
    }

    public void clickRedo(View view) {
        MyLog.d("button", "redo button click");

        de.getDrawingFragment().getBinding().drawingView.redo();
    }

    // fixme nayeon
    public void clickSave() {

        DrawingFragment fragment = de.getDrawingFragment();

        checkPermission(fragment.getContext()); // todo nayeon 권한 체크 앱 처음 실행 시 하도록 수정하기

        // todo nayeon
        DrawingViewController dvc = fragment.getBinding().drawingViewContainer;
        dvc.setDrawingCacheEnabled(true);
        dvc.buildDrawingCache();
        Bitmap captureContainer = dvc.getDrawingCache();


        FileOutputStream fos;
        String fileName = "image-" + client.getTopic() + client.getSavedFileCount() + ".png";
        String filePath = Environment.getExternalStorageDirectory() + File.separator  + "Pictures"
                + File.separator + fileName; // todo nayeon - change file name


        File fileCacheItem = new File(filePath);


        try {
            fos = new FileOutputStream(fileCacheItem);
            captureContainer.compress(Bitmap.CompressFormat.JPEG, 100, fos); // quality
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            fragment.getContext().sendBroadcast(new Intent( Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(fileCacheItem)));             // 갤러리 데이터 갱신
            MyLog.e("export", "capture path == " + filePath);
        }

        dvc.setDrawingCacheEnabled(false);

    }


    public void clickPen(View view) { // drawBtn1, drawBtn2, drawBtn3
        MyLog.d("button", "pen button click");

        changeClickedButtonBackground(view);
        de.setCurrentMode(Mode.DRAW);
        de.setCurrentType(ComponentType.STROKE);

        de.setStrokeWidth(Integer.parseInt(view.getContentDescription().toString())); // fixme nayeon

        MyLog.i("drawing", "mode = " + de.getCurrentMode().toString() + ", type = " + de.getCurrentType().toString());
        //drawingCommands.postValue(new DrawingCommand.PenMode(view));      //fixme nayeon color picker [ View Model 과 Navigator 관계, 이벤트 처리 방식 ]
        preMenuButton = (Button)view; // fixme nayeon 텍스트 편집 후 기본 모드인 드로잉으로 돌아가기 위해 (텍스트 편집 전에 선택했던 드로잉 모드로)
    }

    public void clickEraser(View view) {
        MyLog.d("button", "eraser button click");

        changeClickedButtonBackground(view);
        if(de.getCurrentMode() == Mode.ERASE)
            drawingCommands.postValue(new DrawingCommand.EraserMode(view));     //fixme minj add pixel eraser
        de.setCurrentMode(Mode.ERASE);
        MyLog.i("drawing", "mode = " + de.getCurrentMode().toString());
    }

    public void clickText(View view) {
        MyLog.d("button", "text button click");

        // 사용자가 처음 텍스트 편집창에서 텍스트 생성중인 경우
        // 텍스트 정보들을 모든 사용자가 갖고 있지 않음 ( 편집중인 사람만 갖고 있음 )
        // 따라서 중간자가 들어오고 난 후에 텍스트 생성을 할 수 있도록 막아두기
        if(de.isMidEntered() && !de.getCurrentText().getTextAttribute().isTextInited()) { // todo nayeon ☆☆☆ 텍스트 중간자 처리
           showToastMsg("다른 사용자가 접속 중 입니다 잠시만 기다려주세요");
           return;
        }
        //if(de.isTextBeingEdited()) return; // 다른 텍스트 편집 중일 때 텍스트 클릭 못하도록
        // 텍스트 모드가 끝날 때 까지 (Done Button) 누르기 전 까지, 다른 버튼들 비활성화
        enableDrawingMenuButton(false);
        changeClickedButtonBackground(view);


        de.setCurrentMode(Mode.TEXT);
        MyLog.i("drawing", "mode = " + de.getCurrentMode().toString());
        FrameLayout frameLayout = de.getDrawingFragment().getBinding().drawingViewContainer;


        ((MainActivity)de.getDrawingFragment().getActivity()).setVisibilityToolbarMenus(false); // fixme nayeon

        // 텍스트 속성 설정 ( 기본 도구에서 설정할 것인지 텍스트 도구에서 설정할 것인지? )
        TextAttribute textAttribute = new TextAttribute(de.setTextStringId(), de.getMyUsername(),
                de.getTextSize(), de.getTextColor(), de.getTextBackground(),
                Gravity.CENTER, de.getFontStyle(),

                frameLayout.getWidth(), frameLayout.getHeight());

        Text text = new Text(de.getDrawingFragment(), textAttribute);
        text.createGestureDetector(); // Set Gesture ( Single Tap Up )

        text.changeTextViewToEditText(); // EditText 커서와 키보드 활성화, 텍스트 편집 시작 처리

        //drawingCommands.postValue(new DrawingCommand.TextMode(view));
    }

    public void clickDone(View view) {
        MyLog.d("button", "done button click");

        // 텍스트 모드가 끝나면 다른 버튼들 활성화
        enableDrawingMenuButton(true);

        changeClickedButtonBackground(preMenuButton); //  fixme nayeon  텍스트 편집 후 기본 모드인 드로잉으로 돌아가기 위해

        Text text = de.getCurrentText();
        text.changeEditTextToTextView();

        ((MainActivity)de.getDrawingFragment().getActivity()).setVisibilityToolbarMenus(true); // fixme nayeon

    }

    public void clickShape(View view) {
        MyLog.d("button", "shape button click");

        changeClickedButtonBackground(view);
        de.setCurrentMode(Mode.DRAW);
        de.setCurrentType(ComponentType.RECT);

        preMenuButton = (Button)view; // fixme nayeon 텍스트 편집 후 기본 모드인 드로잉으로 돌아가기 위해 (텍스트 편집 전에 선택했던 드로잉 모드로)

        drawingCommands.postValue(new DrawingCommand.ShapeMode(view));
    }

    public void clickSelector(View view) {
        MyLog.d("button", "selector button click");

        changeClickedButtonBackground(view);
        de.setCurrentMode(Mode.SELECT);
        MyLog.i("drawing", "mode = " + de.getCurrentMode().toString());
    }

    public void clickGroup(View view) {
        changeClickedButtonBackground(view);
        de.setCurrentMode(Mode.GROUP);
        MyLog.i("drawing", "mode = " + de.getCurrentMode().toString());
    }

    // fixme nayeon
    public void clickTextColor(View view) {
        MyLog.d("button", "text color button click");

        de.getCurrentText().finishTextColorChange();
    }

    public void clickSearch(View view) {
        MyLog.d("button", "search button click");

        navigate(R.id.action_drawingFragment_to_searchFragment);
    }

    public void clickWarp(View view) {
        changeClickedButtonBackground(view);
        de.setCurrentMode(Mode.WARP);
    }

    public void changeClickedButtonBackground(View view) {
        LinearLayout drawingMenuLayout = de.getDrawingFragment().getBinding().drawingMenuLayout;

        // fixme nayeon
        // preMenuButton -> 아무것도 누르지 않은 상태에서 텍스트 버튼 클릭했을 때 NULL
        // 제일 첫 번째 버튼 (얇은 펜(그리기)) 로 지정
        if(view == null) { view = drawingMenuLayout.getChildAt(0); }

        for(int i=0; i<drawingMenuLayout.getChildCount(); i++) {
            drawingMenuLayout.getChildAt(i).setBackgroundColor(Color.TRANSPARENT);
        }
        view.setBackgroundColor(Color.rgb(233, 233, 233));
    }

    // fixme nayeon - 텍스트 편집 시 키보드가 내려가면 하단 메뉴 보임, 이들을 비활성화 : 추후에 키보드 내려가는 이벤트 처리로 바꿀 예정
    public void enableDrawingMenuButton(Boolean bool) {
        LinearLayout drawingMenuLayout = de.getDrawingFragment().getBinding().drawingMenuLayout;

        for(int i=0; i<drawingMenuLayout.getChildCount(); i++) {
            drawingMenuLayout.getChildAt(i).setEnabled(bool);
        }
    }

    //fixme jiyeon
    public boolean clickVoice() {
        if (!audioFlag) { // RECORD 시작
            audioFlag = true;
            client.subscribe(client.getTopic() + "_audio");
            recThread = new RecordThread();
            recThread.setFlag(audioFlag);
            recThread.setBufferUnitSize(2);
            new Thread(recThread).start();

            return true;
        } else {
            try {
                audioFlag = false;
                recThread.setFlag(audioFlag);
                audioManager.setSpeakerphoneOn(false);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        }
    }

    // fixme jiyeon
    public boolean changeSpeakerMode() {
        if (audioManager.isSpeakerphoneOn()) {
            audioManager.setSpeakerphoneOn(false);
            MyLog.e("audio", "SPEAKER : " + audioManager.isSpeakerphoneOn());

            return false;
        } else {
            audioManager.setSpeakerphoneOn(true);
            MyLog.e("audio", "SPEAKER : " + audioManager.isSpeakerphoneOn());

            return true;
        }
    }

    public void getImageFromGallery(Fragment fragment) {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK);
        galleryIntent.setType(MediaStore.Images.Media.CONTENT_TYPE);
        fragment.startActivityForResult(galleryIntent, PICK_FROM_GALLERY);
    }

    public void getImageFromCamera(Fragment fragment) {
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
        MyLog.e("kkankkan", photoPath);

        return image;
    }

    public void checkPermission(Context context) {
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
                .setPermissions(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO)
                .check();
    }

    private void showToastMsg(final String message) { Toast.makeText(de.getDrawingFragment().getActivity(), message, Toast.LENGTH_SHORT).show(); }

    public String getPhotoPath() {
        return photoPath;
    }

    public MutableLiveData<String> getUserNum() {
        return userNum;
    }

    public MutableLiveData<String> getUserPrint() { return userPrint; }  // fixme hyeyeon

    public void setUserNum(int num) {
        userNum.postValue(num + "명");
    }

    public void setUserPrint(String user) { userPrint.postValue(user); }  // fixme hyeyoen

}

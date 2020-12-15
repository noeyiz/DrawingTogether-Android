package com.hansung.drawingtogether.view.drawing;

import android.app.AlertDialog;
import android.app.Service;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import android.text.InputType;
import android.util.Log;

import android.view.LayoutInflater;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;

import com.hansung.drawingtogether.AutoDrawInterface;
import com.hansung.drawingtogether.R;
import com.hansung.drawingtogether.data.remote.model.Logger;
import com.hansung.drawingtogether.data.remote.model.MQTTClient;
import com.hansung.drawingtogether.data.remote.model.MyLog;
import com.hansung.drawingtogether.databinding.DialogAutoDrawBinding;
import com.hansung.drawingtogether.view.BaseViewModel;
import com.hansung.drawingtogether.view.SingleLiveEvent;
import com.hansung.drawingtogether.view.main.MQTTSettingData;
import com.hansung.drawingtogether.view.main.MainActivity;
import com.kakao.kakaolink.v2.KakaoLinkResponse;
import com.kakao.kakaolink.v2.KakaoLinkService;
import com.kakao.message.template.ButtonObject;
import com.kakao.message.template.ContentObject;
import com.kakao.message.template.FeedTemplate;
import com.kakao.message.template.LinkObject;
import com.kakao.message.template.TextTemplate;
import com.kakao.network.ErrorResult;
import com.kakao.network.callback.ResponseCallback;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class DrawingViewModel extends BaseViewModel {

    public final SingleLiveEvent<DrawingCommand> drawingCommands = new SingleLiveEvent<>();

    private DrawingEditor de = DrawingEditor.getInstance();
    private Logger logger = Logger.getInstance();

    /* MQTT 관련 변수 */
    private MQTTClient client = MQTTClient.getInstance();
    private MQTTSettingData data = MQTTSettingData.getInstance();
    private String ip;
    private String port;
    private String topic;
    private String password;
    private boolean master;
    private String masterName;
    private String name;

    /* 멤버 리스트를 출력하기 위한 변수 */
    private MutableLiveData<String> userNum = new MutableLiveData<>();
    private MutableLiveData<String> userPrint = new MutableLiveData<>();

    /* 이미지 관련 변수 */
    private final int PICK_FROM_GALLERY = 0;
    private final int PICK_FROM_CAMERA = 1;
    private String photoPath;

    /* 오디오 관련 변수 */
    private boolean micFlag = false;
    private boolean speakerFlag = false;
    private int speakerMode = 0; // 0: mute, 1: speaker on, 2: speaker loud
    private RecordThread recThread;
    private AudioManager audioManager = (AudioManager) MainActivity.context.getSystemService(Service.AUDIO_SERVICE);

    private ImageButton preMenuButton;

    /* 오토드로우 관련 변수 */
    private MutableLiveData<String> autoDrawImage = new MutableLiveData<>();
    private String autoDrawImageUrl = "";

    public DrawingViewModel() {
        setUserNum(0);
        setUserPrint("");

        ip = data.getIp();
        port = data.getPort();
        topic = data.getTopic();
        name = data.getName();
        password = data.getPassword();
        master = data.isMaster();
        masterName = data.getMasterName();


        MyLog.i("MQTTSettingData", "ip : "
                + ip + ", port : " + port + ", topic : " + topic + ", password : " + password + ", name : " + name + ", isMaster : " + master + ", master : " + masterName);

        client.init(topic, name, master, this, ip, port, masterName);

        client.setAliveLimitCount(5);
        client.setCallback();
        client.subscribeAllTopics();


        // fixme nayeon for performance

//        for(int i=0; i<100; i++) {
//            try {
//                client.monitoringClientSetting(new MqttClient("tcp://" + ip + ":" + port, MqttClient.generateClientId(), new MemoryPersistence()), topic);
//            } catch (MqttException e) {
//                e.printStackTrace();
//            }
//        }


        de.setCurrentType(ComponentType.STROKE);    //fixme minj
        de.setCurrentMode(Mode.DRAW);

        /* Record Thread는 DrawingViewModel 생성 시 하나만 생성 */
//        recThread = new RecordThread();
//        recThread.setBufferUnitSize(4);
//        recThread.start();
    }

    public void clickUndo(View view) {
        MyLog.d("button", "undo button click");

        de.getDrawingFragment().getBinding().drawingView.undo();
    }

    public void clickRedo(View view) {
        MyLog.d("button", "redo button click");

        de.getDrawingFragment().getBinding().drawingView.redo();
    }

    public void clickSave() {

        DrawingFragment fragment = de.getDrawingFragment();

        DrawingViewController dvc = fragment.getBinding().drawingViewContainer;
        dvc.setDrawingCacheEnabled(true);
        dvc.buildDrawingCache();
        Bitmap captureContainer = dvc.getDrawingCache();

        FileOutputStream fos;
        String fileName = "image-" + client.getTopic() + client.getSavedFileCount() + ".png";
        String filePath = Environment.getExternalStorageDirectory() + File.separator  + "Pictures"
                + File.separator + fileName;

        File fileCacheItem = new File(filePath);

        try {
            fos = new FileOutputStream(fileCacheItem);
            captureContainer.compress(Bitmap.CompressFormat.JPEG, 100, fos); // quality
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            fragment.getContext().sendBroadcast(new Intent( Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(fileCacheItem))); // 갤러리 데이터 갱신
            MyLog.i("export", "capture path == " + filePath);
        }

        dvc.setDrawingCacheEnabled(false);

        Toast.makeText(fragment.getContext(), R.string.success_save, Toast.LENGTH_SHORT).show();
    }

    public void clickPen(View view) { // drawBtn1, drawBtn2, drawBtn3
        MyLog.d("button", "pen button click");
        de.getDrawingFragment().getBinding().penModeLayout.setVisibility(View.VISIBLE);
        changeClickedButtonBackground(view);

        if(de.getCurrentMode() == Mode.DRAW && de.getCurrentType() == ComponentType.STROKE) {
            drawingCommands.postValue(new DrawingCommand.PenMode(view));
        }

        de.setCurrentMode(Mode.DRAW);
        de.setCurrentType(ComponentType.STROKE);

        MyLog.i("drawing", "mode = " + de.getCurrentMode().toString() + ", type = " + de.getCurrentType().toString());
        //drawingCommands.postValue(new DrawingCommand.PenMode(view)); // color picker [ View Model 과 Navigator 관계, 이벤트 처리 방식 ]
        preMenuButton = (ImageButton)view; // 텍스트 편집 후 기본 모드인 드로잉으로 돌아가기 위해 (텍스트 편집 전에 선택했던 드로잉 모드로)
    }

    public void clickPencil(View view) {
        de.getDrawingFragment().getBinding().pencilBtn.setImageResource(R.drawable.pencil_1);
        de.getDrawingFragment().getBinding().highlightBtn.setImageResource(R.drawable.highlight_0);
        de.getDrawingFragment().getBinding().neonBtn.setImageResource(R.drawable.neon_0);

        de.setPenMode(PenMode.NORMAL);
    }

    public void clickHighlight(View view) {
        de.getDrawingFragment().getBinding().pencilBtn.setImageResource(R.drawable.pencil_0);
        de.getDrawingFragment().getBinding().highlightBtn.setImageResource(R.drawable.highlight_1);
        de.getDrawingFragment().getBinding().neonBtn.setImageResource(R.drawable.neon_0);

        de.setPenMode(PenMode.HIGHLIGHT);
    }

    public void clickNeon(View view) {
        de.getDrawingFragment().getBinding().pencilBtn.setImageResource(R.drawable.pencil_0);
        de.getDrawingFragment().getBinding().highlightBtn.setImageResource(R.drawable.highlight_0);
        de.getDrawingFragment().getBinding().neonBtn.setImageResource(R.drawable.neon_1);

        de.setPenMode(PenMode.NEON);
    }

    public void clickEraser(View view) {
        MyLog.d("button", "eraser button click");
        de.getDrawingFragment().getBinding().penModeLayout.setVisibility(View.INVISIBLE);
        changeClickedButtonBackground(view);

        if(de.getCurrentMode() == Mode.ERASE)
            drawingCommands.postValue(new DrawingCommand.EraserMode(view));
        de.setCurrentMode(Mode.ERASE);
        MyLog.i("drawing", "mode = " + de.getCurrentMode().toString());
    }

    public void clickText(View view) {
        MyLog.d("button", "text button click");
        de.getDrawingFragment().getBinding().penModeLayout.setVisibility(View.INVISIBLE);

        /* 사용자가 처음 텍스트 편집창에서 텍스트 생성중인 경우 */
        /* 텍스트 정보들을 모든 사용자가 갖고 있지 않음 ( 편집중인 사람만 갖고 있음 ) */
        /* 따라서 중간자가 들어오고 난 후에 텍스트 생성을 할 수 있도록 막아두기 */
        // de.setMidEntered(false);

        if(de.isMidEntered() /* && !de.getCurrentText().getTextAttribute().isTextInited() */) { // 텍스트 중간자 처리
            showToastMsg("다른 사용자가 접속 중 입니다 잠시만 기다려주세요");
            return;
        }
        //if(de.isTextBeingEdited()) return; // 다른 텍스트 편집 중일 때 텍스트 클릭 못하도록
        /* 텍스트 모드가 끝날 때 까지 (Done Button 누르기 전 까지) 다른 버튼들 비활성화 & 못 누르는 버튼 표시 (회색으로 표시) */
//        enableDrawingMenuButton(false);
//        changeClickedButtonBackground(view);

        de.setCurrentMode(Mode.TEXT);
        MyLog.i("drawing", "mode = " + de.getCurrentMode().toString());
        FrameLayout frameLayout = de.getDrawingFragment().getBinding().drawingViewContainer;


        ((MainActivity)de.getDrawingFragment().getActivity()).setVisibilityToolbarMenus(false);

        // 텍스트 속성 설정
        TextAttribute textAttribute = new TextAttribute(de.setTextStringId(), de.getMyUsername(),
                de.getTextSize(), de.getTextColor(), frameLayout.getWidth(), frameLayout.getHeight());

        Text text = new Text(de.getDrawingFragment(), textAttribute);
        text.createGestureDetector(); // Set Gesture ( Single Tap Up )

        text.changeTextViewToEditText(); // EditText 커서와 키보드 활성화, 텍스트 편집 시작 처리
    }

    public void clickDone(View view) {
        MyLog.d("button", "done button click");

        // 현재 편집중인 텍스트가 새로 생성하는 텍스트가 아니라, 생성된 후 편집하는 텍스트인 경우 done 버튼 클릭 가능 (username == null 로 세팅하기 위해)
        // 텍스트를 새로 생성하는 경우에 아직 다른 참가자들에게 텍스트 정보가 없기 때문에, 중간 참여자 접속을 기다린 후 생성 가능하도록 처리
        if(de.isMidEntered()
                && de.getCurrentText() != null && !de.getCurrentText().getTextAttribute().isTextInited()) { // 텍스트 중간자 처리
                showToastMsg("다른 참가자가 접속 중 입니다. 잠시만 기다려주세요.");
            return;
        }

        /* 텍스트 모드가 끝나면 다른 버튼들 활성화 */
//        enableDrawingMenuButton(true);
//        changeClickedButtonBackground(preMenuButton); // 텍스트 편집 후 기본 모드인 드로잉 - 펜 버튼 눌림 표시

        Text text = de.getCurrentText();
        text.changeEditTextToTextView();

        changeClickedButtonBackground(preMenuButton); // 텍스트 편집 후 기본 모드인 드로잉 - 펜 버튼 눌림 표시

//        if(preMenuButton.equals(de.getDrawingFragment().getBinding().drawBtn)) // Draw Btn 인 경우에만 펜 종류 표시
        if(preMenuButton == de.getDrawingFragment().getBinding().drawBtn) // Draw Btn 인 경우에만 펜 종류 표시

            de.getDrawingFragment().getBinding().penModeLayout.setVisibility(View.VISIBLE); // 펜 종류 보이도록



        ((MainActivity)de.getDrawingFragment().getActivity()).setVisibilityToolbarMenus(true);
    }

    public void clickShape(View view) {
        MyLog.d("button", "shape button click");
        de.getDrawingFragment().getBinding().penModeLayout.setVisibility(View.INVISIBLE);

        changeClickedButtonBackground(view);
        de.setCurrentMode(Mode.DRAW);
        de.setCurrentType(ComponentType.RECT);

        preMenuButton = (ImageButton)view; // 텍스트 편집 후 기본 모드인 드로잉으로 돌아가기 위해 (텍스트 편집 전에 선택했던 드로잉 모드로)

        drawingCommands.postValue(new DrawingCommand.ShapeMode(view));
    }

    public void clickSelector(View view) {
        MyLog.d("button", "selector button click");
        de.getDrawingFragment().getBinding().penModeLayout.setVisibility(View.INVISIBLE);

        changeClickedButtonBackground(view);
        de.setCurrentMode(Mode.SELECT);
        MyLog.i("drawing", "mode = " + de.getCurrentMode().toString());
    }

    public void clickTextColor(View view) {
        MyLog.d("button", "text color button click");

        de.getCurrentText().finishTextColorChange();
    }

    public void clickWarp(View view) {
        de.getDrawingFragment().getBinding().penModeLayout.setVisibility(View.INVISIBLE);
        changeClickedButtonBackground(view);
        de.setCurrentMode(Mode.WARP);
    }

    public void clickAutoDraw(View view) {
        de.getDrawingFragment().getBinding().penModeLayout.setVisibility(View.INVISIBLE);
        changeClickedButtonBackground(view);
        de.setCurrentMode(Mode.AUTODRAW);

        DialogAutoDrawBinding binding = DialogAutoDrawBinding.inflate(LayoutInflater.from(MainActivity.context));
        binding.webview.getSettings().setJavaScriptEnabled(true);
        binding.webview.setWebChromeClient(new WebChromeClient());
        binding.webview.loadUrl("file:///android_asset/canvas.html");
        binding.webview.addJavascriptInterface(new AutoDrawInterface() {
            @JavascriptInterface
            @Override
            public void setImage(String imageUrl) {
                MyLog.i("img", imageUrl);
                autoDrawImageUrl = imageUrl;
            }
        }, "AutoDrawInterface");
        AlertDialog dialog = new AlertDialog.Builder(MainActivity.context)
                .setTitle("AutoDraw")
                .setCancelable(false)
                .setView(binding.getRoot())
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (!autoDrawImageUrl.isEmpty()) {
                            autoDrawImage.postValue(autoDrawImageUrl);
                        }
                        autoDrawImageUrl = "";
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (autoDrawImageUrl == null)
                            return;
                    }
                })
                .create();

        dialog.show();
    }

    public void changeClickedButtonBackground(View view) {
        LinearLayout drawingMenuLayout = de.getDrawingFragment().getBinding().drawingMenuLayout;

        /* preMenuButton -> 아무것도 누르지 않은 상태에서 텍스트 버튼 클릭했을 때 NULL */
        /* 제일 첫 번째 버튼 (얇은 펜(그리기)) 로 지정 */
        if(view == null) { view = drawingMenuLayout.getChildAt(0); }

        for(int i=0; i<drawingMenuLayout.getChildCount(); i++) {
            drawingMenuLayout.getChildAt(i).setBackgroundColor(Color.TRANSPARENT);
        }
        view.setBackgroundColor(Color.rgb(233, 233, 233));

        de.initSelectedBitmap();
    }

    /* 텍스트 편집 시 키보드가 내려가면 하단 메뉴 보임, 이들을 비활성화 */
//    public void enableDrawingMenuButton(Boolean bool) {
//        LinearLayout drawingMenuLayout = de.getDrawingFragment().getBinding().drawingMenuLayout;
//        drawingMenuLayout.setBackgroundColor(Color.TRANSPARENT);
//        drawingMenuLayout.setEnabled(bool);
//
////        for(int i=0; i<drawingMenuLayout.getChildCount(); i++) {
////            drawingMenuLayout.getChildAt(i).setEnabled(bool);
////            drawingMenuLayout.getChildAt(i).setBackgroundColor(Color.rgb(233, 233, 233));
////        }
//    }

//    public boolean clickMic() {
//        if (!micFlag) { // Record Start
//            micFlag = true;
//            synchronized (recThread.getAudioRecord()) {
//                recThread.getAudioRecord().notify();
//                MyLog.i("Audio", "Mic On - RecordThread Notify");
//            }
//
//            return true;
//        } else { // Record Stop
//            micFlag = false;
//            recThread.setFlag(micFlag);
//            MyLog.i("Audio", "Mic  Off");
//
//            return false;
//        }
//    }

//    public int clickSpeaker() {
//        speakerMode = (speakerMode + 1) % 3; // 0, 1, 2, 0, 1, 2, ...
//
//        if (speakerMode == 0) { // SPEAKER MUTE
//            audioManager.setSpeakerphoneOn(false);
//            speakerFlag = false;
//            try {
//                if (client.getClient().isConnected()) {
//                    client.getClient().unsubscribe(client.getTopic_audio());
//                }
//            } catch (MqttException e) {
//                MyLog.i("Audio", "Topic Audio Unsubscribe error : " + e.getMessage());
//            }
//            for (AudioPlayThread audioPlayThread : client.getAudioPlayThreadList()) {
//                audioPlayThread.setFlag(speakerFlag);
//                audioPlayThread.getBuffer().clear();
//                MyLog.i("Audio", audioPlayThread.getUserName() + " buffer clear");
//            }
//        } else if (speakerMode == 1) { // SPEAKER ON
//            speakerFlag = true;
//            for (AudioPlayThread audioPlayThread : client.getAudioPlayThreadList()) {
//                synchronized (audioPlayThread.getAudioTrack()) {
//                    audioPlayThread.getAudioTrack().notify();
//                }
//            }
//            client.subscribe(client.getTopic_audio());
//        } else if (speakerMode == 2) { // SPEAKER LOUD
//            audioManager.setSpeakerphoneOn(true);
//        }
//
//        return speakerMode;
//    }

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

    /* 카카오링크 친구 초대 버튼 클릭 시 */
    public void clickInvite() {
        MyLog.i("KakaoLink", "Click KakaoLink Invite");

        /* 카카오 이미지 서버로 업로드 */
//        Bitmap bitmap = BitmapFactory.decodeResource(MainActivity.context.getResources(), R.drawable.kakao_link_img);
//        File file = new File(MainActivity.context.getCacheDir(), "kakao_link_img.png");
//        try {
//            FileOutputStream stream = new FileOutputStream(file);
//            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }
//
//          KakaoLinkService.getInstance().uploadImage(MainActivity.context, false, file, new ResponseCallback<ImageUploadResponse>() {
//              @Override
//              public void onFailure(ErrorResult errorResult) {
//                  MyLog.e("KakaoLink", errorResult.getErrorMessage());
//              }
//
//              @Override
//              public void onSuccess(ImageUploadResponse result) {
//                  MyLog.i("KakaoLink", result.getOriginal().getUrl());  // 여기서 얻은 url 사용
//              }
//          });

        /* 카카오링크에 회의명, 비밀번호 전달 - 피드 메시지 */
        FeedTemplate feedTemplate = FeedTemplate.newBuilder(
                ContentObject.newBuilder(
                        "♥︎드로잉투게더♥︎" + " - 회의명 [" + topic + "]",
                        "http://k.kakaocdn.net/dn/bftFB6/bl2J1T0qwdk/RxMA99bZEhHkQIUxQDBkgk/kakaolink40_original.png",
                        LinkObject.newBuilder()
                                .setAndroidExecutionParams("topic=" + topic + "&password=" + password)
                                .setIosExecutionParams("topic=" + topic + "&password=" + password)
                                .build())
                        .setDescrption("#공유 #드로잉 #실시간 #회의")
                        .build())
//                .setSocial(SocialObject.newBuilder()
//                        .setLikeCount(286)
//                        .setCommentCount(45)
//                        .setSharedCount(845)
//                .build())
                .addButton(new ButtonObject(
                        "앱으로 이동",
                        LinkObject.newBuilder()
                                .setAndroidExecutionParams("topic=" + topic + "&password=" + password)
                                .setIosExecutionParams("topic=" + topic + "&password=" + password)
                                .build()))
                .build();


        /* 카카오링크에 회의명, 비밀번호 전달 - 텍스트 메시지 */
//        TextTemplate textTemplate = TextTemplate.newBuilder("드로잉투게더",
//                LinkObject.newBuilder()
//                        .setAndroidExecutionParams("topic=" + topic + "&password=" + password)
//                        .setIosExecutionParams("topic=" + topic + "&password=" + password)
//                        .build())
//                .setButtonTitle("앱으로 이동").build();

        /* 카카오톡 API로 메시지 보내기 */
        KakaoLinkService.getInstance().sendDefault(MainActivity.context, feedTemplate, new ResponseCallback<KakaoLinkResponse>() {
            @Override
            public void onFailure(ErrorResult errorResult) {
                MyLog.e("KakaoLink", "Failure: " + errorResult.getErrorMessage());
                showKakaogAlert("카카오링크 에러", errorResult.getErrorMessage());
            }

            @Override
            public void onSuccess(KakaoLinkResponse result) {
                MyLog.i("KakaoLink", "Success");
            }
        });
    }

    /* 카카오링크 에러 시 */
    public void showKakaogAlert(String title, String message) {

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

    public File createImageFile(Fragment fragment) throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = new File(Environment.getExternalStorageDirectory() + "/Pictures", "seeseecallcall");
        if (!storageDir.exists()) storageDir.mkdirs();
        File  image = File.createTempFile(imageFileName, ".jpg", storageDir);
        photoPath = image.getAbsolutePath();
        MyLog.i("image", photoPath);

        return image;
    }

    /**
     * drawing performance
     * 저장된 stroke 메시지 반복적으로 publish
     */
    String usersComponentId = "";
    String strokeColor = "\"strokeColor\":\"#000000\"";
    int delay = 50;

    //stroke 메시지 저장하기
    public void clickSaveStroke(View view) {
        if(!client.isSaveStroke()) {
            de.getDrawingFragment().getBinding().saveStrokeBtn.setText("저장 완료");
            client.setSaveStroke(true);
            de.getDrawingFragment().getBinding().drawOneStrokeBtn.setEnabled(false);
            de.getDrawingFragment().getBinding().drawStrokesBtn.setEnabled(false);
        } else {
            de.getDrawingFragment().getBinding().saveStrokeBtn.setText("저장 시작");
            client.setSaveStroke(false);
            de.getDrawingFragment().getBinding().drawOneStrokeBtn.setEnabled(true);
            de.getDrawingFragment().getBinding().drawStrokesBtn.setEnabled(true);

            usersComponentId = de.getUsersComponentId();
            for(String message : client.getStrokeMessages()) {
                MyLog.i("segment", message);
            }
        }
    }

    //저장된 stroke 메시지 배열 1개 publish
    public void clickDrawOneStroke(View view) {
        String newUsersComponentId = de.usersComponentIdCounter();

        String str = client.getStrokeMessages().get(0).replaceAll(usersComponentId, newUsersComponentId);
        str = str.replaceAll(strokeColor, "\"strokeColor\":\"" + de.generateRandomHexCode() + "\"");
        client.publish(client.getTopic_data(), str);

        for(int i=1; i<client.getStrokeMessages().size(); i++) {
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            str = client.getStrokeMessages().get(i).replaceAll(usersComponentId, newUsersComponentId);
            client.publish(client.getTopic_data(), str);
        }
    }

    //저장된 stroke 메시지 배열 다이얼로그 입력값만큼 publish
    public void clickDrawStrokes(View view) {

        final EditText input = new EditText(de.getDrawingFragment().getActivity());
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

        AlertDialog dialog = new AlertDialog.Builder(de.getDrawingFragment().getActivity())
                .setTitle("반복할 개수")
                .setView(input)
                .setCancelable(true)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        MyLog.i("segment", input.getText() + "개 반복 시작 ");
                        for(int i=0; i<Integer.parseInt(input.getText().toString()); i++) {
                            clickDrawOneStroke(null);
                            try {
                                Thread.sleep(delay);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        MyLog.i("segment", input.getText() + "개 반복 완료 ");
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) { }
                })
                .create();

        dialog.show();
    }

    public void clickHide() {
        de.getDrawingFragment().getBinding().drawStrokeLayout.setVisibility(View.INVISIBLE);
    }

    public void clickShow() {
        de.getDrawingFragment().getBinding().drawStrokeLayout.setVisibility(View.VISIBLE);
    }



    public void showToastMsg(final String message) { Toast.makeText(de.getDrawingFragment().getActivity(), message, Toast.LENGTH_SHORT).show(); }

    public String getPhotoPath() {
        return photoPath;
    }

    public MutableLiveData<String> getUserNum() {
        return userNum;
    }

    public MutableLiveData<String> getUserPrint() { return userPrint; }

    public void setUserNum(int num) {
        userNum.postValue(num + "명");
    }

    public void setUserPrint(String user) { userPrint.postValue(user); }

    @Override
    public void onCleared() {
        super.onCleared();
        MyLog.i("lifeCycle", "DrawingViewModel onCleared()");
    }

}
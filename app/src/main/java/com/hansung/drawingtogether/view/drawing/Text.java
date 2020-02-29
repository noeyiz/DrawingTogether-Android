package com.hansung.drawingtogether.view.drawing;


import android.content.ClipData;
import android.graphics.Color;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;


import com.hansung.drawingtogether.data.remote.model.MQTTClient;
import com.hansung.drawingtogether.databinding.FragmentDrawingBinding;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Text { // EditTextView

    private DrawingEditor de = DrawingEditor.getInstance();
    private MQTTClient client = MQTTClient.getInstance();
    private JSONParser parser = JSONParser.getInstance();

    private DrawingFragment drawingFragment;
    private FragmentDrawingBinding binding;

    private TextView textView;
    private EditText editText;
    private FrameLayout frameLayout;
    private FrameLayout.LayoutParams frameLayoutParams;
    private InputMethodManager inputMethodManager; // KeyBoard Control

    private TextAttribute textAttribute;

    private ClipData clip;

    private int xDelta;
    private int yDelta;

    private float xRatio;
    private float yRatio;

    private GestureDetector gestureDetector;

    //private boolean isTextInited = false; // fixme nayeon -> TextAttribute 클래스 멤버로
    private boolean isDragging = false;

    private final int MAX_LENGTH = 15;

    public Text(DrawingFragment drawingFragment, TextAttribute textAttr) {    //fixme minj
        this.drawingFragment = drawingFragment;
        this.binding = drawingFragment.getBinding();

        this.textView = new TextView(drawingFragment.getActivity());
        this.textView.setPadding(20, 20, 20, 20);       //fixme minj
        this.editText = new EditText(drawingFragment.getActivity());
        this.editText.setPadding(20, 20, 20, 20);

        editText.setFilters(new InputFilter[] {new InputFilter.LengthFilter(MAX_LENGTH)}); // 텍스트의 최대 글자 수 지정

        this.frameLayout = this.binding.drawingViewContainer;
        this.inputMethodManager = this.drawingFragment.getInputMethodManager();

        this.textAttribute = textAttr;

        setTextViewAttribute();
        setEditTextAttribute();

        // fixme nayeon Edit Text View 가 초기에 놓일 자리
        frameLayoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT); // frameLayoutParams 은 상단 중앙에 대한 위치 저장
        editText.setLayoutParams(frameLayoutParams);
        textView.setLayoutParams(frameLayoutParams);

        if(this.textAttribute.isTextInited() && this.textAttribute.isTextMoved()) { setTextViewLocationInConstructor(); } // 텍스트가 초기화 되어있을 경우 (이미 누군가에 의해 생성된 텍스트) - for middleman
        else { frameLayoutParams.gravity = Gravity.CENTER | Gravity.TOP; }

        setListenerOnTextView();

        // 생성자에서 Drawing Editor 텍스트 배열 리스트에 자기 자신(Text) 추가
        //de.addTexts(this); // changeEditTextToTextView() 에서 추가
    }

    public void sendMqttMessage(TextMode textMode) {
        // todo nayeon
        // 텍스트 동시성 처리를 위해 텍스트 생성시(TextMode.CREATE) 텍스트 배열에 텍스트를 추가하고,
        // 그 상태의 배열 인덱스를 메시지에 함께 전송
        // 이 인덱스 정보를 바탕으로 텍스트를 보낸 송신자도 MQTT Callback 에서 자신의 textID 설정 [ ONLINE ]

        // [ OFFLINE ]일 경우 이 부분에서 아이디 값을 증가시키고 설정시켜주기
        MqttMessageFormat message = new MqttMessageFormat(de.getMyUsername(), Mode.TEXT, de.getCurrentType(), this.textAttribute, textMode, de.getTexts().size()-1);
        client.publish(client.getTopic_data(), parser.jsonWrite(message));
    }

    private void setTextViewAttribute() {
        textView.setText(textAttribute.getText());
        textView.setTextSize(textAttribute.getTextSize());
        textView.setTextColor(textAttribute.getTextColor());
        textView.setBackgroundColor(textAttribute.getTextBackgroundColor());
        textView.setGravity(textAttribute.getTextGravity());
        textView.setTypeface(null, textAttribute.getStyle());
    }

    private void setEditTextAttribute() {
        editText.setText(textAttribute.getText());
        editText.setTextSize(textAttribute.getTextSize());
        editText.setTextColor(textAttribute.getTextColor());
        editText.setBackgroundColor(textAttribute.getTextBackgroundColor());
        editText.setGravity(textAttribute.getTextGravity());
        editText.setTypeface(null, textAttribute.getStyle());
    }

    private void setTextAttribute() {
        textAttribute.setUsername(de.getMyUsername());
        textAttribute.setGeneratedLayoutWidth(frameLayout.getWidth());
        textAttribute.setGeneratedLayoutHeight(frameLayout.getHeight());
    }

    // fixme nayeon Listener
    private void setListenerOnTextView() {
        View.OnTouchListener onTouchListener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                view.getParent().requestDisallowInterceptTouchEvent(true);  //fixme minj

                // 텍스트에 대한 터치 이벤트가 발생하면,
                // 현재 모드를 텍스트로 지정
                // de.setCurrentMode(Mode.TEXT);

                // 텍스트 이름이 지정되어 있지 않거나, 혹은 이름이 지정되어있으나 내 이름이 아닐 때
                // 텍스트 사용불가 (다른 사람이 사용중)
                if(textAttribute.getUsername() != null && !textAttribute.getUsername().equals(de.getMyUsername())) {
                    Toast.makeText(drawingFragment.getActivity(),"This text is being edited by another user !!!", Toast.LENGTH_SHORT).show();
                    return true;
                }
                // fixme nayeon
                // 현재 사용중인(조작중인) 텍스트가 있다면 다른 텍스트에 터치 못하도록
                else if(de.isTextBeingEdited()) {
                    Toast.makeText(drawingFragment.getActivity(),"Other text cannot be edited during text editing ...", Toast.LENGTH_SHORT).show();
                    return true;
                }

                else { textAttribute.setUsername(de.getMyUsername()); } // 텍스트 사용이 가능하다면 텍스트에 자신의 이름을 지정하고 사용 시작

                setTextAttribute(); // 터치가 시작될 때마다 텍스트가 생성된 레이아웃의 크기 지정(비율 계산을 위해)

                if(gestureDetector.onTouchEvent(event)) { return true; }

                return true;
            }
        };



        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int before, int count) {
                //Log.i("Before Text Changed", charSequence.toString() + " " + start + " " + before + " " + count);
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int count, int after) {
                //Log.i("On Text Changed", charSequence.toString() + " " + start + " " + count + " " + after);
            }

            @Override
            public void afterTextChanged(Editable editable) {
                //Log.i("After Text Changed", editable.toString());
                // fixme nayeon ** TextAttribute 세팅 필요성 - 사용자 이름변경은 changeTextViewToEditText 에서

                // 처음 텍스트 생성을 위해 사용자가 텍스트를 수정 중일 경우 메시지 전송 X
                if(!textAttribute.isTextInited()) return;

                textAttribute.setText(editable.toString());
                sendMqttMessage(TextMode.MODIFY);
            }
        };


        // Set Listener
        textView.setOnTouchListener(onTouchListener);
        editText.addTextChangedListener(textWatcher);
    }

    private void eraseText() {
        // 지우개 모드에서 텍스트가 터치 되었을 시
        // 다음 이벤트가 발생하지 않도록 텍스트뷰 비활성화
        textView.setEnabled(false);

        removeTextViewToFrameLayout();
        de.removeTexts(this); // 텍스트 리스트에서 텍스트 제거
    }


    // fixme nayeon Focusing - Edit Text 사용 시 포커스와 키보드 처리
    private void processFocusIn() {
        editText.setBackgroundColor(Color.TRANSPARENT);
        editText.requestFocus();
        inputMethodManager.showSoftInput(editText, 0);
    }

    // todo nayeon - EditText 사용 종료 시 키보드 내리기
    public void processFocusOut() { inputMethodManager.hideSoftInputFromWindow(editText.getWindowToken(), 0); }

    public void calculateRatio(float myLayoutWidth, float myLayoutHeight) {
        this.xRatio = myLayoutWidth / this.textAttribute.getGeneratedLayoutWidth();
        this.yRatio = myLayoutHeight / this.textAttribute.getGeneratedLayoutHeight();
    }


    private void changeTextViewToEditText() {
        de.setCurrentMode(Mode.TEXT); // 텍스트 편집시 텍스트 모드 지정

        textAttribute.setUsername(de.getMyUsername()); // 텍스트 사용을 시작하면 텍스트에 자신의 이름 지정하기

        // TextView 에서 EditText 로 변경하여 레이아웃에 부착할 때
        // TextView 의 위치 필요 ( EditText 도 TextView 가 붙었던 자리에 붙여져야 하기 때문에
        // editText.setX(textView.getX());
        // editText.setY(textView.getY());

        // 텍스트 변경시 그리기 뷰 중앙에 띄워서 수정하기 (EditText 가 놓일 자리 : 중앙)
        // 수정 후에는 원래자리에 붙이기 (TextView 가 놓일 자리 : x, y 좌푯값)
        editText.setLayoutParams(frameLayoutParams);

        textAttribute.setText(textView.getText().toString()); // TextView 에 쓰여져 있던 내용을 TextAttribute 에 저장
        setEditTextAttribute(); // 앞서 변경한 TextAttribute 로 (텍스트 내용) EditText 에 설정

        removeTextViewToFrameLayout(); // TextView 를 레이아웃에서 제거
        addEditTextToFrameLayout(); // EditText 를 레이아웃에 부착
    }

    // Done Button 클릭 시 실행되는 함수
    public void changeEditTextToTextView() {
        // fixme nayeon
        // TextAttribute 에 지정할 속성 : 글자 크기, 색깔, 글씨체 등
        // 현재 EditText 에서 이러한 속성 정보들을 알아내
        // TextAttribute 에 저장하고, TextView 로 바꾸기 전에 TextView의 속성 지정해주기
        //setTextAttribute();
        //setTextViewAttribute(textAttribute);


        // 텍스트가 생성되고 처음 텍스트가 초기화 완료되는 시점에
        // 텍스트 사용 가능 설정을 하고 ( 초기 텍스트 입력이 완료되면 다른 사용자도 텍스트 조작 가능 )
        // MQTT 메시지 전송

        if(!textAttribute.isTextInited()) { // 메시지 수신자가 텍스트를 처음 생성
            // 사용자가 텍스트를 입력하지 않고 텍스트 완료 버튼(DONE BUTTON)을 눌렀을 경우
            // 텍스트 생성하지 않기
            if(editText.getText().toString().matches("")) return;

            textAttribute.setText(editText.getText().toString()); // EditText 에서 변경된 내용(문자열)을 TextAttribute 에 저장
            textAttribute.setCoordinates((int)editText.getX(), (int)editText.getY()); // 텍스트가 처음 초기화 된 이후에는 좌표값 지정 // fixme nayeon

            de.addTexts(this); // todo nayeon 텍스트 구조체에 추가
            sendMqttMessage(TextMode.CREATE); // 변경된 내용을 가진 TextAttribute 를 MQTT 메시지 전송


            textAttribute.setTextInited(true);

            Log.i("drawing", "text create");
            de.addHistory(new DrawingItem(TextMode.CREATE, getTextAttribute())); //fixme minj - addHistory
            if(de.getHistory().size() == 1)
                de.getDrawingFragment().getBinding().undoBtn.setEnabled(true);
            Log.i("drawing", "history.size()=" + de.getHistory().size());
            de.clearUndoArray();
        }

        // 내용이 빈 경우 텍스트 지우기
        if(editText.getText().toString().matches("")) {
            eraseText();
            sendMqttMessage(TextMode.ERASE);
            return;
        }

        textAttribute.setUsername(null);

        textAttribute.setText(editText.getText().toString()); // 변경된 텍스트를 텍스트 속성 클래스에 저장
        setTextViewAttribute(); // TextView 가 변경된 텍스트 속성( 텍스트 문자열 )을 가지도록 지정
        de.setCurrentText(null); // 현재 조작중인 텍스트 null 처리
        de.setTextBeingEdited(false); // 텍스트 편집 모드 false 처리

        removeEditTextToFrameLayout(); // EditText 를 레이아웃에서 제거하고
        addTextViewToFrameLayout();

        String preText = textAttribute.getPreText();
        if(preText != null && !preText.equals(textAttribute.getText())) {   //modify 이전과 text 가 달라졌을 때만 history 에 저장
            textAttribute.setModified(true);
            Log.i("drawing", "text modify");
            de.addHistory(new DrawingItem(TextMode.MODIFY, getTextAttribute()));   //fixme minj - addHistory
            Log.i("drawing", "history.size()=" + de.getHistory().size());
            de.clearUndoArray();
        }

        sendMqttMessage(TextMode.DONE); // 사용 종료를 알리기 위해 보내야함 ( 사용자이름 : null )

        de.setCurrentMode(Mode.DRAW); // 텍스트 편집이 완료 되면 현재 모드는 기본 드로잉 모드로
        drawingFragment.getBinding().drawBtn.performClick();
        textAttribute.setModified(false);

    }

    public void activeEditText() {
        drawingFragment.setDoneButton(); // 사용자로부터 텍스트 입력 완료를 얻기 위한 DONE 버튼 부착
        processFocusIn(); // fixme nayeon - Edit Text 를 붙인 후 자동 포커싱
        de.setCurrentText(this);
        de.setTextBeingEdited(true);
    }

    // TextAttribute 에 저장된 x, y 좌푯값을 바탕으로
    // TextView 의 위치 지정
    public void setTextViewLocation() {
        // 텍스트 위치 비율 계산
        calculateRatio(frameLayout.getWidth(), frameLayout.getHeight()); // xRatio, yRatio 설정

        textView.setX(textAttribute.getX() * xRatio - (textView.getWidth()/2));
        textView.setY(textAttribute.getY() * yRatio - (textView.getHeight()/2));
    }

    // 중간자 또는 불러오기 시 텍스트의 좌표가 정해진 후 지정해서 레이아웃에 붙여야하는 경우
    // 레이아웃에 붙지 않은 상태에서 textView 의 크기 알 수 없음
    public void setTextViewLocationInConstructor() {
        calculateRatio(frameLayout.getWidth(), frameLayout.getHeight()); // xRatio, yRatio 설정

        textView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);

        float x = textAttribute.getX() * xRatio - (textView.getMeasuredWidth()/2);
        float y = textAttribute.getY() * yRatio - (textView.getMeasuredHeight()/2);

        textView.setX(x);
        textView.setY(y);
    }

    public void setTextViewLocation(int x, int y) {
        // 텍스트 위치 비율 계산
        calculateRatio(frameLayout.getWidth(), frameLayout.getHeight()); // xRatio, yRatio 설정

        textView.setX(x * xRatio - (textView.getWidth()/2));
        textView.setY(y * yRatio - (textView.getHeight()/2));
    }

    private Text getText() { return this; }


    /*
     *
     *  뷰를 변경하는 작업 : 메인 스레드에서 처리되어야 하는 작업들
     *
     */

    public void addTextViewToFrameLayout() { frameLayout.addView(textView); }

    public void addEditTextToFrameLayout() { frameLayout.addView(editText); }

    public void removeTextViewToFrameLayout()  { frameLayout.removeView(textView); }

    public void removeEditTextToFrameLayout() { frameLayout.removeView(editText); }

    public void modifyTextViewContent(String text) { textView.setText(text); }

    public void createGestureDetecter() { gestureDetector = new GestureDetector(drawingFragment.getActivity(), new GestureConfirm()); }


    class GestureConfirm implements GestureDetector.OnGestureListener {
        @Override
        public boolean onDown(MotionEvent motionEvent) {
            //System.out.println("onDown() called");

            if(de.getCurrentMode().equals(Mode.ERASE)) {
                eraseText();
                sendMqttMessage(TextMode.ERASE);
                Log.i("drawing", "text erase");
                de.addHistory(new DrawingItem(TextMode.ERASE, getTextAttribute()));    //fixme minj - addHistory
                Log.i("drawing", "history.size()=" + de.getHistory().size());
                de.clearUndoArray();
            } else {
                de.setCurrentMode(Mode.TEXT);
                textView.setBackgroundColor(Color.LTGRAY); // todo nayeon
            }

            return true;
        }

        @Override
        public void onShowPress(MotionEvent motionEvent) {
            //System.out.println("onShowPress() called");
        }

        @Override
        public boolean onSingleTapUp(MotionEvent motionEvent) {
            //System.out.println("onSingleTapUp() called");

            textAttribute.setPreText(textAttribute.getText());
            changeTextViewToEditText();
            activeEditText();

            return true;
        }

        @Override
        public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
            //System.out.println("onScroll() called"+v+", "+v1);

            de.setCurrentMode(Mode.TEXT);

            if(!isDragging) {
                isDragging = true;

                de.setCurrentText(getText());

                ClipData clip = ClipData.newPlainText("TEXT OBJECT", "TEXT");
                textView.setVisibility(View.INVISIBLE);
                textView.startDrag(clip, new View.DragShadowBuilder(textView), textView, 0);
            }

            return true;
        }

        @Override
        public void onLongPress(MotionEvent motionEvent) {
            // System.out.println("onLongPress() called");
        }

        @Override
        public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
            // System.out.println("onFling() called : "+v+", "+v1);
            return true;
        }
    }
}


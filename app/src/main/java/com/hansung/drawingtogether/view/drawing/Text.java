package com.hansung.drawingtogether.view.drawing;


import android.content.ClipData;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
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
import android.widget.LinearLayout;
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
    private FrameLayout textEditLayout;
    private InputMethodManager inputMethodManager; // KeyBoard Control

    private TextAttribute textAttribute;

    private ClipData clip;

    private float xRatio;
    private float yRatio;

    private GestureDetector gestureDetector;

    private boolean isDragging = false;

    private final int MAX_LENGTH = 40;

    public Text(DrawingFragment drawingFragment, TextAttribute textAttr) {
        this.drawingFragment = drawingFragment;
        this.binding = drawingFragment.getBinding();

        this.frameLayout = this.binding.drawingViewContainer;
        this.textEditLayout = this.binding.textEditLayout;
        this.inputMethodManager = this.drawingFragment.getInputMethodManager();

        this.textAttribute = textAttr;


        // fixme nayeon [ TextView 가로 크기는 텍스트 편집 레이아웃 1/3, EditText 가로 크기는 텍스트 편집 레이아웃 3/4 ]
        this.textView = new TextView(drawingFragment.getActivity());
        /*this.textView.setPadding(20, 20, 20, 20);
        this.textView.setWidth(textEditLayout.getWidth()/3);*/ // addTextViewToFrameLayout

        this.editText = this.binding.editText; // fixme nayeon - EditText 를 fragment_drawing.xml 에 정의
        //this.editText.setWidth((int)(textEditLayout.getWidth()*0.75));*/ // drawingFragment



        setTextViewAttribute();
        setTextViewInitialPlace(this.textAttribute);




        setListenerOnTextView();
    }

    public void sendMqttMessage(TextMode textMode) {
        MqttMessageFormat message = new MqttMessageFormat(de.getMyUsername(), Mode.TEXT, de.getCurrentType(), this.textAttribute, textMode, de.getTexts().size()-1);
        client.publish(client.getTopic_data(), parser.jsonWrite(message));
    }

    public void setTextViewAttribute() {
        textView.setText(textAttribute.getText());
        textView.setTextSize(textAttribute.getTextSize());
        textView.setTextColor(textAttribute.getTextColor());
        textView.setBackgroundColor(textAttribute.getTextBackgroundColor());
        textView.setGravity(textAttribute.getTextGravity());
        textView.setTypeface(null, textAttribute.getStyle());
    }

    public void setEditTextAttribute() {
        editText.setText(textAttribute.getText());
        editText.setTextSize(textAttribute.getTextSize());
        editText.setTextColor(textAttribute.getTextColor());
        editText.setBackgroundColor(textAttribute.getTextBackgroundColor());
        editText.setGravity(textAttribute.getTextGravity());
        editText.setTypeface(null, textAttribute.getStyle());

        editText.setSelection(editText.length()); // fixme nayeon - Edit Text 커서 뒤로
    }

    // 중간자 또는 불러오기 시 텍스트의 좌표가 정해진 후 지정해서 레이아웃에 붙여야하는 경우
    // 레이아웃에 붙지 않은 상태에서 textView 의 크기 알 수 없음
    public void setTextViewLocationInConstructor() {
        FrameLayout.LayoutParams frameLayoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT); // frameLayoutParams 은 상단 중앙에 대한 위치 저장
        textView.setLayoutParams(frameLayoutParams); // TextView 크기 설정

        calculateRatio(frameLayout.getWidth(), frameLayout.getHeight()); // xRatio, yRatio 설정

        textView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);

        float x = textAttribute.getX() * xRatio - (textView.getMeasuredWidth()/2);
        float y = textAttribute.getY() * yRatio - (textView.getMeasuredHeight()/2);


        textView.setX(x);
        textView.setY(y);
    }


    private void setViewLayoutParams(View view) {
        FrameLayout.LayoutParams frameLayoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT); // frameLayoutParams 은 상단 중앙에 대한 위치 저장
        frameLayoutParams.gravity = Gravity.CENTER;
        view.setLayoutParams(frameLayoutParams);
    }

    public void setTextViewInitialPlace(TextAttribute textAttribute) {
        // TextView 가 초기에 놓일 자리
        if(textAttribute.isTextInited() && textAttribute.isTextMoved()) {
            Log.e("text", "SET TEXT VIEW LOCATION IN CONSTRUCTOR()");
            setTextViewLocationInConstructor();
        } // 텍스트가 초기화 되어있을 경우 (이미 누군가에 의해 생성된 텍스트) - for middleman
        else {
            setViewLayoutParams(textView);
        } // todo nayeon - ☆ ☆ LayoutParams 와 뷰의 좌푯값의 관계
    }

    private void setTextAttribute() {
        textAttribute.setUsername(de.getMyUsername());
        textAttribute.setGeneratedLayoutWidth(frameLayout.getWidth());
        textAttribute.setGeneratedLayoutHeight(frameLayout.getHeight());
    }

    private void setListenerOnTextView() {
        View.OnTouchListener onTouchListener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                view.getParent().requestDisallowInterceptTouchEvent(true);  // 부모(frame layout)가 터치 이벤트를 가로채지 못 하도록


                // 1. 중간에 다른 사용자가 들어오는 중일 경우 텍스트 터치 막기
                // 2. 텍스트에 다른 사용자 이름이 지정되어 있으면
                // 텍스트 사용불가 (다른 사람이 사용중) - 이름이 null 일 경우만 사용 가능
                // 3. 현재 사용자가 한 텍스트 편집 중이라면, 다른 텍스트 편집 불가능 (한 번에 한 텍스트만 편집 가능)
                if(de.isMidEntered()) {
                    showToastMsg("다른 사용자가 접속 중 입니다 잠시만 기다려주세요");
                    return true;
                }

                else if(textAttribute.getUsername() != null && !textAttribute.getUsername().equals(de.getMyUsername())) {
                    showToastMsg("다른 사용자가 편집중인 텍스트입니다");
                    return true;
                }
                // 현재 사용중인(조작중인) 텍스트가 있다면 다른 텍스트에 터치 못하도록
                else if(de.isTextBeingEdited()) {
                    showToastMsg("편집 중에는 다른 텍스트를 사용할 수 없습니다");
                    return true;
                }

                // else { textAttribute.setUsername(de.getMyUsername()); } // 텍스트 사용이 가능하다면 텍스트에 자신의 이름을 지정하고 사용 시작

                setTextAttribute(); // 터치가 시작될 때마다 텍스트가 생성된 레이아웃의 크기 지정(비율 계산을 위해)

                if(gestureDetector.onTouchEvent(event)) { return true; }

                return true;
            }
        };



        // fixme nayeon
        // EditText 를 하나만 두고 쓰다보니까 현재 편집중인 텍스트를 명확하게 잡아줘야함.
        TextWatcher textWatcher = new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int before, int count) { }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int count, int after) { }

            @Override
            public void afterTextChanged(Editable editable) {
                // fixme nayeon
                Text text = de.getCurrentText();
                if(text != null) { text.getTextAttribute().setText(editable.toString()); }
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
        editText.requestFocus();
        inputMethodManager.showSoftInput(editText, 0);
    }

    // todo nayeon - EditText 사용 종료 시 키보드 내리기
    public void processFocusOut() { inputMethodManager.hideSoftInputFromWindow(editText.getWindowToken(), 0); }

    public void calculateRatio(float myLayoutWidth, float myLayoutHeight) {
        this.xRatio = myLayoutWidth / this.textAttribute.getGeneratedLayoutWidth();
        this.yRatio = myLayoutHeight / this.textAttribute.getGeneratedLayoutHeight();

        Log.e("text", "my layout location = " + myLayoutWidth + ", " + myLayoutHeight);
        Log.e("text", "attached layout location = " + textAttribute.getGeneratedLayoutWidth() + ", " + textAttribute.getGeneratedLayoutHeight());
    }


    public void changeTextViewToEditText() {
        de.setCurrentMode(Mode.TEXT); // 텍스트 편집시 텍스트 모드 지정

        textAttribute.setUsername(de.getMyUsername()); // 텍스트 사용을 시작하면 텍스트에 자신의 이름 지정하기

        textAttribute.setText(textView.getText().toString()); // TextView 에 쓰여져 있던 내용을 TextAttribute 에 저장
        setEditTextAttribute(); // 앞서 변경한 TextAttribute 로 (텍스트 내용) EditText 에 설정 [ 색 굵기 등등 ]

        activeTextEditing();
    }


    // todo nayeon 함수 정리 필요
    // Done Button 클릭 시 실행되는 함수
    public void changeEditTextToTextView() {

        // 텍스트가 생성되고 처음 텍스트가 초기화 완료되는 시점에
        // 텍스트 사용 가능 설정을 하고 ( 초기 텍스트 입력이 완료되면 다른 사용자도 텍스트 조작 가능 )
        // MQTT 메시지 전송
        if(!textAttribute.isTextInited()) { // 메시지 수신자가 텍스트를 처음 생성하는 경우

            // 사용자가 텍스트를 입력하지 않고 텍스트 완료 버튼(DONE BUTTON)을 눌렀을 경우
            // 텍스트 생성하지 않기
            if(isEditTextContentEmpty()) {
                deactivateTextEditing();

                de.setCurrentMode(Mode.DRAW); // 텍스트 편집이 완료 되면 현재 모드는 기본 드로잉 모드로
                return;
            }


            textAttribute.setUsername(null); // send mqtt message
            textAttribute.setTextInited(true);
            textAttribute.setText(editText.getText().toString()); // 변경된 텍스트를 텍스트 속성 클래스에 저장
            setTextViewAttribute(); // TextView 가 변경된 텍스트 속성( 텍스트 문자열 )을 가지도록 지정
            de.setCurrentText(null); // 현재 조작중인 텍스트 null 처리

            de.addTexts(this);

            sendMqttMessage(TextMode.CREATE); // 변경된 내용을 가진 TextAttribute 를 MQTT 메시지 전송

            deactivateTextEditing();

            // set text view properties fixme nayeon
            this.textView.setPadding(20, 20, 20, 20);
            this.textView.setWidth(frameLayout.getWidth()/3);

            addTextViewToFrameLayout(); // TextView 를 레이아웃에 추가 // fixme nayeon ☆ 텍스트 처음 생성 시에만 TEXT VIEW 붙이기


            Log.i("drawing", "text create");
            de.addHistory(new DrawingItem(TextMode.CREATE, getTextAttribute())); //fixme minj - addHistory
            if(de.getHistory().size() == 1)
                de.getDrawingFragment().getBinding().undoBtn.setEnabled(true);
            Log.i("drawing", "history.size()=" + de.getHistory().size());
            de.clearUndoArray();

            de.setCurrentMode(Mode.DRAW); // 텍스트 편집이 완료 되면 현재 모드는 기본 드로잉 모드로
            return;
        }

        // 내용이 빈 경우 텍스트 지우기
        if(isEditTextContentEmpty()) {
            removeTextViewToFrameLayout();
            deactivateTextEditing();
            eraseText();
            sendMqttMessage(TextMode.ERASE);

            de.setCurrentMode(Mode.DRAW); // 텍스트 편집이 완료 되면 현재 모드는 기본 드로잉 모드로
            return;
        }

        textAttribute.setUsername(null); // send mqtt message

        textAttribute.setText(editText.getText().toString()); // 변경된 텍스트를 텍스트 속성 클래스에 저장
        setTextViewAttribute(); // TextView 가 변경된 텍스트 속성( 텍스트 문자열 )을 가지도록 지정 // todo 1. 텍스트뷰 속성 지정
        de.setCurrentText(null); // 현재 조작중인 텍스트 null 처리

        deactivateTextEditing(); // todo 2. 편집 비활성화 [ TextView 는 처음 텍스트 생성 시 한 번만 붙이기 ]

        // for history (undo, redo)
        String preText = textAttribute.getPreText();
        if(preText != null && !preText.equals(textAttribute.getText())) {   //modify 이전과 text 가 달라졌을 때만 history 에 저장
            textAttribute.setModified(true);
            Log.i("drawing", "text modify");
            de.addHistory(new DrawingItem(TextMode.MODIFY, getTextAttribute()));   //fixme minj - addHistory
            Log.i("drawing", "history.size()=" + de.getHistory().size());
            de.clearUndoArray();
        }

        sendMqttMessage(TextMode.DONE); // 사용 종료를 알리기 위해 보내야함 ( 사용자이름 : null )

        //drawingFragment.getBinding().drawBtn.performClick();
        textAttribute.setModified(false);
        de.setCurrentMode(Mode.DRAW); // 텍스트 편집이 완료 되면 현재 모드는 기본 드로잉 모드로
    }

    public void activeTextEditing() {
        de.setCurrentText(this);
        de.setTextBeingEdited(true);

        enableDrawingMenuButton(false); // fixme nayeon

        editText.setWidth((int)(binding.textEditLayout.getWidth() * 0.75)); // fixme nayeon

        binding.redoBtn.setVisibility(View.INVISIBLE);
        binding.undoBtn.setVisibility(View.INVISIBLE); // 텍스트 편집 시 UNDO, REDO 버튼 안보이도록

        textView.setVisibility(View.INVISIBLE);

        binding.doneBtn.setVisibility(View.VISIBLE);
        binding.sizeBar.setVisibility(View.VISIBLE);

        binding.textEditLayout.setBackgroundColor(Color.parseColor("#80D3D3D3")); // 50% 투명도 회색 배경

        editText.setVisibility(View.VISIBLE);
        editText.setEnabled(true);


        processFocusIn(); // Edit Text 를 붙인 후 자동 포커싱
    }

    public void deactivateTextEditing() {
        textEditLayout.setBackgroundColor(Color.TRANSPARENT); // 텍스트 편집이 종료되면 레이아웃 색깔 다시 무색으로

        binding.doneBtn.setVisibility(View.INVISIBLE); // DONE 버튼 숨기기
        binding.sizeBar.setVisibility(View.INVISIBLE);

        textView.setVisibility(View.VISIBLE); // 편집 들어갔던 텍스트 뷰 다시 보이기

        binding.redoBtn.setVisibility(View.VISIBLE);
        binding.undoBtn.setVisibility(View.VISIBLE);

        //binding.textEditLayout.removeView(editText);// todo nayeon - Detach EditText
        editText.setVisibility(View.INVISIBLE);
        editText.setEnabled(false);

        processFocusOut();

        enableDrawingMenuButton(true); // fixme nayeon

        de.setTextBeingEdited(false); // 텍스트 편집 모드 false 처리
    }

    // TextAttribute 에 저장된 x, y 좌푯값을 바탕으로
    // TextView 의 위치 지정
    public void setTextViewLocation() {
        // 텍스트 위치 비율 계산
        calculateRatio(frameLayout.getWidth(), frameLayout.getHeight()); // xRatio, yRatio 설정

        textView.setX(textAttribute.getX() * xRatio - (textView.getWidth()/2));
        textView.setY(textAttribute.getY() * yRatio - (textView.getHeight()/2));
    }


    public void setTextViewLocation(int x, int y) {
        calculateRatio(frameLayout.getWidth(), frameLayout.getHeight()); // xRatio, yRatio 설정

        textView.setX(x * xRatio - (textView.getWidth()/2));
        textView.setY(y * yRatio - (textView.getHeight()/2));
    }

    private Text getText() { return this; } // For Using In Gesture Class


    private void showToastMsg(final String message) { Toast.makeText(drawingFragment.getActivity(), message, Toast.LENGTH_SHORT).show(); }

    public void setDrawingFragment(DrawingFragment drawingFragment) {
        this.drawingFragment = drawingFragment;
        this.binding = drawingFragment.getBinding();

        this.frameLayout = this.binding.drawingViewContainer;
        this.textEditLayout = this.binding.textEditLayout;
        this.inputMethodManager = this.drawingFragment.getInputMethodManager();
    }

    private boolean isEditTextContentEmpty() { return editText.getText().toString().matches(""); }

    public void enableDrawingMenuButton(Boolean bool) {
        LinearLayout drawingMenuLayout = de.getDrawingFragment().getBinding().drawingMenuLayout;

        for(int i=0; i<drawingMenuLayout.getChildCount(); i++) {
            drawingMenuLayout.getChildAt(i).setEnabled(bool);
        }
    }

    /*
     *
     *  뷰를 변경하는 작업 : 메인 스레드에서 처리되어야 하는 작업들
     *
     */

    // todo nayeon ☆ ☆ ☆ 레이아웃에 텍스트 뷰 추가 시 오류 캐치
    public void addTextViewToFrameLayout() {
        try {
            frameLayout.addView(textView);
            Log.e("text", "frameLayout in adding view " + frameLayout.toString());
            Log.e("text", "text view size in adding view " + textView.getWidth() + ", " + textView.getHeight());

        }
        catch(IllegalStateException ie) {
            ie.printStackTrace();

            Log.e("error", "☆ ☆ ☆  frameLayout.addView ☆ ☆ ☆");
            Log.e("drawing editor text size", Integer.toString(de.getTexts().size()));
            Log.e("frame layout children count", Integer.toString(frameLayout.getChildCount()));

            for(Text text: de.getTexts()) { Log.e("text id", text.getTextAttribute().getId()); } // 모든 텍스트 아이디 출력
        }
    }

    public void setTextViewProperties() {  // set text view padding & width
        this.textView.setPadding(20, 20, 20, 20);
        this.textView.setWidth(frameLayout.getWidth()/3);
    }

    public void removeTextViewToFrameLayout()  { frameLayout.removeView(textView); }

    public void modifyTextViewContent(String text) { textView.setText(text); }

    public void createGestureDetecter() { gestureDetector = new GestureDetector(drawingFragment.getActivity(), new GestureConfirm()); }


    /*
     *
     *  텍스트 뷰 터치 이벤트 처리를 위한 제스처 클래스
     *
     */
    class GestureConfirm implements GestureDetector.OnGestureListener {
        // onDown - onShowPress - onLongPress ( not call onSingleTapUp )
        // onDown - onScroll
        // onDown - onSingleTapUp

        @Override
        public boolean onDown(MotionEvent motionEvent) {
            System.out.println("onDown() called");

            if(de.getCurrentMode().equals(Mode.ERASE)) {
                eraseText();
                sendMqttMessage(TextMode.ERASE);
                Log.i("drawing", "text erase");
                de.addHistory(new DrawingItem(TextMode.ERASE, getTextAttribute()));    //fixme minj - addHistory
                Log.i("drawing", "history.size()=" + de.getHistory().size());
                de.clearUndoArray();
            } else {
                textView.setBackground(de.getTextMoveBorderDrawable());
                de.setCurrentMode(Mode.TEXT);
            }

            return true;
        }

        @Override
        public void onShowPress(MotionEvent motionEvent) {
            System.out.println("onShowPress() called");
            //textView.setBackground(de.getTextMoveBorderDrawable());
        }

        @Override
        public boolean onSingleTapUp(MotionEvent motionEvent) {
            System.out.println("onSingleTapUp() called");

            textView.setBackground(null); //

            textAttribute.setPreText(textAttribute.getText());
            changeTextViewToEditText();

            sendMqttMessage(TextMode.MODIFY_START);

            return true;
        }

        @Override
        public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
            System.out.println("onScroll() called"+v+", "+v1);

            de.setCurrentMode(Mode.TEXT);

            if(!isDragging) { // onScroll onScroll onScroll ...
                isDragging = true;

                de.setCurrentText(getText()); // fixme nayeon - frameLayoutDragListene
                textView.setBackground(de.getTextMoveBorderDrawable()); // onDown - onScroll 일 경우 (onShowPress 거치지 않은 경우)

                ClipData clip = ClipData.newPlainText("TEXT OBJECT", "TEXT");
                textView.setVisibility(View.INVISIBLE);
                textView.startDrag(clip, new View.DragShadowBuilder(textView), textView, 0); // fixme nayeon - ShadowBuilder
            }

            return true;
        }

        @Override
        public void onLongPress(MotionEvent motionEvent) {
            System.out.println("onLongPress() called");
        }

        @Override
        public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
            System.out.println("onFling() called : "+v+", "+v1);
            return true;
        }
    }
}


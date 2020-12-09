package com.hansung.drawingtogether.view.drawing;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hansung.drawingtogether.R;
import com.hansung.drawingtogether.data.remote.model.MyLog;
import com.hansung.drawingtogether.view.main.MainActivity;

public class TextEditingLayout extends FrameLayout {

    private Button doneBtn;

    public TextEditingLayout(@NonNull Context context) {
        super(context);
        init(context);
    }

    public TextEditingLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public TextEditingLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context){
        LayoutInflater inflater =(LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.layout_text_editing, this, true);
    }

    public void initialize(final DrawingViewModel drawingViewModel) {
        this.doneBtn = this.findViewById(R.id.doneBtn);
        doneBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                MyLog.d("button", "done button click");

                // 현재 편집중인 텍스트가 새로 생성하는 텍스트가 아니라, 생성된 후 편집하는 텍스트인 경우 done 버튼 클릭 가능 (username == null 로 세팅하기 위해)
                // 텍스트를 새로 생성하는 경우에 아직 다른 참가자들에게 텍스트 정보가 없기 때문에, 중간 참여자 접속을 기다린 후 생성 가능하도록 처리
                if(DrawingEditor.INSTANCE.isMidEntered()
                        && DrawingEditor.INSTANCE.getCurrentText() != null && !DrawingEditor.INSTANCE.getCurrentText().getTextAttribute().isTextInited()) { // 텍스트 중간자 처리
                    drawingViewModel.showToastMsg("다른 참가자가 접속 중 입니다. 잠시만 기다려주세요.");
                    return;
                }

                /* 텍스트 모드가 끝나면 다른 버튼들 활성화 */
                //        enableDrawingMenuButton(true);
                //        changeClickedButtonBackground(preMenuButton); // 텍스트 편집 후 기본 모드인 드로잉 - 펜 버튼 눌림 표시

                Text text = DrawingEditor.INSTANCE.getCurrentText();
                text.changeEditTextToTextView();

                drawingViewModel.changeClickedButtonBackground(drawingViewModel.getPreMenuButton()); // 텍스트 편집 후 기본 모드인 드로잉 - 펜 버튼 눌림 표시

//        if(preMenuButton.equals(de.getDrawingFragment().getBinding().drawBtn)) // Draw Btn 인 경우에만 펜 종류 표시
                if(drawingViewModel.getPreMenuButton() == DrawingEditor.INSTANCE.getDrawingFragment().getBinding().drawBtn) // Draw Btn 인 경우에만 펜 종류 표시

                    DrawingEditor.INSTANCE.getDrawingFragment().getBinding().penModeLayout.setVisibility(View.VISIBLE); // 펜 종류 보이도록



                ((MainActivity)DrawingEditor.INSTANCE.getDrawingFragment().getActivity()).setVisibilityToolbarMenus(true);
            }
        });


    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Log.e("text", "on touch");
        return false; // 텍스트 편집 창에서 와핑 막기
    }
}

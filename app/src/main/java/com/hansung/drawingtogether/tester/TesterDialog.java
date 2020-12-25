package com.hansung.drawingtogether.tester;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.hansung.drawingtogether.R;
import com.hansung.drawingtogether.view.drawing.DrawingEditor;

public class TesterDialog extends Dialog {

    private Button okBtn;
    private Button cancelBtn;

    private EditText backgroundEditText;
    private EditText bSegmentEditText;
    private EditText sendingEditText;
    private EditText sSegmentEditText;
    private EditText countEditText;

    private DrawingTester dt = DrawingTester.getInstance();

    public TesterDialog(@NonNull Context context) {
        super(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_test_parameter);

        /* 다이얼로그 컴포넌트 초기화 */
        okBtn = findViewById(R.id.test_ok_button);
        cancelBtn = findViewById(R.id.test_cancel_button);

        backgroundEditText = findViewById(R.id.tester_background);
        bSegmentEditText = findViewById(R.id.tester_background_segment);

        // 다이얼로그 사이즈 조절 하기
        ViewGroup.LayoutParams params = this.getWindow().getAttributes();
        params.width = ViewGroup.LayoutParams.MATCH_PARENT;
        params.height = ViewGroup.LayoutParams.MATCH_PARENT;
        this.getWindow().setAttributes((android.view.WindowManager.LayoutParams) params);


        /* 기존에 지정했던 파라미터 값 표시 */
//        backgroundEditText.setText(dt.getBackground());
//        bSegmentEditText.setText(dt.getBSegment());
//        sendingEditText.setText(dt.getSending());
//        sSegmentEditText.setText(dt.getSSegment());
//        countEditText.setText(dt.getCount());

        /* 버튼 리스너 */
        okBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                int background = Integer.parseInt(backgroundEditText.getText().toString());
                int bSegment = Integer.parseInt(bSegmentEditText.getText().toString());

                if(!checkValidation(background, bSegment)) {
                    Toast.makeText(getContext(), R.string.test_parameter_validation_text, Toast.LENGTH_SHORT).show();
                    return;
                }

                DrawingTester.getInstance().set(background, bSegment); // 테스트 파라미터 설정
                DrawingEditor.getInstance().getDrawingFragment().getBinding().testEnvButton.setEnabled(true); // 테스트 환경 구축 버튼 활성화

                dismiss();

            }
        });

        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });
    }


    /* 입력 값 유효성 검사 */
    public boolean checkValidation(int background, int bSegment) {
        return (background >= 0 && bSegment >= 0);
    }
}

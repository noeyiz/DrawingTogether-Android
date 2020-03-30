package com.hansung.drawingtogether.view.drawing;

import android.graphics.Color;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;

import com.hansung.drawingtogether.databinding.FragmentDrawingBinding;

public enum AttributeManager {
    INSTANCE;

    private FragmentDrawingBinding binding;
    private DrawingEditor de = DrawingEditor.getInstance();

    private View.OnClickListener colorButtonClickListener;
    private SeekBar.OnSeekBarChangeListener sizeBarChangeListener;

    // DECLARE LISTENER
    public void setListener() {

        colorButtonClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String hexColor = ((Button)view).getText().toString();
                int color = Color.parseColor(hexColor);

                switch (de.getCurrentMode()) {
                    case DRAW:
                        de.setStrokeColor(color);
                        de.setFillColor(color);
                        break;
                    case TEXT:
                        if(de.getCurrentText() != null) { // 현재 선택 된 텍스트 색상 편집
                            Text text = de.getCurrentText();
                            text.getTextAttribute().setTextColor(color);
                            text.setTextViewAttribute();
                            text.setEditTextAttribute();
                        }
                        break;
                }
                showCurrentColor(Color.parseColor(hexColor));
            }
        };

        sizeBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
            int stepOfProgress;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                stepOfProgress = progress / 10 * 10;

                switch(de.getCurrentMode()) {
                    case TEXT:
                        Text text = de.getCurrentText();

                        text.getTextAttribute().setTextSize(stepOfProgress);
                        text.setEditTextAttribute(); // edit text 에 텍스트 크기 적용
                        break;
                    /*case DRAW:
                        de.setStrokeWidth(stepOfProgress);
                        break;*/ // todo nayeon - 펜 굵기
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        };

        setPaletteButtonListener();
        binding.sizeBar.setOnSeekBarChangeListener(sizeBarChangeListener);
    }


    public void showCurrentColor(int color) {
        binding.currentColorBtn.setBackgroundColor(color);
    }

    public void setPaletteButtonListener() {
        LinearLayout colorPaletteLayout =  binding.colorLayout;
        int paletteBtnCount = colorPaletteLayout.getChildCount();

        for(int i=0; i < paletteBtnCount; i++) {
            colorPaletteLayout.getChildAt(i).setOnClickListener(colorButtonClickListener);
        }
    }


    public static AttributeManager getInstance() { return INSTANCE; }

    public void setBinding(FragmentDrawingBinding binding) { this.binding = binding; }
}

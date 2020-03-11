package com.hansung.drawingtogether.view.drawing;

import android.graphics.Color;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.hansung.drawingtogether.databinding.FragmentDrawingBinding;

public enum PaletteManager {
    INSTANCE;

    private FragmentDrawingBinding binding;
    private DrawingEditor de = DrawingEditor.getInstance();

    private View.OnClickListener colorButtonClickListener;

    public void setPaletteButtonListener() {
        LinearLayout colorPaletteLayout =  binding.colorLayout;
        int paletteBtnCount = colorPaletteLayout.getChildCount();

        for(int i=0; i < paletteBtnCount; i++) {
            colorPaletteLayout.getChildAt(i).setOnClickListener(colorButtonClickListener);
        }
    }


    // Declare OnClickListener
    public void setListener() {
        colorButtonClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String hexColor = ((Button)view).getText().toString();

                de.setStrokeColor(Color.parseColor(hexColor));
                de.setFillColor(Color.parseColor(hexColor));

                showCurrentColor(Color.parseColor(hexColor));
            }
        };
    }

    public void showCurrentColor(int color) {
        binding.currentColorBtn.setBackgroundColor(color);
    }

    public static PaletteManager getInstance() { return INSTANCE; }

    public void setBinding(FragmentDrawingBinding binding) { this.binding = binding; }
}

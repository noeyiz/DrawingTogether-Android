package com.hansung.drawingtogether.view.drawing;

import android.graphics.Color;
import android.view.View;
import android.widget.Button;

import com.hansung.drawingtogether.databinding.FragmentDrawingBinding;

public enum ColorButtonClickListener implements View.OnClickListener {
    INSTANCE;

    private DrawingEditor de = DrawingEditor.getInstance();
    protected FragmentDrawingBinding binding;

    @Override
    public void onClick(View view) {
        String hexColor = ((Button)view).getText().toString();

        de.setStrokeColor(Color.parseColor(hexColor));
        de.setFillColor(Color.parseColor(hexColor));

        binding.currentColorBtn.setBackgroundColor(Color.parseColor(hexColor));
    }

    public static ColorButtonClickListener getInstance() { return INSTANCE; }

}

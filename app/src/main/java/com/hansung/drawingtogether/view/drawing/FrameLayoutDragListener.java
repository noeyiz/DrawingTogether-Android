package com.hansung.drawingtogether.view.drawing;

import android.graphics.Color;
import android.view.DragEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

// todo nayeon - 프레임레이아웃에 붙어있는 텍스트뷰를 드래깅 하기 위한 이벤트 리스너 클래스
class FrameLayoutDragListener implements View.OnDragListener {

    private DrawingEditor de = DrawingEditor.getInstance();

    private int preX;
    private int preY;

    @Override
    public boolean onDrag(View view, DragEvent event) {
        TextView textView;
        ViewGroup viewGroup;

        View draggedView = (View) event.getLocalState();
        if(draggedView instanceof TextView) { textView = (TextView) draggedView; }
        else { return true; }

        Text text = de.getCurrentText();
        TextAttribute textAttribute = text.getTextAttribute();

        int x = (int)event.getX() - (textView.getWidth()/2);
        int y = (int)event.getY() - (textView.getHeight()/2);

        switch (event.getAction()) {
            case DragEvent.ACTION_DRAG_STARTED:

                textAttribute.setCoordinates(x, y);
                text.sendMqttMessage(TextMode.DRAG_STARTED);
                break;
            case DragEvent.ACTION_DRAG_ENTERED:
            case DragEvent.ACTION_DRAG_LOCATION:
                textAttribute.setCoordinates(x, y);
                text.sendMqttMessage(TextMode.DRAG_LOCATION);
                break;
            case DragEvent.ACTION_DROP:
                viewGroup = (ViewGroup) textView.getParent(); // ViewGroup = FrameLayout
                viewGroup.removeView(textView);

                textAttribute.setCoordinates(x, y); // TextAttribute 에 좌푯값 저장
                text.setTextViewLocation();
                viewGroup.addView(textView);

                text.sendMqttMessage(TextMode.DROP);
                break;
            case DragEvent.ACTION_DRAG_ENDED:
                textView.setVisibility(View.VISIBLE);

                de.getCurrentText().setDragging(false);
                de.setCurrentText(null);
                textAttribute.setUsername(null);

                text.sendMqttMessage(TextMode.DRAG_ENDED);

                de.setCurrentMode(Mode.DRAW);
                textView.setBackgroundColor(Color.TRANSPARENT); // todo nayeon

                break;
            case DragEvent.ACTION_DRAG_EXITED:
                viewGroup = (ViewGroup) textView.getParent(); // ViewGroup = FrameLayout
                viewGroup.removeView(textView);

                textAttribute.setCoordinates(preX, preY); // TextAttribute 에 좌푯값 저장
                text.setTextViewLocation();
                viewGroup.addView(textView);

                textView.setVisibility(View.VISIBLE);

                text.sendMqttMessage(TextMode.DRAG_EXITED);

                break;
        }
        preX = x;
        preY = y;
        return true;
    }
}
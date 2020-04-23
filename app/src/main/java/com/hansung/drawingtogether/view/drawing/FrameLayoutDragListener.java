package com.hansung.drawingtogether.view.drawing;

import android.view.DragEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.hansung.drawingtogether.data.remote.model.MyLog;


// 프레임레이아웃에 붙어있는 텍스트뷰를 드래깅 하기 위한 이벤트 리스너 클래스
class FrameLayoutDragListener implements View.OnDragListener {

    private DrawingEditor de = DrawingEditor.getInstance();

    private int preX;
    private int preY;

    @Override
    public boolean onDrag(View view, DragEvent event) {
        TextView textView;
        ViewGroup viewGroup;

        MyLog.e("Drag Event", Integer.toString(event.getAction()));

        View draggedView = (View) event.getLocalState();
        if(draggedView instanceof TextView) {
            textView = (TextView) draggedView;
            viewGroup = (ViewGroup) textView.getParent();
        }
        else { return true; }

        Text text = de.getCurrentText();
        TextAttribute textAttribute = text.getTextAttribute();

        int x = (int)event.getX();
        int y = (int)event.getY();

        // todo nayeon 텍스트 뷰 화면 넘어갈 때 처리
        if( (x + (textView.getWidth()/2)) > viewGroup.getWidth() ) { // 좌측으로 넘어갈 경우
            x = viewGroup.getWidth() - textView.getWidth()/2;
        }
        else if( (x - (textView.getWidth()/2)) < 0 ) {
            x = 0 + textView.getWidth()/2; // 우측으로 넘어갈 경우
        }


        switch (event.getAction()) {
            case DragEvent.ACTION_DRAG_STARTED:
                textAttribute.setTextMoved(true);

                textAttribute.setPreCoordinates(x, y);
                textAttribute.setCoordinates(x, y);
                text.sendMqttMessage(TextMode.DRAG_STARTED);
                break;
            case DragEvent.ACTION_DRAG_ENTERED:
                break; // DRAG_ENTERED 이벤트 발생 시 (x, y) = (0, 0)
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
                MyLog.i("drawing", "text drop");
                de.addHistory(new DrawingItem(TextMode.DROP, textAttribute)); //fixme minj - addHistory
                MyLog.i("drawing", "history.size()=" + de.getHistory().size());
                de.clearUndoArray();

                break;
            case DragEvent.ACTION_DRAG_ENDED:
                textView.setVisibility(View.VISIBLE);

                de.getCurrentText().setDragging(false);
                de.setCurrentText(null);
                textAttribute.setUsername(null);

                text.sendMqttMessage(TextMode.DRAG_ENDED);

                de.setCurrentMode(Mode.DRAW);
                textView.setBackground(null); // todo nayeon

                break;
            case DragEvent.ACTION_DRAG_EXITED:
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
package com.hansung.drawingtogether.view.drawing;

import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;

import com.hansung.drawingtogether.R;
import com.hansung.drawingtogether.view.BaseViewModel;
import com.hansung.drawingtogether.view.SingleLiveEvent;

public class DrawingViewModel extends BaseViewModel {
    public final SingleLiveEvent<DrawingCommand> drawingCommands = new SingleLiveEvent<>();

    public void clickPen(View view) {
        drawingCommands.postValue(new DrawingCommand.PenMode(view));
    }

    public void clickEraser(View view) {
        drawingCommands.postValue(new DrawingCommand.EraserMode(view));
    }

    public void clickText(View view) {
        drawingCommands.postValue(new DrawingCommand.TextMode(view));
    }

    public void clickShape(View view) {
        drawingCommands.postValue(new DrawingCommand.ShapeMode(view));
    }

    public void clickSearch(View view) {
        navigate(R.id.action_drawingFragment_to_searchFragment);
    }

}

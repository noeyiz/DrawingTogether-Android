package com.hansung.drawingtogether.view.drawing;

import android.os.AsyncTask;

import com.hansung.drawingtogether.data.remote.model.MyLog;


public class UpdateTextsTask extends AsyncTask<DrawingItem, Void, DrawingItem> {
    private DrawingEditor de = DrawingEditor.getInstance();
    private boolean isUndo;

    public UpdateTextsTask(boolean isUndo) {
        this.isUndo = isUndo;
    }

    @Override
    protected DrawingItem doInBackground(DrawingItem... drawingItems) {
        DrawingItem lastItem = drawingItems[0];
        if (lastItem.getTextAttribute() == null)
            return null;

        MyLog.i("drawing", "text mode = " + lastItem.getTextMode().toString());

        return lastItem;
    }

    @Override
    protected void onPostExecute(DrawingItem lastItem) {
        super.onPostExecute(lastItem);

        TextAttribute textAttr = lastItem.getTextAttribute();
        Text text = de.findTextById(textAttr.getId());

        /*
        switch(lastItem.getTextMode()) {
            case CREATE:
            case ERASE:
                if (text != null && de.getTexts().contains(text)) {              //erase
                    text.setTextAttribute(textAttr);
                    text.removeTextViewToFrameLayout();
                    de.removeTexts(text);
                    MyLog.i("drawing", "texts erase");
                } else {
                    Text newText = new Text(de.getDrawingFragment(), textAttr); //create
                    newText.getTextAttribute().setTextInited(true);
                    newText.setTextViewProperties();
                    newText.addTextViewToFrameLayout();
                    de.addTexts(newText);
                    newText.createGestureDetector();
                    MyLog.i("drawing", "texts create");
                }
                break;

            case MODIFY:
                if (isUndo) {
                    MyLog.i("drawing", "preText=" + textAttr.getPreText() + ", text=" + textAttr.getText());
                    MyLog.i("drawing", "texts modify undo");
                    textAttr.setPostText(textAttr.getText());
                    textAttr.setText(textAttr.getPreText());
                } else {
                    MyLog.i("drawing", "text=" + textAttr.getText() + ", postText=" + textAttr.getPostText());
                    MyLog.i("drawing", "texts modify redo");
                    textAttr.setPreText(textAttr.getText());
                    textAttr.setText(textAttr.getPostText());
                }
                if (text != null)
                    text.modifyTextViewContent(textAttr.getText());

                break;

            case DROP:
                if (isUndo) {
                    MyLog.i("drawing", "preX=" + textAttr.getPreX() + ", x=" + textAttr.getX());
                    MyLog.i("drawing", "texts drop undo");
                    textAttr.setPostX(textAttr.getX());
                    textAttr.setPostY(textAttr.getY());
                    textAttr.setX(textAttr.getPreX());
                    textAttr.setY(textAttr.getPreY());
                } else {
                    MyLog.i("drawing", "x=" + textAttr.getX() + ", postX=" + textAttr.getPostX());
                    MyLog.i("drawing", "texts drop redo");
                    textAttr.setPreX(textAttr.getX());
                    textAttr.setPreY(textAttr.getY());
                    textAttr.setX(textAttr.getPostX());
                    textAttr.setY(textAttr.getPostY());
                }
                if (text != null)
                    text.setTextViewLocation(textAttr.getX(), textAttr.getY());

                break;
        }
         */
    }
}
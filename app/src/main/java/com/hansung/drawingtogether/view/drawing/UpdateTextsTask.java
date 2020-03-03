package com.hansung.drawingtogether.view.drawing;

import android.os.AsyncTask;
import android.util.Log;

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

        Log.i("drawing", "text mode = " + lastItem.getTextMode().toString());

        return lastItem;
    }

    @Override
    protected void onPostExecute(DrawingItem lastItem) {
        super.onPostExecute(lastItem);

        TextAttribute textAttr = lastItem.getTextAttribute();
        Text text = de.findTextById(textAttr.getId());

        switch(lastItem.getTextMode()) {
            case CREATE:
            case ERASE:
                if (text != null && de.getTexts().contains(text)) {              //erase
                    text.setTextAttribute(textAttr);
                    text.removeTextViewToFrameLayout();
                    de.removeTexts(text);
                    Log.i("drawing", "texts erase");
                } else {
                    Text newText = new Text(de.getDrawingFragment(), textAttr); //create
                    newText.getTextAttribute().setTextInited(true);
                    newText.addTextViewToFrameLayout();
                    de.addTexts(newText);
                    newText.createGestureDetecter();
                    Log.i("drawing", "texts create");
                }
                break;

            case MODIFY:
                if (isUndo) {
                    Log.i("drawing", "preText=" + textAttr.getPreText() + ", text=" + textAttr.getText());
                    Log.i("drawing", "texts modify undo");
                    textAttr.setPostText(textAttr.getText());
                    textAttr.setText(textAttr.getPreText());
                } else {
                    Log.i("drawing", "text=" + textAttr.getText() + ", postText=" + textAttr.getPostText());
                    Log.i("drawing", "texts modify redo");
                    textAttr.setPreText(textAttr.getText());
                    textAttr.setText(textAttr.getPostText());
                }
                if (text != null)
                    text.modifyTextViewContent(textAttr.getText());

                break;

            case DROP:
                if (isUndo) {
                    Log.i("drawing", "preX=" + textAttr.getPreX() + ", x=" + textAttr.getX());
                    Log.i("drawing", "texts drop undo");
                    textAttr.setPostX(textAttr.getX());
                    textAttr.setPostY(textAttr.getY());
                    textAttr.setX(textAttr.getPreX());
                    textAttr.setY(textAttr.getPreY());
                } else {
                    Log.i("drawing", "x=" + textAttr.getX() + ", postX=" + textAttr.getPostX());
                    Log.i("drawing", "texts drop redo");
                    textAttr.setPreX(textAttr.getX());
                    textAttr.setPreY(textAttr.getY());
                    textAttr.setX(textAttr.getPostX());
                    textAttr.setY(textAttr.getPostY());
                }
                if (text != null)
                    text.setTextViewLocation(textAttr.getX(), textAttr.getY());

                break;
        }
    }
}
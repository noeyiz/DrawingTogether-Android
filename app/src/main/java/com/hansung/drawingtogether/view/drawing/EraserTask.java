package com.hansung.drawingtogether.view.drawing;

import android.os.AsyncTask;
import android.util.Log;

import java.util.Vector;

public class EraserTask extends AsyncTask<Void, Void, Void> {
    private DrawingEditor de = DrawingEditor.getInstance();
    private Vector<Integer> erasedComponentIds;
    private Vector<DrawingComponent> components;

    public EraserTask(Vector<Integer> erasedComponentIds) {
        //de.setDrawingView(((MainActivity) de.getContext()).getDrawingView());
        this.erasedComponentIds = erasedComponentIds;
        this.components = new Vector<>();
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        de.clearDrawingBitmap();
        //de.redrawErasedDrawingComponent(erasedComponentIds);  //지워진 components 만 xfermode 로 그리기
        //de.getDrawingView().invalidate();

        for(int i=1; i<erasedComponentIds.size(); i++) {
            components.add(de.findDrawingComponentById(erasedComponentIds.get(i)));
        }

        for(int i=1; i<erasedComponentIds.size(); i++) {
            int id = erasedComponentIds.get(i);
            de.removeDrawingComponents(id);
        }

        de.drawAllComponents();
        //de.getDrawingView().invalidate();
    }

    @Override
    protected Void doInBackground(Void...voids) {
        //de.eraseDrawingComponents(erasedComponentIds);
        de.eraseDrawingBoardArray(erasedComponentIds);

        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);

        de.addHistory(new DrawingItem(de.getCurrentMode(), components/*, de.getDrawingBitmap()*/));    //fixme
        Log.i("drawing", "history.size()=" + de.getHistory().size());
        de.setLastDrawingBitmap(de.getDrawingBitmap().copy(de.getDrawingBitmap().getConfig(), true));

        de.clearUndoArray();
        //de.getDrawingView().invalidate();
    }
}

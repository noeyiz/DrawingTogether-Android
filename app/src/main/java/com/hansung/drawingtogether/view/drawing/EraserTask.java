package com.hansung.drawingtogether.view.drawing;

import android.os.AsyncTask;

import com.hansung.drawingtogether.data.remote.model.MQTTClient;
import com.hansung.drawingtogether.data.remote.model.MyLog;

import java.util.Vector;


public class EraserTask extends AsyncTask<Void, Void, Void> {
    private DrawingEditor de = DrawingEditor.getInstance();
    private Vector<Integer> erasedComponentIds;
    private Vector<DrawingComponent> components = new Vector<>();

    private MQTTClient client = MQTTClient.getInstance();

    public EraserTask(Vector<Integer> erasedComponentIds) {
        //de.setDrawingView(((MainActivity) de.getContext()).getDrawingView());
        this.erasedComponentIds = erasedComponentIds;
        this.components.clear();

        de.printCurrentComponents("erase");
        de.printDrawingComponents("erase");
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        de.clearDrawingBitmap();
        //de.redrawErasedDrawingComponent(erasedComponentIds);  //지워진 components 만 xfermode 로 그리기
        //de.getDrawingView().invalidate();

        //for(int i=1; i<erasedComponentIds.size(); i++) {
        for(int i=0; i<erasedComponentIds.size(); i++) {
            try {
                DrawingComponent comp = de.findDrawingComponentById(erasedComponentIds.get(i));
                if((comp != null) && comp.isSelected()) {
                    de.setDrawingComponentSelected(comp.getUsersComponentId(), false);
                    de.clearMyCurrentBitmap();
                    de.getDrawingFragment().getBinding().drawingView.setSelected(false);
                    MyLog.i("isSelected", comp.getUsersComponentId() + ", " + comp.isSelected);
                }
                comp.setSelected(false);
                components.add(comp);

//                if(client.isMaster()) {
//                    switch (comp.getType()) {
//                        case STROKE:
//                            client.getComponentCount().decreaseStroke();
//                            break;
//                        case RECT:
//                            client.getComponentCount().decreaseRect();
//                            break;
//                        case OVAL:
//                            client.getComponentCount().decreaseOval();
//                            break;
//                    }
//                }

            } catch (NullPointerException e) {
                MyLog.w("catch", "EraserTask.setSelected() | NullPointerException");
            }
        }

        //for(int i=1; i<erasedComponentIds.size(); i++) {
        for(int i=0; i<erasedComponentIds.size(); i++) {
            int id = erasedComponentIds.get(i);
            de.removeDrawingComponents(id);
        }

        de.drawAllDrawingComponents();
        //de.drawAllUnselectedDrawingComponents();
        //de.drawAllCurrentStrokes();
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

        de.addHistory(new DrawingItem(Mode.ERASE, (Vector<DrawingComponent>)components.clone()/*, de.getDrawingBitmap()*/));    //fixme
        MyLog.i("drawing", "history.size()=" + de.getHistory().size());

        if(de.getCurrentMode() == Mode.SELECT && de.getDrawingFragment().getBinding().drawingView.isSelected() && de.getSelectedComponent() != null) {
            int id = de.getSelectedComponent().getId();
            DrawingComponent comp = de.findDrawingComponentById(id);
            if((comp != null) && comp.isSelected()) {
                de.setPreSelectedComponents(id);
                de.setPostSelectedComponents(id);

                de.clearMyCurrentBitmap();

                de.setPreSelectedComponentsBitmap();
                de.setPostSelectedComponentsBitmap();

                de.getSelectedComponent().drawComponent(de.getCurrentCanvas());
                de.drawUnselectedComponents();
                de.drawSelectedComponentBorder(de.getSelectedComponent(), de.getMySelectedBorderColor());
            }
        }

        //de.clearUndoArray();
        //de.getDrawingView().invalidate();
    }

    public void doNotInBackground() {
        onPreExecute();
        onPostExecute(null);
        de.eraseDrawingBoardArray(erasedComponentIds);
    }
}

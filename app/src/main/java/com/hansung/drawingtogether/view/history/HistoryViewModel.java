package com.hansung.drawingtogether.view.history;

import android.util.Log;
import android.view.View;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.hansung.drawingtogether.R;
import com.hansung.drawingtogether.view.BaseViewModel;

import java.util.ArrayList;

public class HistoryViewModel extends BaseViewModel {
    private MutableLiveData<ArrayList<HistoryData>> historyData = new MutableLiveData<>();

    public HistoryViewModel() {
        ArrayList<HistoryData> historyData = new ArrayList<>();
        historyData.add(new HistoryData(R.drawable.ex1, "topic1", "time1"));
        historyData.add(new HistoryData(R.drawable.ex2, "topic2", "time2"));
        historyData.add(new HistoryData(R.drawable.ex3, "topic3", "time3"));
        historyData.add(new HistoryData(R.drawable.ex4, "topic4", "time4"));
        historyData.add(new HistoryData(R.drawable.ex5, "topic5", "time5"));
        historyData.add(new HistoryData(R.drawable.ex6, "topic6", "time6"));

        this.historyData.postValue(historyData);
    }

    public LiveData<ArrayList<HistoryData>> getHistoryData() {
        return historyData;
    }

    public void clickDrawing(View view) {
//        navigate(R.id.action_historyFragment_to_drawingFragment);
    }

    public void exit() {
        Log.e("kkankkan", "clicked");
        back();
    }
}

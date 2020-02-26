package com.hansung.drawingtogether.view.search;

import android.util.Log;
import android.widget.GridView;

import androidx.databinding.BindingAdapter;
import androidx.recyclerview.widget.RecyclerView;

public class SearchBindingUtils {
    @BindingAdapter("adapter_grid")
    public static void setAdapter(GridView view, SearchImageAdapter adapter) {
        view.setAdapter(adapter);
    }

    @BindingAdapter("adapter_recycler")
    public static void setAdapter(RecyclerView view, SearchMapAdapter adapter) {
        view.setAdapter(adapter);
    }
}

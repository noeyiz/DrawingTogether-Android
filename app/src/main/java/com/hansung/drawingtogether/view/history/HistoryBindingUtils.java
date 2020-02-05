package com.hansung.drawingtogether.view.history;

import android.widget.GridView;
import android.widget.ImageView;

import androidx.databinding.BindingAdapter;

public class HistoryBindingUtils {
    @BindingAdapter("adapter")
    public static void setAdapter(GridView view, HistoryAdapter adapter) {
        view.setAdapter(adapter);
    }

    @BindingAdapter("imageSrc")
    public static void setImageSrc(ImageView view, int resource) {
        view.setImageResource(resource);
    }
}

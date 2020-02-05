package com.hansung.drawingtogether.data.remote.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class MetaInfo {
    @SerializedName("total_count")
    @Expose
    public int total_count;

    @SerializedName("pageable_count")
    @Expose
    public int pageable_count;

    @SerializedName("is_end")
    @Expose
    public boolean is_end;
}

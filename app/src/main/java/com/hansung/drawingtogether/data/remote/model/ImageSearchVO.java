package com.hansung.drawingtogether.data.remote.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ImageSearchVO {

    @SerializedName("thumbnail_url")
    @Expose
    public String thumbnail_url;

    @SerializedName("image_url")
    @Expose
    public String image_url;
}

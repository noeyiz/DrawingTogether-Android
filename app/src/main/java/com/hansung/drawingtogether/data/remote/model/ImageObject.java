package com.hansung.drawingtogether.data.remote.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

public class ImageObject {

    public MetaInfo metaInfo;
    public ArrayList<ImageDocument> documents;


    public class ImageDocument{

        @SerializedName("thumbnail_url")
        @Expose
        public String thumbnail_url;

        @SerializedName("image_url")
        @Expose
        public String image_url;

        @SerializedName("width")
        @Expose
        public int width;

        @SerializedName("height")
        @Expose
        public int height;

        @SerializedName("display_sitemname")
        @Expose
        public String display_sitemname;

        @SerializedName("doc_url")
        @Expose
        public String doc_url;

    }
}

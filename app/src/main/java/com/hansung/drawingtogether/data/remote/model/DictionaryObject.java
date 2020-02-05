package com.hansung.drawingtogether.data.remote.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

public class DictionaryObject {

    public MetaInfo metaInfo;
    public ArrayList<DictionaryDocument> documents;


    public class DictionaryDocument{
        @Expose
        @SerializedName("datetime")
        public String datetime;

        @Expose
        @SerializedName("contents")
        public String contents;

        @Expose
        @SerializedName("url")
        public String url;

        @Expose
        @SerializedName("title")
        public String title;


    }
}



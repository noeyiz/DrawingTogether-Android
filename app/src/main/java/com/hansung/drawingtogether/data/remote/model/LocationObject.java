package com.hansung.drawingtogether.data.remote.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

public class LocationObject {

    public MetaInfo metaInfo;
    public ArrayList<LocationDocument> documents;

    public class LocationDocument{

        @SerializedName("id")
        @Expose
        public String id;

        @SerializedName("place_name")
        @Expose
        public String place_name;

        @SerializedName("category_name")
        @Expose
        public String category_name;

        @SerializedName("category_group_code")
        @Expose
        public String category_group_code;

        @SerializedName("category_group_name")
        @Expose
        public String category_group_name;

        @SerializedName("phone")
        @Expose
        public String phone;

        @SerializedName("address_name")
        @Expose
        public String address_name;

        @SerializedName("road_address_name")
        @Expose
        public String road_address_name;

        @SerializedName("x")
        @Expose
        public String x;

        @SerializedName("y")
        @Expose
        public String y;

        @SerializedName("place_url")
        @Expose
        public String place_url;

        @SerializedName("distance")
        @Expose
        public String distance;

    }
}

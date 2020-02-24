package com.hansung.drawingtogether.data.remote.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class LocationVO {
    @SerializedName("id")
    @Expose
    public String id;

    @SerializedName("place_name")
    @Expose
    public String place_name;

    @SerializedName("road_address_name")
    @Expose
    public String road_address_name;

    @SerializedName("x")
    @Expose
    public String x;

    @SerializedName("y")
    @Expose
    public String y;
}

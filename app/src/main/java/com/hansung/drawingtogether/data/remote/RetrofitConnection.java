package com.hansung.drawingtogether.data.remote;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitConnection{

    private String URL;
    public RetrofitAPI retrofitAPI;

    public RetrofitConnection(String category) {
        if (category.equals("map")) {
            URL = "m.map.kakao.com/actions/";
        } else
            URL = "https://dapi.kakao.com/";

        Retrofit retrofit = new Retrofit.Builder().baseUrl(URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        retrofitAPI = retrofit.create(RetrofitAPI.class);
    }

}

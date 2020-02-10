package com.hansung.drawingtogether.data.remote;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitConnection{

    private String URL = "https://dapi.kakao.com/";
    public RetrofitAPI retrofitAPI;

    public RetrofitConnection() {
        Retrofit retrofit = new Retrofit.Builder().baseUrl(URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        retrofitAPI = retrofit.create(RetrofitAPI.class);
        
    }
}

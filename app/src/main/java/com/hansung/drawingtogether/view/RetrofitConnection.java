package com.hansung.drawingtogether.view;

import com.hansung.drawingtogether.data.remote.model.RetrofitAPI;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitConnection {
    private String URL;
    private Retrofit retrofit;
    private RetrofitAPI retrofitAPI;

    public RetrofitConnection() {
        URL = "https://dapi.kakao.com/";
        retrofit = new Retrofit.Builder().baseUrl(URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        retrofitAPI = retrofit.create(RetrofitAPI.class);
    }

    public RetrofitAPI getRetrofitAPI() {
        return retrofitAPI;
    }
}

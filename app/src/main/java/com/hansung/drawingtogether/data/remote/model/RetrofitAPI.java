package com.hansung.drawingtogether.data.remote.model;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Query;

public interface RetrofitAPI {

    @Headers("Authorization: KakaoAK 718d10252629b164f45374d0a9519575")
    @GET("v2/search/image")
    Call<ImageComponent> getImageDataList(@Query("query") String name,
                                          @Query("page") int page,
                                          @Query("size")int size);

    @Headers("Authorization: KakaoAK 718d10252629b164f45374d0a9519575")
    @GET("v2/local/search/keyword.json")
    Call<LocationComponent> getLocationList(@Query("query") String location);
}

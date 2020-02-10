package com.hansung.drawingtogether.data.remote;

import com.hansung.drawingtogether.data.remote.model.DictionaryObject;
import com.hansung.drawingtogether.data.remote.model.ImageObject;
import com.hansung.drawingtogether.data.remote.model.LocationObject;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Query;

public interface RetrofitAPI {

    @Headers("Authorization: KakaoAK 718d10252629b164f45374d0a9519575")
    @GET("v2/search/image")
    Call<ImageObject> getImageDataList(@Query("query") String name,
                                       @Query("page") int page,
                                       @Query("size")int size);

    @Headers("Authorization: KakaoAK 718d10252629b164f45374d0a9519575")
    @GET("v2/local/search/keyword.json")
    Call<LocationObject> getLocationList(@Query("query") String location);



    @Headers("Authorization: KakaoAK 718d10252629b164f45374d0a9519575")
    @GET("v2/search/web")
    Call<DictionaryObject>getDictionaryList(@Query("query") String keyword,
                                            @Query("page") int page,
                                            @Query("size") int size);

}

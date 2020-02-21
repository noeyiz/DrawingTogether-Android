package com.hansung.drawingtogether.data.remote.model;

import com.hansung.drawingtogether.data.remote.model.ImageComponent;
import com.hansung.drawingtogether.data.remote.model.LocationComponent;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Query;

public interface RetrofitAPI {

@Headers("Authorization: KakaoAK 3c0c1584c7feb57194f22c4b1fcabd78")
@GET("v2/search/image")
Call<ImageComponent> getImageDataList(@Query("query") String name,
                                      @Query("page") int page,
                                      @Query("size") int size);

@Headers("Authorization: KakaoAK 3c0c1584c7feb57194f22c4b1fcabd78")
@GET("v2/local/search/keyword.json")
Call<LocationComponent> getLocationList(@Query("query") String location);

//@Headers("Authorization: KakaoAK 3c0c1584c7feb57194f22c4b1fcabd78")
//@GET("v2/search/web")
//Call<DictionaryComponent>getDictionaryList(@Query("query") String keyword,
//                                           @Query("page") int page,
//                                           @Query("size") int size);
}

package com.hansung.drawingtogether.view.search;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.hansung.drawingtogether.data.remote.RetrofitConnection;
import com.hansung.drawingtogether.data.remote.model.ImageObject;
import com.hansung.drawingtogether.databinding.FragmentSearchImageBinding;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchImageFragment extends Fragment {

    private String keyword;
    private int page = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        FragmentSearchImageBinding binding = FragmentSearchImageBinding.inflate(inflater, container, false);

        keyword = getArguments().getString("data");
        binding.setData(keyword);

        return binding.getRoot();
    }


    private void callImageDataList(String text){

        RetrofitConnection retrofitConnection = new RetrofitConnection("image");
        Call<ImageObject> imageComponentCall = retrofitConnection.retrofitAPI.getImageDataList(text, ++page, 30);
        imageComponentCall.enqueue(new Callback<ImageObject>() {
            @Override
            public void onResponse(Call<ImageObject> call, Response<ImageObject> response) {
                if(response.isSuccessful()){
                    Log.e("success", "response success");
                    ImageObject image = response.body();
                    Log.e("success", response.toString());

//                    imageSearchList = image.documents;

                }
                else{

                    Log.e("fail", response.toString());
                }
            }

            @Override
            public void onFailure(Call<ImageObject> call, Throwable t) {
                t.printStackTrace();
            }
        });

    }
}

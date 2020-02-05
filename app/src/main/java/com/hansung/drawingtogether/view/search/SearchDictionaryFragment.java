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
import com.hansung.drawingtogether.data.remote.model.DictionaryObject;
import com.hansung.drawingtogether.data.remote.model.ImageObject;
import com.hansung.drawingtogether.databinding.FragmentSearchDictionaryBinding;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchDictionaryFragment extends Fragment {

    private String keyword;
    private int page = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        FragmentSearchDictionaryBinding binding = FragmentSearchDictionaryBinding.inflate(inflater, container, false);
        keyword = getArguments().getString("data");
        binding.setData(keyword);
        return binding.getRoot();
    }

    private void callDictionaryList(String text){

        RetrofitConnection retrofitConnection = new RetrofitConnection("image");
        Call<DictionaryObject> dictionaryObjectCall = retrofitConnection.retrofitAPI.getDictionaryList(text, ++page, 30);
        dictionaryObjectCall.enqueue(new Callback<DictionaryObject>() {
            @Override
            public void onResponse(Call<DictionaryObject> call, Response<DictionaryObject> response) {
                if(response.isSuccessful()){
                    Log.e("success", "response success");
                    DictionaryObject dictionary = response.body();
                    if(dictionary.metaInfo.is_end){
                        return;
                    }
                    Log.e("success", response.toString());

//                    imageSearchList = image.documents;

                }
                else{

                    Log.e("fail", response.toString());
                }
            }

            @Override
            public void onFailure(Call<DictionaryObject> call, Throwable t) {
                t.printStackTrace();
            }
        });

    }
}

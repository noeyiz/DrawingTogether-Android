package com.hansung.drawingtogether.view.search;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import com.hansung.drawingtogether.data.remote.model.ImageComponent;
import com.hansung.drawingtogether.data.remote.model.ImageSearchVO;
import com.hansung.drawingtogether.databinding.FragmentSearchImageBinding;
import com.hansung.drawingtogether.view.RetrofitConnection;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchImageFragment extends Fragment {

    private SearchViewModel searchViewModel;
    private SearchImageAdapter searchImageAdapter;
    private ArrayList<ImageSearchVO> imageList = new ArrayList<>();
    private String keyword;
    private int page = 1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final FragmentSearchImageBinding binding = FragmentSearchImageBinding.inflate(inflater, container, false);
        searchViewModel = ViewModelProviders.of(this).get(SearchViewModel.class);

        searchImageAdapter = new SearchImageAdapter(getContext());
        searchImageAdapter.setData(imageList);
        binding.setAdapter(searchImageAdapter);

        binding.searchImageNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                page++;
                callImageDataList(keyword);
                binding.searchImageGrid.setSelection(0);
            }
        });

        binding.searchImageBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                page--;
                callImageDataList(keyword);
                binding.searchImageGrid.setSelection(0);
            }
        });

        binding.setVm(searchViewModel);

        keyword = getArguments().getString("keyword");
        callImageDataList(keyword);

        return binding.getRoot();
    }

    //서버 연결하여 json 객체를 받아와 gson으로 parsing
    private void callImageDataList(String keyword){
        if (page == 0) {
            page++;
            return;
        }
        RetrofitConnection retrofitConnection = new RetrofitConnection();
        Call<ImageComponent> imageComponentCall = retrofitConnection.getRetrofitAPI().getImageDataList(keyword, page, 50);
        imageComponentCall.enqueue(new Callback<ImageComponent>() {
            @Override
            public void onResponse(Call<ImageComponent> call, Response<ImageComponent> response) {
                if(response.isSuccessful()){
                    ImageComponent image = response.body();

                    imageList = image.documents;
                    searchImageAdapter.setData(imageList);
                }
                else{
                    Log.e("fail", response.toString());
                }
            }

            @Override
            public void onFailure(Call<ImageComponent> call, Throwable t) {
                t.printStackTrace();
            }
        });
    }
}

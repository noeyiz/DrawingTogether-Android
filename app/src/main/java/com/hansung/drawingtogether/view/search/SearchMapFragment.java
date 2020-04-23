package com.hansung.drawingtogether.view.search;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.hansung.drawingtogether.data.remote.model.LocationComponent;
import com.hansung.drawingtogether.data.remote.model.LocationVO;
import com.hansung.drawingtogether.data.remote.model.MyLog;
import com.hansung.drawingtogether.databinding.FragmentSearchMapBinding;
import com.hansung.drawingtogether.view.RetrofitConnection;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchMapFragment extends Fragment {

    private SearchViewModel searchViewModel;
    private SearchMapAdapter searchMapAdapter;
    private ArrayList<LocationVO> locationList = new ArrayList<>();
    private String keyword;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final FragmentSearchMapBinding binding = FragmentSearchMapBinding.inflate(inflater, container, false);
        searchViewModel = ViewModelProviders.of(this).get(SearchViewModel.class);

        searchMapAdapter = new SearchMapAdapter(getContext(), searchViewModel);
        searchMapAdapter.setData(locationList);
        binding.setAdapter(searchMapAdapter);
        binding.searchMapRecycler.setLayoutManager(new LinearLayoutManager(getContext()));

        binding.setVm(searchViewModel);

        keyword = getArguments().getString("keyword");
        callLocationList(keyword);

        return binding.getRoot();
    }

    //서버 연결하여 json 객체를 받아와 gson으로 parsing
    private void callLocationList(String keyword){
        RetrofitConnection connection = new RetrofitConnection();
        Call<LocationComponent> locationComponentCall = connection.getRetrofitAPI().getLocationList(keyword);
        locationComponentCall.enqueue(new Callback<LocationComponent>() {
            @Override
            public void onResponse(Call<LocationComponent> call, Response<LocationComponent> response) {
                if(response.isSuccessful()){
                    LocationComponent locationComponent = response.body();

                    locationList = locationComponent.documents;
                    searchMapAdapter.setData(locationList);
                }
                else{
                    MyLog.e("fail", response.toString());
                }
            }

            @Override
            public void onFailure(Call<LocationComponent> call, Throwable t) {
                t.printStackTrace();
            }
        });
    }
}

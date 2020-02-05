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
import com.hansung.drawingtogether.data.remote.model.LocationObject;
import com.hansung.drawingtogether.databinding.FragmentSearchImageBinding;
import com.hansung.drawingtogether.databinding.FragmentSearchMapBinding;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchMapFragment extends Fragment {

    private String location;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        super.onCreateView(inflater, container, savedInstanceState);
        FragmentSearchMapBinding binding = FragmentSearchMapBinding.inflate(inflater, container, false);
        location = getArguments().getString("data");
        binding.setData(location);

        return binding.getRoot();
    }

    private void callLocationList(String text){
        RetrofitConnection connection = new RetrofitConnection("map");
        Call<LocationObject> locationComponentCall = connection.retrofitAPI.getLocationList(text);
        locationComponentCall.enqueue(new Callback<LocationObject>() {
            @Override
            public void onResponse(Call<LocationObject> call, Response<LocationObject> response) {
                if(response.isSuccessful()){
                    Log.e("success", "response success");
                    Log.e("success", call.request().toString());
                    LocationObject locationComponent = response.body();
//                    locationVOArrayList = locationComponent.documents;
//                    Log.e("success", Integer.toString(locationVOArrayList.size()));

                }
                else{
                    Log.e("fail", response.toString());
                }
            }

            @Override
            public void onFailure(Call<LocationObject> call, Throwable t) {
                t.printStackTrace();
            }
        });

    }
}

package com.hansung.drawingtogether.view.search;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.ObservableField;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.MutableLiveData;

import com.hansung.drawingtogether.R;
import com.hansung.drawingtogether.data.remote.model.ImageComponent;
import com.hansung.drawingtogether.data.remote.model.ImageSearchVO;
import com.hansung.drawingtogether.view.BaseViewModel;
import com.hansung.drawingtogether.view.RetrofitConnection;


import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchViewModel extends BaseViewModel {
    private ObservableField<String> keyword;

    public SearchViewModel() {
        this.keyword = new ObservableField<>();
    }

    public void clickSearch(View view) {
        Bundle bundle = new Bundle();
        bundle.putString("keyword", keyword.get());
        callFragment(view.getContext(), new SearchImageFragment(), bundle);
    }

    public void clickImage(View view) {
        Bundle bundle = new Bundle();
        bundle.putString("keyword", keyword.get());
        callFragment(view.getContext(), new SearchImageFragment(), bundle);
    }

    public void clickMap(View view) {
        Bundle bundle = new Bundle();
        bundle.putString("keyword", keyword.get());
        callFragment(view.getContext(), new SearchMapFragment(), bundle);
    }

    public void callFragment(Context context, Fragment fragment, Bundle bundle) {
        FragmentTransaction transaction = ((AppCompatActivity)context).getSupportFragmentManager().beginTransaction();
        fragment.setArguments(bundle);
        transaction.replace(R.id.search_container, fragment);
        transaction.commit();
    }

    public void exit() {
        back();
    }

    public ObservableField<String> getKeyword() {
        return keyword;
    }
}

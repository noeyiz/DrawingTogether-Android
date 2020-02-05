package com.hansung.drawingtogether.view.search;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.hansung.drawingtogether.databinding.FragmentSearchImageBinding;

public class SearchImageFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        FragmentSearchImageBinding binding = FragmentSearchImageBinding.inflate(inflater, container, false);

        String data = getArguments().getString("data");
        binding.setData(data);

        return binding.getRoot();
    }

}

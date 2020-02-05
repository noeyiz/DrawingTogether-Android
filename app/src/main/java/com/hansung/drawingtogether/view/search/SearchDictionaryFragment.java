package com.hansung.drawingtogether.view.search;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.hansung.drawingtogether.databinding.FragmentSearchDictionaryBinding;

public class SearchDictionaryFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        FragmentSearchDictionaryBinding binding = FragmentSearchDictionaryBinding.inflate(inflater, container, false);

        return binding.getRoot();
    }
}

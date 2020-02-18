package com.hansung.drawingtogether.view.search;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.hansung.drawingtogether.R;
import com.hansung.drawingtogether.databinding.FragmentSearchBinding;
import com.hansung.drawingtogether.view.main.MainActivity;

public class SearchFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final FragmentSearchBinding binding = FragmentSearchBinding.inflate(inflater, container, false);

        /*
        !지금은 검색 버튼에 리스너 달아놓지 않은 상태 !

        할 일
        - fragment 디폴트로 이미지 보이게 하기
        - 이미지, 사전, 지도 버튼 눌렀을 때 누른 버튼 표시하기
         */
        binding.searchImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callFragment(new SearchImageFragment(), binding.searchEdit.getText().toString());
            }
        });

        binding.searchDictionary.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                callFragment(new SearchDictionaryFragment());
            }
        });

        binding.searchMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                callFragment(new SearchMapFragment());
            }
        });
        return binding.getRoot();
    }

    private void callFragment(Fragment fragment, String data) {
        FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
        // data 전달
        Bundle bundle = new Bundle();
        bundle.putString("data", data);
        fragment.setArguments(bundle);
        transaction.replace(R.id.search_container, fragment);
        transaction.commit();
    }

    @Override
    public void onResume() {
        super.onResume();
        ((MainActivity)getActivity()).setToolbarTitle("Search");
        ((MainActivity)getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }
}
package com.hansung.drawingtogether.view.search;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.navigation.fragment.NavHostFragment;

import com.hansung.drawingtogether.R;
import com.hansung.drawingtogether.databinding.FragmentSearchBinding;
import com.hansung.drawingtogether.view.NavigationCommand;
import com.hansung.drawingtogether.view.main.MainActivity;

public class SearchFragment extends Fragment {

    private SearchViewModel searchViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final FragmentSearchBinding binding = FragmentSearchBinding.inflate(inflater, container, false);
        searchViewModel = ViewModelProviders.of(this).get(SearchViewModel.class);

        searchViewModel.navigationCommands.observe(this, new Observer<NavigationCommand>() {
            @Override
            public void onChanged(NavigationCommand navigationCommand) {
                if (navigationCommand instanceof  NavigationCommand.Back) {
                    NavHostFragment.findNavController(SearchFragment.this).popBackStack();
                }
            }
        });

        binding.setVm(searchViewModel);

        ((MainActivity)getActivity()).setOnBackListener(new MainActivity.OnBackListener() {
            @Override
            public void onBack() {
                searchViewModel.exit();
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
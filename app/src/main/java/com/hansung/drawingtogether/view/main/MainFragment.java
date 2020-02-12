package com.hansung.drawingtogether.view.main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.navigation.fragment.NavHostFragment;

import com.hansung.drawingtogether.databinding.FragmentMainBinding;
import com.hansung.drawingtogether.view.NavigationCommand;

public class MainFragment extends Fragment {

    private MainViewModel mainViewModel;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        FragmentMainBinding binding = FragmentMainBinding.inflate(inflater, container, false);

        mainViewModel = ViewModelProviders.of(this).get(MainViewModel.class);
        mainViewModel.navigationCommands.observe(this, new Observer<NavigationCommand>() {
            @Override
            public void onChanged(NavigationCommand navigationCommand) {
                if (navigationCommand instanceof NavigationCommand.To) {
                    NavHostFragment.findNavController(MainFragment.this)
                            .navigate(((NavigationCommand.To) navigationCommand).getDestinationId());
                }
            }
        });
        binding.setVm(mainViewModel);

        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        ((MainActivity)getActivity()).setTitleBar("Drawing Together");
    }
}

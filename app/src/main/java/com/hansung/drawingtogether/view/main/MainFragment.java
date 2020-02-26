package com.hansung.drawingtogether.view.main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
                else if (navigationCommand instanceof NavigationCommand.ToBundle) {
                    NavigationCommand.ToBundle toBundle = (NavigationCommand.ToBundle) navigationCommand;
                    NavHostFragment.findNavController(MainFragment.this)
                            .navigate(toBundle.getDestinationId(), toBundle.getBundle());
                }
            }
        });
        binding.setVm(mainViewModel);
        binding.setLifecycleOwner(this);

        if (!((MainActivity)getActivity()).getTopicPassword().equals("")) {
            String topic = ((MainActivity)getActivity()).getTopicPassword().split("/")[0];
            String password = ((MainActivity)getActivity()).getTopicPassword().split("/")[1];

            binding.getVm().getTopic().postValue(topic);
            binding.getVm().getPassword().postValue(password);

            ((MainActivity) getActivity()).setTopicPassword("");
        }

        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        ((MainActivity)getActivity()).setToolbarTitle("Drawing Together");
        ((MainActivity)getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(false);
    }
}

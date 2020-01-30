package com.hansung.drawingtogether.view.topic;

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

import com.hansung.drawingtogether.view.NavigationCommand;
import com.hansung.drawingtogether.databinding.FragmentTopicBinding;


public class TopicFragment extends Fragment {
    private TopicViewModel topicViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        FragmentTopicBinding binding = FragmentTopicBinding.inflate(inflater, container, false);
        topicViewModel = ViewModelProviders.of(this).get(TopicViewModel.class);
        topicViewModel.navigationCommands.observe(this, new Observer<NavigationCommand>() {
            @Override
            public void onChanged(NavigationCommand navigationCommand) {
                if (navigationCommand instanceof NavigationCommand.To) {
                    NavHostFragment.findNavController(TopicFragment.this)
                            .navigate(((NavigationCommand.To) navigationCommand).getDestinationId());
                }
            }
        });
        binding.setVm(topicViewModel);
        return binding.getRoot();
    }
}

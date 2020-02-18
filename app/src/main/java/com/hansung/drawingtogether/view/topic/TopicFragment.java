package com.hansung.drawingtogether.view.topic;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
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
import com.hansung.drawingtogether.view.main.MainActivity;

public class TopicFragment extends Fragment {
    private TopicViewModel topicViewModel;
    private FragmentTopicBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentTopicBinding.inflate(inflater, container, false);
        topicViewModel = ViewModelProviders.of(this).get(TopicViewModel.class);
        topicViewModel.navigationCommands.observe(this, new Observer<NavigationCommand>() {
            @Override
            public void onChanged(NavigationCommand navigationCommand) {
                if (navigationCommand instanceof NavigationCommand.To) {
                    NavHostFragment.findNavController(TopicFragment.this)
                            .navigate(((NavigationCommand.To) navigationCommand).getDestinationId());
                }else if(navigationCommand instanceof NavigationCommand.ToBundle) {
                    NavigationCommand.ToBundle command = (NavigationCommand.ToBundle) navigationCommand;
                    NavHostFragment.findNavController(TopicFragment.this).navigate(command.getDestinationId(), command.getBundle());
                }
            }
        });

        binding.setVm(topicViewModel);

        if (!((MainActivity)getActivity()).getTopicPassword().equals("")) {
            String topic = ((MainActivity)getActivity()).getTopicPassword().split("/")[0];
            String password = ((MainActivity)getActivity()).getTopicPassword().split("/")[1];

            binding.getVm().getTopicEditText().setValue(topic);
            binding.getVm().getPasswordEditText().setValue(password);

            ((MainActivity) getActivity()).setTopicPassword("");
        }

        Log.e("kkankkan", "토픽프레그먼트 온크리에이트뷰");

        //binding.setVm(topicViewModel);
        return binding.getRoot();
    }
}

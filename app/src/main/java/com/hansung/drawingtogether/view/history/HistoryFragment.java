package com.hansung.drawingtogether.view.history;

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

import com.hansung.drawingtogether.databinding.FragmentHistoryBinding;
import com.hansung.drawingtogether.view.NavigationCommand;
import com.hansung.drawingtogether.view.main.MainActivity;

import java.util.ArrayList;

public class HistoryFragment extends Fragment {

    private HistoryViewModel historyViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        FragmentHistoryBinding binding = FragmentHistoryBinding.inflate(inflater, container, false);

        historyViewModel = ViewModelProviders.of(this).get(HistoryViewModel.class);
        historyViewModel.navigationCommands.observe(getViewLifecycleOwner(), new Observer<NavigationCommand>() {
            @Override
            public void onChanged(NavigationCommand navigationCommand) {
                if (navigationCommand instanceof NavigationCommand.To) {
                    NavHostFragment.findNavController(HistoryFragment.this)
                            .navigate(((NavigationCommand.To) navigationCommand).getDestinationId());
                }
                if (navigationCommand instanceof  NavigationCommand.Back) {
                    NavHostFragment.findNavController(HistoryFragment.this).popBackStack();
                }
            }
        });

        final HistoryAdapter historyAdapter = new HistoryAdapter(getContext(), historyViewModel);
        binding.setAdapter(historyAdapter);

        historyViewModel.getHistoryData().observe(getViewLifecycleOwner(), new Observer<ArrayList<HistoryData>>() {
            @Override
            public void onChanged(ArrayList<HistoryData> historyData) {
                historyAdapter.setData(historyData);
            }
        });

        ((MainActivity)getActivity()).setOnLeftTopBackListener(new MainActivity.OnLeftTopBackListener() {
            @Override
            public void onLeftTopBackPressed() {
                historyViewModel.exit();
            }
        });

        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        ((MainActivity)getActivity()).setToolbarTitle("History");
        ((MainActivity)getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }
}

package com.hansung.drawingtogether.view.main;

import android.content.Context;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.navigation.fragment.NavHostFragment;

import com.hansung.drawingtogether.data.remote.model.MyLog;
import com.hansung.drawingtogether.databinding.FragmentMainBinding;
import com.hansung.drawingtogether.view.NavigationCommand;


import java.util.Objects;

public class MainFragment extends Fragment {

    private MainViewModel mainViewModel;
    private InputMethodManager inputMethodManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final FragmentMainBinding binding = FragmentMainBinding.inflate(inflater, container, false);

        mainViewModel = ViewModelProviders.of(this).get(MainViewModel.class);

        /*
        inputMethodManager = (InputMethodManager) Objects.requireNonNull(getActivity()).getSystemService(Context.INPUT_METHOD_SERVICE);
        binding.topic.requestFocus();
        binding.name.getEditText().setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if(i == EditorInfo.IME_ACTION_DONE) {
                    //binding.masterLogin.performClick();
                    //binding.join.performClick();
                    inputMethodManager.hideSoftInputFromWindow(binding.name.getWindowToken(), 0);
                }

                return false;
            }
        });
         */

        mainViewModel.navigationCommands.observe(getViewLifecycleOwner(), new Observer<NavigationCommand>() {
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

        /* 카카오링크로 전달 받은 topic, password를 textField에 setting */
        MainActivity mainActivity = (MainActivity)getActivity();
        if ((mainActivity.getTopic() != null && mainActivity.getPassword() != null)) {
            String topic = mainActivity.getTopic();
            String password = mainActivity.getPassword();

            binding.getVm().getTopic().postValue(topic);
            binding.getVm().getPassword().postValue(password);

            mainActivity.setTopic(null);
            mainActivity.setPassword(null);
        }

        ((MainActivity)getActivity()).setToolbarInvisible();

        mainViewModel.checkPermission(mainActivity.getApplicationContext());

        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();
        MyLog.i("LifeCycle", "MainFragment onStart()");
    }

    @Override
    public void onResume() {
        super.onResume();
        MyLog.i("LifeCycle", "MainFragment onPause()");
    }

    @Override
    public void onPause() {
        super.onPause();
        MyLog.i("LifeCycle", "MainFragment onPause()");
    }

    @Override
    public void onStop() {
        super.onStop();
        MyLog.i("LifeCycle", "MainFragment onStop()");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        MyLog.i("LifeCycle", "MainFragment onDestroyView()");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        MyLog.i("LifeCycle", "MainFragment onDestroy()");
    }

}

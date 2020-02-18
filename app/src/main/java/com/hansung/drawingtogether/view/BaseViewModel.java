package com.hansung.drawingtogether.view;

import android.os.Bundle;

import androidx.lifecycle.ViewModel;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

public class BaseViewModel extends ViewModel {
    private CompositeDisposable disposable = new CompositeDisposable();
    public final SingleLiveEvent<NavigationCommand> navigationCommands = new SingleLiveEvent<>();

    public void addDisposable(Disposable d) {
        disposable.add(d);
    }

    @Override
    public void onCleared() {
        super.onCleared();
        disposable.clear();
    }

    public void navigate(int destinationId) {
        navigationCommands.postValue(new NavigationCommand.To(destinationId));
    }

    public void back() {
        navigationCommands.postValue(new NavigationCommand.Back());
    }

    public void navigate(int destinationId, Bundle bundle) {
        navigationCommands.postValue(new NavigationCommand.ToBundle(destinationId, bundle));
    }
}

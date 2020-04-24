package ro.atm.corden.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import java.util.List;

import ro.atm.corden.model.user.Action;
import ro.atm.corden.util.websocket.Repository;

public class MainActivityAdminViewModel extends AndroidViewModel {
    private MutableLiveData<List<Action>> actions;

    public MainActivityAdminViewModel(@NonNull Application application) {
        super(application);
        actions = new MutableLiveData<>();
    }

    public MutableLiveData<List<Action>> getActions() {
        return actions;
    }

    public void setActions(String date) {
        this.actions.setValue(Repository.getInstance().requestServerLogOnDate(date));
    }
}

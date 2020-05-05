package ro.atm.corden.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import java.util.List;

import ro.atm.corden.model.user.LiveStreamer;
import ro.atm.corden.util.websocket.Repository;

public class LiveStreamersViewModel extends AndroidViewModel {
    private final MutableLiveData<List<LiveStreamer>> liveStreamers;
    public LiveStreamersViewModel(@NonNull Application application) {
        super(application);
        liveStreamers = new MutableLiveData<>();
    }

    public void setLiveStreamers(){
        liveStreamers.setValue(Repository.getInstance().requestLiveStreamers());
    }

    public MutableLiveData<List<LiveStreamer>> getLiveStreamers(){
        return liveStreamers;
    }

    public boolean isListEmpty(){
        return liveStreamers.getValue().isEmpty();
    }
}

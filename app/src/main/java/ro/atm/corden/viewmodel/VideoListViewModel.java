package ro.atm.corden.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import java.util.List;

import ro.atm.corden.model.video.Video;
import ro.atm.corden.util.websocket.Repository;

public class VideoListViewModel extends AndroidViewModel {
    private final MutableLiveData<List<Video>> videos;

    public VideoListViewModel(@NonNull Application application) {
        super(application);
        videos = new MutableLiveData<>();
    }

    public void setVideos(String username){
        videos.setValue(Repository.getInstance().requestVideosForUsername(username, null));
    }

    public void setVideosByDate(String username, String date){
        videos.setValue(Repository.getInstance().requestVideosForUsername(username, date));
    }

    public MutableLiveData<List<Video>> getVideos() {
        return videos;
    }

    public boolean isDataSetEmpty(){
        return videos.getValue().isEmpty();
    }
}

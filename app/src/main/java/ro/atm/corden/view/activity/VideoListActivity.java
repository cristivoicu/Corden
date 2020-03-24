package ro.atm.corden.view.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.os.Bundle;
import android.widget.RelativeLayout;

import java.util.List;

import ro.atm.corden.R;
import ro.atm.corden.databinding.ActivityVideoListBinding;
import ro.atm.corden.model.transport_model.Video;
import ro.atm.corden.util.adapter.VideoAdapter;
import ro.atm.corden.util.constant.ExtraConstant;
import ro.atm.corden.util.websocket.Repository;
import ro.atm.corden.viewmodel.VideoListViewModel;

public class VideoListActivity extends AppCompatActivity {
    private ActivityVideoListBinding binding;
    private VideoListViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_video_list);
        viewModel = ViewModelProvider.AndroidViewModelFactory
                .getInstance(this.getApplication())
                .create(VideoListViewModel.class);
        binding.setViewModel(viewModel);

        binding.videosList.setLayoutManager(new LinearLayoutManager(this));
        binding.videosList.setHasFixedSize(true);

        String getUsername = getIntent().getStringExtra(ExtraConstant.GET_USERNAME);
        if(getUsername != null){
            viewModel.setVideos(getUsername);
        }

        final VideoAdapter videoAdapter = new VideoAdapter();
        binding.videosList.setAdapter(videoAdapter);

        viewModel.getVideos().observe(this, new Observer<List<Video>>() {
            @Override
            public void onChanged(List<Video> videos) {
                videoAdapter.setVideos(videos);
            }
        });
    }
}

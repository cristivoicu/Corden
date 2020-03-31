package ro.atm.corden.view.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.os.Bundle;
import android.view.View;
import android.widget.RelativeLayout;

import java.util.List;

import ro.atm.corden.R;
import ro.atm.corden.databinding.ActivityVideoListBinding;
import ro.atm.corden.model.transport_model.Video;
import ro.atm.corden.util.adapter.VideoAdapter;
import ro.atm.corden.util.constant.ExtraConstant;
import ro.atm.corden.util.websocket.Repository;
import ro.atm.corden.view.Fragment.VideoListFragment;
import ro.atm.corden.viewmodel.VideoListViewModel;

public class VideoListActivity extends AppCompatActivity {
    private ActivityVideoListBinding binding;
    private VideoListViewModel viewModel;
    private FragmentManager fragmentManager = getSupportFragmentManager();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_video_list);
        viewModel = ViewModelProvider.AndroidViewModelFactory
                .getInstance(this.getApplication())
                .create(VideoListViewModel.class);
        binding.setViewModel(viewModel);
        binding.toolbar.setTitle("Recorded videos");
        binding.toolbar.setSubtitle("For user");
        binding.toolbar.setSubtitleTextColor(getResources().getColor(R.color.colorWhile));
        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setTitle("Recorded videos");
        if(binding.frameLayout != null){
            if(savedInstanceState != null){
                return;
            }
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            VideoListFragment videoListFragment = new VideoListFragment();
            Bundle bundle = new Bundle();
            bundle.putString(ExtraConstant.GET_USERNAME, getIntent().getStringExtra(ExtraConstant.GET_USERNAME));
            videoListFragment.setArguments(bundle);
            fragmentTransaction.add(R.id.frameLayout, videoListFragment, null);
            fragmentTransaction.commit();
        }
    }

}

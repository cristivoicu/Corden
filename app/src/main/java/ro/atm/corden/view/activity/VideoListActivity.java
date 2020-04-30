package ro.atm.corden.view.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;

import ro.atm.corden.R;
import ro.atm.corden.databinding.ActivityVideoListBinding;
import ro.atm.corden.util.constant.AppConstants;
import ro.atm.corden.view.fragment.VideoListFragment;
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
        binding.toolbar.setSubtitle("For user: " + getIntent().getStringExtra(AppConstants.GET_USERNAME));
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
            bundle.putString(AppConstants.GET_USERNAME, getIntent().getStringExtra(AppConstants.GET_USERNAME));
            videoListFragment.setArguments(bundle);
            fragmentTransaction.add(R.id.frameLayout, videoListFragment, null);
            fragmentTransaction.commit();
        }
    }

}

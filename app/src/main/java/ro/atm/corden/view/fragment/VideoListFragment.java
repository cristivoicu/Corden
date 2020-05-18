package ro.atm.corden.view.fragment;

import android.app.DatePickerDialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.os.Handler;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;

import java.util.Calendar;
import java.util.List;

import ro.atm.corden.R;
import ro.atm.corden.databinding.FragmentVideoListBinding;
import ro.atm.corden.model.video.Video;
import ro.atm.corden.util.adapter.VideoAdapter;
import ro.atm.corden.util.constant.AppConstants;
import ro.atm.corden.viewmodel.VideoListViewModel;

/**
 *
 */
public class VideoListFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {
    private FragmentVideoListBinding binding;
    private VideoListViewModel viewModel;

    private String mUsername;

    public VideoListFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.timeline_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.selectDate:
                final Calendar cldr = Calendar.getInstance();
                int day = cldr.get(Calendar.DAY_OF_MONTH);
                int month = cldr.get(Calendar.MONTH);
                int year = cldr.get(Calendar.YEAR);
                DatePickerDialog picker = new DatePickerDialog(this.getContext(),
                        new DatePickerDialog.OnDateSetListener() {
                            @Override
                            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                                String date = String.format("%s-%s-%s", year, monthOfYear + 1, dayOfMonth);
                                viewModel.setVideosByDate(mUsername, date);
                            }
                        }, year, month, day);
                picker.show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_video_list, container, false);
        viewModel = ViewModelProvider.AndroidViewModelFactory
                .getInstance(getActivity().getApplication())
                .create(VideoListViewModel.class);
        binding.setViewModel(viewModel);

        binding.videosList.setLayoutManager(new LinearLayoutManager(this.getContext()));
        binding.videosList.setHasFixedSize(true);

        mUsername = getArguments().getString(AppConstants.GET_USERNAME);
        if (mUsername != null) {
            viewModel.setVideos(mUsername);
        }

        final VideoAdapter videoAdapter = new VideoAdapter();
        binding.videosList.setAdapter(videoAdapter);

        viewModel.getVideos().observe(getViewLifecycleOwner(), videos -> {
            videoAdapter.setVideos(videos);
            if(viewModel.isDataSetEmpty()){
                binding.hasNoData.setVisibility(View.VISIBLE);
            }else{
                binding.hasNoData.setVisibility(View.GONE);
            }
        });

        binding.swipeRefreshLayout.setOnRefreshListener(this);

        videoAdapter.setOnItemClickListener(video -> {

            FragmentManager fragmentManager = getParentFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            PlayerFragment playerFragment = new PlayerFragment();

            Bundle videoBundle = new Bundle();

            videoBundle.putSerializable(AppConstants.GET_VIDEO, video);
            playerFragment.setArguments(videoBundle);
            fragmentTransaction.addToBackStack("videoList");
            fragmentTransaction.hide(VideoListFragment.this);
            fragmentTransaction.add(R.id.frameLayout, playerFragment);



            fragmentTransaction.commit();

        });

        return binding.getRoot();
    }

    @Override
    public void onRefresh() {
        viewModel.setVideos(mUsername);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                binding.swipeRefreshLayout.setRefreshing(false);
            }
        }, 2000);
    }
}

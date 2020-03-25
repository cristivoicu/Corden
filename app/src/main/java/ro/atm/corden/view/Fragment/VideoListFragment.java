package ro.atm.corden.view.Fragment;

import android.os.Bundle;

import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import ro.atm.corden.R;
import ro.atm.corden.databinding.FragmentVideoListBinding;
import ro.atm.corden.model.transport_model.Video;
import ro.atm.corden.util.adapter.VideoAdapter;
import ro.atm.corden.util.constant.ExtraConstant;
import ro.atm.corden.viewmodel.VideoListViewModel;

/**

 */
public class VideoListFragment extends Fragment {
    private FragmentVideoListBinding binding;
    private VideoListViewModel viewModel;

    public VideoListFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

        String getUsername = getArguments().getString(ExtraConstant.GET_USERNAME);
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

        return binding.getRoot();
    }
}

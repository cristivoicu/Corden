package ro.atm.corden.view.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import ro.atm.corden.R;
import ro.atm.corden.databinding.FragmentLiveStreamersListBinding;
import ro.atm.corden.model.user.LiveStreamer;
import ro.atm.corden.util.adapter.LiveStreamerAdapter;
import ro.atm.corden.util.websocket.SignallingClient;
import ro.atm.corden.util.websocket.subscribers.LiveStreamerSubscriber;
import ro.atm.corden.viewmodel.LiveStreamersViewModel;

public class LiveStreamersListFragment extends Fragment
implements LiveStreamerSubscriber {
    private FragmentLiveStreamersListBinding mBinding;
    private LiveStreamersViewModel mViewModel;
    LiveStreamerAdapter mAdpater;

    public LiveStreamersListFragment(){
        // require empty public constructor
    }

    @Override
    public void onStart() {
        super.onStart();
        SignallingClient.getInstance().sendMessageToSubscribeToLiveStreamersEvents(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        SignallingClient.getInstance().sendMessageToUnsubscribeFromLiveStreamersEvents();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(inflater,
                R.layout.fragment_live_streamers_list,
                container,
                false);
        mViewModel = ViewModelProvider.AndroidViewModelFactory
                .getInstance(getActivity().getApplication())
                .create(LiveStreamersViewModel.class);
        mBinding.setViewModel(mViewModel);
        mBinding.liveStreamers.setLayoutManager(new LinearLayoutManager(getContext()));
        mBinding.liveStreamers.setHasFixedSize(true);

        mViewModel.setLiveStreamers();
        mAdpater = new LiveStreamerAdapter();
        mBinding.liveStreamers.setAdapter(mAdpater);

        mViewModel.getLiveStreamers().observe(getViewLifecycleOwner(), liveStreamers -> {
            mAdpater.setStreamers(liveStreamers);
            if(mViewModel.isListEmpty()){
                mBinding.noOneStreams.setVisibility(View.VISIBLE);
            }else {
                mBinding.noOneStreams.setVisibility(View.GONE);
            }
        });

        if(mViewModel.isListEmpty()){
            mBinding.noOneStreams.setVisibility(View.VISIBLE);
        }

        mAdpater.setListener(liveStreamer -> {
            FragmentTransaction fragmentTransaction = getParentFragmentManager().beginTransaction();
            LiveWatchFragment liveWatchFragment = new LiveWatchFragment();
            liveWatchFragment.setUsername(liveStreamer.getUsername());

            fragmentTransaction.addToBackStack("liveWatches");
            fragmentTransaction.hide(this);
            fragmentTransaction.add(R.id.frameLayout, liveWatchFragment);
            fragmentTransaction.commit();
        });

        return mBinding.getRoot();
    }

    @Override
    public void onNewSubscriber(LiveStreamer liveStreamer) {
        Log.d("TAG", "onNewSubscriber");
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                mAdpater.addStreamer(liveStreamer);
                if(mViewModel.isListEmpty()){
                    mBinding.noOneStreams.setVisibility(View.VISIBLE);
                }else {
                    mBinding.noOneStreams.setVisibility(View.GONE);
                }
            }
        });
    }

    @Override
    public void onSubscribeStop(LiveStreamer liveStreamer) {
        Log.d("TAG", "onSubscribeStop");
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                mAdpater.removeStreamer(liveStreamer);
                if(mViewModel.isListEmpty()){
                    mBinding.noOneStreams.setVisibility(View.VISIBLE);
                }else {
                    mBinding.noOneStreams.setVisibility(View.GONE);
                }
            }
        });
    }
}

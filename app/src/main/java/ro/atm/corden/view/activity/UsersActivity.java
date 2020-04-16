package ro.atm.corden.view.activity;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.content.Intent;
import android.os.Bundle;
import android.view.ActionMode;

import java.util.List;

import ro.atm.corden.R;
import ro.atm.corden.databinding.ActivityUsersBinding;
import ro.atm.corden.model.user.User;
import ro.atm.corden.util.adapter.UserAdapter;
import ro.atm.corden.util.constant.ExtraConstant;
import ro.atm.corden.util.websocket.SignallingClient;
import ro.atm.corden.util.websocket.subscribers.UserSubscriber;
import ro.atm.corden.viewmodel.UsersViewModel;

public class UsersActivity extends AppCompatActivity
        implements UserSubscriber {
    private ActivityUsersBinding binding;
    private UsersViewModel viewModel;

    UserAdapter userAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_users);
        binding.setLifecycleOwner(this);

        viewModel = ViewModelProvider.AndroidViewModelFactory
                .getInstance(this.getApplication())
                .create(UsersViewModel.class);
        binding.setViewModel(viewModel);

        setSupportActionBar(binding.toolbar);

        binding.usersList.setLayoutManager(new LinearLayoutManager(this));
        binding.usersList.setHasFixedSize(true);

        String getType = getIntent().getStringExtra(ExtraConstant.GET_USERS_TYPE);
        if (getType.equals(ExtraConstant.GET_USERS_ALL)) {
            viewModel.setAllUsers();
        }


        userAdapter = new UserAdapter();
        binding.usersList.setAdapter(userAdapter);

        viewModel.getAllUsers().observe(this, new Observer<List<User>>() {
            @Override
            public void onChanged(List<User> users) {
                userAdapter.setUsers(users);
            }
        });

        userAdapter.setOnItemClickListener(new UserAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(User user) {
                Intent intent = new Intent(UsersActivity.this, UserDetailActivity.class);
                intent.putExtra("user", user);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        SignallingClient.getInstance().subscribeUserListListener(this);
    }

    @Override
    protected void onStop() {
        super.onStop();

        SignallingClient.getInstance().unsubscribeUserListListener(this);
    }

    @Override
    public void onUserUpdated(User user) {

    }
}

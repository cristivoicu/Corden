package ro.atm.corden.view.activity;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import java.util.List;

import ro.atm.corden.R;
import ro.atm.corden.databinding.ActivityUsersBinding;
import ro.atm.corden.model.user.Status;
import ro.atm.corden.model.user.User;
import ro.atm.corden.util.adapter.UserAdapter;
import ro.atm.corden.util.constant.Constant;
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

        String getType = getIntent().getStringExtra(Constant.GET_USERS_TYPE);
        if (getType.equals(Constant.GET_USERS_ALL)) {
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
        viewModel.setAllUsers();

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                SignallingClient.getInstance().sendMessageToSubscribeToUserList();
                return null;
            }
        }.execute();
    }

    @Override
    protected void onStop() {
        super.onStop();

        SignallingClient.getInstance().unsubscribeUserListListener();

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                SignallingClient.getInstance().sendMessageToUnsubscribeFromUserList();
                return null;
            }
        }.execute();
    }

    @Override
    public void onUserDataChanged(User user) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                userAdapter.updateUserData(user);
            }
        });

    }

    @Override
    public void onUserStatusChanged(String username, Status status) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                userAdapter.updateStatus(username, status);
                Toast.makeText(UsersActivity.this,
                        String.format("User %s is now %s", username, status.name()),
                        Toast.LENGTH_SHORT)
                        .show();
            }
        });
    }
}

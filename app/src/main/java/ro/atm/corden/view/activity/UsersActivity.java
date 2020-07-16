package ro.atm.corden.view.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.SearchView;
import android.widget.Toast;

import java.util.List;

import ro.atm.corden.R;
import ro.atm.corden.databinding.ActivityUsersBinding;
import ro.atm.corden.model.user.Status;
import ro.atm.corden.model.user.User;
import ro.atm.corden.util.adapter.UserAdapter;
import ro.atm.corden.util.constant.AppConstants;
import ro.atm.corden.util.websocket.SignallingClient;
import ro.atm.corden.util.websocket.subscribers.UserSubscriber;
import ro.atm.corden.viewmodel.UsersViewModel;

public class UsersActivity extends AppCompatActivity
        implements UserSubscriber, SwipeRefreshLayout.OnRefreshListener {

    private ActivityUsersBinding binding;
    private UsersViewModel viewModel;
    private boolean isOnlineUsers = false;

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

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        binding.usersList.setLayoutManager(new LinearLayoutManager(this));
        binding.usersList.setHasFixedSize(true);

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
        binding.swipeRefreshLayout.setOnRefreshListener(this);
        registerForContextMenu(binding.usersList);
    }

    @Override
    protected void onStart() {
        super.onStart();

        SignallingClient.getInstance().subscribeUserListListener(this);
        String getType = getIntent().getStringExtra(AppConstants.GET_USERS_TYPE);
        if (getType.equals(AppConstants.GET_USERS_ALL)) {
            viewModel.setAllUsers();
        }
        if (getType.equals(AppConstants.GET_USERS_ONLINE)) {
            isOnlineUsers = true;
            viewModel.setOnlineUsers();
        }

        SignallingClient.getInstance().sendMessageToSubscribeToUserList();
    }

    @Override
    protected void onStop() {
        super.onStop();

        SignallingClient.getInstance().unsubscribeUserListListener();

        SignallingClient.getInstance().sendMessageToUnsubscribeFromUserList();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        SignallingClient.getInstance().unsubscribeUserListListener();

        SignallingClient.getInstance().sendMessageToUnsubscribeFromUserList();
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
                if (isOnlineUsers) {
                    userAdapter.updateStatusOnOnlineActivity(username, status);
                } else {
                    userAdapter.updateStatus(username, status);
                }

                Toast.makeText(UsersActivity.this,
                        String.format("User %s is now %s", username, status.name()),
                        Toast.LENGTH_SHORT)
                        .show();
            }
        });
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        final User user = userAdapter.getUser();

        if (user == null)
            return super.onContextItemSelected(item);
        switch (item.getItemId()) {
            case R.id.ctx_editUser:
                Intent intent = new Intent(this, EditUserDetailsActivity.class);
                intent.putExtra(AppConstants.GET_USERNAME, user.getUsername());
                intent.putExtra(Intent.EXTRA_USER, user);
                startActivityForResult(intent, AppConstants.USER_DETAIL_ACTIVITY);
                break;
            case R.id.ctx_notifyStream:
                SignallingClient.getInstance().sendStreamRequest(user.getUsername());
                break;
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.search_menu, menu);

        SearchView searchView = (SearchView) menu.findItem(R.id.searchAction).getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                userAdapter.getFilter().filter(newText);
                return false;
            }
        });
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onRefresh() {
        viewModel.setAllUsers();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                binding.swipeRefreshLayout.setRefreshing(false);
            }
        }, 2000);
    }


}

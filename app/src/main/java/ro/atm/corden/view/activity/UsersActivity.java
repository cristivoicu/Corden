package ro.atm.corden.view.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.content.Intent;
import android.os.Bundle;

import java.util.List;

import ro.atm.corden.R;
import ro.atm.corden.databinding.ActivityUsersBinding;
import ro.atm.corden.model.transport_model.User;
import ro.atm.corden.util.adapter.UserAdapter;
import ro.atm.corden.util.constant.ExtraConstant;
import ro.atm.corden.util.websocket.Repository;
import ro.atm.corden.util.websocket.SignallingClient;
import ro.atm.corden.util.websocket.callback.GetUsersListener;
import ro.atm.corden.viewmodel.UsersViewModel;

public class UsersActivity extends AppCompatActivity implements GetUsersListener {
    private ActivityUsersBinding binding;
    private UsersViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_users);
        binding.setLifecycleOwner(this);

        viewModel = ViewModelProvider.AndroidViewModelFactory.getInstance(this.getApplication()).create(UsersViewModel.class);
        binding.setViewModel(viewModel);

        setSupportActionBar(binding.toolbar);

        binding.usersList.setLayoutManager(new LinearLayoutManager(this));
        binding.usersList.setHasFixedSize(true);

        String getType = getIntent().getStringExtra(ExtraConstant.GET_USERS_TYPE);
        if(getType.equals(ExtraConstant.GET_USERS_ALL)){
            viewModel.setAllUsers();
        }


        final UserAdapter userAdapter = new UserAdapter();
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
    public void gotAllUsers(User[] users) {

    }
}

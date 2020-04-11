package ro.atm.corden.view.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import ro.atm.corden.R;
import ro.atm.corden.databinding.ActivityUserDetailBinding;
import ro.atm.corden.model.user.Role;
import ro.atm.corden.model.user.User;
import ro.atm.corden.util.constant.ExtraConstant;
import ro.atm.corden.viewmodel.UserDetailViewModel;

public class UserDetailActivity extends AppCompatActivity {
    private ActivityUserDetailBinding binding;
    private UserDetailViewModel viewModel;
    private User user = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_user_detail);

        viewModel = ViewModelProvider.AndroidViewModelFactory.getInstance(this.getApplication())
                .create(UserDetailViewModel.class);
        binding.setViewModel(viewModel);

        setSupportActionBar(binding.toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);


        Bundle extras = getIntent().getExtras();
        if(extras != null){
            user = (User)getIntent().getSerializableExtra("user");
        }

        if(user != null){
            if(user.getRoles().equals(Role.ADMIN.name())){
                binding.image.setImageResource(R.drawable.ic_boss);
                binding.textUserType.setText("ADMIN");
            }
            viewModel.setRole(user.getRoles());
            viewModel.setUserAddress(user.getAddress());
            viewModel.setUsername(user.getUsername());
            viewModel.setUserPhoneNumber(user.getPhoneNumber());
            viewModel.setUserProgram(String.format("Program from %s to %s", user.getProgramStart(), user.getProgramEnd()));
            viewModel.setUserRealName(user.getName());
        }
    }

    public void onListVideosClicked(View view) {
        Intent intent = new Intent(this, VideoListActivity.class);
        intent.putExtra(ExtraConstant.GET_USERNAME, user.getUsername());
        startActivity(intent);
    }

    public void onAssignJobClicked(View view) {
        Intent intent = new Intent(this, UserJobsMapsActivity.class);
        startActivity(intent);
    }

    public void onTimelineButtonClicked(View view) {
        Intent intent = new Intent(this, UserTimelineActivity.class);
        intent.putExtra(ExtraConstant.GET_USERNAME, user.getUsername());
        startActivity(intent);
    }

    public void onEditUserButtonClicked(View view) {
        Intent intent = new Intent(this, EditUserDetailsActivity.class);
        intent.putExtra(ExtraConstant.GET_USERNAME, user.getUsername());
        intent.putExtra(Intent.EXTRA_USER, user);
        startActivity(intent);
    }
}

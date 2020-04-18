package ro.atm.corden.view.activity;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import java.util.Locale;

import ro.atm.corden.R;
import ro.atm.corden.databinding.ActivityUserDetailBinding;
import ro.atm.corden.model.user.Role;
import ro.atm.corden.model.user.User;
import ro.atm.corden.util.constant.Constant;
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
            viewModel.setData(user);
        }
    }

    public void onListVideosClicked(View view) {
        Intent intent = new Intent(this, VideoListActivity.class);
        intent.putExtra(Constant.GET_USERNAME, user.getUsername());
        startActivity(intent);
    }

    public void onAssignJobClicked(View view) {
        Intent intent = new Intent(this, UserJobsMapsActivity.class);
        startActivity(intent);
    }

    public void onTimelineButtonClicked(View view) {
        Intent intent = new Intent(this, UserTimelineActivity.class);
        intent.putExtra(Constant.GET_USERNAME, user.getUsername());
        startActivity(intent);
    }

    public void onEditUserButtonClicked(View view) {
        Intent intent = new Intent(this, EditUserDetailsActivity.class);
        intent.putExtra(Constant.GET_USERNAME, user.getUsername());
        intent.putExtra(Intent.EXTRA_USER, user);
        startActivityForResult(intent, Constant.USER_DETAIL_ACTIVITY);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == Constant.USER_DETAIL_ACTIVITY){
            if(resultCode == Activity.RESULT_OK){
                user = (User) data.getSerializableExtra(Intent.EXTRA_USER);
                viewModel.setData(user);
            }
        }
    }

    public void onDisableAccountClicked(View view) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle("Warning")
                .setMessage("You want to disable this account?\nThis action is irreversible!")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("Yes", (dialog, which) ->
                {
                    viewModel.diseableUser();
                });
        AlertDialog alertDialog = dialogBuilder.create();
        alertDialog.show();
    }

    public void onRealTimeLocationButtonClicked(View view) {

    }
}

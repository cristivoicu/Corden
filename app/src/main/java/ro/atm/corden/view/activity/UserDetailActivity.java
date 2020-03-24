package ro.atm.corden.view.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import android.content.Intent;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.View;

import java.io.Serializable;

import ro.atm.corden.R;
import ro.atm.corden.databinding.ActivityUserDetailBinding;
import ro.atm.corden.model.Roles;
import ro.atm.corden.model.transport_model.User;
import ro.atm.corden.util.constant.ExtraConstant;

public class UserDetailActivity extends AppCompatActivity {
    private ActivityUserDetailBinding binding;
    private User user = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_user_detail);

        setSupportActionBar(binding.toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);


        Bundle extras = getIntent().getExtras();
        if(extras != null){
            user = (User)getIntent().getSerializableExtra("user");
        }

        if(user != null){
            if(user.getRoles() == Roles.ADMIN){
                binding.image.setImageResource(R.drawable.toolbar_ic_admin);
                binding.textUserType.setText("ADMIN");
            }
        }
    }

    public void onListVideosClicked(View view) {
        Intent intent = new Intent(this, VideoListActivity.class);
        intent.putExtra(ExtraConstant.GET_USERNAME, user.getUsername());
        startActivity(intent);
    }
}

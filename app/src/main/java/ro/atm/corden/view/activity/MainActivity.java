package ro.atm.corden.view.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;

import ro.atm.corden.R;
import ro.atm.corden.databinding.ActivityMainBinding;
import ro.atm.corden.util.constant.ExtraConstant;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        binding.cardViewActiveUsers.setBackgroundResource(R.drawable.main_card);
        binding.cardViewAllUsers.setBackgroundResource(R.drawable.main_card);
        binding.cardViewCreateAccount.setBackgroundResource(R.drawable.main_card);
        binding.cardViewLiveVideo.setBackgroundResource(R.drawable.main_card);
        binding.cardViewRecordedVideos.setBackgroundResource(R.drawable.main_card);
        binding.cardViewAboutApp.setBackgroundResource(R.drawable.main_card);
        binding.cardViewSettings.setBackgroundResource(R.drawable.main_card);
        binding.cardViewLocation.setBackgroundResource(R.drawable.main_card);

        setSupportActionBar(binding.toolbar);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.admin_main_menu, menu);
        return true;
    }

    public void onCreateAccountClicked(View view) {
        Intent intent = new Intent(this, RegisterUserActivity.class);
        startActivity(intent);
    }

    public void onShowAllUsersClicked(View view) {
        Intent intent = new Intent(this, UsersActivity.class);
        intent.putExtra(ExtraConstant.GET_USERS_TYPE, ExtraConstant.GET_USERS_ALL);
        startActivity(intent);
    }

    public void onLiveVideoClicked(View view) {
        Intent intent = new Intent(this, MediaActivity.class);
        startActivity(intent);
    }

    public void onClickedSettings(View view) {

    }
}

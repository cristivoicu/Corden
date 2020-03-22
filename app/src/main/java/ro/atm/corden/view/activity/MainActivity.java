package ro.atm.corden.view.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import ro.atm.corden.R;
import ro.atm.corden.util.constant.ExtraConstant;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
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
}

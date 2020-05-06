package ro.atm.corden.view.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.DatePicker;

import com.google.android.material.bottomsheet.BottomSheetBehavior;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import ro.atm.corden.R;
import ro.atm.corden.databinding.ActivityMainBinding;
import ro.atm.corden.model.user.Action;
import ro.atm.corden.util.adapter.ServerLogAdapter;
import ro.atm.corden.util.constant.AppConstants;
import ro.atm.corden.util.services.LocationService;
import ro.atm.corden.util.websocket.SignallingClient;
import ro.atm.corden.viewmodel.MainActivityAdminViewModel;

public class MainActivityAdmin extends AppCompatActivity {
    private ActivityMainBinding binding;
    private MainActivityAdminViewModel viewModel;
    private BottomSheetBehavior bottomSheetBehavior;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        binding.setLifecycleOwner(this);

        viewModel = ViewModelProvider.AndroidViewModelFactory
                .getInstance(this.getApplication())
                .create(MainActivityAdminViewModel.class);

        bottomSheetBehavior = bottomSheetBehavior.from(binding.bottomSheet);

        binding.cardViewActiveUsers.setBackgroundResource(R.drawable.main_card);
        binding.cardViewAllUsers.setBackgroundResource(R.drawable.main_card);
        binding.cardViewCreateAccount.setBackgroundResource(R.drawable.main_card);
        binding.cardViewLiveVideo.setBackgroundResource(R.drawable.main_card);
        binding.cardViewRecordedVideos.setBackgroundResource(R.drawable.main_card);
        binding.cardViewLocation.setBackgroundResource(R.drawable.main_card);

        setSupportActionBar(binding.toolbar);

        binding.serverLog.setLayoutManager(new LinearLayoutManager(this));
        binding.serverLog.setHasFixedSize(true);

        final ServerLogAdapter serverLogAdapter = new ServerLogAdapter(this);
        binding.serverLog.setAdapter(serverLogAdapter);

        binding.date.setText(new SimpleDateFormat("dd-MM-YYYY").format(new Date()));
        viewModel.setActions(new SimpleDateFormat("YYYY-MM-dd").format(new Date()));
        viewModel.getActions().observe(this, new Observer<List<Action>>() {
            @Override
            public void onChanged(List<Action> actions) {
                serverLogAdapter.setActions(actions);
            }
        });

        Intent serviceIntent = new Intent(this, LocationService.class);
        ContextCompat.startForegroundService(this, serviceIntent);

        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                switch (newState) {
                    case BottomSheetBehavior.STATE_COLLAPSED:
                        Log.d("bottomsheet","Collapsed");
                        break;
                    case BottomSheetBehavior.STATE_DRAGGING:
                        Log.d("bottomsheet","Dragging...");
                        break;
                    case BottomSheetBehavior.STATE_EXPANDED:
                        Log.d("bottomsheet","Expanded");

                        break;
                    case BottomSheetBehavior.STATE_HIDDEN:
                        Log.d("bottomsheet","Hidden");
                        break;
                    case BottomSheetBehavior.STATE_SETTLING:
                        Log.d("bottomsheet", "Settling...");
                        break;
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {

            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.admin_main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.itemLogout:
                SignallingClient.getInstance().logout();
                // stopping services
                stopService(new Intent(MainActivityAdmin.this, LocationService.class));
                finish();
                break;
            case R.id.itemMyAccount:
                Intent intent = new Intent(this, EditUserAccountActivity.class);
                startActivity(intent);
                break;
            case R.id.itemSetting:
                intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                break;
        }
        return true;
    }

    public void onCreateAccountClicked(View view) {
        Intent intent = new Intent(this, RegisterUserActivity.class);
        startActivity(intent);
    }

    public void onShowAllUsersClicked(View view) {
        Intent intent = new Intent(this, UsersActivity.class);
        intent.putExtra(AppConstants.GET_USERS_TYPE, AppConstants.GET_USERS_ALL);
        startActivity(intent);
    }

    public void onShowOnlineUsersClicked(View view) {
        Intent intent = new Intent(this, UsersActivity.class);
        intent.putExtra(AppConstants.GET_USERS_TYPE, AppConstants.GET_USERS_ONLINE);
        startActivity(intent);
    }

    public void onLiveVideoClicked(View view) {
        Intent intent = new Intent(this, MediaActivity.class);
        startActivity(intent);
    }

    public void onLocationCardClicked(View view) {
        Intent intent = new Intent(this, AdminMapsActivity.class);
        startActivity(intent);
    }

    public void onDateClicked(View view) {
        final Calendar cldr = Calendar.getInstance();
        int day = cldr.get(Calendar.DAY_OF_MONTH);
        int month = cldr.get(Calendar.MONTH);
        int year = cldr.get(Calendar.YEAR);
        DatePickerDialog picker = new DatePickerDialog(MainActivityAdmin.this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                        String date = String.format("%s-%s-%s", year, monthOfYear + 1, dayOfMonth);
                        String uiDate = String.format("%s-%s-%s", dayOfMonth, monthOfYear + 1, year);
                        binding.date.setText(uiDate);
                        viewModel.setActions(date);
                    }
                }, year, month, day);
        picker.show();
    }

    @Override
    public void onBackPressed() {

    }
}

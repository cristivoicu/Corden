package ro.atm.corden.view.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TimePicker;

import java.util.Locale;

import ro.atm.corden.R;
import ro.atm.corden.databinding.ActivityEditUserDetailsBinding;
import ro.atm.corden.model.user.User;
import ro.atm.corden.util.constant.ExtraConstant;
import ro.atm.corden.util.exception.websocket.UserNotLoggedInException;
import ro.atm.corden.util.websocket.SignallingClient;
import ro.atm.corden.util.websocket.callback.UpdateUserListener;
import ro.atm.corden.view.fragment.TimePickerFragment;
import ro.atm.corden.viewmodel.EditUserViewModel;

public class EditUserDetailsActivity extends AppCompatActivity
        implements TimePickerDialog.OnTimeSetListener,
        UpdateUserListener {
    private ActivityEditUserDetailsBinding binding;
    private EditUserViewModel viewModel;

    private User user;

    private boolean isProgramStart = false;

    /**
     * hour of day for program start
     */
    private int hour = 24;
    /**
     * minute for program start
     */
    private int minute = 60;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.edit_menu, menu);
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_edit_user_details);
        binding.setLifecycleOwner(this);
        viewModel = ViewModelProvider.AndroidViewModelFactory
                .getInstance(this.getApplication())
                .create(EditUserViewModel.class);
        binding.setViewModel(viewModel);
        String username = getIntent().getStringExtra(ExtraConstant.GET_USERNAME);

        binding.toolbar.setTitle("Edit user account");
        binding.toolbar.setSubtitle("For user: " + username);

        setSupportActionBar(binding.toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            user = (User) getIntent().getSerializableExtra(Intent.EXTRA_USER);
        }
        viewModel.init(user);

        binding.programStart.setOnClickListener(v -> {
            isProgramStart = true;
            DialogFragment timePicker = new TimePickerFragment();
            timePicker.show(getSupportFragmentManager(), "time picker");
        });
        binding.programEnd.setOnClickListener(v -> {
            isProgramStart = false;
            DialogFragment timePicker = new TimePickerFragment();
            timePicker.show(getSupportFragmentManager(), "time picker");
        });

        try {
            SignallingClient.getInstance().subscribeUpdateUserListener(this);
        } catch (UserNotLoggedInException e) {
            e.printStackTrace();
            finish();
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.save:
                viewModel.modifyUser();
                break;
        }

        return true;
    }

    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        if (isProgramStart) {
            binding.programStart.setText(String.format(Locale.getDefault(),"%02d:%02d", hourOfDay, minute));
            viewModel.setStartHour(String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute));
            hour = hourOfDay;
            this.minute = minute;
        } else {
            // verify
            if (hourOfDay < hour || (hourOfDay == hour && minute <= this.minute)) {
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
                dialogBuilder.setTitle("Warning")
                        .setMessage("The end program time is invalid!")
                        .setPositiveButton("OK", null);
                AlertDialog alertDialog = dialogBuilder.create();
                alertDialog.show();
                return;
            }
            binding.programEnd.setText(String.format(Locale.getDefault(),"%02d:%02d", hourOfDay, minute));
            viewModel.setEndHour(String.format(Locale.getDefault(),"%02d:%02d", hourOfDay, minute));
        }
    }

    @Override
    public void onUpdateSuccess() {
        new Handler(Looper.getMainLooper())
                .post(() -> {
                    AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
                    dialogBuilder.setTitle("Information")
                            .setMessage("User data was successfully updated!")
                            .setIcon(android.R.drawable.ic_dialog_info)
                            .setPositiveButton("OK", null);
                    AlertDialog alertDialog = dialogBuilder.create();
                    alertDialog.show();
                });
    }

    @Override
    public void onUpdateFailure() {
        new Handler(Looper.getMainLooper())
                .post(() -> {
                    AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
                    dialogBuilder.setTitle("Information")
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setMessage("Failed to update user information...!")
                            .setPositiveButton("OK", null);
                    AlertDialog alertDialog = dialogBuilder.create();
                    alertDialog.show();
                });
    }
}

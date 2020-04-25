package ro.atm.corden.view.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;

import ro.atm.corden.R;
import ro.atm.corden.databinding.ActivityEditUserAccountBinding;
import ro.atm.corden.model.user.LoginUser;
import ro.atm.corden.model.user.User;
import ro.atm.corden.util.exception.websocket.UserNotLoggedInException;
import ro.atm.corden.util.websocket.SignallingClient;
import ro.atm.corden.util.websocket.callback.UpdateUserListener;
import ro.atm.corden.viewmodel.EditUserAccountViewModel;
import ro.atm.corden.viewmodel.EditUserViewModel;

import static android.content.Intent.EXTRA_USER;

public class EditUserAccountActivity extends AppCompatActivity implements UpdateUserListener {
    private ActivityEditUserAccountBinding binding;
    private EditUserAccountViewModel mViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_edit_user_account);
        binding.setLifecycleOwner(this);
        mViewModel = ViewModelProvider.AndroidViewModelFactory
                .getInstance(this.getApplication())
                .create(EditUserAccountViewModel.class);
        binding.setViewModel(mViewModel);

        setSupportActionBar(binding.toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        mViewModel.setData(LoginUser.username);


    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.save:
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
                dialogBuilder.setTitle("Information")
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setMessage("Do you want to save?")
                        .setPositiveButton("OK", (dialog, which) -> {
                            mViewModel.modifyData();
                        });
                AlertDialog alertDialog = dialogBuilder.create();
                alertDialog.show();

                break;
        }

        return true;
    }

    @Override
    public void onBackPressed() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle("Information")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setMessage("Do you want to save?")
                .setPositiveButton("OK", (dialog, which) -> {
                    mViewModel.modifyData();
                    super.onBackPressed();
                })
                .setNegativeButton("No", (dialog, which) -> {
                    super.onBackPressed();
                });
        AlertDialog alertDialog = dialogBuilder.create();
        alertDialog.show();

    }

    @Override
    protected void onStart() {
        super.onStart();
        try {
            SignallingClient.getInstance().subscribeUpdateUserListener(this);
        } catch (UserNotLoggedInException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        SignallingClient.getInstance().unsubscribeUpdateUserListener();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.edit_menu, menu);
        return true;
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
                            .setMessage("Failed to update information...!")
                            .setPositiveButton("OK", null);
                    AlertDialog alertDialog = dialogBuilder.create();
                    alertDialog.show();
                });
    }

    @Override
    public void onUserDisableSuccess() {

    }

    @Override
    public void onUserDisableFailure() {

    }
}

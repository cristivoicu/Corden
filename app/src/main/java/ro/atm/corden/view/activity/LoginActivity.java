package ro.atm.corden.view.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.List;

import de.adorsys.android.securestoragelibrary.SecurePreferences;
import de.adorsys.android.securestoragelibrary.SecureStorageException;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;
import ro.atm.corden.R;
import ro.atm.corden.databinding.ActivityLoginBinding;
import ro.atm.corden.model.user.LoginUser;
import ro.atm.corden.model.user.Role;
import ro.atm.corden.util.constant.AppConstants;
import ro.atm.corden.util.exception.login.CertNoPassException;
import ro.atm.corden.util.exception.login.EmptyTextException;
import ro.atm.corden.util.exception.login.LoginListenerNotInitialisedException;
import ro.atm.corden.util.websocket.callback.LoginListener;
import ro.atm.corden.util.websocket.SignallingClient;
import ro.atm.corden.view.dialog.CertificatePasswordDialog;
import ro.atm.corden.viewmodel.LoginViewModel;

import static ro.atm.corden.util.constant.ExceptionCodes.EMPTY_FIELD_CODE;
import static ro.atm.corden.util.constant.ExceptionCodes.LOGIN_LISTENER_NOT_INITIALISED_CODE;
import static ro.atm.corden.util.constant.ExceptionCodes.OK_CODE;

public class LoginActivity extends AppCompatActivity implements LoginListener,
        EasyPermissions.PermissionCallbacks, CertificatePasswordDialog.CertificatePasswordDialogListener {
    private LoginViewModel viewModel;
    private static ActivityLoginBinding binding;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {

    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
        if (EasyPermissions.somePermissionDenied(this, AppConstants.locationPermissions)) {
            new AppSettingsDialog.Builder(this).build().show();
        }
        if (EasyPermissions.somePermissionPermanentlyDenied(this, Arrays.asList(AppConstants.locationPermissions))) {
            new AppSettingsDialog.Builder(this).build().show();
        }
    }

    private void initWebSocket(){
        try {
            SignallingClient.getInstance().initWebSocket(this);
            binding.loginButton.setEnabled(true);
            SignallingClient.getInstance().subscribeLoginListener(this);
        } catch (IOException | CertNoPassException e) {
            CertificatePasswordDialog certificatePasswordDialog = new CertificatePasswordDialog();
            certificatePasswordDialog.show(getSupportFragmentManager(), "CertPassDialog");
            binding.loginButton.setEnabled(false);
            Toast.makeText(this, "Password is not correct!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("Log", "onCreate");

        binding = DataBindingUtil.setContentView(this, R.layout.activity_login);

        initWebSocket();

        viewModel = ViewModelProvider.AndroidViewModelFactory.getInstance(this.getApplication())
                .create(LoginViewModel.class);

        binding.setLifecycleOwner(this);
        binding.setViewmodel(viewModel);

        //set live data listeners
        viewModel.getUser().observe(this, (LoginUser loginUser) -> {
            Log.d("Log", "on observe clicked.");
            LoginAsyncTask loginAsyncTask = new LoginAsyncTask();
            loginAsyncTask.context = this;
            loginAsyncTask.execute(loginUser);
        });

        binding.textInputUsername.getEditText().setOnClickListener(v -> {
            binding.textInputUsername.setError(null);
            binding.textInputPassword.setError(null);
        });
        binding.textInputPassword.getEditText().setOnClickListener(v -> {
            binding.textInputUsername.setError(null);
            binding.textInputPassword.setError(null);
        });

        requestPermissions();
    }

    @Override
    protected void onStart() {
        Log.e("login", "onstart: " + getIntent().getFlags());
        if(getIntent().hasExtra("onClose")){
            String message = "";
            try {
                switch (getIntent().getStringExtra("onClose")){
                    case "4999":
                        message = "Your account was disable by admin ...";
                        break;
                    case "1002":
                        message = "Access denied!";
                        break;
                    default:
                        message = "Logout by unknown event ...";
                }
            } catch (NullPointerException e) {
                message = "Logout by unknown event ...";
            }
            Log.e("login", "onstartFLAG");
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
            dialogBuilder.setTitle("Information")
                    .setMessage(message)
                    .setPositiveButton("OK", null);
            AlertDialog alertDialog = dialogBuilder.create();
            alertDialog.show();
        }
        super.onStart();
    }

    @Override
    protected void onDestroy() {
        SignallingClient.getInstance().disconnect();
        super.onDestroy();
    }

    @AfterPermissionGranted(AppConstants.LOCATION_REQUEST_CODE)
    private void requestPermissions() {
        if (!EasyPermissions.hasPermissions(this, AppConstants.locationPermissions)) {
            EasyPermissions.requestPermissions(this,
                    "You need to accept the permission for the app runs correctly",
                    AppConstants.LOCATION_REQUEST_CODE,
                    AppConstants.locationPermissions);
        }
    }

    @Override
    public void onLoginError() {
        // because it comes from another thread (different from UI thread)
        new Handler(Looper.getMainLooper()).post(() -> {
            binding.textInputPassword.setError("Password might be wrong ...");
            binding.textInputUsername.setError("Username might be incorrect ...");
        });

    }

    @Override
    public void onLoginSuccess(Role role) {
        if (role == Role.ADMIN) {
            Intent intent = new Intent(LoginActivity.this, MainActivityAdmin.class);
            startActivity(intent);
            return;
        }
        if (role == Role.USER) {
            Intent intent = new Intent(this, MainActivityUser.class);
            startActivity(intent);
            return;
        }

    }

    public void onLoginButtonClicked(View view) {
        requestPermissions();
        if (EasyPermissions.hasPermissions(this, AppConstants.locationPermissions))
            viewModel.onClick(view);
    }

    @Override
    public void onPassword(String password) {
        try {
            SecurePreferences.setValue(this, "trustStorePass", password);
            SecurePreferences.setValue(this, "keyStorePass", password);
            initWebSocket();
        } catch (SecureStorageException e) {
            e.printStackTrace();
        }
    }

    private static class LoginAsyncTask extends AsyncTask<LoginUser, Void, Integer> {

        ProgressDialog progressDialog;
        Context context;

        @Override
        protected Integer doInBackground(LoginUser... loginUsers) {
            try {
                loginUsers[0].checkLogin();
            } catch (LoginListenerNotInitialisedException e) {
                return LOGIN_LISTENER_NOT_INITIALISED_CODE;
            } catch (EmptyTextException e) {
                return EMPTY_FIELD_CODE;
            }
            return OK_CODE;
        }

        @Override
        protected void onPostExecute(Integer integer) {
            if (integer == EMPTY_FIELD_CODE) {
                binding.textInputPassword.setError("Please enter some text here!");
                binding.textInputUsername.setError("Please enter some text here!");
            }
            progressDialog.dismiss();
        }

        @Override
        protected void onPreExecute() {
            progressDialog = new ProgressDialog(context);
            progressDialog.setTitle("Wait for login");
            progressDialog.show();
        }
    }
}

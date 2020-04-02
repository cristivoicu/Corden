package ro.atm.corden.view.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import ro.atm.corden.R;
import ro.atm.corden.databinding.ActivityLoginBinding;
import ro.atm.corden.model.LoginUser;
import ro.atm.corden.model.Roles;
import ro.atm.corden.util.exception.login.EmptyTextException;
import ro.atm.corden.util.exception.login.LoginListenerNotInitialisedException;
import ro.atm.corden.util.websocket.callback.LoginListener;
import ro.atm.corden.util.websocket.SignallingClient;
import ro.atm.corden.viewmodel.LoginViewModel;

import static ro.atm.corden.util.constant.ExceptionCodes.EMPTY_FIELD_CODE;
import static ro.atm.corden.util.constant.ExceptionCodes.LOGIN_LISTENER_NOT_INITIALISED_CODE;
import static ro.atm.corden.util.constant.ExceptionCodes.OK_CODE;

public class LoginActivity extends AppCompatActivity implements LoginListener {
    private LoginViewModel viewModel;
    private static ActivityLoginBinding binding;
    private static Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("Log", "onCreate");
        context = this;
        binding = DataBindingUtil.setContentView(this, R.layout.activity_login);

        viewModel = ViewModelProvider.AndroidViewModelFactory.getInstance(this.getApplication())
                .create(LoginViewModel.class);

        SignallingClient.getInstance().subscribeLoginListener(this);

        binding.setLifecycleOwner(this);
        binding.setViewmodel(viewModel);

        //set live data listeners
        viewModel.getUser().observe(this, (LoginUser loginUser) -> {
            Log.d("Log", "on observe clicked.");
            LoginAsyncTask loginAsyncTask = new LoginAsyncTask();
            loginAsyncTask.execute(loginUser);
        });
    }

    @Override
    public void onLoginError() {
        new Handler(Looper.getMainLooper()).post(() -> {
            binding.textInputPassword.setError("Invalid");
            binding.textInputUsername.setError("Invalid");
        });

    }

    @Override
    public void onLoginSuccess(Roles role) {
        if(role == Roles.ADMIN){
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
            return;
        }
        if(role == Roles.USER){
            Intent intent = new Intent(this, MainActivityUser.class);
            startActivity(intent);
            return;
        }

    }

    private static class LoginAsyncTask extends AsyncTask<LoginUser, Void, Integer> {

       ProgressDialog progressDialog;

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
            if(integer == EMPTY_FIELD_CODE){
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

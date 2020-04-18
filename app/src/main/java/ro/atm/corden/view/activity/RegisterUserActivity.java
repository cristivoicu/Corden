package ro.atm.corden.view.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TimePicker;

import java.util.Locale;

import ro.atm.corden.R;
import ro.atm.corden.databinding.ActivityRegisterUserBinding;
import ro.atm.corden.model.user.Role;
import ro.atm.corden.model.user.User;
import ro.atm.corden.util.exception.register.EmptyFieldException;
import ro.atm.corden.util.exception.register.InvalidPasswordException;
import ro.atm.corden.util.exception.register.LengthException;
import ro.atm.corden.util.exception.register.PhoneNumberException;
import ro.atm.corden.util.exception.websocket.UserNotLoggedInException;
import ro.atm.corden.util.websocket.Repository;
import ro.atm.corden.util.websocket.SignallingClient;
import ro.atm.corden.util.websocket.callback.EnrollListener;
import ro.atm.corden.view.fragment.TimePickerFragment;
import ro.atm.corden.viewmodel.RegisterViewModel;

import static ro.atm.corden.util.constant.ExceptionCodes.EMPTY_FIELD_CODE;
import static ro.atm.corden.util.constant.ExceptionCodes.INVALID_PASSWORD_CODE;
import static ro.atm.corden.util.constant.ExceptionCodes.INVALID_PHONE_NUMBER_CODE;
import static ro.atm.corden.util.constant.ExceptionCodes.OK_CODE;

public class RegisterUserActivity extends AppCompatActivity
        implements TimePickerDialog.OnTimeSetListener, EnrollListener {
    private static ActivityRegisterUserBinding binding;
    private RegisterViewModel mViewModel;

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
    protected void onStart() {
        super.onStart();
        try {
            SignallingClient.getInstance().subscribeEnrollListener(this);
        } catch (UserNotLoggedInException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        SignallingClient.getInstance().unsubscribeEnrollListener();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_register_user);

        mViewModel = ViewModelProvider.AndroidViewModelFactory
                .getInstance(this.getApplication())
                .create(RegisterViewModel.class);

        binding.setLifecycleOwner(this);
        binding.setViewModel(mViewModel);


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

        mViewModel.getUser().observe(this, new Observer<User>() {
            @Override
            public void onChanged(User user) {
                // setting error to null
                binding.textAccountName.setError(null);
                binding.userAddress.setError(null);
                binding.textInputUsername.setError(null);
                binding.userPassword.setError(null);
                binding.userPhoneNumber.setError(null);

                EnrollUserAsyncTask asyncTask = new EnrollUserAsyncTask();
                asyncTask.execute(user);

            }
        });

        binding.userPassword.setStartIconOnClickListener(v ->{
            binding.infoLayout.setVisibility(View.VISIBLE);
        });
    }

    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        if (isProgramStart) {
            binding.programStart.setText(String.format(Locale.getDefault(),"From: %02d:%02d", hourOfDay, minute));
            mViewModel.startHour.setValue(String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute));
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
            binding.programEnd.setText(String.format(Locale.getDefault(), "to: %02d:%02d", hourOfDay, minute));
            mViewModel.endHour.setValue(String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute));
        }
    }

    public void onUnderstandButtonClicked(View view) {
        binding.infoLayout.setVisibility(View.INVISIBLE);
    }

    private void refreshView() {
        binding.userPhoneNumber.getEditText().setText("");
        binding.userPassword.getEditText().setText("");
        binding.textInputUsername.getEditText().setText("");
        binding.userAddress.getEditText().setText("");
        binding.userPhoneNumber.getEditText().setText("");
        binding.textAccountName.getEditText().setText("");
        binding.programStart.setText("Start hour");
        binding.programEnd.setText("End hour");
    }

    @Override
    public void onEnrollSuccess() {
        // because response comes from a thread different from UI thread
        new Handler(Looper.getMainLooper())
                .post(() -> {
                    refreshView();
                    AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
                    dialogBuilder.setTitle("Information")
                            .setMessage("User was added to the database")
                            .setPositiveButton("OK", null);
                    AlertDialog alertDialog = dialogBuilder.create();
                    alertDialog.show();
                });
    }

    @Override
    public void onEnrollError() {

    }

    private static class EnrollUserAsyncTask extends AsyncTask<User, Void, Integer> {
        private boolean isUsernameEmpty = false;
        private boolean isPasswordEmpty = false;
        private boolean isPhoneNumberEmpty = false;
        private boolean isNameEmpty = false;
        private boolean isAddressEmpty = false;

        @Override
        protected Integer doInBackground(User... user) {
            try {
                user[0].validateData();
                Log.i("USER JSON", user[0].toJson());
                Repository.getInstance().enrollUser(user[0]);
            } catch (LengthException e) {
                // do nothing
            } catch (EmptyFieldException | NullPointerException e) {
                // check for empty fields and show errors
                if (user[0].isUsernameEmpty) {
                    isUsernameEmpty = true;
                }
                if (user[0].isAddressEmpty) {
                    isAddressEmpty = true;
                }
                if (user[0].isNameEmpty) {
                    isNameEmpty = true;
                }
                if (user[0].isPasswordEmpty) {
                    isPasswordEmpty = true;
                }
                if (user[0].isPhoneNumberEmpty) {
                    isPhoneNumberEmpty = true;
                }
                return EMPTY_FIELD_CODE;
            } catch (InvalidPasswordException e) {
                return INVALID_PASSWORD_CODE;
            } catch (PhoneNumberException e) {
                return INVALID_PHONE_NUMBER_CODE;
            }
            return OK_CODE;
        }

        @Override
        protected void onPostExecute(Integer integer) {
            switch (integer) {
                case EMPTY_FIELD_CODE:
                    if (isUsernameEmpty) {
                        binding.textAccountName.setError("You have to enter the username!");
                    }
                    if (isAddressEmpty) {
                        binding.userAddress.setError("You have to enter the user address!");
                    }
                    if (isNameEmpty) {
                        binding.textInputUsername.setError("You have to enter the user real name!");
                    }
                    if (isPasswordEmpty) {
                        binding.userPassword.setError("You have to enter the user password!");
                    }
                    if (isPhoneNumberEmpty) {
                        binding.userPhoneNumber.setError("You have to enter the user phone number!");
                    }
                    break;
                case INVALID_PASSWORD_CODE:
                    binding.userPassword.setError("Password does not meet the criteria!");
                    break;
                case INVALID_PHONE_NUMBER_CODE:
                    binding.userPhoneNumber.setError("Phone number length is too short!");
                    break;
            }
        }
    }
}

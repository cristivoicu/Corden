package ro.atm.corden.viewmodel;

import android.app.Application;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import java.util.Date;

import ro.atm.corden.model.user.Role;
import ro.atm.corden.model.user.User;

public class RegisterViewModel extends AndroidViewModel {
    public MutableLiveData<String> userName = new MutableLiveData<>();
    public MutableLiveData<String> password = new MutableLiveData<>();

    public MutableLiveData<String> userRealName = new MutableLiveData<>();
    public MutableLiveData<String> userAddress = new MutableLiveData<>();
    public MutableLiveData<String> userPhoneNumber = new MutableLiveData<>();

    public MutableLiveData<Role> userRole = new MutableLiveData<>();
    public MutableLiveData<String> startHour = new MutableLiveData<>();
    public MutableLiveData<String> endHour = new MutableLiveData<>();

    private MutableLiveData<User> user = new MutableLiveData<>();

    public RegisterViewModel(@NonNull Application application) {
        super(application);
    }

    public MutableLiveData<User> getUser() {

        if (user == null) {
            user = new MutableLiveData<>();
        }
        return user;
    }

    public void onClick(View view){
        User user = new User(userName.getValue(),
                             password.getValue(),
                             userPhoneNumber.getValue(),
                             userRealName.getValue(),
                             userAddress.getValue(),
                             startHour.getValue(),
                             endHour.getValue(),
                             new Date(),
                             userRole.getValue());
        this.user.setValue(user);
    }
}

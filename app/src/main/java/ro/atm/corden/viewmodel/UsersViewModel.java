package ro.atm.corden.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import java.util.List;

import ro.atm.corden.model.user.User;
import ro.atm.corden.util.websocket.Repository;

public class UsersViewModel extends AndroidViewModel {
    private MutableLiveData<List<User>> allUsers;

    public UsersViewModel(@NonNull Application application) {
        super(application);
        allUsers = new MutableLiveData<>();

    }

    public void setAllUsers(){
        allUsers.setValue(Repository.getInstance().requestAllUsers());
    }

    public MutableLiveData<List<User>> getAllUsers() {
        return allUsers;
    }
}

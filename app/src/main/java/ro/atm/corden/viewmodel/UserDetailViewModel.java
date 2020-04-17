package ro.atm.corden.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import java.util.Locale;

import ro.atm.corden.model.user.User;
import ro.atm.corden.util.websocket.Repository;

public class UserDetailViewModel extends AndroidViewModel {
    private String userRealName;
    private String userAddress;
    private String userPhoneNumber;
    private String username;
    private String userProgram;
    private String role;

    public UserDetailViewModel(@NonNull Application application) {
        super(application);
    }

    public String getUserRealName() {
        return userRealName;
    }

    public void setUserRealName(String userRealName) {
        this.userRealName = userRealName;
    }

    public String getUserAddress() {
        return userAddress;
    }

    public void setUserAddress(String userAddress) {
        this.userAddress = userAddress;
    }

    public String getUserPhoneNumber() {
        return userPhoneNumber;
    }

    public void setUserPhoneNumber(String userPhoneNumber) {
        this.userPhoneNumber = userPhoneNumber;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUserProgram() {
        return userProgram;
    }

    public void setUserProgram(String userProgram) {
        this.userProgram = userProgram;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public void setData(User user){
        userRealName = user.getName();
        userAddress = user.getAddress();
        userPhoneNumber = user.getPhoneNumber();
        username = user.getUsername();
        userProgram = String.format(Locale.getDefault(),
                "Program from %s to %s",
                user.getProgramStart(),
                user.getProgramEnd());
        role = user.getRoles();
    }

    public void diseableUser(){
        Repository.getInstance().disableUser(this.username);
    }
}

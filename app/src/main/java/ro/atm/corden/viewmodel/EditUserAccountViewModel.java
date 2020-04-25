package ro.atm.corden.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import ro.atm.corden.model.user.User;
import ro.atm.corden.util.websocket.Repository;

public class EditUserAccountViewModel extends AndroidViewModel {
    private String name = "";
    private String address = "";
    private String phoneNumber = "";

    private String username = "";
    private String password = "";

    private User user;

    public EditUserAccountViewModel(@NonNull Application application) {
        super(application);
    }

    public void setData(String username){
        user =  Repository.getInstance().requestUserData(username);
        name = user.getName();
        address = user.getAddress();
        phoneNumber = user.getPhoneNumber();
        this.username = user.getUsername();
        this.password = user.getPassword();
    }

    public void modifyData(){
        user.setName(name);
        user.setAddress(address);
        user.setPhoneNumber(phoneNumber);
        user.setPassword(password);
        Repository.getInstance().updateUser(user);
    }

    //region Getters and setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
    //endregion
}

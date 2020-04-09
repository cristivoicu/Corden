package ro.atm.corden.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import ro.atm.corden.model.user.Role;
import ro.atm.corden.model.user.User;
import ro.atm.corden.util.websocket.Repository;

public class EditUserViewModel extends AndroidViewModel {

    private String name = "";
    private String address = "";
    private String phoneNumber = "";

    private String username = "";
    private String password = "";

    private Role role;
    private String startHour = "";
    private String endHour = "";

    private boolean isAdmin;
    private boolean isUser;

    private User user = null;

    public EditUserViewModel(@NonNull Application application) {
        super(application);
    }

    public void init(User user){
        name = user.getName();
        address = user.getAddress();
        phoneNumber = user.getPhoneNumber();
        username = user.getUsername();
        password = user.getPassword();
        role = user.getRoles();
        startHour = user.getProgramStart();
        endHour = user.getProgramEnd();

        switch (role){
            case ADMIN:
                isAdmin= true;
                isUser = false;
                break;
            case USER:
                isUser = true;
                isAdmin = false;
                break;
            default:
                isUser = false;
                isAdmin = false;
        }

        this.user = user;
    }

    public void modifyUser(){
        user.setUsername(username);
        user.setAddress(address);
        user.setPhoneNumber(phoneNumber);
        user.setName(name);
        user.setPassword(password);
        user.setProgramStart(startHour);
        user.setProgramEnd(endHour);
        if(isAdmin){
            user.setRoles(Role.ADMIN);
        }
        if(isUser){
            user.setRoles(Role.USER);
        }

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

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getStartHour() {
        return startHour;
    }

    public void setStartHour(String startHour) {
        this.startHour = startHour;
    }

    public String getEndHour() {
        return endHour;
    }

    public void setEndHour(String endHour) {
        this.endHour = endHour;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public void setAdmin(boolean admin) {
        isAdmin = admin;
    }

    public boolean isUser() {
        return isUser;
    }

    public void setUser(boolean user) {
        isUser = user;
    }

    //endregion
}

package ro.atm.corden.model.user;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.Date;

import ro.atm.corden.util.exception.register.EmptyFieldException;
import ro.atm.corden.util.exception.register.InvalidPasswordException;
import ro.atm.corden.util.exception.register.LengthException;
import ro.atm.corden.util.exception.register.PhoneNumberException;

public class User implements Serializable {
    public User(String username, String password, String phoneNumber, String name, String address, String programStart, String programEnd, Date creationDate, Role roles) {
        this.username = username;
        this.password = password;
        this.phoneNumber = phoneNumber;
        this.name = name;
        this.address = address;
        this.programStart = programStart;
        this.programEnd = programEnd;
        this.creationDate = creationDate;
        this.roles = roles;
    }

    @Expose(serialize = false, deserialize = false)
    public transient boolean isUsernameEmpty;
    @Expose(serialize = false, deserialize = false)
    public transient boolean isPasswordEmpty;
    @Expose(serialize = false, deserialize = false)
    public transient boolean isPhoneNumberEmpty;
    @Expose(serialize = false, deserialize = false)
    public transient boolean isNameEmpty;
    @Expose(serialize = false, deserialize = false)
    public transient boolean isAddressEmpty;

//    public boolean isProgramStartEmpty;
//    public boolean isProgramEndEmpty;
//    public boolean isRoleEmpty;

    private String username;
    private String password;

    private String phoneNumber;
    private String name;
    private String address;

    private String programStart;
    private String programEnd;
    private Date creationDate;

    private boolean isOnline;

    @SerializedName("role")
    private Role roles;

    public String getUsername() {
        return username;
    }

    /**
     * Method used to validate user data, it throws an exception if something is wrong.
     * This method turn the fields flag true.
     *
     * @throws LengthException          if any filed exceeds the length
     * @throws EmptyFieldException      if a filed is empty, also sets the flag of the field on true
     * @throws InvalidPasswordException if password field does not meet the criteria
     * @throws PhoneNumberException     if the phone number field does not meet the criteria
     * @author Cristian VOICU
     */
    public void validateData() throws LengthException, EmptyFieldException, InvalidPasswordException, PhoneNumberException {
        try {
            if (username.trim().isEmpty()) {
                isUsernameEmpty = true;
                throw new EmptyFieldException();
            }
        } catch (NullPointerException e) {
            isUsernameEmpty = true;
        }

        try {

            if (password.trim().isEmpty()) {
                isPasswordEmpty = true;
                throw new EmptyFieldException();
            }
        } catch (NullPointerException e) {
            isPasswordEmpty = true;
        }
        try {
            if (phoneNumber.trim().isEmpty()) {
                isPhoneNumberEmpty = true;
                throw new EmptyFieldException();
            }
        } catch (NullPointerException e) {
            isPhoneNumberEmpty = true;
        }
        try {
            if (name.trim().isEmpty()) {
                isNameEmpty = true;
                throw new EmptyFieldException();
            }
        } catch (NullPointerException e) {
            isNameEmpty = true;
        }
        try {

            if (address.trim().isEmpty()) {
                isAddressEmpty = true;
                throw new EmptyFieldException();
            }
        } catch (NullPointerException e) {
            isAddressEmpty = true;
        }
        if (username.length() > 50 ||
                address.length() > 150 ||
                phoneNumber.length() > 10 ||
                name.length() > 50 ||
                password.length() > 255) {
            throw new LengthException();
        }
        if (phoneNumber.length() < 10) {
            throw new PhoneNumberException();
        }
        String pattern = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}$";

        if (!password.matches(pattern) || password.length() < 8) {
            throw new InvalidPasswordException();
        }

    }

    //region Getters and setters
    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

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

    public String getProgramStart() {
        return programStart;
    }

    public void setProgramStart(String programStart) {
        this.programStart = programStart;
    }

    public String getProgramEnd() {
        return programEnd;
    }

    public void setProgramEnd(String programEnd) {
        this.programEnd = programEnd;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public Role getRoles() {
        return roles;
    }

    public void setRoles(Role roles) {
        this.roles = roles;
    }

    public boolean isOnline() {
        return isOnline;
    }

    //endregion

    public String toJson() {
        Gson gson = new GsonBuilder().setDateFormat("MMM dd, yyyy, h:mm:ss a").setPrettyPrinting().create();
        return gson.toJson(this, User.class);
    }

    public static User fromJson(String json){
        Gson gson = new GsonBuilder().setDateFormat("MMM dd, yyyy, h:mm:ss a").setPrettyPrinting().create();
        return gson.fromJson(json, User.class);
    }
}

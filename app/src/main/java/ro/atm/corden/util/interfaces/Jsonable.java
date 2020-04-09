package ro.atm.corden.util.interfaces;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import ro.atm.corden.model.user.User;

public interface Jsonable {
    Gson gson = new GsonBuilder().setDateFormat("MMM dd, yyyy, h:mm:ss a").setPrettyPrinting().create();

    String toJson();
}

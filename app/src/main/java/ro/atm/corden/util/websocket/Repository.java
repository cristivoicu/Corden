package ro.atm.corden.util.websocket;

import android.os.AsyncTask;

import java.util.List;
import java.util.concurrent.ExecutionException;

import ro.atm.corden.model.transport_model.User;

/**
 * This class is used to get data from the server
 * Singleton class
 */
public class Repository {

    private static Repository INSTANCE = new Repository();

    private Repository() {

    }

    public static Repository getInstance() {
        return INSTANCE;
    }

    private static SignallingClient signallingClient = SignallingClient.getInstance();

    public List<User> requestAllUsers() {
        RequestUsersAsyncTask requestUsersAsyncTask = new RequestUsersAsyncTask();
        try {
            return requestUsersAsyncTask.execute().get();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static class RequestUsersAsyncTask extends AsyncTask<Void, Void, List<User>> {

        @Override
        protected List<User> doInBackground(Void... voids) {
            return signallingClient.getAllUsersRequest();
        }
    }

}

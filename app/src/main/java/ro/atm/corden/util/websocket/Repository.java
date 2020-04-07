package ro.atm.corden.util.websocket;

import android.os.AsyncTask;
import android.view.View;

import java.util.List;
import java.util.concurrent.ExecutionException;

import ro.atm.corden.model.transport_model.Action;
import ro.atm.corden.model.transport_model.User;
import ro.atm.corden.model.transport_model.Video;
import ro.atm.corden.util.exception.websocket.TransportException;

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

    public void updateUser(User user){
        UpdateUserAsyncTask updateUserAsyncTask = new UpdateUserAsyncTask();

        updateUserAsyncTask.execute(user);
    }

    public List<User> requestAllUsers() {
        RequestUsersAsyncTask requestUsersAsyncTask = new RequestUsersAsyncTask();
        try {
            return requestUsersAsyncTask.execute().get();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<Video> requestVideosForUsername(String username){
        RequestVideosAsyncTask requestVideosAsyncTask = new RequestVideosAsyncTask();
        try {
            return requestVideosAsyncTask.execute(username).get();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<Action> requestTimelineForUserOnDate(String username, String date){
        RequestTimelineAsyncTask requestTimelineAsyncTask = new RequestTimelineAsyncTask();
        try {
            return requestTimelineAsyncTask.execute(username, date).get();
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

    private static class RequestVideosAsyncTask extends AsyncTask<String, Void, List<Video>>{

        @Override
        protected List<Video> doInBackground(String... usernames) {
            try {
                return signallingClient.getVideosForUser(usernames[0]);
            } catch (TransportException e) {
                return null;
            }
        }

        @Override
        protected void onPostExecute(List<Video> videos) {
            super.onPostExecute(videos);
        }
    }

    private static class RequestTimelineAsyncTask extends AsyncTask<String, Void, List<Action>>{

        @Override
        protected List<Action> doInBackground(String... strings) {
            return signallingClient.getTimelineForUser(strings[0], strings[1]);
        }
    }

    private static class UpdateUserAsyncTask extends  AsyncTask<User, Void, Void>{

        @Override
        protected Void doInBackground(User... users) {
            signallingClient.updateUser(users[0]);
            return null;
        }
    }
}

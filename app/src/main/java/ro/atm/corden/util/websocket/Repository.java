package ro.atm.corden.util.websocket;

import android.os.AsyncTask;
import android.os.ConditionVariable;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.concurrent.ExecutionException;

import ro.atm.corden.model.map.MapItem;
import ro.atm.corden.model.user.Action;
import ro.atm.corden.model.user.User;
import ro.atm.corden.model.video.Video;
import ro.atm.corden.util.exception.websocket.TransportException;

import static ro.atm.corden.util.constant.JsonConstants.ID;
import static ro.atm.corden.util.constant.JsonConstants.ID_LIST_USERS;
import static ro.atm.corden.util.constant.JsonConstants.TYPE;
import static ro.atm.corden.util.constant.JsonConstants.USERS_TYPE_REQ_ALL;

/**
 * This class is used to get data from the server
 * Singleton class
 */
public class Repository {
    private static final String TAG = "Repository";
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

    public List<User> requestUsersWithUserRole(){
        RequestUsersWithUserRoleAsyncTask requestUsersWithUserRoleAsyncTask = new RequestUsersWithUserRoleAsyncTask();
        try {
            return requestUsersWithUserRoleAsyncTask.execute().get();
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

    public void saveMapItems(List<MapItem> items){
        SendMapItemsToServerAsyncTask sendMapItemsToServerAsyncTask = new SendMapItemsToServerAsyncTask();
        sendMapItemsToServerAsyncTask.execute(items);
    }

    private static class RequestUsersAsyncTask extends AsyncTask<Void, Void, List<User>> {
        @Override
        protected List<User> doInBackground(Void... voids) {
            //return signallingClient.getAllUsersRequest();
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put(ID, ID_LIST_USERS);
                jsonObject.put(TYPE, USERS_TYPE_REQ_ALL);

                Log.i(TAG, "Get all users event: " + jsonObject.toString());
                signallingClient.webSocket.send(jsonObject.toString());
                signallingClient.webSocket.usersConditionVariable = new ConditionVariable(false);

                signallingClient.webSocket.usersConditionVariable.block();
                if (signallingClient.webSocket.users != null) {
                    return signallingClient.webSocket.users;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
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

    private static class RequestUsersWithUserRoleAsyncTask extends AsyncTask<Void, Void, List<User>>{

        @Override
        protected List<User>doInBackground(Void... voids) {
            return  signallingClient.getUsersRequestWithUserRole();
        }
    }

    private static class SendMapItemsToServerAsyncTask extends AsyncTask<List<MapItem>, Void, Void>{

        @Override
        protected Void doInBackground(List<MapItem>... lists) {
            SignallingClient.getInstance().saveMapItems(lists[0]);
            return null;
        }
    }

}

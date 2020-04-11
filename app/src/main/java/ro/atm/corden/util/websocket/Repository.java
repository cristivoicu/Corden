package ro.atm.corden.util.websocket;

import android.os.AsyncTask;
import android.os.ConditionVariable;
import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.concurrent.ExecutionException;

import ro.atm.corden.model.map.MapItem;
import ro.atm.corden.model.map.Mark;
import ro.atm.corden.model.map.Path;
import ro.atm.corden.model.map.Zone;
import ro.atm.corden.model.user.Action;
import ro.atm.corden.model.user.User;
import ro.atm.corden.model.video.Video;
import ro.atm.corden.util.exception.websocket.TransportException;

import static ro.atm.corden.util.constant.JsonConstants.ID;
import static ro.atm.corden.util.constant.JsonConstants.ID_LIST_USERS;
import static ro.atm.corden.util.constant.JsonConstants.ID_UPDATE_USER;
import static ro.atm.corden.util.constant.JsonConstants.REQ_USER_TIMELINE;
import static ro.atm.corden.util.constant.JsonConstants.TYPE;
import static ro.atm.corden.util.constant.JsonConstants.USER;
import static ro.atm.corden.util.constant.JsonConstants.USERS_TYPE_REQ_ALL;
import static ro.atm.corden.util.constant.JsonConstants.USERS_TYPE_REQ_USER_ROLE;

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

    public void updateUser(User user) {
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

    public List<Video> requestVideosForUsername(String username) {
        RequestVideosAsyncTask requestVideosAsyncTask = new RequestVideosAsyncTask();
        try {
            return requestVideosAsyncTask.execute(username).get();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<Action> requestTimelineForUserOnDate(String username, String date) {
        RequestTimelineAsyncTask requestTimelineAsyncTask = new RequestTimelineAsyncTask();
        try {
            return requestTimelineAsyncTask.execute(username, date).get();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void saveMapItems(List<MapItem> items) {
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

    private static class RequestVideosAsyncTask extends AsyncTask<String, Void, List<Video>> {

        @Override
        protected List<Video> doInBackground(String... usernames) {
            try {
                JsonObject message = new JsonObject();

                message.addProperty(ID, "recordedVideos");
                message.addProperty("forUser", usernames[0]);

                signallingClient.webSocket.send(message.toString());
                signallingClient.webSocket.videosConditionVariable = new ConditionVariable(false);
                signallingClient.webSocket.videosConditionVariable.block();
                if (signallingClient.webSocket.videos != null) {
                    return signallingClient.webSocket.videos;
                }

                throw new TransportException("Could not get the requested videos!");
            } catch (TransportException e) {
                return null;
            }
        }

        @Override
        protected void onPostExecute(List<Video> videos) {
            super.onPostExecute(videos);
        }
    }

    private static class RequestTimelineAsyncTask extends AsyncTask<String, Void, List<Action>> {

        @Override
        protected List<Action> doInBackground(String... strings) {
            JsonObject message = new JsonObject();

            message.addProperty(ID, REQ_USER_TIMELINE);
            message.addProperty("forUser", strings[0]);
            message.addProperty("date", strings[1]);

            signallingClient.webSocket.send(message.toString());

            signallingClient.webSocket.timelineConditionVariable = new ConditionVariable(false);
            signallingClient.webSocket.timelineConditionVariable.block();
            if (signallingClient.webSocket.actions != null) {
                return signallingClient.webSocket.actions;
            }
            return null;
        }
    }

    private static class UpdateUserAsyncTask extends AsyncTask<User, Void, Void> {

        @Override
        protected Void doInBackground(User... users) {
            JsonObject message = new JsonObject();
            message.addProperty(ID, ID_UPDATE_USER);
            message.addProperty(USER, users[0].toJson());

            signallingClient.webSocket.send(message.toString());
            return null;
        }
    }

    private static class RequestUsersWithUserRoleAsyncTask extends AsyncTask<Void, Void, List<User>> {

        @Override
        protected List<User> doInBackground(Void... voids) {
            JsonObject jsonObject = new JsonObject();

            jsonObject.addProperty(ID, ID_LIST_USERS);
            jsonObject.addProperty(TYPE, USERS_TYPE_REQ_USER_ROLE);

            Log.i(TAG, "Get all users event: " + jsonObject.toString());
            signallingClient.webSocket.send(jsonObject.toString());
            signallingClient.webSocket.usersConditionVariable = new ConditionVariable(false);

            signallingClient.webSocket.usersConditionVariable.block();
            if (signallingClient.webSocket.users != null) {
                return signallingClient.webSocket.users;
            }

            return null;
        }
    }

    private static class SendMapItemsToServerAsyncTask extends AsyncTask<List<MapItem>, Void, Void> {

        @Override
        protected Void doInBackground(List<MapItem>... lists) {
            Log.i(TAG, "Sending map items to the server.");

            JsonObject message = new JsonObject();

            message.addProperty(ID, "saveMapItems");

            JsonArray marks = new JsonArray();
            JsonArray paths = new JsonArray();
            JsonArray zones = new JsonArray();

            for (MapItem mapItem : lists[0]){
                String json = "";
                if(mapItem instanceof Mark){
                    json = mapItem.toJson();
                    marks.add(json);
                }
                if(mapItem instanceof Zone){
                    json = mapItem.toJson();
                    paths.add(json);
                }
                if(mapItem instanceof Path){
                    json = mapItem.toJson();
                    zones.add(json);
                }
            }
            message.add("marks", marks);
            message.add("paths", paths);
            message.add("zones", zones);

            signallingClient.webSocket.send(message.toString());
            return null;
        }
    }

}

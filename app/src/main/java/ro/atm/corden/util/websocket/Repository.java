package ro.atm.corden.util.websocket;

import android.os.AsyncTask;
import android.os.ConditionVariable;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.ReferenceQueue;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

import ro.atm.corden.model.map.MapItem;
import ro.atm.corden.model.user.Action;
import ro.atm.corden.model.user.LiveStreamer;
import ro.atm.corden.model.user.User;
import ro.atm.corden.model.video.Video;
import ro.atm.corden.util.exception.websocket.TransportException;
import ro.atm.corden.util.websocket.protocol.Message;
import ro.atm.corden.util.websocket.protocol.events.RequestEventTypes;
import ro.atm.corden.util.websocket.protocol.events.UpdateEventType;

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

    public void disableUser(String username) {
        MessageToDisableUserAsyncTask messageToDisableUserAsyncTask = new MessageToDisableUserAsyncTask();
        messageToDisableUserAsyncTask.execute(username);
    }

    public User requestUserData(String username) {
        RequestUserAsyncTask requestUserAsyncTask = new RequestUserAsyncTask();
        try {
            return requestUserAsyncTask.execute(username).get();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }
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

    public void requestLiveLocation(){
        RequestLiveLocationsAsyncTask requestLiveLocationsAsyncTask = new RequestLiveLocationsAsyncTask();
        requestLiveLocationsAsyncTask.execute();
    }


    public List<User> requestOnlineUsers() {
        RequestUsersOnlineAsyncTask requestUsersOnlineAsyncTask = new RequestUsersOnlineAsyncTask();
        try {
            return requestUsersOnlineAsyncTask.execute().get();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<LiveStreamer> requestLiveStreamers() {
        RequestLiveStreamerAsyncTask requestLiveStreamerAsyncTask = new RequestLiveStreamerAsyncTask();
        try {
            return requestLiveStreamerAsyncTask.execute().get();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<MapItem> requestMapItems(){
        RequestMapItemsAsyncTask requestMapItemsAsyncTask = new RequestMapItemsAsyncTask();
        try {
            return requestMapItemsAsyncTask.execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<Video> requestVideosForUsername(String username, String date) {
        RequestVideosAsyncTask requestVideosAsyncTask = new RequestVideosAsyncTask();
        try {
            return requestVideosAsyncTask.execute(username, date).get();
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

    public List<Action> requestServerLogOnDate(String date) {
        RequestServerLogAsyncTask requestServerLogAsyncTask = new RequestServerLogAsyncTask();
        try {
            return requestServerLogAsyncTask.execute(date).get();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void enrollUser(User user) {
        EnrollUserAsyncTask enrollUserAsyncTask = new EnrollUserAsyncTask();
        enrollUserAsyncTask.execute(user);
    }

    public void saveMapItems(HashMap<String, MapItem> items) {
        SendMapItemsToServerAsyncTask sendMapItemsToServerAsyncTask = new SendMapItemsToServerAsyncTask();
        sendMapItemsToServerAsyncTask.execute(items);
    }

    public void subscribeToMapItemsChanges() {
        SubscribeToMapChangesAsyncTask subscribeToMapChangesAsyncTask = new SubscribeToMapChangesAsyncTask();
        subscribeToMapChangesAsyncTask.execute();
    }

    public void unsubscribeToMapItemsChanges() {
        UnsubscribeToMapChangesAsyncTask unsubscribeToMapChangesAsyncTask = new UnsubscribeToMapChangesAsyncTask();
        unsubscribeToMapChangesAsyncTask.execute();
    }

    private static class RequestUsersAsyncTask extends AsyncTask<Void, Void, List<User>> {
        @Override
        protected List<User> doInBackground(Void... voids) {

            Message message = new Message.RequestMessageBuilder()
                    .addEvent(RequestEventTypes.USERS_ALL)
                    .build();

            signallingClient.webSocket.send(message.toString());

            signallingClient.webSocket.usersConditionVariable = new ConditionVariable(false);
            signallingClient.webSocket.usersConditionVariable.block();

            if (signallingClient.webSocket.users != null) {
                return signallingClient.webSocket.users;
            }
            return null;
        }
    }

    private static class RequestUsersOnlineAsyncTask extends AsyncTask<Void, Void, List<User>> {

        @Override
        protected List<User> doInBackground(Void... voids) {
            Message message = new Message.RequestMessageBuilder()
                    .addEvent(RequestEventTypes.USERS_ONLINE)
                    .build();

            signallingClient.webSocket.send(message.toString());

            signallingClient.webSocket.usersConditionVariable = new ConditionVariable(false);
            signallingClient.webSocket.usersConditionVariable.block();

            if (signallingClient.webSocket.users != null) {
                return signallingClient.webSocket.users;
            }
            return null;
        }
    }

    private static class RequestUserAsyncTask extends AsyncTask<String, Void, User> {

        @Override
        protected User doInBackground(String... strings) {
            Message message = new Message.RequestMessageBuilder()
                    .addEvent(RequestEventTypes.USER_DATA)
                    .addUser(strings[0])
                    .build();

            signallingClient.webSocket.send(message.toString());

            signallingClient.webSocket.userDataConditionVariable = new ConditionVariable(false);
            signallingClient.webSocket.userDataConditionVariable.block();
            return signallingClient.webSocket.users.get(0);
        }
    }

    private static class RequestLiveStreamerAsyncTask extends AsyncTask<Void, Void, List<LiveStreamer>> {

        @Override
        protected List<LiveStreamer> doInBackground(Void... voids) {
            Message message = new Message.RequestMessageBuilder()
                    .addEvent(RequestEventTypes.REQUEST_LIVE_STREAMER)
                    .build();
            signallingClient.webSocket.send(message.toString());

            signallingClient.webSocket.userDataConditionVariable = new ConditionVariable(false);
            signallingClient.webSocket.userDataConditionVariable.block();
            return signallingClient.webSocket.liveStreamers;
        }
    }

    private static class RequestVideosAsyncTask extends AsyncTask<String, Void, List<Video>> {

        @Override
        protected List<Video> doInBackground(String... data) {
            try {
                Message.RequestMessageBuilder message = new Message.RequestMessageBuilder()
                        .addEvent(RequestEventTypes.RECORDED_VIDEOS)
                        .addUser(data[0]);
                if(data[1] != null) {
                    message.addDate(data[1]);
                }

                signallingClient.webSocket.send(message.build().toString());

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
            Message message = new Message.RequestMessageBuilder()
                    .addEvent(RequestEventTypes.TIMELINE)
                    .addUser(strings[0])
                    .addDate(strings[1])
                    .build();

            signallingClient.webSocket.send(message.toString());

            signallingClient.webSocket.timelineConditionVariable = new ConditionVariable(false);
            signallingClient.webSocket.timelineConditionVariable.block();
            if (signallingClient.webSocket.actions != null) {
                return signallingClient.webSocket.actions;
            }
            return null;
        }
    }

    private static class RequestServerLogAsyncTask extends AsyncTask<String, Void, List<Action>> {
        @Override
        protected List<Action> doInBackground(String... strings) {
            Message message = new Message.RequestMessageBuilder()
                    .addEvent(RequestEventTypes.SERVER_LOG)
                    .addDate(strings[0])
                    .build();
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
            Message message = new Message.UpdateMessageBuilder()
                    .addEvent(UpdateEventType.UPDATE_USER)
                    .addPayload(users[0])
                    .build();

            signallingClient.webSocket.send(message.toString());
            return null;
        }
    }

    private static class SendMapItemsToServerAsyncTask extends AsyncTask<HashMap<String, MapItem>, Void, Void> {

        @Override
        protected Void doInBackground(HashMap<String, MapItem>... lists) {
            Log.i(TAG, "Sending map items to the server.");

            Message message = new Message.UpdateMessageBuilder()
                    .addEvent(UpdateEventType.MAP_ITEMS)
                    .addPayload(lists[0])
                    .build();

            signallingClient.webSocket.send(message.toString());
            return null;
        }
    }

    private static class RequestMapItemsAsyncTask extends AsyncTask<Void, Void, List<MapItem>>{

        @Override
        protected List<MapItem> doInBackground(Void... voids) {
            Log.i(TAG, "Sending request for map items...");

            Message message = new Message.RequestMessageBuilder()
                    .addEvent(RequestEventTypes.REQUEST_MAP_ITEMS)
                    .build();

            signallingClient.webSocket.send(message.toString());

            signallingClient.webSocket.conditionVariable = new ConditionVariable(false);
            signallingClient.webSocket.conditionVariable.block();

            if(signallingClient.webSocket.mapItems != null){
                return signallingClient.webSocket.mapItems;
            }

            return null;
        }
    }

    private static class EnrollUserAsyncTask extends AsyncTask<User, Void, Void> {

        @Override
        protected Void doInBackground(User... users) {
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("method", "update");
                jsonObject.put("event", "enroll");
                jsonObject.put("payload", users[0].toJson());

                Log.i(TAG, "Enroll event: " + jsonObject.toString());

                signallingClient.webSocket.send(jsonObject.toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    private static class MessageToDisableUserAsyncTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... strings) {
            Message message = new Message.UpdateMessageBuilder()
                    .addEvent(UpdateEventType.DISABLE_USER)
                    .addPayload(strings[0])
                    .build();

            signallingClient.webSocket.send(message.toString());
            return null;
        }
    }

    private static class RequestLiveLocationsAsyncTask extends AsyncTask<Void, Void, Void>{

        @Override
        protected Void doInBackground(Void... voids) {
            Message message = new Message.RequestMessageBuilder()
                    .addEvent(RequestEventTypes.REQUEST_LIVE_LOCATION)
                    .build();
            signallingClient.webSocket.send(message.toString());
            return null;
        }
    }

    private static class SubscribeToMapChangesAsyncTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            SignallingClient.getInstance().sendMessageToSubscribeToMapItems();
            return null;
        }
    }

    private static class UnsubscribeToMapChangesAsyncTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            SignallingClient.getInstance().sendMessageToUnsubscribeFromMapItems();
            return null;
        }
    }
}

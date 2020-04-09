package ro.atm.corden.util.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import ro.atm.corden.R;
import ro.atm.corden.model.user.User;

public class UserMapAdapter extends RecyclerView.Adapter<UserMapAdapter.UserMapViewHolder> {

    private List<User> users = new ArrayList<>();

    public void setUsers(List<User> users){
        this.users = users;
    }

    @NonNull
    @Override
    public UserMapViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_users_map,
                parent, false);
        return new UserMapViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull UserMapViewHolder holder, int position) {
        User currentUser = users.get(position);

        holder.username.setText(currentUser.getUsername());
        holder.name.setText(currentUser.getName());

    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    class UserMapViewHolder extends RecyclerView.ViewHolder{

        private CheckBox checkBox;
        private TextView username;
        private TextView name;

        public UserMapViewHolder(@NonNull View itemView) {
            super(itemView);

            checkBox = itemView.findViewById(R.id.checkbox);
            username = itemView.findViewById(R.id.username);
            name = itemView.findViewById(R.id.name);
        }
    }
}

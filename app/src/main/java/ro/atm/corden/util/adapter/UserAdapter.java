package ro.atm.corden.util.adapter;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import ro.atm.corden.R;
import ro.atm.corden.model.user.Status;
import ro.atm.corden.model.user.User;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserHolder> {
    private List<User> users = new ArrayList<>();
    private OnItemClickListener listener;

    public void setUsers(List<User> users) {
        this.users = users;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public UserHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_users, parent, false);
        return new UserHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull UserHolder holder, int position) {
        User currentUser = users.get(position);
        holder.name.setText(currentUser.getName());
        holder.username.setText(currentUser.getUsername());
        holder.program.setText(String.format("Program: %s to %s.", currentUser.getProgramStart(), currentUser.getProgramEnd()));

        if(currentUser.getStatus().equals(Status.ONLINE.name())){
            holder.status.setCardBackgroundColor(0xFF006400); // color online
        }if(currentUser.getStatus().equals(Status.OFFLINE.name())){
            holder.status.setCardBackgroundColor(0xFF696969); // color offline
        }if(currentUser.getStatus().equals(Status.DISABLED.name())){
            holder.status.setCardBackgroundColor(0xFF8B0000); // color disabled
        }
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    class UserHolder extends RecyclerView.ViewHolder {
        private TextView name;
        private TextView username;
        private TextView program;
        private CardView status;

        private UserHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.name);
            username = itemView.findViewById(R.id.username);
            program = itemView.findViewById(R.id.program);
            status = itemView.findViewById(R.id.onlineStatus);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();
                    if (listener != null && position != RecyclerView.NO_POSITION) {
                        listener.onItemClick(users.get(position));
                    }
                }
            });
        }
    }

    public interface OnItemClickListener {
        void onItemClick(User user);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }
}

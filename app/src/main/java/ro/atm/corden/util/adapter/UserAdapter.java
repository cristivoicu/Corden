package ro.atm.corden.util.adapter;

import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import ro.atm.corden.R;
import ro.atm.corden.model.user.Role;
import ro.atm.corden.model.user.Status;
import ro.atm.corden.model.user.User;
import ro.atm.corden.util.websocket.Repository;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserHolder>
        implements Filterable {
    final private List<User> users = new LinkedList<>();
    private List<User> allUsers;
    private OnItemClickListener listener;

    private int mPosition;

    public void setUsers(List<User> users) {
        this.users.clear();
        this.users.addAll(users);
        allUsers = new LinkedList<>(users);
        notifyDataSetChanged();
    }

    public void updateStatus(String username, Status newStatus) {
        for (User user : users) {
            if (user.getUsername().equals(username)) {
                user.setStatus(newStatus.name());
                break;
            }
        }
        for (User user : allUsers) {
            if (user.getUsername().equals(username)) {
                user.setStatus(newStatus.name());
                break;
            }
        }
        notifyDataSetChanged();
    }

    public void updateStatusOnOnlineActivity(String username, Status newStatus) {
        if (newStatus.equals(Status.ONLINE)) {
            User newUser = Repository.getInstance().requestUserData(username);
            this.users.add(newUser);
            this.allUsers.add(newUser);
            notifyDataSetChanged();
            return;
        }
        for (User user : this.users) {
            if (user.getUsername().equals(username)) {
                if (newStatus.equals(Status.OFFLINE)) {
                    this.users.remove(user);
                    this.allUsers.remove(user);
                }
                break;
            }
        }
        notifyDataSetChanged();
    }

    public void updateUserData(User modifiedUser) {
        boolean found = false;
        for (User user : users) {
            if (user.getUsername().equals(modifiedUser.getUsername())) {
                found = true;
                users.remove(user);
                break;
            }
        }
        for (User user : allUsers) {
            if (user.getUsername().equals(modifiedUser.getUsername())) {
                allUsers.remove(user);
                break;
            }
        }
        if (found) {
            users.add(modifiedUser);
            notifyDataSetChanged();
        }
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
        // disable drawable if user is not an admin.
        if (!currentUser.getRoles().equals(Role.ADMIN.name())) {
            holder.name.setCompoundDrawables(null, null, null, null);
        }

        if (currentUser.getStatus().equals(Status.ONLINE.name())) {
            holder.status.setCardBackgroundColor(0xFF006400); // color online
        }
        if (currentUser.getStatus().equals(Status.OFFLINE.name())) {
            holder.status.setCardBackgroundColor(0xFF696969); // color offline
        }
        if (currentUser.getStatus().equals(Status.DISABLED.name())) {
            holder.status.setCardBackgroundColor(0xFF8B0000); // color disabled
        }

        holder.itemView.setOnLongClickListener(v -> {
            setPosition(holder.getAdapterPosition());
            return false;
        });
    }

    @Override
    public void onViewRecycled(@NonNull UserHolder holder) {
        super.onViewRecycled(holder);
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    public User getUser() {
        return users.get(mPosition);
    }

    public void setPosition(int mPosition) {
        this.mPosition = mPosition;
    }

    @Override
    public Filter getFilter() {
        return userFilter;
    }

    private Filter userFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<User> filtredList = new ArrayList<>();
            if (constraint == null || constraint.length() == 0) {
                filtredList.addAll(allUsers);
            } else {
                String filterPattern = constraint.toString().toLowerCase().trim();
                for (User user : allUsers) {
                    if (user.getName().toLowerCase().contains(filterPattern)) {
                        filtredList.add(user);
                    }
                }
            }
            FilterResults results = new FilterResults();
            results.values = filtredList;
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            users.clear();
            users.addAll((List) results.values);
            notifyDataSetChanged();
        }
    };

    class UserHolder extends RecyclerView.ViewHolder implements View.OnCreateContextMenuListener {
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

            itemView.setOnCreateContextMenuListener(this);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (listener != null && position != RecyclerView.NO_POSITION) {
                    listener.onItemClick(users.get(position));
                }
            });
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
            menu.setHeaderTitle("Select an action");
            menu.setHeaderIcon(R.drawable.ic_menu);
            menu.add(Menu.NONE, R.id.ctx_editUser, Menu.NONE, "Edit user");
            menu.add(Menu.NONE, R.id.ctx_notifyStream, Menu.NONE, "Notify to start stream");
        }
    }


    public interface OnItemClickListener {
        void onItemClick(User user);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }
}

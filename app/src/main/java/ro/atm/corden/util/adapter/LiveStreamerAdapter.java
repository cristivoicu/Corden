package ro.atm.corden.util.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import ro.atm.corden.R;
import ro.atm.corden.model.user.LiveStreamer;

public class LiveStreamerAdapter extends RecyclerView.Adapter<LiveStreamerAdapter.LiveStreamerViewHolder>{
    private OnItemClickListener listener;
    private List<LiveStreamer> streamers = new ArrayList<>();

    @NonNull
    @Override
    public LiveStreamerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater
                .from(parent.getContext())
                .inflate(R.layout.list_item_live_streamer, parent, false);
        return new LiveStreamerViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull LiveStreamerViewHolder holder, int position) {
        LiveStreamer currentLiveStreamer = streamers.get(position);
        holder.name.setText(currentLiveStreamer.getName());
        holder.username.setText(currentLiveStreamer.getUsername());
    }

    @Override
    public int getItemCount() {
        return streamers.size();
    }

    class LiveStreamerViewHolder extends RecyclerView.ViewHolder{
        private TextView name;
        private TextView username;

        public LiveStreamerViewHolder(@NonNull View itemView) {
            super(itemView);

            name = itemView.findViewById(R.id.name);
            username = itemView.findViewById(R.id.username);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if(listener != null && position != RecyclerView.NO_POSITION){
                    listener.onItemClick(streamers.get(position));
                }
            });
        }
    }

    public void setStreamers(List<LiveStreamer> streamers){
        this.streamers = streamers;
        notifyDataSetChanged();
    }

    public void addStreamer(LiveStreamer liveStreamer){
        this.streamers.add(liveStreamer);
        notifyDataSetChanged();
    }

    public void removeStreamer(LiveStreamer liveStreamer){
        for (LiveStreamer streamer : streamers) {
            if (streamer.getName().equals(liveStreamer.getName()) &&
                    streamer.getUsername().equals(liveStreamer.getUsername())) {
                this.streamers.remove(streamer);
                notifyDataSetChanged();
                break;
            }
        }
    }

    public interface OnItemClickListener{
        void onItemClick(LiveStreamer liveStreamer);
    }

    public void setListener(OnItemClickListener listener) {
        this.listener = listener;
    }
}

package ro.atm.corden.util.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import ro.atm.corden.R;
import ro.atm.corden.model.video.Video;

public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.VideoHolder> {
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd - MM - yyyy");
    private static final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
    private List<Video> videos = new ArrayList<>();
    private OnItemClickListener listener;

    private boolean isAllUsers;

    public VideoAdapter(boolean isAllUsers) {
        this.isAllUsers = isAllUsers;
    }

    public void setVideos(List<Video> videos){
        this.videos = videos;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VideoHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_videos, parent, false);
        return new VideoHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull VideoHolder holder, int position) {
        Video currentVideo = videos.get(position);
        holder.date.setText(dateFormat.format(currentVideo.getDate()));
        holder.duration.setText(String.format("at %s", timeFormat.format(currentVideo.getDate())));
        if(isAllUsers) {
            holder.username.setVisibility(View.VISIBLE);
            holder.username.setText(currentVideo.getUsername());
        }
    }

    @Override
    public int getItemCount() {
        return videos.size();
    }

    class VideoHolder extends RecyclerView.ViewHolder{
        private TextView date;
        private TextView duration;
        private TextView username;

        public VideoHolder(@NonNull View itemView) {
            super(itemView);
            date = itemView.findViewById(R.id.date);
            duration = itemView.findViewById(R.id.duration);
            username = itemView.findViewById(R.id.username);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();
                    if (listener != null && position != RecyclerView.NO_POSITION) {
                        listener.onItemClick(videos.get(position));
                    }
                }
            });
        }
    }

    public interface OnItemClickListener{
        void onItemClick(Video video);
    }

    public void setOnItemClickListener(OnItemClickListener listener){
        this.listener = listener;
    }
}

package ro.atm.corden.util.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.github.vipulasri.timelineview.TimelineView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import ro.atm.corden.R;
import ro.atm.corden.model.user.Action;

public class TimelineAdapter extends RecyclerView.Adapter<TimelineAdapter.TimelineViewHolder> {
    private List<Action> actions = new ArrayList<>();

    public void setActions(List<Action> actions){
        this.actions = actions;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TimelineViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_timeline, parent, false);
        return new TimelineViewHolder(itemView, viewType);
    }

    @Override
    public int getItemViewType(int position) {
        return TimelineView.getTimeLineViewType(position, getItemCount());
    }

    @Override
    public void onBindViewHolder(@NonNull TimelineViewHolder holder, int position) {
        Action currentAction = actions.get(position);

        holder.hour.setText(new SimpleDateFormat("HH:mm").format(currentAction.getDate()));
        holder.description.setText(currentAction.getDescription());
        holder.doneBy.setText("USER");
    }

    @Override
    public int getItemCount() {
        return actions.size();
    }

    class TimelineViewHolder extends RecyclerView.ViewHolder{
        private TimelineView timelineView;

        private TextView hour;
        private TextView description;
        private TextView doneBy;

        public TimelineViewHolder(@NonNull View itemView, int viewType) {
            super(itemView);

            hour = itemView.findViewById(R.id.time);
            description = itemView.findViewById(R.id.description);
            doneBy = itemView.findViewById(R.id.doneBy);

            timelineView = (TimelineView) itemView.findViewById(R.id.timelineView);
            timelineView.initLine(viewType);
        }
    }
}

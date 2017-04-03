package rocks.stalin.android.app.ui;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import rocks.stalin.android.app.R;
import rocks.stalin.android.app.model.Group;

/**
 * Created by delusional on 4/4/17.
 */

public class GroupItemAdapter extends RecyclerView.Adapter<GroupItemAdapter.GroupItemViewHolder>{
    private List<Group> groups;

    public GroupItemAdapter(List<Group> groups) {
        this.groups = groups;
    }

    @Override
    public GroupItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_group, parent, false);
        GroupItemViewHolder holder = new GroupItemViewHolder(v);
        return holder;
    }

    @Override
    public void onBindViewHolder(GroupItemViewHolder holder, int position) {
        holder.name.setText(groups.get(position).name);
    }

    @Override
    public int getItemCount() {
        return groups.size();
    }

    public class GroupItemViewHolder extends RecyclerView.ViewHolder {
        TextView name;

        public GroupItemViewHolder(View itemView) {
            super(itemView);
            name = (TextView) itemView.findViewById(R.id.group_name);
        }
    }
}

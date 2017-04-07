package rocks.stalin.android.app.ui;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import rocks.stalin.android.app.R;
import rocks.stalin.android.app.model.Group;

/**
 * Created by delusional on 4/4/17.
 */

public class GroupItemAdapter extends RecyclerView.Adapter<GroupItemAdapter.GroupItemViewHolder>{
    private List<Group> groups;
    private final ItemSelectedListener selectedListener;

    public GroupItemAdapter(List<Group> groups, ItemSelectedListener selectedListener) {
        this.groups = groups;
        this.selectedListener =  selectedListener;
    }

    @Override
    public GroupItemViewHolder onCreateViewHolder(final ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_group, parent, false);
        final GroupItemViewHolder holder = new GroupItemViewHolder(v);
        v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int itemPos = holder.getAdapterPosition();
                selectedListener.select(groups.get(itemPos));
            }
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(GroupItemViewHolder holder, int position) {
        Group group = groups.get(position);
        holder.name.setText(group.name);
        holder.id.setText(group.id);
    }

    @Override
    public int getItemCount() {
        return groups.size();
    }

    public class GroupItemViewHolder extends RecyclerView.ViewHolder {
        TextView name;
        TextView id;

        public GroupItemViewHolder(View itemView) {
            super(itemView);
            name = (TextView) itemView.findViewById(R.id.group_name);
            id = (TextView) itemView.findViewById(R.id.id);
        }
    }

    public interface ItemSelectedListener {
        void select(Group group);
    }
}

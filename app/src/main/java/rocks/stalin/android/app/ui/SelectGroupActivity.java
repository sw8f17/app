package rocks.stalin.android.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import rocks.stalin.android.app.R;
import rocks.stalin.android.app.model.Group;

/**
 * Created by delusional on 4/3/17.
 */

public class SelectGroupActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_group);

        List<Group> groups = new ArrayList<Group>();
        groups.add(new Group("Test1"));
        groups.add(new Group("Test2"));

        RecyclerView rv = (RecyclerView) findViewById(R.id.rv);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
        rv.setLayoutManager(layoutManager);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(rv.getContext(),
                layoutManager.getOrientation());
        rv.addItemDecoration(dividerItemDecoration);
        GroupItemAdapter adapter = new GroupItemAdapter(groups);
        rv.setAdapter(adapter);
    }
}

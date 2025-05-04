package com.example.soundwatch;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class GroupAdapter extends ArrayAdapter<Group> {

    private Context context;
    private ArrayList<Group> groupList;

    public GroupAdapter(Context context, ArrayList<Group> groups) {
        super(context, 0, groups);
        this.context = context;
        this.groupList = groups;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Group group = groupList.get(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_group, parent, false);
        }

        TextView nameView = convertView.findViewById(R.id.textGroupName);
        TextView descView = convertView.findViewById(R.id.textGroupDesc);

        nameView.setText(group.getName());
        descView.setText(group.getDescription());

        return convertView;
    }
}

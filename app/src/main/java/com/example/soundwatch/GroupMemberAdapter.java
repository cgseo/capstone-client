package com.example.soundwatch;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class GroupMemberAdapter extends RecyclerView.Adapter<GroupMemberAdapter.ViewHolder> {

    private List<GroupMember> members;
    private Context context;

    public GroupMemberAdapter(List<GroupMember> members, Context context) {
        this.members = members;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_member, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        GroupMember member = members.get(position);
        holder.nameText.setText(member.getName());

        SharedPreferences prefs = context.getSharedPreferences("my_app_settings", Context.MODE_PRIVATE);
        int warningDecibel = prefs.getInt("selectedDecibel", 30); // 저장된 경고 데시벨 값 불러오기, 값이 없으면 기본 값 30

        if (member.getActive()) {
            holder.decibelText.setText(member.getDecibel() + " dB");

            if (member.getDecibel() > warningDecibel) {
                holder.statusImage.setImageResource(R.drawable.ic_noise);
            } else {
                holder.statusImage.setImageResource(R.drawable.ic_alive);
            }
        } else {
            holder.decibelText.setText("-");
            holder.statusImage.setImageResource(R.drawable.ic_sleep);
        }
    }

    @Override
    public int getItemCount() {
        return members.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameText, decibelText;
        ImageView  statusImage;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.txtName);
            statusImage = itemView.findViewById(R.id.imgStatus);
            decibelText = itemView.findViewById(R.id.txtDecibel);
        }
    }
}

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

public class MemberNoiseLogAdapter extends RecyclerView.Adapter<MemberNoiseLogAdapter.ViewHolder> {

    private List<MemberNoiseLog> members;
    private Context context;

    public MemberNoiseLogAdapter(List<MemberNoiseLog> members, Context context) {
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
        MemberNoiseLog member = members.get(position);
        holder.nameText.setText(member.getNickname());

        SharedPreferences prefs = context.getSharedPreferences("SoundWatchPrefs", Context.MODE_PRIVATE);
        int warningDecibel = prefs.getInt("selectedDecibel", 30); // 저장된 경고 데시벨 값 불러오기, 값이 없으면 기본 값 30


        if (member.isOnline()) {
            if (member.getNoise_level() != null) {
                holder.decibelText.setText(String.format("%.1f dB", member.getMax_db()));

                if (member.getMax_db() > warningDecibel) {
                    holder.statusImage.setImageResource(R.drawable.ic_noise);
                } else {
                    holder.statusImage.setImageResource(R.drawable.ic_alive);
                }
            } else {
                holder.decibelText.setText("기록 없음");
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
        ImageView statusImage;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.txtName);
            decibelText = itemView.findViewById(R.id.txtDecibel);
            statusImage = itemView.findViewById(R.id.imgStatus);
        }
    }
}

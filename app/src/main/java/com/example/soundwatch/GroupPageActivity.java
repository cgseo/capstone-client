package com.example.soundwatch;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class GroupPageActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private MemberNoiseLogAdapter adapter;
    private List<MemberNoiseLog> memberList = new ArrayList<>();
    private OkHttpClient client = new OkHttpClient();
    private String serverUrl = "http://10.0.2.2:3000";
    private String userId;
    private String groupId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_page);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new MemberNoiseLogAdapter(memberList, this);
        recyclerView.setAdapter(adapter);

        groupId = getIntent().getStringExtra("groupId");
        String groupName = getIntent().getStringExtra("groupName");
        userId = getIntent().getStringExtra("userId");

        TextView tvGroupName = findViewById(R.id.tvGroupName);
        tvGroupName.setText(groupName);

        ImageButton btnSetting = findViewById(R.id.btnGroupSetting);
        btnSetting.setOnClickListener(v -> {
            Intent intent = new Intent(GroupPageActivity.this, GroupSettingsActivity.class);
            intent.putExtra("groupId", groupId);
            intent.putExtra("userId", userId);

            startActivity(intent);
        });

        fetchGroupMembersFromServer(groupId);
    }


    private void fetchGroupMembersFromServer(String groupId) {
        Request request = new Request.Builder()
                .url(serverUrl + "/api/noise/membersNoiseLogs?groupId=" + groupId)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(GroupPageActivity.this, "네트워크 오류", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String json = response.body().string();
                    runOnUiThread(() -> {
                        try {
                            JSONArray array = new JSONArray(json);
                            memberList.clear();

                            for (int i = 0; i < array.length(); i++) {
                                JSONObject obj = array.getJSONObject(i);
                                MemberNoiseLog member = new MemberNoiseLog();
                                member.setUser_id(obj.getString("user_id"));
                                member.setNickname(obj.getString("nickname"));
                                member.setOnline(obj.getBoolean("is_online"));

                                if (!obj.isNull("noise_level")) {
                                    member.setNoise_level(obj.getDouble("noise_level"));
                                }

                                if (!obj.isNull("max_db")) {
                                    member.setMax_db(obj.getDouble("max_db"));
                                }

                                memberList.add(member);
                            }

                            adapter.notifyDataSetChanged();

                        } catch (Exception e) {
                            Toast.makeText(GroupPageActivity.this, "데이터 파싱 오류", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
    }
}
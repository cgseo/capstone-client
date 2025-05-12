package com.example.soundwatch;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
;
import androidx.appcompat.app.AlertDialog;
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
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GroupPageActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private GroupMemberAdapter adapter;
    private List<GroupMember> memberList = new ArrayList<>();
    private OkHttpClient client = new OkHttpClient();
    private String serverUrl = "http://10.0.2.2:3000";
    private String userId;
    String groupId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_page);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new GroupMemberAdapter(memberList, this);
        recyclerView.setAdapter(adapter);

        groupId = getIntent().getStringExtra("groupId");
        String groupName = getIntent().getStringExtra("groupName");
        userId = getIntent().getStringExtra("userId");

        TextView tvGroupName = findViewById(R.id.tvGroupName);
        tvGroupName.setText(groupName);

        Button btnLeave = findViewById(R.id.btnLeave);
        btnLeave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLeaveConfirmationDialog();
            }
        });

        fetchGroupMembersFromServer(groupId);
    }

    private void showLeaveConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("퇴실 확인")
                .setMessage("해당 그룹에서 삭제됩니다. 정말 퇴실 하시겠습니까?")
                .setPositiveButton("확인", (dialog, which) -> {
                    leaveGroupFromServer();
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void leaveGroupFromServer() {
        // 서버에 그룹 퇴실 요청을 보낸다.
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("group_id", groupId);
            jsonObject.put("user_id", userId);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "JSON 생성 오류", Toast.LENGTH_SHORT).show();
            return;
        }
        RequestBody requestBody = RequestBody.create(jsonObject.toString(), JSON);

        Request request = new Request.Builder()
                .url(serverUrl + "/api/noise/groups/leave")
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(GroupPageActivity.this, "네트워크 오류", Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        Toast.makeText(GroupPageActivity.this, "그룹에서 퇴실했습니다.", Toast.LENGTH_SHORT).show();
                        // 퇴실 성공 후 액티비티 종료 또는 그룹 목록 화면으로 이동
                        finish();
                    } else {
                        try {
                            JSONObject errorBody = new JSONObject(response.body().string());
                            String errorMessage = errorBody.optString("message", "그룹 퇴실 실패");
                            Toast.makeText(GroupPageActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            Toast.makeText(GroupPageActivity.this, "그룹 퇴실 실패", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
    }

    private void fetchGroupMembersFromServer(String groupId) {
        // 선택한 그룹 ID를 가진 모든 유저를 가져온다.
        Request request = new Request.Builder()
                .url(serverUrl + "/api/noise/groups/members?group_id=" + groupId)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(GroupPageActivity.this, "네트워크 오류", Toast.LENGTH_SHORT).show()
                );
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
                                GroupMember member = new GroupMember();
                                member.setId(obj.getString("id"));
                                member.setUserId(obj.getString("user_id"));
                                member.setName(obj.getString("name"));
                                member.setActive(obj.getBoolean("active"));
                                member.setJoined_at(obj.getLong("joined_at"));
                                member.setIs_owner(obj.getBoolean("is_owner"));
                                if (!obj.isNull("decibel")) {
                                    member.setDecibel(obj.getDouble("decibel"));
                                } else {
                                    member.setDecibel(0);
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
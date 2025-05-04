package com.example.soundwatch;

import android.os.Bundle;

import android.app.AlertDialog;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GroupActivity extends AppCompatActivity {

    private int userID;
    private ArrayList<Group> groupList;
    private GroupAdapter adapter;
    private ListView groupListView;
    private OkHttpClient client = new OkHttpClient();
    private String serverUrl = "http://10.0.2.2:3000";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group);

        groupList = new ArrayList<>();
        groupListView = findViewById(R.id.groupListView);
        adapter = new GroupAdapter(this, groupList);
        groupListView.setAdapter(adapter);

        Button btnCreateGroup = findViewById(R.id.btnCreateGroup);
        btnCreateGroup.setOnClickListener(v -> showCreateGroupDialog());

        Button btnJoinGroup = findViewById(R.id.btnJoinGroup);
        btnJoinGroup.setOnClickListener(v -> showJoinGroupDialog());

        // 그룹 리스트 클릭 시 새 창 GroupPageActivity.java로 이동, 그룹 id, 이름, 초대 코드, 유저 ID? 전달(임의로 선정, 서버 구축 후 변경 가능성 있음)
        groupListView.setOnItemClickListener((parent, view, position, id) -> {
            Group selectedGroup = groupList.get(position);
            Intent intent = new Intent(GroupActivity.this, GroupPageActivity.class);
            intent.putExtra("groupId", selectedGroup.getId());
            intent.putExtra("groupName", selectedGroup.getName());
            intent.putExtra("inviteCode", selectedGroup.getInviteCode());
            // intent.putExtra("userID",userID);
            startActivity(intent);
        });

        fetchGroupsFromServer(); // 액티비티 생성 시 서버에서 그룹 목록을 가져옵니다.
    }

    private void fetchGroupsFromServer() {
        Request request = new Request.Builder()
                .url(serverUrl + "/groups")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("GroupActivity", "그룹 목록 가져오기 실패: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(GroupActivity.this, "네트워크 오류 발생", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    final String responseData = response.body().string();
                    try {
                        JSONArray jsonArray = new JSONArray(responseData);
                        groupList.clear();
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject jsonObject = jsonArray.getJSONObject(i);
                            String id = jsonObject.getString("id");
                            String name = jsonObject.getString("name");
                            String description = jsonObject.getString("description");
                            String inviteCode = jsonObject.getString("inviteCode");
                            groupList.add(new Group(id, name, description, inviteCode));
                        }
                        runOnUiThread(() -> adapter.notifyDataSetChanged());
                    } catch (Exception e) {
                        Log.e("GroupActivity", "그룹 목록 파싱 오류: " + e.getMessage());
                        runOnUiThread(() -> Toast.makeText(GroupActivity.this, "데이터 파싱 오류", Toast.LENGTH_SHORT).show());
                    }
                } else {
                    Log.e("GroupActivity", "그룹 목록 가져오기 응답 실패: " + response.code());
                    runOnUiThread(() -> Toast.makeText(GroupActivity.this, "서버 응답 오류: " + response.code(), Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void showCreateGroupDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_create_group, null);
        EditText edtGroupName = dialogView.findViewById(R.id.edtGroupName);
        EditText edtGroupDesc = dialogView.findViewById(R.id.edtGroupDesc);
        EditText edtGroupNickname = dialogView.findViewById(R.id.edtGroupNickname);

        new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("생성", (dialog, which) -> {
                    String name = edtGroupName.getText().toString();
                    String desc = edtGroupDesc.getText().toString();
                    String nickname = edtGroupNickname.getText().toString();
                    // 그룹 이름과 설명, 닉네임을 서버(/groups)에 전송하고 서버에게 그룹 생성을 요청
                    createGroupOnServer(name, desc, nickname);
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void createGroupOnServer(String name, String description, String nickname) {
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("name", name);
            jsonBody.put("description", description);
            jsonBody.put("nickname", nickname);
        } catch (Exception e) {
            Log.e("GroupActivity", "JSON 생성 오류: " + e.getMessage());
            return;
        }
        RequestBody requestBody = RequestBody.create(JSON, jsonBody.toString());

        Request request = new Request.Builder()
                .url(serverUrl + "/groups")
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("GroupActivity", "그룹 생성 요청 실패: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(GroupActivity.this, "네트워크 오류 발생", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        runOnUiThread(() -> {
                            Toast.makeText(GroupActivity.this, "그룹 생성 완료!", Toast.LENGTH_LONG).show();
                            fetchGroupsFromServer(); // 그룹 목록 다시 로드
                        });
                    } catch (Exception e) {
                        Log.e("GroupActivity", "그룹 생성 응답 파싱 오류: " + e.getMessage());
                        runOnUiThread(() -> Toast.makeText(GroupActivity.this, "데이터 파싱 오류", Toast.LENGTH_SHORT).show());
                    }
                } else {
                    Log.e("GroupActivity", "그룹 생성 응답 실패: " + response.code());
                    runOnUiThread(() -> Toast.makeText(GroupActivity.this, "그룹 생성 실패: " + response.code(), Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void showJoinGroupDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_join_group, null);
        EditText edtInviteCode = dialogView.findViewById(R.id.edtJoinInviteCode);
        EditText edtNickname = dialogView.findViewById(R.id.edtJoinNickname);

        new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("가입", (dialog, which) -> {
                    String inviteCode = edtInviteCode.getText().toString();
                    String nickname = edtNickname.getText().toString();
                    // 서버(/groups/join)로 초대 코드와 닉네임을 전달하여, 서버에서 사용자가 그룹에 가입할 수 있도록 처리,
                    joinGroupOnServer(inviteCode, nickname);
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void joinGroupOnServer(String inviteCode, String nickname) {
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("inviteCode", inviteCode);
            jsonBody.put("nickname", nickname);
        } catch (Exception e) {
            Log.e("GroupActivity", "JSON 생성 오류 (가입): " + e.getMessage());
            return;
        }
        RequestBody requestBody = RequestBody.create(JSON, jsonBody.toString());

        Request request = new Request.Builder()
                .url(serverUrl + "/groups/join")
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("GroupActivity", "그룹 가입 요청 실패: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(GroupActivity.this, "네트워크 오류 발생", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                final String responseData = response.body().string();
                final int responseCode = response.code();
                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        try {
                            JSONObject jsonObject = new JSONObject(responseData);
                            Toast.makeText(GroupActivity.this, "그룹 가입 완료!", Toast.LENGTH_LONG).show();
                            fetchGroupsFromServer(); // 그룹 목록 다시 로드
                        } catch (Exception e) {
                            Log.e("GroupActivity", "그룹 가입 응답 파싱 오류: " + e.getMessage());
                            Toast.makeText(GroupActivity.this, "데이터 파싱 오류", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        try {
                            JSONObject jsonObject = new JSONObject(responseData);
                            Toast.makeText(GroupActivity.this, jsonObject.getString("error"), Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            Toast.makeText(GroupActivity.this, "그룹 가입 실패: " + responseCode, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
    }

}

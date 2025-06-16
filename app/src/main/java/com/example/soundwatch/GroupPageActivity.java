package com.example.soundwatch;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log; // Log import 추가
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException; // JSONException import 추가

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
    private String serverUrl = "http://172.30.1.31:3000";
    private String userId;
    private String groupId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_page);

        recyclerView = findViewById(R.id.recyclerView);
        // GridLayoutManager를 사용하여 2열로 아이템을 표시한다.
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new MemberNoiseLogAdapter(memberList, this);
        recyclerView.setAdapter(adapter);

        groupId = getIntent().getStringExtra("groupId");
        String groupName = getIntent().getStringExtra("groupName");
        userId = getIntent().getStringExtra("userId");

        TextView tvGroupName = findViewById(R.id.tvGroupName);
        tvGroupName.setText(groupName);

        // 그룹 설정 버튼 클릭 시 GroupSettingsActivity로 이동
        ImageButton btnSetting = findViewById(R.id.btnGroupSetting);
        btnSetting.setOnClickListener(v -> {
            Intent intent = new Intent(GroupPageActivity.this, GroupSettingsActivity.class);
            intent.putExtra("groupId", groupId);
            intent.putExtra("userId", userId);
            startActivity(intent);
        });

        // 그룹 멤버 및 소음 로그 정보를 서버에서 가져옵니다.
        fetchGroupMembersFromServer(groupId);
    }


    private void fetchGroupMembersFromServer(String groupId) {
        Request request = new Request.Builder()
                .url(serverUrl + "/api/noise/membersNoiseLogs?groupId=" + groupId)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    Log.e("GroupPageActivity", "그룹 멤버 정보 요청 실패: " + e.getMessage());
                    Toast.makeText(GroupPageActivity.this, "네트워크 오류", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    String json = response.body().string();
                    Log.d("GroupPageActivity", "서버 응답 JSON: " + json); // 서버 응답 로그 출력

                    runOnUiThread(() -> {
                        try {
                            JSONArray array = new JSONArray(json);
                            memberList.clear(); // 기존 목록 비우기

                            for (int i = 0; i < array.length(); i++) {
                                JSONObject obj = array.getJSONObject(i);
                                MemberNoiseLog member = new MemberNoiseLog();
                                member.setUser_id(obj.getString("user_id"));
                                member.setNickname(obj.getString("nickname"));
                                member.setOnline(obj.getInt("is_online") == 1);

                                // noise_level과 max_db는 null일 수 있으므로 isNull 체크 후 가져온다.
                                if (!obj.isNull("noise_level")) {
                                    member.setNoise_level(obj.getDouble("noise_level"));
                                } else {
                                    member.setNoise_level(0.0); // null일 경우 기본값 설정
                                }

                                if (!obj.isNull("max_db")) {
                                    member.setMax_db(obj.getDouble("max_db"));
                                } else {
                                    member.setMax_db(0.0); // null일 경우 기본값 설정
                                }

                                memberList.add(member);
                            }

                            // 데이터가 변경되었음을 어댑터에 알립니다.
                            adapter.notifyDataSetChanged();

                        } catch (JSONException e) { // JSON 파싱 오류를 더 명확하게 캐치
                            Log.e("GroupPageActivity", "데이터 파싱 오류 상세: " + e.getMessage(), e); // 상세 로그 추가
                            Toast.makeText(GroupPageActivity.this, "데이터 파싱 오류: " + e.getMessage(), Toast.LENGTH_LONG).show(); // 오류 메시지를 더 자세히 표시
                        } catch (Exception e) { // 기타 예외 처리
                            Log.e("GroupPageActivity", "알 수 없는 오류 발생: " + e.getMessage(), e);
                            Toast.makeText(GroupPageActivity.this, "데이터 처리 중 알 수 없는 오류 발생", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    // 응답이 성공적이지 않은 경우 (예: 404, 500 에러)
                    Log.e("GroupPageActivity", "그룹 멤버 정보 로드 실패: " + response.code() + " - " + response.message());
                    runOnUiThread(() -> Toast.makeText(GroupPageActivity.this, "그룹 멤버 정보 로드 실패: " + response.code(), Toast.LENGTH_SHORT).show());
                }
            }
        });
    }
}
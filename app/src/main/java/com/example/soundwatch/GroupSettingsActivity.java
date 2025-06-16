package com.example.soundwatch;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class GroupSettingsActivity extends AppCompatActivity {

    private String groupId;
    private String userId;
    private boolean is_Owner = false;
    private OkHttpClient client = new OkHttpClient();
    private String serverUrl = "http://172.30.1.31:3000";
    private Group group; // 그룹 정보

    private Button btnOut;
    private Button btnDeleteGroup;
    private Button btnUpdateInfoGroup;
    private TextView textInviteCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_group_settings);

        groupId = getIntent().getStringExtra("groupId");
        userId = getIntent().getStringExtra("userId");

        btnOut = findViewById(R.id.btnOut);
        btnDeleteGroup = findViewById(R.id.btnDeleteGroup);
        btnUpdateInfoGroup = findViewById(R.id.btnUpdateInfoGroup);
        textInviteCode = findViewById(R.id.tvInviteCode);

        // 초기에 모든 버튼 숨김 처리(생성자 확인 후 표시)
        btnOut.setVisibility(View.GONE);
        btnDeleteGroup.setVisibility(View.GONE);
        btnUpdateInfoGroup.setVisibility(View.GONE);

        checkIfOwner(); // 액티비티 생성 시 그룹 생성자인지 확인
        fetchGroupInfo(groupId); // 그룹 정보 가져오기

        btnOut.setOnClickListener(v -> showOutDialog());
        btnDeleteGroup.setOnClickListener(v -> showDeleteGroupDialog());
        btnUpdateInfoGroup.setOnClickListener(v -> showUpdateInfoDialog());
    }

    // 그룹 생성자 구별
    private void checkIfOwner() {
        if (userId == null || groupId == null) {
            Log.e("GroupSettingsActivity", "userId 또는 groupId가 null입니다. 소유자 확인 불가.");
            return;
        }

        HttpUrl url = HttpUrl.parse(serverUrl + "/api/group/isOwner")
                .newBuilder()
                .addQueryParameter("userId", userId)
                .addQueryParameter("groupId", groupId)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("GroupSettingsActivity", "isOwner 요청 실패: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(GroupSettingsActivity.this, "소유자 정보 확인 실패", Toast.LENGTH_SHORT).show());
                // 네트워크 오류 시에도 모든 버튼 숨김
                runOnUiThread(() -> {
                    btnOut.setVisibility(View.GONE);
                    btnDeleteGroup.setVisibility(View.GONE);
                    btnUpdateInfoGroup.setVisibility(View.GONE);
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    final String responseData = response.body().string();
                    runOnUiThread(() -> {
                        try {
                            is_Owner = Boolean.parseBoolean(responseData);
                            if (is_Owner) { // is_Owner가 true이면 퇴실 숨김, 그룹삭제, 정보변경 모두 표시
                                btnOut.setVisibility(View.GONE);
                                btnDeleteGroup.setVisibility(View.VISIBLE);
                                btnUpdateInfoGroup.setVisibility(View.VISIBLE);
                                Log.d("GroupSettingsActivity", "그룹 관리 버튼 표시 (소유자 맞음)");
                            } else { // false이면 퇴실 표시. 그룹삭제, 정보변경 모두 숨김
                                btnOut.setVisibility(View.VISIBLE);
                                btnDeleteGroup.setVisibility(View.GONE);
                                btnUpdateInfoGroup.setVisibility(View.GONE);
                                Log.d("GroupSettingsActivity", "그룹 관리 버튼 숨김 (소유자 아님)");
                            }
                        } catch (Exception e) {
                            Log.e("GroupSettingsActivity", "isOwner 응답 파싱 오류: " + e.getMessage());
                            Toast.makeText(GroupSettingsActivity.this, "소유자 정보 파싱 오류", Toast.LENGTH_SHORT).show();
                            // 파싱 오류 시에도 숨김
                            btnOut.setVisibility(View.GONE);
                            btnDeleteGroup.setVisibility(View.GONE);
                            btnUpdateInfoGroup.setVisibility(View.GONE);
                        }
                    });
                } else {
                    Log.e("GroupSettingsActivity", "isOwner 응답 실패: " + response.code());
                    runOnUiThread(() -> Toast.makeText(GroupSettingsActivity.this, "소유자 정보 확인 실패: " + response.code(), Toast.LENGTH_SHORT).show());
                    // 응답 실패 시에도 숨김
                    runOnUiThread(() -> {
                        btnOut.setVisibility(View.GONE);
                        btnDeleteGroup.setVisibility(View.GONE);
                        btnUpdateInfoGroup.setVisibility(View.GONE);
                    });
                }
            }
        });
    }

    // 그룹 정보 가져오기
    private void fetchGroupInfo(String groupId) {
        if (groupId == null) {
            Log.e("GroupSettingsActivity", "그룹 ID가 null이어서 그룹 정보를 가져올 수 없습니다.");
            return;
        }

        HttpUrl url = HttpUrl.parse(serverUrl + "/api/group/info")
                .newBuilder()
                .addQueryParameter("id", groupId)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try (ResponseBody responseBody = response.body()) {
                        String jsonResponse = responseBody.string();
                        Gson gson = new Gson();
                        group = gson.fromJson(jsonResponse, Group.class);

                        runOnUiThread(() -> {

                            if (group != null && group.getInviteCode() != null) {
                                textInviteCode.setText("초대 코드: " + group.getInviteCode());
                                Log.d("GroupSettingsActivity", "그룹 정보 로드 성공: " + group.hashCode() + ", 초대 코드: " + group.getInviteCode());
                            } else {
                                textInviteCode.setText("초대 코드: (데이터 오류)");
                                Log.e("GroupSettingsActivity", "그룹 정보 로드 성공했지만 데이터가 불완전합니다.");
                            }
                        });
                    } catch (IOException e) {
                        Log.e("GroupSettingsActivity", "응답 본문 읽기 오류: " + e.getMessage());
                        runOnUiThread(() -> {
                            Toast.makeText(GroupSettingsActivity.this, "데이터 처리 오류: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            textInviteCode.setText("초대 코드: (데이터 오류)");
                        });
                    } catch (com.google.gson.JsonSyntaxException e) {
                        Log.e("GroupSettingsActivity", "JSON 파싱 오류: " + e.getMessage());
                        runOnUiThread(() -> {
                            Toast.makeText(GroupSettingsActivity.this, "데이터 형식 오류: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            textInviteCode.setText("초대 코드: (파싱 오류)");
                        });
                    }
                } else {
                    Log.e("GroupSettingsActivity", "그룹 정보 로드 실패: " + response.code() + " - " + response.message());
                    runOnUiThread(() -> {
                        Toast.makeText(GroupSettingsActivity.this, "그룹 정보 로드 실패: " + response.code(), Toast.LENGTH_SHORT).show();
                        textInviteCode.setText("초대 코드: (로드 실패)");
                    });
                }
            }

            // 네트워크 요청 자체가 실패한 경우 (예: 서버에 연결할 수 없음)
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("GroupSettingsActivity", "그룹 정보 요청 실패: " + e.getMessage());
                runOnUiThread(() -> {
                    Toast.makeText(GroupSettingsActivity.this, "네트워크 오류: 그룹 정보 로드 실패", Toast.LENGTH_SHORT).show();
                    textInviteCode.setText("초대 코드: (네트워크 오류)"); // 실패 시 메시지
                });
            }
        });
    }

    private void showOutDialog() {
        if (is_Owner) { // 다시 한번 소유자 여부 확인
            Toast.makeText(this, "퇴실 권한이 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("탈퇴")
                .setMessage("정말로 이 그룹에서 탈퇴하시겠습니까?")
                .setPositiveButton("예", (dialog, which) -> sendOutRequest())
                .setNegativeButton("아니오", null)
                .show();
    }

    private void showDeleteGroupDialog() {
        if (!is_Owner) { // 다시 한번 소유자 여부 확인
            Toast.makeText(this, "그룹 삭제 권한이 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("그룹 삭제")
                .setMessage("정말로 이 그룹을 삭제하시겠습니까? 모든 멤버가 그룹에서 탈퇴됩니다.")
                .setPositiveButton("예", (dialog, which) -> showFinalDeleteConfirmDialog())
                .setNegativeButton("아니오", null)
                .show();
    }

    private void showFinalDeleteConfirmDialog() {
        new AlertDialog.Builder(this)
                .setMessage("다시는 되돌릴 수 없습니다. 그래도 삭제하시겠습니까?")
                .setPositiveButton("예", (dialog, which) -> sendDeleteGroupRequest())
                .setNegativeButton("아니요", null)
                .show();
    }

    private void showUpdateInfoDialog(){
        if (!is_Owner) {
            Toast.makeText(this, "그룹 정보 수정 권한이 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_change_group, null);
        EditText edtGroupName = dialogView.findViewById(R.id.edtGroupName);
        EditText edtGroupDesc = dialogView.findViewById(R.id.edtGroupDesc);
        edtGroupName.setText(group.getGroup_name());
        edtGroupDesc.setText(group.getDescription() != null ? group.getDescription() : "");

        new android.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("수정", (dialog, which) -> {
                    String group_name = edtGroupName.getText().toString();
                    String desc = edtGroupDesc.getText().toString();
                    try {
                        updateInfoGroupOnServer(group_name, desc);
                    } catch (JSONException e) {
                        Log.e("GroupSettingsActivity", "그룹 정보 수정 JSON 생성 오류: " + e.getMessage());
                        Toast.makeText(GroupSettingsActivity.this, "그룹 정보 수정 요청 오류 발생", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("취소", null)
                .show();
    }

    // 그룹 정보 수정 시 비어 있지 않은 값만 전송
    private void updateInfoGroupOnServer(String group_name, String description) throws JSONException {
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", groupId.trim());
        jsonObject.put("group_name", group_name);
        jsonObject.put("description", description);

        RequestBody requestBody = RequestBody.create(jsonObject.toString(), JSON);

        Request request = new Request.Builder()
                .url(serverUrl + "/api/group/updateInfo")
                .patch(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("GroupSettingsActivity", "그룹 정보 수정 실패: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(GroupSettingsActivity.this, "네트워크 오류 발생", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                final String responseData = response.body() != null ? response.body().string() : "";
                final int responseCode = response.code();

                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        Toast.makeText(GroupSettingsActivity.this, "그룹 정보 수정 완료!", Toast.LENGTH_LONG).show();
                        Intent intent = new Intent(GroupSettingsActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        Log.e("GroupSettingsActivity", "그룹 정보 수정 실패: 응답 코드 " + responseCode + ", 응답 본문: " + responseData); // 로그 태그 통일
                        try {
                            JSONObject errorJson = new JSONObject(responseData);
                            String errorMessage = errorJson.optString("message", "알 수 없는 오류");
                            Toast.makeText(GroupSettingsActivity.this, "그룹 정보 수정 실패: " + errorMessage, Toast.LENGTH_SHORT).show();
                        } catch (JSONException e) {
                            Log.e("GroupSettingsActivity", "그룹 정보 수정 응답 파싱 오류: " + e.getMessage());
                            Toast.makeText(GroupSettingsActivity.this, "그룹 정보 수정 실패: " + responseCode, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
    }




    private void sendOutRequest() {
        try {
            HttpUrl url = HttpUrl.parse(serverUrl + "/api/noise/groups/out")
                    .newBuilder()
                    .addQueryParameter("user_id", userId.trim())
                    .addQueryParameter("group_id", groupId.trim())
                    .build();

            Request request = new Request.Builder()
                    .url(url)
                    .delete()
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() ->
                            Toast.makeText(GroupSettingsActivity.this, "네트워크 오류", Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onResponse(Call call, Response response) {
                    runOnUiThread(() -> {
                        if (response.isSuccessful()) {
                            Toast.makeText(GroupSettingsActivity.this, "그룹 퇴실 완료", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(GroupSettingsActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(GroupSettingsActivity.this, "퇴실 실패", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void sendDeleteGroupRequest() {
        HttpUrl url = HttpUrl.parse(serverUrl + "/api/group/deleteGroup")
                .newBuilder()
                .addQueryParameter("user_id", userId)
                .addQueryParameter("group_id", groupId)
                .build();

        Log.d("GroupSettingsActivity", "Final DELETE Request URL: " + url.toString());

        Request request = new Request.Builder()
                .url(url)
                .delete()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(GroupSettingsActivity.this, "네트워크 오류", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) {
                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        Toast.makeText(GroupSettingsActivity.this, "그룹 삭제 완료", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(GroupSettingsActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(GroupSettingsActivity.this, "삭제 실패", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

}
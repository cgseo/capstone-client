package com.example.soundwatch;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

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

public class GroupSettingsActivity extends AppCompatActivity {

    private String groupId;
    private String userId;
    private boolean is_Owner = false;
    private OkHttpClient client = new OkHttpClient();
    private String serverUrl = "http://10.0.2.2:3000";

    private Button btnOut;
    private Button btnDeleteGroup;
    private Button btnUpdateInfoGroup;

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

        // 초기에 모든 버튼 숨김 처리(생성자 확인 후 표시)
        btnOut.setVisibility(View.GONE);
        btnDeleteGroup.setVisibility(View.GONE);
        btnUpdateInfoGroup.setVisibility(View.GONE);

        checkIfOwner(); // 액티비티 생성 시 그룹 생성자인지 확인

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

        new android.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("수정", (dialog, which) -> {
                    String group_name = edtGroupName.getText().toString();
                    String desc = edtGroupDesc.getText().toString();
                    updateInfoGroupOnServer(group_name, desc);
                })
                .setNegativeButton("취소", null)
                .show();
    }

    // 그룹 정보 수정 시 비어 있지 않은 값만 전송
    private void updateInfoGroupOnServer(String group_name, String description) {
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("id", groupId);

            // group_name이 비어있지 않으면 추가
            if (!group_name.isEmpty()) {
                jsonObject.put("group_name", group_name);
            }

            // description이 비어있지 않으면 추가
            if (!description.isEmpty()) {
                jsonObject.put("description", description);
            }

            // 만약 group_name과 description 모두 비어있다면, 수정할 내용이 없음을 알림
            if (jsonObject.length() <= 1) { // "id"만 있는 경우
                Toast.makeText(GroupSettingsActivity.this, "수정할 그룹 이름 또는 설명을 입력해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(GroupSettingsActivity.this, "JSON 생성 오류", Toast.LENGTH_SHORT).show();
            return;
        }

        RequestBody requestBody = RequestBody.create(jsonObject.toString(), JSON);

        Request request = new Request.Builder()
                .url(serverUrl + "/api/group/updateInfo")
                .patch(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("GroupActivity", "그룹 정보 수정 실패: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(GroupSettingsActivity.this, "네트워크 오류 발생", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    runOnUiThread(() -> {
                        Toast.makeText(GroupSettingsActivity.this, "그룹 정보 수정 완료!", Toast.LENGTH_LONG).show();
                    });
                } else {
                    Log.e("GroupActivity", "그룹 정보 수정 실패: " + response.code());
                    runOnUiThread(() -> Toast.makeText(GroupSettingsActivity.this, "그룹 정보 수정 실패: " + response.code(), Toast.LENGTH_SHORT).show());
                }
            }
        });
    }




    private void sendOutRequest() {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("group_id", groupId);
            jsonObject.put("user_id", userId);

            MediaType JSON = MediaType.parse("application/json; charset=utf-8");
            RequestBody requestBody = RequestBody.create(jsonObject.toString(), JSON);

            Request request = new Request.Builder()
                    .url(serverUrl + "/api/noise/groups/out")
                    .post(requestBody)
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
                .addQueryParameter("userId", userId)
                .addQueryParameter("groupId", groupId)
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
                        Toast.makeText(GroupSettingsActivity.this, "그룹 삭제 완료", Toast.LENGTH_SHORT).show();
                        finish(); // 설정 액티비티 종료
                    } else {
                        Toast.makeText(GroupSettingsActivity.this, "삭제 실패", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

}
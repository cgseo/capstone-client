package com.example.soundwatch;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SignupActivity extends AppCompatActivity {

    private EditText etSignupEmail, etSignupPassword, etSignupConfirmPassword;
    private Button btnSignup;
    private TextView tvBackToLogin;
    private OkHttpClient client = new OkHttpClient();
    private String serverUrl = "http://10.0.2.2:3000";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        etSignupEmail = findViewById(R.id.etSignupEmail);
        etSignupPassword = findViewById(R.id.etSignupPassword);
        etSignupConfirmPassword = findViewById(R.id.etSignupConfirmPassword);
        btnSignup = findViewById(R.id.btnSignup);
        tvBackToLogin = findViewById(R.id.tvBackToLogin);

        btnSignup.setOnClickListener(v -> {
            String email = etSignupEmail.getText().toString().trim();
            String password = etSignupPassword.getText().toString().trim();
            String confirmPassword = etSignupConfirmPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "모든 항목을 입력하세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!password.equals(confirmPassword)) {
                Toast.makeText(this, "비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show();
                return;
            }

            signupToServer(email, password);
        });

        tvBackToLogin.setOnClickListener(v -> {
            Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void signupToServer(String email, String password) {
        try {
            JSONObject json = new JSONObject();
            json.put("email", email);
            json.put("password", password);

            RequestBody body = RequestBody.create(
                    json.toString(),
                    MediaType.parse("application/json; charset=utf-8")
            );


            Request request = new Request.Builder()
                    .url(serverUrl + "/api/user/signup")
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() ->
                            Toast.makeText(SignupActivity.this, "네트워크 오류", Toast.LENGTH_SHORT).show());
                    e.printStackTrace();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        runOnUiThread(() -> {
                            Toast.makeText(SignupActivity.this, "회원가입 성공! 로그인해주세요.", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
                            startActivity(intent);
                            finish();
                        });
                    } else {
                        String responseBody = response.body().string();
                        try {
                            JSONObject errorJson = new JSONObject(responseBody);
                            String errorMessage = errorJson.optString("message", "회원가입 실패");
                            runOnUiThread(() ->
                                    Toast.makeText(SignupActivity.this, errorMessage, Toast.LENGTH_SHORT).show());
                        } catch (Exception e) {
                            runOnUiThread(() ->
                                    Toast.makeText(SignupActivity.this, "회원가입 실패", Toast.LENGTH_SHORT).show());
                        }
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
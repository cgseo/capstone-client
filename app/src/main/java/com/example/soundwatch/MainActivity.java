package com.example.soundwatch;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    private Button btnLogin;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        btnLogin = (Button) findViewById(R.id.btnLogin); // 로그인
        Button btnMeasure = (Button) findViewById(R.id.btnMeasure); //측정
        Button btnGroup = (Button) findViewById(R.id.btnGroup); //기록
        Button btnRecord = (Button) findViewById(R.id.btnRecord); //그룹

        // SharedPreferences 초기화
        prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        // 로그인 상태에 따라 버튼 텍스트와 색상 변경
        updateLoginButtonState();

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 현재 로그인 상태 확인
                if (isLoggedIn()) {
                    // 로그아웃 처리
                    performLogout();
                } else {
                    // 로그인 화면으로 이동
                    Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                    startActivity(intent);
                }
            }
        });

        btnMeasure.setOnClickListener(new View.OnClickListener() { //측정
            @Override
            public void onClick(View view) { // 새 창 MesureDecibel.java
                Intent intent = new Intent(getApplicationContext(), MesureDecibel.class);
                startActivity(intent);
            }

        });

        btnRecord.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) { // 새 창 RecodeActivity.java
                Intent intent = new Intent(getApplicationContext(), RecodeActivity.class);
                startActivity(intent);
            }
        });

        btnGroup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { // 새창 GroupActivity.java
                if(isLoggedIn()){
                    Intent intent = new Intent(getApplicationContext(),GroupActivity.class);
                    startActivity(intent);
                }else{
                    Toast.makeText(MainActivity.this, "로그인 후 실행해주세요.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // MainActivity가 다시 활성화될 때마다 로그인 상태 업데이트
        updateLoginButtonState();
    }

    private void updateLoginButtonState() {
        if (isLoggedIn()) {
            btnLogin.setText("로그아웃");
            btnLogin.setBackgroundColor(Color.RED);
        } else {
            btnLogin.setText("로그인");
            btnLogin.setBackgroundColor(Color.parseColor("#2EAB67"));
        }
    }

    private boolean isLoggedIn() {
        // SharedPreferences에서 userId를 확인하여 로그인 여부 판단
        return prefs.getString("userId", null) != null;
    }

    private void performLogout() {
        // SharedPreferences에서 userId 제거
        prefs.edit().remove("userId").apply();
        Toast.makeText(this, "로그아웃 되었습니다.", Toast.LENGTH_SHORT).show();
        // 로그아웃 후 버튼 상태 업데이트
        updateLoginButtonState();
    }
}
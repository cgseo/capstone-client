package com.example.soundwatch;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
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
                if(isLoggedIn()){
                    Intent intent = new Intent(getApplicationContext(), RecodeActivity.class);
                    startActivity(intent);
                }else{
                    Toast.makeText(MainActivity.this, "로그인 후 실행해주세요.", Toast.LENGTH_SHORT).show();
                }
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

        // wifi 연결을 확인하여 group_id를 세팅하는 ForegroundService 실행
        // android 10 이상부터 ACCESS_BACKGROUND_LOCATION 권한 요청 해야하기 때문에 나눠서 진행
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // 먼저 FINE_LOCATION만 요청
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, 1001);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            // FINE은 이미 허용됨 → BACKGROUND 따로 요청
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
            }, 1002);
        } else {
            // 모든 권한 허용됨 → 서비스 시작
            startForegroundService();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // MainActivity가 다시 활성화될 때마다 로그인 상태 업데이트
        updateLoginButtonState();

        // 앱 설정에서 돌아왔을 때 권한 확인
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {
                // 모든 권한 OK → 서비스 시작
                startForegroundService();
            }
        }
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

    private void startForegroundService() {
        Intent serviceIntent = new Intent(this, WifiForegroundService.class);
        ContextCompat.startForegroundService(this, serviceIntent);
    }

    private void checkAndRequestLocationPermissions() {
        // FINE_LOCATION 권한 확인
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    1001);

        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            // BACKGROUND 권한이 필요한데, Android 10+ 이고 아직 허용 안 됐으면
            goToAppSettingsForBackgroundLocation();
        } else {
            // 모든 권한 OK → 서비스 시작
            startForegroundService();
        }
    }

    private void goToAppSettingsForBackgroundLocation() {
        Toast.makeText(this, "설정에서 위치 권한을 '항상 허용'으로 바꿔주세요.", Toast.LENGTH_LONG).show();

        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1001) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // FINE_LOCATION 허용됨 → BACKGROUND 확인
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                                != PackageManager.PERMISSION_GRANTED) {
                    goToAppSettingsForBackgroundLocation();
                } else {
                    startForegroundService();
                }
            } else {
                Toast.makeText(this, "위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
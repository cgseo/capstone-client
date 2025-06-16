package com.example.soundwatch;

import android.Manifest;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;


import android.provider.Settings;



import androidx.annotation.NonNull;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private Button btnLogin;
    private SharedPreferences prefs;

    //상단바 알림 위함
    private static String CHANNEL_ID = "channel1";
    private static String CHANNEL_NAME = "channel1";
    NotificationManager manager;

    //백그라운드에서 브로드캐스트로 보내는 측정 데시벨 값 받기 위함
    private BroadcastReceiver decibelReceiver;
    TextView txtBroaddB;

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
        createNotificationChannel(); //알림 위한 채널 생성
        checkAudioPermission();

        ImageButton btnSettings = (ImageButton) findViewById(R.id.btnSettings); //설정
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

        txtBroaddB = (TextView) findViewById(R.id.txtBroaddB); //브로드캐스트 값 확인

        btnSettings.setOnClickListener(new View.OnClickListener() { //설정
            @Override
            public void onClick(View view) { // 새 창 Settings.java
                Intent intent = new Intent(getApplicationContext(), DecibelSettings.class);
                startActivity(intent);
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

        decibelReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d("MainActivity", "Broadcast received in MainActivity");

                if ("com.example.soundwatch.ACTION_UPDATE_DECIBEL".equals(intent.getAction())) {
                    double decibel = intent.getDoubleExtra("decibel", 0.0);
                    Log.d("MainActivity", "Received decibel in activity: " + decibel);
                    runOnUiThread(() -> {
                        if (txtBroaddB != null) {
                            txtBroaddB.setText(String.format(Locale.getDefault(), "%.2f dB", decibel));
                        } else {
                            Log.e("MainActivity", "txtBroaddB is null");
                        }
                    });
                }
            }
        };
    }

    private void createNotificationChannel() { //알림을 위한 채널 생성
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is not in the Support Library.

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("소음 알림");

            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this.
            manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            manager.createNotificationChannel(channel);
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
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

        Log.d("MainActivity", "onResume called, registering receiver");
        IntentFilter filter = new IntentFilter("com.example.soundwatch.ACTION_UPDATE_DECIBEL");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(decibelReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(decibelReceiver, filter);
        }

        SharedPreferences backGroundprefs = getSharedPreferences("servicePrefer", MODE_PRIVATE);
        boolean isRunning = backGroundprefs.getBoolean("service_running", false);

        if (isRunning) {
            Intent serviceIntent = new Intent(this, ForegroundService.class);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(this, serviceIntent);
            } else {
                startService(serviceIntent);
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
        String userId = prefs.getString("userId", null);
        if (userId != null) {
            // 로그아웃 시 is_online alter
            IsOnlineUtil.updateIsOnline(userId);
        }
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

    @Override
    protected void onPause() {
        super.onPause();
        if (decibelReceiver != null) {
            unregisterReceiver(decibelReceiver);
        }
    }

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 205;

    private void checkAudioPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.RECORD_AUDIO},
                    REQUEST_RECORD_AUDIO_PERMISSION);
        }
    }

}
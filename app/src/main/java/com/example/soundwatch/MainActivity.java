package com.example.soundwatch;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
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

        createNotificationChannel(); //알림 위한 채널 생성
        checkAudioPermission();

        ImageButton btnSettings = (ImageButton) findViewById(R.id.btnSettings); //설정
        Button btnMeasure = (Button) findViewById(R.id.btnMeasure); //측정
        Button btnGroup = (Button) findViewById(R.id.btnGroup); //기록
        Button btnRecord = (Button) findViewById(R.id.btnRecord); //그룹

        txtBroaddB = (TextView) findViewById(R.id.txtBroaddB); //브로드캐스트 값 확인

        btnSettings.setOnClickListener(new View.OnClickListener() { //설정
            @Override
            public void onClick(View view) { // 새 창 Settings.java
                Intent intent = new Intent(getApplicationContext(), Settings.class);
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
                Intent intent = new Intent(getApplicationContext(), RecodeActivity.class);
                startActivity(intent);
            }
        });

        btnGroup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(MainActivity.this, "업데이트 예정", Toast.LENGTH_SHORT).show();
            }
        });

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

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(decibelReceiver);
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
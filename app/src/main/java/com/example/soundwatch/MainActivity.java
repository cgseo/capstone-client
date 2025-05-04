package com.example.soundwatch;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    //상단바 알림 위함
    private static String CHANNEL_ID = "channel1";
    private static String CHANNEL_NAME = "channel1";
    NotificationManager manager;

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

        ImageButton btnSettings = (ImageButton) findViewById(R.id.btnSettings); //설정
        Button btnMeasure = (Button) findViewById(R.id.btnMeasure); //측정
        Button btnGroup = (Button) findViewById(R.id.btnGroup); //기록
        Button btnRecord = (Button) findViewById(R.id.btnRecord); //그룹

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

}
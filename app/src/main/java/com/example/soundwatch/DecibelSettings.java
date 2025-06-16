package com.example.soundwatch;

import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.view.View;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import java.util.Locale;

public class DecibelSettings extends MainActivity {

    private TextView txtWaringdB;
    public int selectedDecibel; //초기값 30으로 설정
    private Switch swBackgroundOnOff;

    //시간 설정용
    private SharedPreferences timePrefs;
    private TextView txtStartTime, txtStopTime;
    private Button btnSetStartTime, btnSetStopTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        //소음 경고 데시벨 값. 없으면 초기값 30
        SharedPreferences sharedPref = getSharedPreferences("SoundWatchPrefs", Context.MODE_PRIVATE);
        selectedDecibel = sharedPref.getInt("selectedDecibel", 30);

        txtWaringdB = (TextView) findViewById(R.id.txtWaringdB);
        txtWaringdB.setText(selectedDecibel + "dB");
        txtWaringdB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDecibelPickerDialog();
            }
        });

        //백그라운드
        swBackgroundOnOff = (Switch) findViewById(R.id.swBackgroundOnOff);

        //백그라운드 스위치값 저장용
        SharedPreferences prefs = getSharedPreferences("servicePrefer", MODE_PRIVATE);

        //저장된 스위치 값 복원
        boolean isRunning = prefs.getBoolean("service_running", false);
        swBackgroundOnOff.setChecked(isRunning);

        swBackgroundOnOff.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                prefs.edit().putBoolean("service_running", isChecked).apply();

                if(isChecked){
                    Intent intent = new Intent(DecibelSettings.this, ForegroundService.class);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        ContextCompat.startForegroundService(DecibelSettings.this, intent);
                    } else {
                        startService(intent);
                    }
                    Toast.makeText(DecibelSettings.this, "백그라운드 측정을 시작합니다.", Toast.LENGTH_SHORT).show();

                } else {
                    //체크되어있지 않으면 아무런 측정도 X, 이미 체크되어있었다면 백그라운드 중지
                    Intent intent = new Intent(DecibelSettings.this, ForegroundService.class);
                    stopService(intent);
                    Toast.makeText(DecibelSettings.this, "백그라운드 측정을 종료합니다.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        timePrefs = getSharedPreferences("soundwatch_time_prefs", MODE_PRIVATE);
        txtStartTime = (TextView) findViewById(R.id.txtStartTime);
        txtStopTime = (TextView) findViewById(R.id.txtStopTime);
        btnSetStartTime = (Button) findViewById(R.id.btnSetStartTime);
        btnSetStopTime = (Button) findViewById(R.id.btnSetStopTime);

        int startHour = timePrefs.getInt("start_hour", 9);
        int startMinute = timePrefs.getInt("start_minute", 0);
        int stopHour = timePrefs.getInt("stop_hour", 18);
        int stopMinute = timePrefs.getInt("stop_minute", 0);

        btnSetStartTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showTimePicker(true);
            }
        });
        btnSetStopTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showTimePicker(false);
            }
        });

        updateTimeText();
        AlarmSetting.setRepeating(DecibelSettings.this, startHour, startMinute, "com.example.soundwatch.START_MEASUREMENT");
        AlarmSetting.setRepeating(DecibelSettings.this, stopHour, stopMinute, "com.example.soundwatch.STOP_MEASUREMENT");

    }

    private void showDecibelPickerDialog() {
        DialogWarning dialogWarning = new DialogWarning(this, selectedDecibel, new DialogWarning.OnNumberSelectedListener() {
            public void onNumberSelected(int number) {
                selectedDecibel = number; //선택된 값 가져옴
                txtWaringdB.setText(String.valueOf(selectedDecibel) + "dB"); // 화면에 표시

                saveWarningDecibel(selectedDecibel); //decibel값 저장 -> 서버?
            }
        });
        dialogWarning.show();
    }

    private void saveWarningDecibel(int decibel) { //데시벨 값 저장 -> Activity간 교환 위함
        SharedPreferences sharedPref = getSharedPreferences("SoundWatchPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt("selectedDecibel", decibel);
        editor.apply();
    }

    private void showTimePicker(boolean isStart) { //타임피커로 예약 시간 설정
        int hour = timePrefs.getInt(isStart ? "start_hour" : "stop_hour", 9); //저장
        int minute = timePrefs.getInt(isStart ? "start_minute" : "stop_minute", 0);

        TimePickerDialog dialog = new TimePickerDialog(this,
                (view, hourOfDay, minute1) -> {
                    SharedPreferences.Editor editor = timePrefs.edit();
                    if (isStart) {
                        editor.putInt("start_hour", hourOfDay);
                        editor.putInt("start_minute", minute1);
                    } else {
                        editor.putInt("stop_hour", hourOfDay);
                        editor.putInt("stop_minute", minute1);
                    }
                    editor.apply();

                    if (isStart) {
                        AlarmSetting.setRepeating(this, hourOfDay, minute1, "com.example.soundwatch.START_MEASUREMENT");
                    } else {
                        AlarmSetting.setRepeating(this, hourOfDay, minute1, "com.example.soundwatch.STOP_MEASUREMENT");
                    }


                    updateTimeText();
                }, hour, minute, true);
        dialog.show();
    }

    private void updateTimeText() { //설정 화면에 시간 표시
        int startHour = timePrefs.getInt("start_hour", 9);
        int startMinute = timePrefs.getInt("start_minute", 0);
        int stopHour = timePrefs.getInt("stop_hour", 18);
        int stopMinute = timePrefs.getInt("stop_minute", 0);

        txtStartTime.setText(String.format(Locale.getDefault(), "시작 시간: %02d:%02d", startHour, startMinute));
        txtStopTime.setText(String.format(Locale.getDefault(), "종료 시간: %02d:%02d", stopHour, stopMinute));
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateTimeText();
    }

}

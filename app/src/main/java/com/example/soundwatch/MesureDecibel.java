package com.example.soundwatch;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import androidx.core.app.NotificationCompat;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;

public class MesureDecibel extends MainActivity {

    private static final int SAMPLE_RATE = 44100;  //모든 장치에서 작동이 보장되는 유일한 속도. 헤르츠로 표현된 샘플링 속도
    private TextView decibelText;  // 현재 데시벨
    // UI 업데이트 핸들러는 전역 변수로 선언하여 시작/정지 시 접근 가능하게 함
    private Handler updateHandler;
    private Runnable updateRunnable;

    private Timestamp startTime;
    private Timestamp endTime;
    private double maxDecibel = 0.0; // maxDecibel 초기화 및 측정 중 업데이트 필요
    private LocationManager locationManager;
    private LocationListener locationListener;
    private String currentLocation = "위치 정보 없음";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 101;
    private NoiseLogApi noiseLogApi;

    // 그래프를 위한 변수
    private LineChart lineChart;
    private LineDataSet lineDataSet;
    private LineData lineData;
    private int xIndex = 0;

    // 소음 경고
    DecibelCheck decibelCheck;

    private static String CHANNEL_ID = "channel1";

    int warningDecibel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mesure);

        Button btnMesureStart = findViewById(R.id.btnMesureStart);
        noiseLogApi = RetrofitClient.getApiService();
        warningDecibel = getWarningDecibel();
        decibelText = findViewById(R.id.txtNowdB);

        decibelCheck = new DecibelCheck(this);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                currentLocation = String.format("위도: %.4f, 경도: %.4f", location.getLatitude(), location.getLongitude());
            }

            @Override
            public void onProviderDisabled(@NonNull String provider) {
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(() -> Toast.makeText(MesureDecibel.this, "GPS를 켜주세요.", Toast.LENGTH_SHORT).show());
            }
        };

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            requestLocationUpdates();
        }

        lineChart = findViewById(R.id.chart);
        lineDataSet = new LineDataSet(new ArrayList<>(), "현재 데시벨"); // 라벨 변경
        lineDataSet.setColor(Color.BLACK);
        lineDataSet.setDrawCircles(false);
        lineDataSet.setLineWidth(2f);
        lineData = new LineData(lineDataSet);
        lineChart.setData(lineData);

        lineChart.getDescription().setEnabled(false);
        lineChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        lineChart.getAxisRight().setEnabled(false);
        lineChart.getLegend().setEnabled(false);
        lineChart.getAxisLeft().setDrawLabels(false);

        YAxis yAxis = lineChart.getAxisLeft();
        yAxis.setAxisMinimum(0f);
        yAxis.setAxisMaximum(120f);

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setLabelRotationAngle(-45f);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "소음 경고를 위한 채널";
            String description = "소음 경고";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 2);
            }
        }

        // updateHandler와 updateRunnable을 onCreate 밖에서 정의하여 접근 가능하게 함
        updateHandler = new Handler();
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                double db = decibelCheck.getCurrentDecibel();
                decibelText.setText(String.format("%.1f dB", db));
                lineData.addEntry(new Entry(xIndex++, (float) db), 0);
                lineData.notifyDataChanged();
                lineChart.notifyDataSetChanged();
                lineChart.setVisibleXRangeMaximum(20);
                lineChart.moveViewToX(lineData.getEntryCount());

                // 측정 중 maxDecibel 업데이트
                if (db > maxDecibel) {
                    maxDecibel = db;
                }

                if (db > warningDecibel) {
                    warningDecibelNotification();
                }
                if (decibelCheck.isRecording()) { // 녹음 중일 때만 반복 실행
                    updateHandler.postDelayed(this, 200);
                }
            }
        };

        btnMesureStart.setOnClickListener(view -> {
            if (ActivityCompat.checkSelfPermission(MesureDecibel.this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MesureDecibel.this, new String[]{Manifest.permission.RECORD_AUDIO}, 1);
            } else {
                if (!decibelCheck.isRecording()) {
                    // 측정 시작
                    decibelCheck.startMesure();
                    startTime = new Timestamp(System.currentTimeMillis());
                    maxDecibel = 0.0; // 새로운 측정 시작 시 maxDecibel 초기화
                    xIndex = 0; // 차트 X축 인덱스 초기화
                    lineDataSet.clear(); // 이전 차트 데이터 초기화
                    lineChart.invalidate(); // 차트 새로 고침
                    updateHandler.post(updateRunnable); // 측정 시작 시 Runnable 실행
                    btnMesureStart.setText("STOP");
                } else {
                    // 측정 중지
                    decibelCheck.stopMesure();
                    endTime = new Timestamp(System.currentTimeMillis());
                    updateHandler.removeCallbacks(updateRunnable); // 중요: 핸들러 콜백 중지
                    saveNoiseLogToServer();
                    btnMesureStart.setText("START");
                }
            }
        });
    }

    private void requestLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, locationListener);
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 1, locationListener);
        }
    }

    private int getWarningDecibel() {
        SharedPreferences sharedPref = getSharedPreferences("SoundWatchPrefs", Context.MODE_PRIVATE);
        return sharedPref.getInt("selectedDecibel", 30);
    }

    private void saveNoiseLogToServer() {
        double currentDecibel;
        try {
            // 현재 decibelText에 표시된 값은 마지막 측정값임.
            // 필요하다면 이 값을 NoiseLog의 noiseLevel로 사용하거나,
            // 전체 측정 기간 동안의 평균값 등을 고려할 수 있음.
            currentDecibel = Double.parseDouble(decibelText.getText().toString().replace(" dB", ""));
        } catch (NumberFormatException e) {
            Toast.makeText(this, "데시벨 값 파싱 오류", Toast.LENGTH_SHORT).show();
            return;
        }

        NoiseLog noiseLog = new NoiseLog();
        noiseLog.setNoiseLevel(currentDecibel); // 마지막 측정된 데시벨 값
        long durationInMillis = endTime.getTime() - startTime.getTime();
        noiseLog.setLogTime(durationInMillis);
        noiseLog.setStartTime(startTime);
        noiseLog.setEndTime(endTime);
        noiseLog.setLocation(currentLocation);
        noiseLog.setMaxDb(maxDecibel); // 측정 중 업데이트된 최댓값 사용

        SharedPreferences prefs = getApplicationContext().getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        String userId = prefs.getString("userId", null);
        if (userId != null) {
            noiseLog.setUserId(Integer.parseInt(userId));
        }
        noiseLog.setGroupId(prefs.getString("groupId", null));

        Call<Integer> call = noiseLogApi.insertNoiseLog(noiseLog);
        call.enqueue(new Callback<Integer>() {
            @Override
            public void onResponse(Call<Integer> call, Response<Integer> response) {
                if (response.isSuccessful() && response.body() != null) {
                    int newLogId = response.body();
                    Toast.makeText(MesureDecibel.this, "소음 기록 저장 성공, ID: " + newLogId, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MesureDecibel.this, "소음 기록 저장 실패", Toast.LENGTH_SHORT).show();
                    // 오류 응답 본문 로깅하여 디버깅에 활용
                    try {
                        Log.e("NoiseLog", "Error response: " + response.errorBody().string());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(Call<Integer> call, Throwable t) {
                Toast.makeText(MesureDecibel.this, "소음 기록 저장 실패 (네트워크 오류)", Toast.LENGTH_SHORT).show();
                Log.e("NoiseLog", "Network error: " + t.getMessage(), t); // 네트워크 오류 로깅
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (locationManager != null && locationListener != null) {
            locationManager.removeUpdates(locationListener);
        }
        // 액티비티 종료 시 Handler 콜백도 확실히 제거
        if (updateHandler != null && updateRunnable != null) {
            updateHandler.removeCallbacks(updateRunnable);
        }
        if (decibelCheck != null && decibelCheck.isRecording()) {
            decibelCheck.stopMesure();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // 권한 부여 후 별도로 decibelCheck.startMesure()를 호출할 필요 없음.
            // 버튼 클릭 시 다시 로직을 타게 됨.
            Toast.makeText(this, "오디오 녹음 권한이 허용되었습니다.", Toast.LENGTH_SHORT).show();
        }
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            requestLocationUpdates();
            Toast.makeText(this, "위치 권한이 허용되었습니다.", Toast.LENGTH_SHORT).show();
        } else if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            Toast.makeText(this, "위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
        }
        if (requestCode == 2 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d("Permission", "알림 권한 허용됨");
        } else if (requestCode == 2) {
            Toast.makeText(this, "알림 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
        }
    }

    public void warningDecibelNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_warning)
                .setContentTitle("소음 경고!")
                .setContentText(warningDecibel + "dB보다 큰 소음이 발생 중입니다.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            manager.notify(1, builder.build());
        }
    }

    public class TimeAxisValueFormatter extends ValueFormatter {
        private final long startTimeMillis;

        public TimeAxisValueFormatter(long startTimeMillis) {
            this.startTimeMillis = startTimeMillis;
        }

        @Override
        public String getFormattedValue(float value) {
            long millisOffset = (long) value * 1000L;
            long timeMillis = startTimeMillis + millisOffset;
            SimpleDateFormat sdf = new SimpleDateFormat("mm:ss", Locale.getDefault());
            return sdf.format(new Date(timeMillis));
        }
    }
}
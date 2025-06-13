package com.example.soundwatch;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
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

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

// 그래프를 위한 import
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;

public class MesureDecibel extends MainActivity {

    boolean isRecording = false; // 측정 중인지 판단
    private static final int SAMPLE_RATE = 44100; //모든 장치에서 작동이 보장되는 유일한 속도. 헤르츠로 표현된 샘플링 속도
    private AudioRecord audioRecord; //오디오 API
    private int bufferSize; //오디오 녹음 저장 버퍼
    private TextView decibelText; // 현재 데시벨
    private Handler uiHandler = new Handler();


    // 측정을 위해 추가
    private Timestamp startTime; // 측정 시작 시간
    private Timestamp endTime; // 측정 종료 시간
    private double maxDecibel = 0.0; // 최대 데시벨 값
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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mesure);

        Button btnMesureStart = (Button) findViewById(R.id.btnMesureStart); // 측정 시작
        noiseLogApi = RetrofitClient.getApiService(); // NoiseLogApi 인스턴스 생성

        // 위치 정보 획득
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                currentLocation = String.format("위도: %.4f, 경도: %.4f", location.getLatitude(), location.getLongitude());
            }

            @Override
            public void onProviderEnabled(@NonNull String provider) {
            }

            @Override
            public void onProviderDisabled(@NonNull String provider) {
                Toast.makeText(MesureDecibel.this, "GPS를 켜주세요.", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }
        };

        // 위치 권한 확인 및 요청
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            requestLocationUpdates();
        }

        lineChart = findViewById(R.id.chart);
        lineDataSet = new LineDataSet(new ArrayList<>(), "최대 데시벨");
        lineDataSet.setColor(Color.BLACK);
        lineDataSet.setDrawCircles(false);
        lineDataSet.setLineWidth(2f);

        lineData = new LineData(lineDataSet);
        lineChart.setData(lineData);

        // 그래프 설정
        lineChart.getDescription().setEnabled(false);
        lineChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        lineChart.getAxisRight().setEnabled(false);
        lineChart.getLegend().setEnabled(false);
        lineChart.getAxisLeft().setDrawLabels(false);

        YAxis yAxis = lineChart.getAxisLeft();
        yAxis.setAxisMinimum(0f);    // 그래프의 최소 데시벨
        yAxis.setAxisMaximum(120f);  // 그래프의 최대 데시벨

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f); // 초 단위로 간격 고정
        xAxis.setLabelRotationAngle(-45f); // 라벨이 겹치지 않게


        btnMesureStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ActivityCompat.checkSelfPermission(MesureDecibel.this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    // 권한 요청
                    ActivityCompat.requestPermissions(MesureDecibel.this, new String[]{Manifest.permission.RECORD_AUDIO}, 1);
                } else {
                    if (!isRecording) {
                        startMesure(); // 측정 시작
                        btnMesureStart.setText("STOP");
                    } else {
                        stopMesure(); // 측정 중단
                        btnMesureStart.setText("START");
                    }
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
    private void startMesure() {
        bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        // 권한 요청2
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            ActivityCompat.requestPermissions(MesureDecibel.this, new String[]{Manifest.permission.RECORD_AUDIO}, 1);
            return;
        }
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, //마이크로 들어오는 음성을 받기 위해선 1
                SAMPLE_RATE, //샘플 헤르츠
                AudioFormat.CHANNEL_IN_MONO, //상수 값: 16(0x00000010)
                AudioFormat.ENCODING_PCM_16BIT, //샘플당 16비트 PCM
                bufferSize); // 객체 생성

        isRecording = true;
        audioRecord.startRecording(); // 녹음 시작
        startTime = new Timestamp(System.currentTimeMillis()); // 측정 시작 시간 기록
        maxDecibel = 0.0; // 최대 데시벨

        // 그래프 x축을 위해 초 단위 시간 기록
        long startTimeMillis = System.currentTimeMillis();
        lineChart.getXAxis().setValueFormatter(new TimeAxisValueFormatter(startTimeMillis));



        Thread recordingThread = new Thread(() -> {
            short[] buffer = new short[bufferSize];

            while (isRecording) {
                int read = audioRecord.read(buffer, 0, buffer.length); // 버퍼에 저장
                double sum = 0; //RMS 계산을 위해 전체 제곱 더함
                for (int i = 0; i < read; i++) {
                    sum += buffer[i] * buffer[i];
                }

                double rms = Math.sqrt(sum / read); //RMS 계산
                double decibel = 20 * Math.log10(rms); //RMS -> dB 변환
                if (decibel < 0) decibel = 0; //음수 오류 방지

                // 최대 데시벨 저장
                if (decibel > maxDecibel) {
                    maxDecibel = decibel;
                }

                decibelText = (TextView) findViewById(R.id.txtNowdB);
                double finalDecibel = decibel; //최종 데시벨 값
                uiHandler.post(() -> {
                    decibelText.setText(String.format("%.2f dB", finalDecibel));

                    // 그래프에 새 데이터 추가
                    lineData.addEntry(new Entry(xIndex++, (float) finalDecibel), 0);
                    lineData.notifyDataChanged();
                    lineChart.notifyDataSetChanged();
                    lineChart.setVisibleXRangeMaximum(20); // 한 화면에 최대 20개 점
                    lineChart.moveViewToX(lineData.getEntryCount());
                });

                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        recordingThread.start();
    }

    private void stopMesure() { //측정 종료
        isRecording = false;
        endTime = new Timestamp(System.currentTimeMillis()); // 측정 종료 시간 기록
        if (audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }
        saveNoiseLogToServer(); // MySQL로 데이터 저장
    }
    private void saveNoiseLogToServer() {
        double currentDecibel; // 현재 데시벨
        try {
            // decibelText에서 double 형 데시벨 값을 가져온다.
            currentDecibel = Double.parseDouble(decibelText.getText().toString().replace(" dB", ""));
        } catch (NumberFormatException e) {
            Log.e("MesureDecibel", "데시벨 값 파싱 오류: " + e.getMessage());
            Toast.makeText(this, "데시벨 값 파싱 오류", Toast.LENGTH_SHORT).show();
            return;
        }

        NoiseLog noiseLog = new NoiseLog(); // 측정된 소음 데이터를 담기 위한 객체
        noiseLog.setNoiseLevel(currentDecibel);
        // 총 측정 시간을 밀리초 단위로 계산
        long durationInMillis = endTime.getTime() - startTime.getTime();
        noiseLog.setLogTime(durationInMillis); // logTime에 총 측정 시간 저장
        noiseLog.setStartTime(startTime); // 측정 시작 시간
        noiseLog.setEndTime(endTime);   // 측정 종료 시간
        noiseLog.setLocation(currentLocation); // 현재 위치 정보
        noiseLog.setMaxDb(maxDecibel); // 최대 데시벨 크기

        SharedPreferences prefs = getApplicationContext().getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        String userId = prefs.getString("userId", null);
        if(userId != null){
            noiseLog.setUserId(Integer.parseInt(userId)); // 사용자 ID
        }

        noiseLog.setGroupId(prefs.getString("groupId", null));

        Log.d("nosie_log", prefs.getString("userId", null)+ " "+prefs.getString("groupId", null));

        // 서버에 소음 데이터를 저장하는 API 호출
        Call<Integer> call = noiseLogApi.insertNoiseLog(noiseLog);
        call.enqueue(new Callback<Integer>() {
            @Override
            public void onResponse(Call<Integer> call, Response<Integer> response) {
                if (response.isSuccessful() && response.body() != null) {
                    int newLogId = response.body();
                    Toast.makeText(MesureDecibel.this, "소음 기록 저장 성공, ID: " + newLogId, Toast.LENGTH_SHORT).show();
                    Log.d("API Response", "소음 기록 저장 성공, ID: " + newLogId);
                } else {
                    Toast.makeText(MesureDecibel.this, "소음 기록 저장 실패", Toast.LENGTH_SHORT).show();
                    Log.e("API Error", "응답 실패: " + response.code() + " - " + response.message());
                }
            }

            @Override
            public void onFailure(Call<Integer> call, Throwable t) {
                Toast.makeText(MesureDecibel.this, "소음 기록 저장 실패 (네트워크 오류)", Toast.LENGTH_SHORT).show();
                Log.e("API Failure", "네트워크 오류: " + t.getMessage());
            }
        });
    }

    // 그래프 x축 시간 설정
    public class TimeAxisValueFormatter extends ValueFormatter {
        private final long startTimeMillis;

        public TimeAxisValueFormatter(long startTimeMillis) {
            this.startTimeMillis = startTimeMillis;
        }

        @Override
        public String getFormattedValue(float value) {
            long millisOffset = (long) value * 1000L; // X값은 초 단위로 가정
            long timeMillis = startTimeMillis + millisOffset;

            SimpleDateFormat sdf = new SimpleDateFormat("mm:ss", Locale.getDefault());
            return sdf.format(new Date(timeMillis));
        }
    }


    @Override
    protected void onDestroy() { //화면 종료 or 앱 종료 시
        super.onDestroy();
        isRecording = false;
        if (audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }
        // locationManager 제거
        if (locationManager != null && locationListener != null) {
            locationManager.removeUpdates(locationListener);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1 && grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startMesure();
        } else {
            decibelText.setText("데시벨 측정을 위해 녹음 권한이 필요합니다.");
        }
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            requestLocationUpdates();
        } else {
            decibelText.setText("위치 정보 확인을 위해 위치 정보 권한 필요합니다.");
        }
    }
}
package com.example.soundwatch;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
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
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import java.sql.Timestamp;

import javax.xml.transform.OutputKeys;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MesureDecibel extends MainActivity {

    //boolean isRecording = false; // 측정 중인지 판단
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
    DecibelCheck decibelCheck;

    //상단바 알림 위함
    private static String CHANNEL_ID = "channel1";

    int warningDecibel; // 경고 소음 수준

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mesure);

        Button btnMesureStart = (Button) findViewById(R.id.btnMesureStart); // 측정 시작
        noiseLogApi = RetrofitClient.getApiService(); // NoiseLogApi 인스턴스 생성
        warningDecibel = getWarningDecibel();
        decibelText = (TextView) findViewById(R.id.txtNowdB);

        decibelCheck = new DecibelCheck(this); //측정 알고리즘 파일

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
                Handler handler = new Handler(Looper.getMainLooper());
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run()
                    {
                        Toast.makeText(MesureDecibel.this, "GPS를 켜주세요.", Toast.LENGTH_SHORT).show();
                    }
                }, 0);
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

        //알림 채널 설정
            // Create the NotificationChannel, but only on API 26+ because
            // the NotificationChannel class is not in the Support Library.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                CharSequence name = "소음 경고를 위한 채널";
                String description = "소음 경고";
                int importance = NotificationManager.IMPORTANCE_DEFAULT;
                NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
                channel.setDescription(description);
                // Register the channel with the system; you can't change the importance
                // or other notification behaviors after this.
                NotificationManager notificationManager = getSystemService(NotificationManager.class);
                notificationManager.createNotificationChannel(channel);
            }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 2);
            }
        }


        btnMesureStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ActivityCompat.checkSelfPermission(MesureDecibel.this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    // 권한 요청
                    ActivityCompat.requestPermissions(MesureDecibel.this, new String[]{Manifest.permission.RECORD_AUDIO}, 1);
                } else {
                    if (!decibelCheck.isRecording()) {
                        decibelCheck.startMesure();
                        startTime = new Timestamp(System.currentTimeMillis());
                        Handler handler = new Handler();
                        Runnable updateRunnable = new Runnable() {
                            @Override
                            public void run() {
                                double db = decibelCheck.getCurrentDecibel();
                                decibelText.setText(String.format("%.1f dB", db));
                                handler.postDelayed(this, 200); // 0.2초마다 UI 업데이트
                                if (db > warningDecibel) {
                                    warningDecibelNotification();
                                }
                            }
                        };
                        handler.post(updateRunnable);
                        //startMesure(); // 측정 시작
                        btnMesureStart.setText("STOP");
                    } else {
                        //stopMesure(); // 측정 중단
                        decibelCheck.stopMesure();
                        endTime = new Timestamp(System.currentTimeMillis());
                        saveNoiseLogToServer(); // MySQL로 데이터 저장
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

    /*
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

                //decibelText = (TextView) findViewById(R.id.txtNowdB);
                double finalDecibel = decibel; //최종 데시벨 값
                uiHandler.post(() -> decibelText.setText(String.format("%.2f dB", finalDecibel)));

                if(finalDecibel > warningDecibel) {
                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run()
                        {
                            Toast.makeText(MesureDecibel.this, "경고! 큰 소음이 발생 중입니다. " + warningDecibel + "dB 이상입니다.", Toast.LENGTH_SHORT).show();
                        }
                    }, 0);
                    warningDecibelNotification();
                }

                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        recordingThread.start();
    }
    */

    private int getWarningDecibel() { //저장한 데시벨 값 가져오기
        SharedPreferences sharedPref = getSharedPreferences("SoundWatchPrefs", Context.MODE_PRIVATE);
        return sharedPref.getInt("selectedDecibel", 30); //있으면 그대로, 없으면 기본값 30으로
    }

    /*
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
     */

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
        noiseLog.setUserId(1); // 임시 사용자 ID

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
    @Override
    protected void onDestroy() { //화면 종료 or 앱 종료 시
        super.onDestroy();
        //isRecording = false;
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
            //startMesure();
            decibelCheck = new DecibelCheck(this);
            decibelCheck.startMesure();
            Handler handler = new Handler();
            Runnable updateRunnable = new Runnable() {
                @Override
                public void run() {
                    double db = decibelCheck.getCurrentDecibel();
                    decibelText.setText(String.format("%.1f dB", db));
                    handler.postDelayed(this, 200); // 0.2초마다 UI 업데이트
                }
            };
            handler.post(updateRunnable);
        } else {
            decibelText.setText("데시벨 측정을 위해 녹음 권한이 필요합니다.");
        }
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            requestLocationUpdates();
        } else {
            decibelText.setText("위치 정보 확인을 위해 위치 정보 권한 필요합니다.");
        }
        if (requestCode == 2 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d("Permission", "알림 권한 허용됨");
        } else if (requestCode == 2) {
            Toast.makeText(this, "알림 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
        }

    }

    public void warningDecibelNotification() { //알림 보냄
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_warning)
                .setContentTitle("소음 경고!")
                .setContentText(warningDecibel + "dB보다 큰 소음이 발생 중입니다.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {

            // TODO: Consider calling
            // ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            // public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                        int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.

            return;
        }
        manager.notify(1, builder.build());
    }
}

package com.example.soundwatch;

import android.Manifest;
import android.app.ForegroundServiceStartNotAllowedException;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;
import androidx.core.content.ContextCompat;

public class ForegroundService extends Service {

    public static final String EXTRA_DECIBEL_VALUE = "com.example.soundwatch.EXTRA_DECIBEL_VALUE";
    private static final String CHANNEL_ID = "channel1";
    private static final int NOTIFICATION_ID = 103;
    DecibelCheck decibelCheck; //데시벨 측정 기능

    //브로드캐스트 위함
    private Handler handler;
    private Runnable broadcastRunnable;
    private HandlerThread handlerThread;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (decibelCheck != null) {
            decibelCheck.stopMesure();
            decibelCheck = null;
        }

        handler.removeCallbacks(broadcastRunnable);
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
        if (handlerThread != null) {
            handlerThread.quitSafely();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) { //여기서 호출해야 서비스 작동
        createNotificationChannel(); //알림 채널 생성
        startForeground(); //서비스 시작
        if (decibelCheck == null) {
            decibelCheck = new DecibelCheck(this);
            decibelCheck.startMesure();
        }
        startBroadcastLoop();

        return START_STICKY; //임의의 기간 동안 명시적으로 시작되고 중지되는 작업에 적합
    }

    private void startForeground() { //서비스 시작 함수
        // 권한 체크
        int audioPermission =
                ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
        if (audioPermission == PackageManager.PERMISSION_DENIED) {
            // Without camera permissions the service cannot run in the
            // foreground. Consider informing user or updating your app UI if
            // visible.
            stopSelf();
            return;
        }

        try { //백그라운드 측정 중 알림
            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("소음 측정 중")
                    .setContentText("백그라운드에서 데시벨을 측정하고 있습니다.")
                    .setSmallIcon(R.drawable.ic_notification)
                    .setOngoing(true)
                    .build();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);
            } else {
                startForeground(1, notification);
            }

            /*
            ServiceCompat.startForeground(
                    this,
                    NOTIFICATION_ID,
                    notification,
                    type
            );
            */
        } catch (Exception e) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    e instanceof ForegroundServiceStartNotAllowedException) {
                Log.e("DecibelService", "백그라운드에서 포그라운드 서비스 시작 불가");
            }
            stopSelf();
        }
    }

    private void createNotificationChannel() { //알림을 위한 채널 개설
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "소음 서비스 채널",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void startBroadcastLoop() {
        handlerThread = new HandlerThread("DecibelBroadcastThread");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());

        broadcastRunnable = new Runnable() {
            @Override
            public void run() {
                if (decibelCheck != null && decibelCheck.isRecording()) {
                    double decibel = decibelCheck.getCurrentDecibel();

                    Intent intent = new Intent("com.example.soundwatch.ACTION_UPDATE_DECIBEL");
                    intent.putExtra("decibel", decibel);
                    intent.setPackage("com.example.soundwatch"); //앱 내 리시버만 수신 가능

                    Log.d("ForegroundService", "Sending decibel broadcast: " + decibel);

                    sendBroadcast(intent);

                    handler.postDelayed(this, 1000); //1초마다 반복
                }
            }
        };

        handler.post(broadcastRunnable); //첫 실행
    }

}

package com.example.soundwatch;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

// wifi 연결을 확인하여 그룹 생성/가입 시 db에 저장하였던 wifi bssid와 일치하는 경우,
// sharedpreferenced에 group_id 변수를 해당 그룹의 id로 변경하여
// 측정한 noise를 그룹별로 저장하기 위한 목적

// 연결 변동 시 동작
// sharedpreferenced group_id
    // http 요청으로 wifi BSSID로 가입된 그룹이 있는 경우, 해당 그룹의 group_id를 저장
    // 아닌 경우(혹은 오류) null
public class WifiForegroundService extends Service {

    private ConnectivityManager.NetworkCallback networkCallback;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // foreground 알림 등록
        createNotificationChannel();
        Notification notification = new NotificationCompat.Builder(this, "wifi_channel")
                .setContentTitle("Wi-Fi 추적 중")
                .setContentText("네트워크 변경 감지 중")
                .build();
        startForeground(1, notification);

        // wifi 연결 request 생성
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkRequest request = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build();
        Log.d("wifi_service", "start");


        // wifi 연결에 관한 network callback 생성
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                // wifi 연결된 경우 실행
          //      ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            //    Network network1 = cm.getActiveNetwork();
           //     NetworkCapabilities capabilities = cm.getNetworkCapabilities(network1);


                // 현재 연결된 wifi의 bssid
                WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                String bssid = wifiInfo.getBSSID();
                Log.d("wifi_service", "현재 연결된 BSSID: " + bssid);

                // http 요청을 보내서 해당 와이파이 주소로 그룹에 가입되어있는지 확인
                // 가입되어있는 경우 prefs의 groupId를 해당 그룹의 group_id로 변경
                if (bssid != null) { //&& capabilities != null &&
                       // capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                      //  capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
                    // 현재 연결된 wifi BSSID와 userId를 요청변수로 group_id GET 요청
                    WifiGroupUtil.sendHttp(getApplicationContext(), bssid);
                }
            }

            @Override
            public void onLost(@NonNull Network network) {
                // wifi 연결 해제된 경우 실행
                SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
                prefs.edit().remove("groupId").apply();
                Log.d("wifi_service", "Wi-Fi 연결 끊김");
                Log.d("wifi_service", "groupId:"+prefs.getString("groupId", null));
            }
        };


        // 콜백 함수 등록
        cm.registerNetworkCallback(request, networkCallback);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        // 콜백 삭제
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (networkCallback != null) {
            cm.unregisterNetworkCallback(networkCallback);
        }
        super.onDestroy();
    }



    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "wifi_channel", "Wi-Fi Foreground", NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}


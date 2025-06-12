package com.example.soundwatch;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

// 현재 연결된 wifi BSSID와 userId를 요청변수로
// group_id GET 요청 util
public class WifiGroupUtil {
    public static void sendHttp(Context context, String bssid) {
        SharedPreferences prefs = context.getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        String userId = prefs.getString("userId", null);

        // userId가 없으면(로그인이 안 되어 있으면) > 요청 ㄴ
        if(userId == null) {
            prefs.edit().remove("groupId").apply();
            Log.d("wifi_service", "userId is null, stop thread(groupId:"+prefs.getString("groupId", null));
            return;
        }
        new Thread(() -> {
            try {
                URL url = new URL("http://10.0.2.2:3000/api/group/getGroupIdByWifi?"
                        + "userId=" + userId
                        + "&wifiBssid=" + bssid);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // 해당 bssid로 가입된 그룹이 있는 경우 해당 그룹의 group_id를 prefs에 저장
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;

                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    // JSON 파싱
                    JSONObject json = new JSONObject(response.toString());
                    String groupId = json.optString("group_id", null);  // int면 optInt 사용 가능

                    if (groupId == null) return;

                    prefs.edit().putString("groupId", groupId).apply();
                    Log.d("wifi_service", "groupId: "+prefs.getString("groupId", null));
                } else {
                    // 서버 오류 혹은 해당 그룹이 없음 >>> null
                    prefs.edit().remove("groupId").apply();
                    Log.e("wifi_service", "groupId: "+prefs.getString("groupId", null)+" (해당 wifi BSSID로 가입된 그룹 없음)");
                }

            } catch (Exception e) {
                Log.e("wifi_service", "HTTP 요청 실패, userid: "+ userId, e);
            }
        }).start();


    }

}

package com.example.soundwatch;

import android.util.Log;

import androidx.annotation.NonNull;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class IsOnlineUtil {

    private static final String TAG = "IsOnlineUtil";
    private static final String BASE_SERVER_URL = "http://172.30.1.31:3000/api/noise/users/online";
    private static final OkHttpClient client = new OkHttpClient();

    public static void updateIsOnline(String userId) {
        try {
            HttpUrl.Builder urlBuilder = HttpUrl.parse(BASE_SERVER_URL).newBuilder();
            urlBuilder.addQueryParameter("user_id", userId);

            String url = urlBuilder.build().toString();
            Log.d(TAG, "Request URL: " + url);

            RequestBody body = RequestBody.create("", MediaType.parse("application/json; charset=utf-8"));

            Request request = new Request.Builder()
                    .url(url)
                    .patch(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e(TAG, "is_online PATCH 요청 실패: " + e.getMessage(), e);
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    String responseBody = response.body() != null ? response.body().string() : "No response body";
                    Log.d(TAG, "응답 코드: " + response.code() + ", 메시지: " + response.message() + ", 응답 본문: " + responseBody);

                    if (response.isSuccessful()) {
                        Log.d(TAG, "is_online PATCH 요청 성공");
                    } else {
                        Log.e(TAG, "is_online PATCH 요청 실패 - 상태 코드: " + response.code() + ", 메시지: " + response.message() + ", 응답 본문: " + responseBody);
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "요청 생성 또는 발송 오류: " + e.getMessage(), e);
        }
    }
}
package com.example.soundwatch;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.content.SharedPreferences;
import android.util.Log;

public class AlarmBroadReciver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d("AlarmBroadReciver", "Received alarm: " + action);

        if ("com.example.soundwatch.START_MEASUREMENT".equals(action)) {

            Intent serviceIntent = new Intent(context, ForegroundService.class);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }

            // 다음 날 알람 재등록
            SharedPreferences prefs = context.getSharedPreferences("soundwatch_time_prefs", Context.MODE_PRIVATE);
            int hour = prefs.getInt("start_hour", 9);
            int minute = prefs.getInt("start_minute", 0);
            AlarmSetting.setRepeating(context, hour, minute, "com.example.soundwatch.START_MEASUREMENT");

        } else if ("com.example.soundwatch.STOP_MEASUREMENT".equals(action)) {
            context.stopService(new Intent(context, ForegroundService.class));

            // 다음 날 알람 재등록
            SharedPreferences prefs = context.getSharedPreferences("soundwatch_time_prefs", Context.MODE_PRIVATE);
            int hour = prefs.getInt("start_hour", 9);
            int minute = prefs.getInt("start_minute", 0);
            AlarmSetting.setRepeating(context, hour, minute, "com.example.soundwatch.START_MEASUREMENT");
        }
    }
}

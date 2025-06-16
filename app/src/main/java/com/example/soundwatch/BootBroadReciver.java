package com.example.soundwatch;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class BootBroadReciver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
            // Set the alarm here.

            SharedPreferences timePrefs = context.getSharedPreferences("soundwatch_time_prefs", Context.MODE_PRIVATE);

            int startHour = timePrefs.getInt("startHour", 8);   // 기본값: 8시
            int startMinute = timePrefs.getInt("startMinute", 0);
            int stopHour = timePrefs.getInt("stopHour", 22);    // 기본값: 22시
            int stopMinute = timePrefs.getInt("stopMinute", 0);

            AlarmSetting.setRepeating(context, startHour, startMinute, "com.example.soundwatch.START_MEASUREMENT");
            AlarmSetting.setRepeating(context, stopHour, stopMinute, "com.example.soundwatch.STOP_MEASUREMENT");
        }
    }
}

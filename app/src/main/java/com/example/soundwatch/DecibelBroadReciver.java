package com.example.soundwatch;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.Objects;

public class DecibelBroadReciver extends BroadcastReceiver {

    public static final String EXTRA_DECIBEL_VALUE = "com.example.soundwatch.ACTION_UPDATE_DECIBEL";
    private static final String TAG = "DecibelReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Objects.equals(intent.getAction(), "com.example.soundwatch.ACTION_UPDATE_DECIBEL")) {
            double decibel = intent.getDoubleExtra("decibel", -1);

            if (decibel != -1) {
                Log.d(TAG, "Received decibel value: " + decibel);
                // TODO:알림이나 저장, DB연결?? -> 활용방안?
            } else {
                Log.w(TAG, "Received intent without decibel value");
            }
        }
    }
}
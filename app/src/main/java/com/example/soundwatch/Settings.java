package com.example.soundwatch;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.view.View;

public class Settings extends MainActivity {

    private TextView txtWaringdB;
    public int selectedDecibel = 30; //초기값 30으로 설정

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);


        txtWaringdB = (TextView) findViewById(R.id.txtWaringdB);
        txtWaringdB.setText(selectedDecibel + "dB");
        txtWaringdB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDecibelPickerDialog();
            }
        });
    }

    private void showDecibelPickerDialog() {
        DialogWarning dialogWarning = new DialogWarning(this, selectedDecibel, new DialogWarning.OnNumberSelectedListener() {
            public void onNumberSelected(int number) {
                selectedDecibel = number; //선택된 값 가져옴
                txtWaringdB.setText(String.valueOf(selectedDecibel)); // 화면에 표시

                saveWarningDecibel(selectedDecibel); //decibel값 저장 -> 서버?
            }
        });
        dialogWarning.show();
    }

    private void saveWarningDecibel(int decibel) { //데시벨 값 저장 -> Activity간 교환 위함
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt("selectedDecibel", decibel);
        editor.apply();
    }
}

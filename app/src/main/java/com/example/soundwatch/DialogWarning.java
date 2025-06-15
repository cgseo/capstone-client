package com.example.soundwatch;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.NumberPicker;

import androidx.annotation.NonNull;

public class DialogWarning extends Dialog {
    private NumberPicker warningDecibelPicker;
    private Button btnWarningOK, btnWarningCancle;
    private Settings settings;

    private int selectedDecibel; //초기값 30으로 설정
    private OnNumberSelectedListener listener;
    private String[] decibelArr = {"0", "10", "20", "30", "40", "50", "60", "70", "80", "90", "100", "110", "120"};
    // 위 - 경고 수준 데시벨 저장 0~120까지만


    public DialogWarning(@NonNull Context context, int initDecibel, OnNumberSelectedListener listener) {
        super(context);
        this.selectedDecibel = initDecibel;
        this.listener = listener; // 값 주고받기 위함
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_warning_decibel);

        btnWarningOK = (Button) findViewById(R.id.btnWarningOK);
        btnWarningCancle = (Button) findViewById(R.id.btnWarningCancle);
        warningDecibelPicker = (NumberPicker) findViewById(R.id.numPickerWarningdB);

        warningDecibelPicker.setMinValue(0); //최소값 0
        warningDecibelPicker.setMaxValue(12); //최대값 120
        warningDecibelPicker.setValue(selectedDecibel / 10); //배열 인덱스 0~12
        warningDecibelPicker.setDisplayedValues(decibelArr); //배열인덱스 값 표시
        warningDecibelPicker.setWrapSelectorWheel(false); //값 순환하지 않도록

        btnWarningOK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectedDecibel = warningDecibelPicker.getValue() * 10;
                if (listener != null) {
                    listener.onNumberSelected(selectedDecibel);
                }
                dismiss();
            }
        });

        btnWarningCancle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });

    }

    public interface OnNumberSelectedListener {
        void onNumberSelected(int number);
    }
}

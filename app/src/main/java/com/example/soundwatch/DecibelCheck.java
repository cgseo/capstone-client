package com.example.soundwatch;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import androidx.core.app.ActivityCompat;

import java.sql.Timestamp;

public class DecibelCheck {

    private int warningDecibel;
    boolean isRecording = false; // 측정 중인지 판단
    private static final int SAMPLE_RATE = 44100; //모든 장치에서 작동이 보장되는 유일한 속도. 헤르츠로 표현된 샘플링 속도
    private AudioRecord audioRecord; //오디오 API
    private int bufferSize; //오디오 녹음 저장 버퍼
    private volatile double currentDecibel = 0.0; // 실시간으로 넘겨줄 측정값

    // 측정을 위해 추가
    private Timestamp startTime; // 측정 시작 시간
    private Timestamp endTime; // 측정 종료 시간
    private double maxDecibel = 0.0; // 최대 데시벨 값

    // 권한 체크 위함
    private Context context;
    public DecibelCheck(Context context) {
        this.context = context;
    }

    public boolean isRecording() {
        return isRecording;
    }

    public Timestamp getStartTime() {
        return startTime;
    }

    public Timestamp getEndTime() {
        return endTime;
    }

    public double getCurrentDecibel() {
        return currentDecibel;
    }

    public void startMesure() {
        bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
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

                //double finalDecibel = decibel; //최종 데시벨 값
                currentDecibel = decibel;

                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        recordingThread.start();
    }

    public void stopMesure() { //측정 종료
        isRecording = false;
        endTime = new Timestamp(System.currentTimeMillis()); // 측정 종료 시간 기록
        if (audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }
    }
}

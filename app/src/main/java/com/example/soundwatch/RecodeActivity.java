package com.example.soundwatch;

import android.graphics.Color;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.soundwatch.RetrofitClient;
import com.example.soundwatch.NoiseLogApi;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.OnDateSelectedListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RecodeActivity extends AppCompatActivity {

    private MaterialCalendarView calendarView; // 달력
    private TextView selectDateTextView; // 날짜 선택
    private LinearLayout dataContainer; // 로그들을 동적으로 표시
    private NoiseLogApi noiseLogApi; // 서버와 통신 인터페이스
    // 날짜 형식
    private SimpleDateFormat dateOnlyFormatter = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
    private Date selectedDate;

    // 날짜를 선택하면 해당 날짜의 데이터를 가져온다.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recode);

        calendarView = findViewById(R.id.calendarView);
        selectDateTextView = findViewById(R.id.selectDateTextView);
        dataContainer = findViewById(R.id.dataContainer);
        noiseLogApi = RetrofitClient.getApiService();

        calendarView.setOnDateChangedListener(new OnDateSelectedListener() {
            @Override
            public void onDateSelected(@NonNull MaterialCalendarView widget, @NonNull CalendarDay date, boolean selected) {
                selectedDate = date.getDate();
                selectDateTextView.setText(dateOnlyFormatter.format(selectedDate));
                loadNoiseLogsForDate(selectedDate);
            }
        });

        // 초기에는 오늘 날짜의 로그를 보여준다.
        selectedDate = new Date();
        calendarView.setSelectedDate(CalendarDay.today());
        loadNoiseLogsForDate(selectedDate);
    }

    // 서버에서 해당하는 날짜의 소음 데이터 목록을 가져온다.
    private void loadNoiseLogsForDate(Date selectedDate) {
        dataContainer.removeAllViews();
        String dateString = dateOnlyFormatter.format(selectedDate);
        // 실제 서버 연동 (실제 서버 오픈시 이 부분 사용)
        /*
        Call<List<NoiseLog>> call = noiseLogApi.getNoiseLogsByDate(dateString);
        call.enqueue(new Callback<List<NoiseLog>>() {
            @Override
            public void onResponse(Call<List<NoiseLog>> call, Response<List<NoiseLog>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    showLogs(response.body());
                } else {
                    showMessage("소음 데이터 로드 실패: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<List<NoiseLog>> call, Throwable t) {
                showMessage("네트워크 오류: " + t.getMessage());
            }
        });
        */

        // 더미 데이터로 테스트
        List<NoiseLog> allLogs = RecodeTestData.getDummyLogs();
        List<NoiseLog> filteredLogs = new ArrayList<>();

        // 날짜 필터링
        for (NoiseLog log : allLogs) {
            String logDate = dateOnlyFormatter.format(log.getStartTime());
            if (logDate.equals(dateString)) {
                filteredLogs.add(log);
            }
        }

        showLogs(filteredLogs);
    }

    // 소음 측정 기록 열람
    private void showLogs(List<NoiseLog> logs) {
        dataContainer.removeAllViews();

        if (logs.isEmpty()) {
            TextView noDataTextView = new TextView(this);
            noDataTextView.setText("해당 날짜에 기록된 소음 데이터가 없습니다.");
            dataContainer.addView(noDataTextView);
            return;
        }

        for (NoiseLog log : logs) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);

            LinearLayout logRow = new LinearLayout(this);
            logRow.setOrientation(LinearLayout.HORIZONTAL);

            // 시작 시간
            TextView startTimeView = createCell(timeFormatter.format(log.getStartTime()), 80);
            logRow.addView(startTimeView);

            // 종료 시간
            TextView endTimeView = createCell(timeFormatter.format(log.getEndTime()), 80);
            logRow.addView(endTimeView);

            // 위치
            TextView locationView = createCell(log.getLocation(), 100);
            logRow.addView(locationView);

            // 최대 데시벨
            TextView dbView = createCell(String.format(Locale.getDefault(), "%.2f dB", log.getMaxDb()), 70);
            logRow.addView(dbView);

            // 삭제 버튼
            Button deleteButton = new Button(this);
            deleteButton.setText("삭제");
            deleteButton.setOnClickListener(v -> deleteNoiseLog(log.getId()));
            logRow.addView(deleteButton);

            // 행 추가
            dataContainer.addView(logRow);
        }
    }

    // 공통 셀 생성 메서드
    private TextView createCell(String text, int widthDp) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setSingleLine(true);
        textView.setHorizontallyScrolling(true);
        textView.setMovementMethod(new ScrollingMovementMethod());
        textView.setEllipsize(null);
        textView.setGravity(Gravity.CENTER_VERTICAL);
        textView.setPadding(8, 8, 8, 8);
        textView.setBackgroundColor(Color.parseColor("#EEEEEE"));

        textView.setLayoutParams(new LinearLayout.LayoutParams(
                dpToPx(widthDp),
                dpToPx(50)
        ));

        return textView;
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                getResources().getDisplayMetrics()
        );
    }


    // 로그 삭제, 해당하는 ID의 로그를 삭제
    private void deleteNoiseLog(int logId) {
        Call<Void> call = noiseLogApi.deleteNoiseLog(logId);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(RecodeActivity.this, "삭제 완료", Toast.LENGTH_SHORT).show();
                    loadNoiseLogsForDate(selectedDate);
                } else {
                    Toast.makeText(RecodeActivity.this, "삭제 실패", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(RecodeActivity.this, "삭제 실패 (네트워크 오류)", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        Log.e("RecodeActivity", message);
    }
}
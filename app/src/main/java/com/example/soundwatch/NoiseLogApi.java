package com.example.soundwatch;

import com.example.soundwatch.NoiseLog;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.QueryMap;

// API 정의
public interface NoiseLogApi {

    /* SELECT */
    // 특정 user가 가진 전체 noiseLog 가져오기
    @GET("api/noise/")
    public Call<List<NoiseLog>> getNoiseLogsByUserId(
            @Query("userId") int userId
    );

    // 캘린더에 max_db 표시를 위한 noiseLog 가져오기
    // 전달받은 NoiseLog의 log_time, max_db를 제외한 모든 속성 값은 null
    @GET("api/noise/maxDbList")
    public Call<List<NoiseLog>> getMaxDecibelsForMonth(
            @QueryMap Map<String, Integer> filter
    );

    /* INSERT */
    // 측정한 NoiseLog db에 저장
    // 저장된 NoiseLog의 id 리턴
    @POST("api/noise/insertNoiseLog")
    public Call<Integer> insertNoiseLog(
            @Body NoiseLog noiseLog
    );

    // 추가된 부분
    @POST("/api/noise/save") // 데이터 저장
    Call<Integer> saveNoiseLog(@Body NoiseLog noiseLog);

    @GET("/api/noise/byDate") //특정 날짜 데이터 조회
        // userId 쿼리 파라미터 추가
    Call<List<NoiseLog>> getNoiseLogsByDate(@Query("userId") String userId, @Query("date") String date);

    @GET("/api/noise/deleteOld") // 오래된 로그 삭제
    Call<Integer> deleteOldLogs();

    @DELETE("/api/noise/delete") // 데이터 삭제
    Call<Void> deleteNoiseLog(@Query("id") int id);
}

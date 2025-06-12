package com.example.soundwatch;

import com.google.gson.annotations.SerializedName;

import java.sql.Timestamp;

    public class NoiseLog {
        private int id;
        @SerializedName("noise_level")
        private double noiseLevel;
        @SerializedName("log_time")
        private long logTime; // 현재 데시벨 측정 시간
        @SerializedName("start_time")
        private Timestamp startTime; // 측정 시작 시간
        @SerializedName("end_time")
        private Timestamp endTime; // 측정 종료 시간
        private String location;
        @SerializedName("max_db")
        private double maxDb;
        @SerializedName("user_id")
        private int userId; // 사용자 ID 추가

        @SerializedName("group_id")
        private int groupId; // 그룹 ID 추가


        public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setNoiseLevel(double noiseLevel) {
        this.noiseLevel = noiseLevel;
    }

    public long getLogTime() {
        return logTime;
    }

    public void setLogTime(long logTime) {
        this.logTime = logTime;
    }

    public Timestamp getStartTime() {
        return startTime;
    }

    public void setStartTime(Timestamp startTime) {
        this.startTime = startTime;
    }

    public Timestamp getEndTime() {
        return endTime;
    }

    public void setEndTime(Timestamp endTime) {
        this.endTime = endTime;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public double getMaxDb() {
        return maxDb;
    }

    public void setMaxDb(double maxDb) {
        this.maxDb = maxDb;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getGroupId() {return groupId;}

    public void setGroupId(int groupId) { this.groupId = groupId; }

    @Override
    public String toString() {
        return "NoiseLog{" +
                "id=" + id +
                ", noiseLevel=" + noiseLevel +
                ", logTime=" + logTime +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", location='" + location + '\'' +
                ", maxDb=" + maxDb +
                ", userId=" + userId +
                ", groupId=" + groupId +
                '}';
    }
}
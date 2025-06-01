package com.example.soundwatch;

public class MemberNoiseLog {
    private String user_id;
    private String nickname;
    private boolean is_online;
    private Double noise_level;
    private Double max_db;

    public String getUser_id() { return user_id; }
    public String getNickname() { return nickname; }
    public boolean isOnline() { return is_online; }
    public Double getNoise_level() { return noise_level; }
    public Double getMax_db() { return max_db; }

    public void setUser_id(String user_id) { this.user_id = user_id; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public void setOnline(boolean is_online) { this.is_online = is_online; }
    public void setNoise_level(Double noise_level) { this.noise_level = noise_level; }
    public void setMax_db(Double max_db) { this.max_db = max_db; }
}


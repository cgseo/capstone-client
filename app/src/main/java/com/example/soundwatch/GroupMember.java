package com.example.soundwatch;

public class GroupMember {
    private String Id;
    private String groupId;
    private String userId;
    private Long joined_at;
    private boolean is_owner;
    private boolean active; // 그룹 멤버의 활성화 여부
    private double decibel; // 그룹 멥버의 현재 데시벨
    private String name; // 그룹 멤버의 이름

    public String getId() {
        return Id;
    }

    public void setId(String id) {
        Id = id;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getGroupId() {
        return groupId;
    }
    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public Long getJoined_at(long joinedAt) {
        return joined_at;
    }

    public void setJoined_at(Long joined_at) {
        this.joined_at = joined_at;
    }

    public boolean getIs_owner() {
        return is_owner;
    }

    public void setIs_owner(boolean is_owner) {
        this.is_owner = is_owner;
    }

    public boolean getActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public double getDecibel() {
        return decibel;
    }

    public void setDecibel(double decibel) {
        this.decibel = decibel;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

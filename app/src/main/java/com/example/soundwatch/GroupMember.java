package com.example.soundwatch;

public class GroupMember {
    private String Id;
    private String groupId;
    private String userId;
    private Long joined_at;
    private boolean is_owner;
    private String nickname; // 그룹 멤버의 이름

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


    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
}

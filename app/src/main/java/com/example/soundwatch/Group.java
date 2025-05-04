package com.example.soundwatch;

import com.google.gson.annotations.SerializedName;

public class Group {

    private String id;
    private String name;
    private String description;
    @SerializedName("invite_code")
    private String inviteCode;

    public Group(String id, String name, String description, String inviteCode) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.inviteCode = inviteCode;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getInviteCode(){
        return inviteCode;
    }

    @Override
    public String toString() {
        return name + " | " + description;
    }

    /* 초대 코드 생성을 위해 문자와 숫자를 무작위로 배열한 6자리 문장 출력, 서버에서 처리할 수 있도록 조치 요망.
    private String generateInviteCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            int randomIndex = (int) (Math.random() * chars.length());
            code.append(chars.charAt(randomIndex));
        }
        return code.toString();
    }
     */
}


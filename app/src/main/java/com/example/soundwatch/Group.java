package com.example.soundwatch;

public class Group {

    private String id;
    private String group_name;
    private String created_at;
    private String description;
    private String invite_code;

    public Group(String id, String group_name, String description, String invite_code) {
        this.id = id;
        this.group_name = group_name;
        this.description = description;
        this.invite_code = invite_code;
    }
    public Group(String id, String group_name, String created_at, String description, String invite_code) {
        this.id = id;
        this.group_name = group_name;
        this.created_at =  created_at;
        this.description = description;
        this.invite_code = invite_code;
    }

    public String getId() {
        return id;
    }

    public String getGroup_name() {
        return group_name;
    }

    public String getDescription() {
        return description;
    }

    public String getInviteCode(){
        return invite_code;
    }

    @Override
    public String toString() {
        return group_name + " | " + description;
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


package com.example.travelrecord;

//사용자 계정 정보 모델 클래스
public class UserAccount {
    private String idToken; //Firebase Uid(고유 토큰 정보)
    private String name;  //이름
    private String emailId;  //이메일 아이디
    private String password; //비밀번호

    //파이어 베이스를 이용할 시, 생성자를 비워둬야 오류가 나지 않음
    public UserAccount() { }


    public String getIdToken() {
        return idToken;
    }

    public void setIdToken(String idToken) {
        this.idToken = idToken;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmailId() {
        return emailId;
    }

    public void setEmailId(String emailId) {
        this.emailId = emailId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }


}

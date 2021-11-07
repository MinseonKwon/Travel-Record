package com.example.travelrecord;

import java.util.Date;

public class CountryData {
    private String countryName; //나라 이름
    private String imgUri; //국기 이미지 경로
    private String memo; //간단한 메모
    private String startDate; //여행 시작 날짜
    private String finishDate;  //여행 마지막 날짜
    private String budget;  //예산
    private String ISO;  //국가코드
    private String background;  //카드뷰 배경화면
    private String documentId;  //각각의 나라의 고유의 id => 각각의 나라가 document에 해당

    public CountryData(){
    }

    public CountryData(String countryName, String imgUri, String memo, String startDate, String finishDate, String budget, String ISO, String background, String documentId) {
        this.countryName = countryName;
        this.imgUri = imgUri;
        this.memo = memo;
        this.startDate = startDate;
        this.finishDate = finishDate;
        this.budget = budget;
        this.ISO = ISO;
        this.background = background;
        this.documentId = documentId;
    }

    public String getCountryName() {
        return countryName;
    }

    public void setCountryName(String countryName) {
        this.countryName = countryName;
    }

    public String getImgUri() {
        return imgUri;
    }

    public void setImgUri(String imgUri) {
        this.imgUri = imgUri;
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getFinishDate() {
        return finishDate;
    }

    public void setFinishDate(String finishDate) {
        this.finishDate = finishDate;
    }

    public String getBudget() {
        return budget;
    }

    public void setBudget(String budget) {
        this.budget = budget;
    }

    public String getISO() {
        return ISO;
    }

    public void setISO(String ISO) {
        this.ISO = ISO;
    }

    public String getBackground() {
        return background;
    }

    public void setBackground(String background) {
        this.background = background;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

}

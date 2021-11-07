package com.example.travelrecord;

import android.os.Parcel;
import android.os.Parcelable;

public class PriceData implements Parcelable {
    private String price_category; //비용의 카테고리
    private String price_spend;  //비용
    private String price_icon;      //카테고리 아이콘
    private String price_memo;   //간단한 메모
    private String price_day;          //D-day
    private String price_item;      //카테고리의 항목명 <= 내가 지은 ex)버거킹, 박물관...
    private String price_picture;   //사진
    private String price_add_time;   //priceAdd에서 설정한 날짜(입력시간이 아닐 수 있음)
    private String price_current_time; //지출 금액을 추가할 때의 시간 -> unique document Id
    private String chart_background; //차트 아이콘 배경
    private String currency_sign; //통화 기호

    public PriceData(){
    }


    //Parecl에서 PriceData안에 들어가 있는 변수를 read로 복원
    public PriceData(Parcel src){
        price_category = src.readString();
        price_spend = src.readString();
        price_icon = src.readString();
        price_memo = src.readString();
        price_day = src.readString();
        price_item = src.readString();
        price_picture = src.readString();
        price_add_time = src.readString();
        price_current_time = src.readString();
        chart_background = src.readString();
        currency_sign = src.readString();
    }

    //역직렬화할 때 사용 => 바이트 스트림을 원래 데이터 객체로 변환
    public static final Creator CREATOR = new Creator(){
        //parcel된 데이터를 다시 원래대로 만들어 줌.
        @Override
        public PriceData createFromParcel(Parcel parcel) {
            return new PriceData(parcel);
        }
        //Parcel.createTypeArray()를 호출했을 때 불림
        @Override
        public PriceData[] newArray(int i) {
            return new PriceData[i];
        }
    };

    //Parcel의 내용을 기술
    //FileDescriptor 같은 특별한 객체가 들어가면 이 부분을 통해 알려줘야함
    //보통은 0 리턴
    @Override
    public int describeContents() {
        return 0;
    }
    //PriceData를 Parcel로 바꿔줌(Parcel 안에 데이터를 넣음)
    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(price_category);
        parcel.writeString(price_spend);
        parcel.writeString(price_icon);
        parcel.writeString(price_memo);
        parcel.writeString(price_day);
        parcel.writeString(price_item);
        parcel.writeString(price_picture);
        parcel.writeString(price_add_time);
        parcel.writeString(price_current_time);
        parcel.writeString(chart_background);
        parcel.writeString(currency_sign);
    }

    public String getPrice_category() {
        return price_category;
    }

    public void setPrice_category(String price_category) {
        this.price_category = price_category;
    }

    public String getPrice_spend() {
        return price_spend;
    }

    public void setPrice_spend(String price_spend) {
        this.price_spend = price_spend;
    }

    public String getPrice_icon() {
        return price_icon;
    }

    public void setPrice_icon(String price_icon) {
        this.price_icon = price_icon;
    }

    public String getPrice_memo() {
        return price_memo;
    }

    public void setPrice_memo(String price_memo) {
        this.price_memo = price_memo;
    }

    public String getPrice_day() {
        return price_day;
    }

    public void setPrice_day(String price_day) {
        this.price_day = price_day;
    }

    public String getPrice_item() {
        return price_item;
    }

    public void setPrice_item(String price_item) {
        this.price_item = price_item;
    }

    public String getPrice_picture() {
        return price_picture;
    }

    public void setPrice_picture(String price_picture) {
        this.price_picture = price_picture;
    }

    public String getPrice_add_time() {
        return price_add_time;
    }

    public void setPrice_add_time(String price_add_time) {
        this.price_add_time = price_add_time;
    }

    public String getPrice_current_time() {
        return price_current_time;
    }

    public void setPrice_current_time(String price_current_time) {
        this.price_current_time = price_current_time;
    }

    public String getChart_background() {
        return chart_background;
    }

    public void setChart_background(String chart_background) {
        this.chart_background = chart_background;
    }

    public String getCurrency_sign() {
        return currency_sign;
    }

    public void setCurrency_sign(String currency_sign) {
        this.currency_sign = currency_sign;
    }
}

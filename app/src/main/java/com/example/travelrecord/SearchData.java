package com.example.travelrecord;

import android.widget.ImageView;
import android.widget.RadioButton;

public class SearchData {
    private String imgUrl; //국기사진 
    private String countryName; //나라이름
    private RadioButton rb_country; //라디오버튼
    private String ISO; //국가 ISO 3166 코드(2자리)
    private String currency_code; //국가통화코드

    public SearchData() {
    }

    public String getImgUrl() {
        return imgUrl;
    }

    public void setImgUrl(String imgUrl) {
        this.imgUrl = imgUrl;
    }

    public String getCountryName() {
        return countryName;
    }

    public void setCountryName(String countryName) {
        this.countryName = countryName;
    }

    public RadioButton getRb_country() {
        return rb_country;
    }

    public void setRb_country(RadioButton rb_country) {
        this.rb_country = rb_country;
    }

    public String getISO() {
        return ISO;
    }

    public void setISO(String ISO) {
        this.ISO = ISO;
    }

    public String getCurrency_code() {
        return currency_code;
    }

    public void setCurrency_code(String currency_code) {
        this.currency_code = currency_code;
    }
}

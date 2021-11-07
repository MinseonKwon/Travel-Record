package com.example.travelrecord;

public class DateData {
    private String item_year;
    private String item_week;
    private String item_date;
    private String item_month;

    public DateData() {
    }

    public DateData(String item_year, String item_week, String item_date, String item_month) {
        this.item_year = item_year;
        this.item_week = item_week;
        this.item_date = item_date;
        this.item_month = item_month;
    }

    public String getItem_week() {
        return item_week;
    }

    public void setItem_week(String item_week) {
        this.item_week = item_week;
    }

    public String getItem_date() {
        return item_date;
    }

    public void setItem_date(String item_date) {
        this.item_date = item_date;
    }

    public String getItem_month() {
        return item_month;
    }

    public void setItem_month(String item_month) {
        this.item_month = item_month;
    }

    public String getItem_year() {
        return item_year;
    }

    public void setItem_year(String item_year) {
        this.item_year = item_year;
    }
}

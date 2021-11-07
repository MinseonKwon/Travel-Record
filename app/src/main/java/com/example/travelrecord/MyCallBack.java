package com.example.travelrecord;

import java.util.ArrayList;
import java.util.HashMap;

public interface MyCallBack {
    void onCallback(ArrayList<String> sectionList, HashMap<String, ArrayList<PriceData>> itemList,String budget); //PriceList

    void onCountryCallback(ArrayList<CountryData> countryList);  //CountryList

    void onChartCallback(ArrayList<PriceData> chartList);  //Chart
}

package com.example.travelrecord;

import android.view.View;

//어댑터 객체 밖에서 리스너를 설정하고 설정된 리스너 쪽으로 이벤트를 전달 받게 함
public interface OnCardItemClickListener {
    public void OnItemClick(CountryAdapter.CustomViewHolder holder, View view, int position); //CountryList 아이템 클릭
    public void OnDateItemClick(DateAdapter.CustomViewHolder holder, View view, int position);  //PriceList 수평리사이클러뷰 아이템 클릭
}
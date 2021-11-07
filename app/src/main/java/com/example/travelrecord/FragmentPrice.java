package com.example.travelrecord;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Currency;
import java.util.HashMap;

//데이터가 존재하는 fragment in PriceList
public class FragmentPrice extends Fragment{
    private ArrayList<String> sectionList = new ArrayList<>(); //section이 들어가는 리스트
    private PriceAdapter priceAdapter;
    private RecyclerView recyclerView;
    private LinearLayoutManager linearLayoutManager;
    private HashMap<String, ArrayList<PriceData>> itemList = new HashMap<>(); // sectionList를 key로 데이터가 들어가있는 hashMap
    private String left; //남은 돈 -> String
    private TextView price_tv_money;  //지출 한 금액
    private TextView price_tv_left_money;  //지출 후 남은 금액
    private double total; //총 지출 금액

    public FragmentPrice() {
    }

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @NonNull ViewGroup container, @NonNull Bundle savedInstanceState){
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.price_fragment,container,false);
        recyclerView = (RecyclerView) rootView.findViewById(R.id.rv_price);
        linearLayoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(linearLayoutManager);

        //PriceList activity에서 데이터 받기
        if(getArguments() != null){
            //전달한 key 값
            sectionList = getArguments().getStringArrayList("sectionList"); // 2021-10-10 -> key
            itemList  = (HashMap<String, ArrayList<PriceData>>) getArguments().getSerializable("itemList"); //키값 기준 아이콘, 카테고리 항목명, 금액
            left = getArguments().getString("left"); //지출 후 남은 금액
        }

        priceAdapter = new PriceAdapter(getActivity(),sectionList,itemList);
        priceAdapter.shouldShowHeadersForEmptySections(false);
        recyclerView.setAdapter(priceAdapter); //Adapter set
        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        SharedPreferences id = getActivity().getSharedPreferences("id", Context.MODE_PRIVATE);
        String iso = id.getString("iso", "");

        String symbol = itemList.get(sectionList.get(0)).get(0).getCurrency_sign();

        //모든 지출 금액 setText
        total = 0.0;
        price_tv_money = (TextView) view.findViewById(R.id.price_tv_money);
        for (int i = 0; i < itemList.size(); i++) {
            for (int j = 0; j < itemList.get(sectionList.get(i)).size(); j++) {
                total += Double.parseDouble(itemList.get(sectionList.get(i)).get(j).getPrice_spend());
            }
        }

        DecimalFormat decimalFormat = new DecimalFormat("###,###.##");
        price_tv_money.setText(symbol + " " + decimalFormat.format(total));

        //각 아이템의 금액 부분을 클릭 시, 환율에 따라 금액 변환 => 총 지출 금액
        if (!iso.equals("KRW")) {
            price_tv_money.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Currency currencykr = Currency.getInstance("KRW");
                    String exchange, exchange_kr;
                    if (String.valueOf(price_tv_money.getText().toString().charAt(0)).equals(currencykr.getSymbol())) { //원화 -> 다른나라 통화
                        price_tv_money.setText(symbol + " " + decimalFormat.format(total));
                    }
                    else {  //다른나라 통화 -> 원화
                        exchange = id.getString("currency", "");
                        exchange_kr = id.getString("currency_kr", "");
                        NumberFormat numberFormat = NumberFormat.getCurrencyInstance();

                        if (exchange.equals("1")) { //유로 일 때,
                            double result = Double.parseDouble(exchange_kr) * total;
                            int r_result = (int) Math.round(result);
                            price_tv_money.setText(numberFormat.format(r_result));
                        }
                        else { //나머지
                            double result = ((1 / Double.parseDouble(exchange)) * Double.parseDouble(exchange_kr)) * total;
                            int r_result = (int) Math.round(result);
                            price_tv_money.setText(numberFormat.format(r_result));
                        }
                    }
                }
            });
        }

        //예산에서 지출 금액을 뺀 남은 돈
        price_tv_left_money = view.findViewById(R.id.price_tv_left_money);
        price_tv_left_money.setText(symbol + " " + decimalFormat.format(Double.parseDouble(left)));

        //각 아이템의 금액 부분을 클릭 시, 환율에 따라 금액 변환 => 남은 금액
        if (!iso.equals("KRW")) {
            price_tv_left_money.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Currency currencykr = Currency.getInstance("KRW");
                    String exchange, exchange_kr;
                    if (String.valueOf(price_tv_left_money.getText().toString().charAt(0)).equals(currencykr.getSymbol())) {
                        price_tv_left_money.setText(symbol + " " + decimalFormat.format(Double.parseDouble(left)));
                    } else {
                        exchange = id.getString("currency", "");
                        exchange_kr = id.getString("currency_kr", "");
                        NumberFormat numberFormat = NumberFormat.getCurrencyInstance();

                        if (exchange.equals("1")) { //유로일 때,
                            double result = Double.parseDouble(exchange_kr) * Double.parseDouble(left);
                            int r_result = (int) Math.round(result);
                            price_tv_left_money.setText(numberFormat.format(r_result));
                        }
                        else {
                            double result = ((1 / Double.parseDouble(exchange)) * Double.parseDouble(exchange_kr)) * Double.parseDouble(left);
                            int r_result = (int) Math.round(result);
                            price_tv_left_money.setText(numberFormat.format(r_result));
                        }
                    }
                }
            });
        }
    }
}

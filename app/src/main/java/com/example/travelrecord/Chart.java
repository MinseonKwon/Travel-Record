package com.example.travelrecord;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;

import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Currency;
import java.util.HashMap;
import java.util.Map;

public class Chart extends AppCompatActivity {
    private TextView tv_chart_explain_category; //가장 많은 부분을 차지하는 항목명
    private TextView tv_chart_explain_percent;  //가장 높은 percent 표시
    private PieChart pieChart; //차트
    private ArrayList<PriceData> chartList; //차트에 들어갈 값 리스트
    private RecyclerView recyclerView; //차트 밑에 항목별 값이 들어갈 리사이클러뷰
    private LinearLayoutManager linearLayoutManager;
    private ChartAdapter chartAdapter;
    private TextView all_spend_chart; //총 지출 금액 표시
    private String[] chartBackground = {"first_background","second_background","third_background","fourth_background","fifth_background","sixth_background"}; //리사이클러뷰 아이콘을 수정한 xml 파일의 이름
    private String[] chart_sort = {"교통","관광","숙박","기타","쇼핑","식비"}; //차트 값의 순서를 정해 둠
    private ArrayList<Integer> colors; //차트 구성 컬러들
    private int maxColorNumber; //가장 높은 percent를 차지한 항목의 color
    private SharedPreferences id;
    private double total;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //상태바 제거
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_chart);

        pieChart = findViewById(R.id.pie_chart);
        showPieChart(); //차트를 보여주는 method

        initView(); //UI


    }

    private void initView() {
        //Toolbar setting
        Toolbar toolbar = findViewById(R.id.chart_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        //리사이클러뷰 설정
        recyclerView = (RecyclerView) findViewById(R.id.chart_rv);
        recyclerView.setHasFixedSize(true); //리사이클러뷰 성능 강화
        linearLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setItemViewCacheSize(5);

        //데이터 받기 => 전 activity에서 리스트로 db에서 받아옴
        ArrayList<PriceData> getChartList = readChartData();

        //데이터 정렬 <= 미리 정해놓은 String 배열 순으로 정렬
        ArrayList<PriceData> sortedList = new ArrayList<>();
        int order = 0; //새로 정렬된 리스트의 index
        for(int i=0; i<chart_sort.length; i++){ //먼저 정해놓은 순서에 따라 받아온 리스트를 새로 정렬
            String stand = chart_sort[i]; //기준 값
            for(int j=0; j<getChartList.size(); j++){
                PriceData compare = getChartList.get(j); //비교 값
                if(stand.equals(compare.getPrice_category())){
                    sortedList.add(order,compare);
                    order++; //정렬된 리스트의 index
                }
            }
        }

        //chartList에 정해놓은 drawable 폴더이름(미리 정해놓은 아이콘의 background)을 sortedList의 값에 추가하여 저장
        chartList = new ArrayList<>(); //항목 아이콘에 배경을 추가하기 위함
        for(int i=0; i<sortedList.size(); i++){
            PriceData data = new PriceData();
            data.setPrice_icon(sortedList.get(i).getPrice_icon());
            data.setPrice_category(sortedList.get(i).getPrice_category());
            data.setCurrency_sign(sortedList.get(i).getCurrency_sign());
            data.setPrice_spend(sortedList.get(i).getPrice_spend());
            data.setChart_background(chartBackground[i]); //=> 아이콘에 배경 추가
            chartList.add(data);
        }

        //adapter set
        chartAdapter = new ChartAdapter(chartList,getApplicationContext());
        recyclerView.setAdapter(chartAdapter);
        chartAdapter.notifyDataSetChanged();

        //총 지출 금액 구하기
        all_spend_chart = findViewById(R.id.all_spend_chart);
        total = 0;
        for(int i=0; i<chartList.size(); i++){
            String spend = chartList.get(i).getPrice_spend();
            total += Double.parseDouble(spend); //총 지출 금액
        }

        DecimalFormat decimalFormat = new DecimalFormat("###,###.##"); //format 설정
        all_spend_chart.setText(chartList.get(0).getCurrency_sign() +" "+decimalFormat.format(total)); //지출 금액 set => currency symbol은 모든 리스트 다 같음

        id = getSharedPreferences("id",MODE_PRIVATE);
        String iso = id.getString("iso","");
        if(!iso.equals("KRW")){ //여행하는 나라가 대한민국이 아닐때만 클릭 허용
            all_spend_chart.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Currency currencykr = Currency.getInstance("KRW");
                    String exchange,exchange_kr;
                    if (String.valueOf(all_spend_chart.getText().toString().charAt(0)).equals(currencykr.getSymbol())) { //총지출 금액의 통화 기호가 원화면,
                        all_spend_chart.setText(chartList.get(0).getCurrency_sign() +" "+decimalFormat.format(total)); //설정해 놓은 다른나라 통화로 set
                    } else { //아니면, 환율에 따른 변환 계산 필요
                        exchange = id.getString("currency",""); //유로 기준 다른나라 환율
                        exchange_kr = id.getString("currency_kr",""); //유로 기준 우리나라 환율
                        NumberFormat numberFormat = NumberFormat.getCurrencyInstance();

                        if(exchange.equals("1")){ //유로일 때,
                            double result = Double.parseDouble(exchange_kr) * total; //ex) ₩1,362 * €300
                            int r_result = (int)Math.round(result); //반올림
                            all_spend_chart.setText(numberFormat.format(r_result));
                        }
                        else{
                            double result = ((1 / Double.parseDouble(exchange))*Double.parseDouble(exchange_kr)) * total; //ex) ((1/1.2948)*₩1,362) * $300
                            int r_result = (int)Math.round(result); //반올림
                            all_spend_chart.setText(numberFormat.format(r_result));
                        }
                    }
                }
            });
        }


        //차트 값 중에서 가장 많이 차지한 value를 TextView에 Set
        //Max 값 구하기 => chartList
        double max = Double.parseDouble(chartList.get(0).getPrice_spend());
        for(int i=1; i<chartList.size(); i++){
            double compare = Double.parseDouble(chartList.get(i).getPrice_spend());
            if(compare > max){
                max = compare;
                maxColorNumber = i; //해당하는 카테고리의 index를 저장
            }
        }
        //가장 큰 값의 percent 구하기
        double percent = max / total * 100.0;

        //Set TextView
        tv_chart_explain_category = findViewById(R.id.tv_chart_explain_category);
        tv_chart_explain_percent = findViewById(R.id.tv_chart_explain_percent);

        tv_chart_explain_category.setText(chartList.get(maxColorNumber).getPrice_category()); //가장 큰 항목 set
        tv_chart_explain_category.setTextColor(colors.get(maxColorNumber)); //항목 글씨 color set
        tv_chart_explain_percent.setText(String.format("%.2f",percent) +"%"); //가장 높은 percent set
        tv_chart_explain_percent.setTextColor(colors.get(maxColorNumber)); //가장 높은 percent color set

    }

    //뒤로가기 버튼 동작
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()){
            case android.R.id.home:{  //뒤로가기
                finish();
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    //priceList로부터 chartData 받아오기. => ArrayList<PriceData> 형태. SharedPreferences에 저장해 놓음.
    private ArrayList<PriceData> readChartData(){
        SharedPreferences chart = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        Gson gson = new Gson();
        String json = chart.getString("chartList","");  //저장해 놓은 데이터
        Type type = new TypeToken<ArrayList<PriceData>>(){  //타입
        }.getType();
        ArrayList<PriceData> arrayList = gson.fromJson(json,type); //arrayList에 저장
        return arrayList;
    }

    //차트 로드
    private void showPieChart() {
        ArrayList<PieEntry> pieEntries = new ArrayList<>();
        String label = "type";

        chartList = readChartData();  //데이터 저장

        //initializing data <= chartList
        Map<String, Integer> typeAmountMap = new HashMap<>();
        for(int i=0; i<chartList.size(); i++){
            double spend = Double.parseDouble(chartList.get(i).getPrice_spend());
            typeAmountMap.put(chartList.get(i).getPrice_category(), (int) spend); //각각 카테고리를 키로, 지출금액의 map
        }

        //color들을 초기화 => 원하는 컬러 설정
        colors = new ArrayList<>();
        colors.add(Color.parseColor("#ff9999"));
        colors.add(Color.parseColor("#e88ece"));
        colors.add(Color.parseColor("#d395d0"));
        colors.add(Color.parseColor("#ffc000"));
        colors.add(Color.parseColor("#8fd9b6"));
        colors.add(Color.parseColor("#acc2e8"));



        //데이터를 pieEntries에 add
        for(String type : typeAmountMap.keySet()){
            pieEntries.add(new PieEntry(typeAmountMap.get(type).floatValue(), type));
        }

        //collecting the entries with label name
        PieDataSet pieDataSet = new PieDataSet(pieEntries,label);
        pieDataSet.setSliceSpace(5f); //차트 각 항목 공백
        pieDataSet.setSelectionShift(5f);
        pieDataSet.setValueTextSize(15f);
        pieDataSet.setColors(colors);
        //grouping the data set from entry to chart

        pieChart.getDrawingCacheBackgroundColor();

        PieData pieData = new PieData(pieDataSet);
        pieData.setValueFormatter(new PercentFormatter(pieChart)); //값을 percent로 change
        pieData.setValueTextSize(13f);
        pieData.setDrawValues(true);

        initPieChart();

        pieChart.setData(pieData); //차트에 data set
        pieChart.invalidate();
    }

    //chart 초기화
    private void initPieChart(){
        pieChart.setUsePercentValues(true); //percent 사용
        pieChart.getDescription().setEnabled(false);
        pieChart.setExtraOffsets(5,10,5,5);

        pieChart.setDragDecelerationFrictionCoef(0.95f);

        pieChart.setDrawHoleEnabled(true); //차트 가운데 hole
        pieChart.setHoleColor(android.R.color.white);
        pieChart.setTransparentCircleRadius(61f);

        //animation
        pieChart.animateY(1400, Easing.EaseInOutQuad);

        pieChart.setTouchEnabled(false); //touch x

        Legend l = pieChart.getLegend();
        l.setEnabled(false); // x-Values List false 안보이게 / true 보이게
    }
}

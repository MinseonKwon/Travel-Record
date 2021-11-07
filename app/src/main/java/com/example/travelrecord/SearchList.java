package com.example.travelrecord;

import android.content.ClipData;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Adapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.SearchView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.loader.content.AsyncTaskLoader;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONArray;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;

public class SearchList extends AppCompatActivity implements TextWatcher {

    private ArrayList<SearchData> arrayList = new ArrayList<>(); //SearchAdapter에 필요한 데이터
    private SearchData searchData = null;
    private SearchAdapter searchAdapter; //Adapter
    private RecyclerView recyclerView;
    private LinearLayoutManager linearLayoutManager;
    private String requestUrl2 = "http://country.io/currency.json";
    private String requestUrl1 = "http://apis.data.go.kr/1262000/CountryFlagService2/getCountryFlagList2?serviceKey=805Sv5yoaWKxuhh88JkbRTAMRsHCxGPqEBl%2Fomb%2FqN8L1TE69%2BxNflAQRt3xaqhgr2cC7rtOjZZpREmncC5EsA%3D%3D&numOfRows=220";
    private ImageView iv_out; //끝내는 버튼
    private TextInputLayout et_search; //검색창


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //상태바 제거
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.list_search);

        //AsyncTask 초기화 및 실행
        MyAsyncTask myAsyncTask = new MyAsyncTask();
        try {
            arrayList = myAsyncTask.execute().get(); //파싱된 Json 데이터를 arrayList에 저장
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        //이 액티비티를 나가기 위한 버튼
        iv_out = findViewById(R.id.iv_out);
        iv_out.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });


        //리사이클러뷰 설정
        recyclerView = (RecyclerView) findViewById(R.id.rv);
        recyclerView.setHasFixedSize(true); //리사이클러뷰 기존 성능 강화
        linearLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager((linearLayoutManager));


        //RecyclerView에 Adapter 연결
        searchAdapter = new SearchAdapter(arrayList,this);
        recyclerView.setAdapter(searchAdapter); //리사이클러뷰에 어댑터 연결

        //Adpater 업데이트
        searchAdapter.notifyDataSetChanged();


        //검색 기능
        et_search = findViewById(R.id.et_search);
        EditText editText = et_search.getEditText();

        //검색 기능을 위한 method
        editText.addTextChangedListener(this);

    }


    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        searchAdapter.getFilter().filter(charSequence); //검색기능
    }

    @Override
    public void afterTextChanged(Editable editable) { }

    //공공데이터 포털에서 json데이터를 불러오기 위한 작업
    public class MyAsyncTask extends AsyncTask<String, Void, ArrayList<SearchData>> {
        private String jsonData;
        private ArrayList<SearchData> list = new ArrayList<>();
        private String name,img,iso;
        private ArrayList<String> currencyCodeList = new ArrayList<>();

        @Override
        protected ArrayList<SearchData> doInBackground(String... strings) {
            Log.d("파싱이 시작됩니다.","파싱 시작");
            try{
                String[] urls = {requestUrl1, requestUrl2};
                for(int i=0; i<urls.length; i++){ //url이 2개 -> 국기사진, 나라이름, ISO 3166(2자리), ISO 4217 필요
                    URL url = new URL(urls[i]);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("Content-type", "application/json");
                    Log.i("Response code: " , conn.getResponseCode()+"");
                    BufferedReader rd; //buffer에 데이터 저장
                    if(conn.getResponseCode() >= 200 && conn.getResponseCode() <= 300) { //정상
                        rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    } else {
                        rd = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                    }
                    StringBuffer sb = new StringBuffer();
                    String line;
                    while ((line = rd.readLine()) != null) { //한줄씩 stringbuffer에 저장
                        sb.append(line);
                    }
                    jsonData = sb.toString();
                    rd.close();
                    conn.disconnect();


                    //Json 데이터 가공
                    if(i == 0){ //공공데이터 포털
                        JSONArray jsonArray = new JSONObject(jsonData).getJSONArray("data"); //key가 data인 value를 JSONArray객체로 생성
                        for(int j=0; j<jsonArray.length(); j++){ //나라의 수만큼 반복
                            searchData = new SearchData();
                            JSONObject jsonObject = jsonArray.getJSONObject(j); //순서대로 JSONObject객체 생성

                           name = jsonObject.optString("country_nm"); //나라이름
                            img = jsonObject.optString("download_url"); //국기사진 url
                            iso = jsonObject.optString("country_iso_alp2"); //ISO 3166(2자리)

                            searchData.setCountryName(name);
                            searchData.setImgUrl(img);
                            searchData.setISO(iso);
                            list.add(searchData); //리스트에 저장
                        }
                    }
                    else if(i == 1){ //country.io
                        JSONObject jsonObject = new JSONObject(jsonData); //JSONObject형태로 jsonData 저장
                        arrayList = new ArrayList<>();
                        Iterator iterator = jsonObject.keys(); //jsonObject에서 key 값 추출
                        while (iterator.hasNext()) {
                            String b = iterator.next().toString();
                            currencyCodeList.add(b); //KeyList에 key값들 저장
                        }

                        //공공데이터 포털 데이터와 country.io 데이터 비교해서 리스트에 저장
                        for(int j=0; j<list.size(); j++){ //공공데이터 포털의 나라 수만큼 반복
                            for(int z=0; z<currencyCodeList.size(); z++){ //country.io 나라 수만큼 반복
                                if(list.get(j).getISO().equals(currencyCodeList.get(z))){ //ISO 3166(2자리) 기준으로 비교 -> 같으면
                                    searchData = new SearchData();
                                    searchData.setCountryName(list.get(j).getCountryName());
                                    searchData.setImgUrl(list.get(j).getImgUrl());
                                    searchData.setISO(list.get(j).getISO());
                                    searchData.setCurrency_code(jsonObject.getString(currencyCodeList.get(z)));
                                    arrayList.add(searchData); //리스트에 추가
                                }
                            }

                        }
                    }
                }

            } catch (Exception e){
                e.printStackTrace();
            }
            return arrayList;
        }


        //doInBackground메소드가 종료된 후
        //UI 작업을 위해 자동 실행되는 메소드
        protected void onPostExecute(ArrayList<SearchData> s) {
            super.onPostExecute(s);
        }
    }
}


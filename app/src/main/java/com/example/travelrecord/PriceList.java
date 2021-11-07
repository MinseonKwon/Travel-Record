package com.example.travelrecord;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.load.model.ModelLoader;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.gson.Gson;

import org.json.JSONArray;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Currency;
import java.util.Date;
import java.util.HashMap;

public class PriceList extends AppCompatActivity {
    private ArrayList<String> dates = new ArrayList<>(); //string형으로 받아온 날짜 리스트
    private ArrayList<DateData> result = new ArrayList<>();  //DateData 객체 형으로 받아온 날짜 리스트
    private RecyclerView recyclerView;
    private LinearLayoutManager linearLayoutManager;
    private DateAdapter dateAdapter; //날짜 Adapter
    private DateData data;


    private SharedPreferences id;
    private String countryName, startDate, finishDate, documentId, budget; //나라이름, 시작날짜, 종료날짜, 문서 Id, 예산
    private String allDates; //날짜 사이의 날짜들

    private String[] week={"월","화","수","목","금","토","일"}; //요일 변환을 위한 배열
    //Fragment
    private FragmentManager fragmentManager;
    private FragmentTransaction transaction;
    private FragmentPrice fragmentPrice;

    //Fragment data
    private ArrayList<String> sectionList; //section이 들어가는 리스트
    private HashMap<String, ArrayList<PriceData>> itemList; //section과 priceData가 들어가는 리스트
    private ArrayList<PriceData> priceList;  //priceData가 들어가는 리스트
    private PriceData priceData; //priceData 객체


    private TextView price_btn_all; //총 지출 금액
    private FloatingActionButton fa_add; //price_add button

    //초기화
    private FirebaseAuth firebaseAuth = FirebaseAuth.getInstance(); //파이어베이스 인증 처리
    private FirebaseFirestore db = FirebaseFirestore.getInstance(); //firestore
    

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //상태바 제거
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.list_price);

        //처음 리사이클러뷰 상태 초기화
        initView();

        //지출 목록 load => fragment
        initFragment();

    }

    //Firestore에서 데이터 읽기 -> 비동기 -> callback 함수
    public void readData(MyCallBack myCallBack){
        //db경로 설정
        //경로 -> collection-document-collection-document-collection-document
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        DocumentReference df = db.collection("users").document(firebaseUser.getUid());
        DocumentReference sf = df.collection("countries").document(documentId);

        //Price adapter set에 필요한 sectionList, ItemList를 가공 & chart에 필요한 데이터 가공
        sf.collection("Price").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if(task.isSuccessful()){
                    ArrayList<String> documentList = new ArrayList<>(); //for sectionList
                    ArrayList<PriceData> allDocumentList = new ArrayList<>();  //for priceList, sectionList, chartList
                    ArrayList<PriceData> allPriceList = new ArrayList<>(); //for priceList
                    sectionList = new ArrayList<>();
                    itemList = new HashMap<>();
                    double total = 0; //남은돈을 위한 총 사용한 금액

                    PriceData neData;
                    //DB(Firestore)로부터 해당하는 collection("Price")의 모든 문서를 받아서 리스트에 저장
                    for(QueryDocumentSnapshot document : task.getResult()){
                        neData = new PriceData();
                        neData = document.toObject(PriceData.class); //document의 데이터를 PriceData형으로 받기
                        allDocumentList.add(neData);    //모든 문서들의 모든 데이터들을 allDocumentList에 저장
                    }

                    //받은 데이터로 가공 시작
                    //sectionList 가공 -> itemList의 key
                    //sectionList에 필요한 price_add_time만 documentList에 add
                    for(int i=0; i<allDocumentList.size(); i++){
                        String time = allDocumentList.get(i).getPrice_add_time();
                        documentList.add(time);
                    }

                    //sectionList에 저장 -> 중복 방지
                    for(int i=0; i<documentList.size(); i++){
                        String str = documentList.get(i); //중복 x => sectionList
                        if(!sectionList.contains(str))
                            sectionList.add(str);
                    }
                    //test
                    for(int i=0; i<sectionList.size(); i++){
                        Log.i("알림 sectionList",sectionList.get(i));
                    }


                    //sectionList와 itemList 정렬
                    //sectionList를 오름차순 정렬
                    ArrayList<Date> sectionSort = new ArrayList<>();
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                    for(int i=0; i<sectionList.size(); i++){
                        try {
                            sectionSort.add(sdf.parse(sectionList.get(i)));  //sectionList를 Date 리스트 형인 sectionSort에 저장
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    }

                    //sectionSort를 정렬
                    Collections.sort(sectionSort, new Comparator<Date>() {
                        @Override
                        public int compare(Date date1, Date date2) {
                            return date1.compareTo(date2);  //sectionSort 리스트를 오름차순 정렬
                        }
                    });
                    //최종 sectionList
                    sectionList = new ArrayList<>();
                    for(int i=0; i<sectionSort.size(); i++){
                        sectionList.add(sdf.format(sectionSort.get(i))); //정렬된 sectionSort를 sectionList에 저장
                        Log.i("알림 정렬된 sectionList",sectionList.get(i));
                    }


                    //allDocumentList에서 필요한 데이터만 골라서 allPriceList에 저장
                    //allPriceList -> itemList에 들어갈 value값
                    for(int i=0; i<allDocumentList.size(); i++){
                        neData = new PriceData();
                        neData.setPrice_spend(allDocumentList.get(i).getPrice_spend());
                        neData.setPrice_icon(allDocumentList.get(i).getPrice_icon());
                        neData.setPrice_item(allDocumentList.get(i).getPrice_item());
                        neData.setPrice_day(allDocumentList.get(i).getPrice_day());
                        neData.setCurrency_sign(allDocumentList.get(i).getCurrency_sign());
                        neData.setPrice_add_time(allDocumentList.get(i).getPrice_add_time());
                        neData.setPrice_memo(allDocumentList.get(i).getPrice_memo());
                        neData.setPrice_picture(allDocumentList.get(i).getPrice_picture());
                        neData.setPrice_current_time(allDocumentList.get(i).getPrice_current_time());
                        neData.setPrice_category(allDocumentList.get(i).getPrice_category());
                        allPriceList.add(neData);

                        //총 사용한 금액 구하기
                        total += Double.parseDouble(allDocumentList.get(i).getPrice_spend());
                    }
                    //test
                    for(int i=0; i< allPriceList.size(); i++){
                        Log.i("알림 allPriceList",allPriceList.get(i).getPrice_spend());
                    }

                    //allPriceList를 sectionList별로 분류해서 itemList에 저장 <= itemList와 sectionList가 priceAdapter에서 바인딩할 때 필요한 data
                    for(int i=0; i<sectionList.size(); i++) {
                        priceList = new ArrayList<>();
                        for (int j = 0; j < allPriceList.size(); j++) {
                            String str = allPriceList.get(j).getPrice_add_time();
                            if (sectionList.get(i).equals(str)) { //날짜로 구분
                                priceList.add(allPriceList.get(j));
                            }
                        }
                        itemList.put(sectionList.get(i), priceList); //itemList 완료
                    }

                    //남은 금액 구하기
                    String left = String.valueOf(Double.parseDouble(budget) - total);

                    myCallBack.onCallback(sectionList,itemList,left); //callback -> priceList


                    //차트를 위한 데이터 => 식비, 교통, 관광, 숙박, 쇼핑, 기타의 spend, category,icon만 받아옴
                    ArrayList<PriceData> chartList = new ArrayList<>();
                    ArrayList<String> checkList = new ArrayList<>(); //6가지 카테고리 중에서 존재하는 카테고리 목록
                    double spend = 0; //지출 금액

                    //같은 카테고리끼리의 데이터를 모아서 지출금액을 정리
                    for(int i=0; i<allDocumentList.size(); i++){ //기준 카테고리 반복
                        neData = new PriceData();
                        PriceData stand = allDocumentList.get(i); //기준
                        spend = Double.parseDouble(stand.getPrice_spend());

                        if(!checkList.contains(stand.getPrice_category())){ //checkList에 기준 카테고리가 없으면
                            for(int j=i+1; j<allDocumentList.size(); j++) { //비교 카테고리 반복
                                PriceData compare = allDocumentList.get(j); //비교
                                if(stand.getPrice_category().equals(compare.getPrice_category())){ //기준카테고리와 비교카테고리가 같으면
                                    spend += Double.parseDouble(compare.getPrice_spend()); //지출금액을 더해줌
                                }
                            }
                            checkList.add(stand.getPrice_category()); //checkList에 카테고리 값 추가
                            neData.setPrice_category(stand.getPrice_category());
                            neData.setPrice_icon(stand.getPrice_icon());
                            neData.setCurrency_sign(stand.getCurrency_sign());
                            neData.setPrice_spend(String.valueOf(spend));
                            chartList.add(neData); //chartList에 추가
                        }
                    }
                    myCallBack.onChartCallback(chartList); //callback -> chartList
                }
            }
        });
    }


    //초기화
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void initView() {
        //sharedPreferences =>각각 profileSet or countryList에서 데이터 받기
        id = getSharedPreferences("id",MODE_PRIVATE);
        countryName = id.getString("countryName","");
        startDate = id.getString("startDate","");
        finishDate = id.getString("finishDate","");
        documentId = id.getString("documentId","");
        budget = id.getString("budget","");

        //Toolbar setting
        Toolbar toolbar = findViewById(R.id.price_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        //Toolbar title set
        TextView title = toolbar.findViewById(R.id.toolbar_title);
        title.setText(countryName);

        //모든 데이터를 보여주는 버튼
        price_btn_all = findViewById(R.id.price_btn_all);
        price_btn_all.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //데이터 불러오기
                readData(new MyCallBack() {
                    @Override
                    public void onCallback(ArrayList<String> sectionList, HashMap<String, ArrayList<PriceData>> itemList, String left) {
                        fragmentManager = getSupportFragmentManager();
                        transaction = fragmentManager.beginTransaction();

                        if(sectionList.size() == 0){ //db에 데이터가 없을 때,
                            FragmentFirst fragmentFirst = new FragmentFirst();
                            transaction.replace(R.id.frag_container, fragmentFirst); //fragmentFirst
                            transaction.commit();
                        }
                        else { //데이터가 존재 할 때,
                            fragmentPrice = new FragmentPrice();
                            //bundle에 데이터 저장 -> fragment에서 recyclerview
                            Bundle bundle = new Bundle();
                            bundle.putStringArrayList("sectionList", sectionList);
                            bundle.putSerializable("itemList", itemList);
                            bundle.putString("left", left);
                            fragmentPrice.setArguments(bundle);

                            transaction.replace(R.id.frag_container, fragmentPrice); //fragmentPrice
                            transaction.commit();
                        }
                    }

                    @Override
                    public void onCountryCallback(ArrayList<CountryData> countryList) {
                    }
                    @Override
                    public void onChartCallback(ArrayList<PriceData> chartList) {
                    }
                });
            }
        });

        dates = new ArrayList<>();
        //여행 기간을 구해서 dates에 저장
        dates = addList(startDate,finishDate); //날짜 사이간 날짜들을 구하는 함수

        //여행 기간 날짜를 DateData 객체로 생성해서 result 리스트에 저장
        for(int i=0;i<dates.size();i++){
            String[] dsplit = dates.get(i).split("-");
            LocalDate localDate = LocalDate.of(Integer.valueOf(dsplit[0]),Integer.valueOf(dsplit[1]),Integer.valueOf(dsplit[2]));
            DayOfWeek dayOfWeek = localDate.getDayOfWeek(); //요일을 구하기 위한 객체
            int dayOfWeekNumber = dayOfWeek.getValue(); //요일 숫자가 나옴

            //연,요일,일,월
            data = new DateData(dsplit[0],week[dayOfWeekNumber-1],dsplit[2],dsplit[1]); //week는 따로 한글로 요일을 만드는 배열열
            result.add(data);
        }

        //상단 날짜 recyclerview
        recyclerView = (RecyclerView) findViewById(R.id.rv_date);
        linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(RecyclerView.HORIZONTAL); //수평
        recyclerView.setLayoutManager(linearLayoutManager);

        dateAdapter = new DateAdapter(result,this);
        recyclerView.setAdapter(dateAdapter); //adapter set
        dateAdapter.notifyDataSetChanged(); //adapter update
        dateAdapter.setItemClickListenenr(listener); //adapter item click

        //SharedPreferences에 저장을 위해 날짜들을 String형으로 저장(구분 ",")
        allDates="";
        for(int i=0; i<dates.size(); i++){
            allDates += dates.get(i)+",";
        }
        SharedPreferences.Editor editor = id.edit(); //날짜들을 저장 => PriceAdd 액티비티에 필요
        editor.putString("dates",allDates);
        editor.commit();

        //지출금액 추가 버튼
        fa_add = findViewById(R.id.fa_add);
        fa_add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent  = new Intent(PriceList.this, PriceAdd.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

                SharedPreferences price = getSharedPreferences("price",MODE_PRIVATE);
                SharedPreferences.Editor editor1 = price.edit();
                editor1.clear();
                editor1.commit();

                startActivity(intent);
            }
        });
        
        //차트 정보 저장 -> priceList에서 차트 데이터 처리
        readData(new MyCallBack() {
            @Override
            public void onCallback(ArrayList<String> sectionList, HashMap<String, ArrayList<PriceData>> itemList, String budget) {}
            @Override
            public void onCountryCallback(ArrayList<CountryData> countryList) {}
            @Override
            public void onChartCallback(ArrayList<PriceData> chartList) {
                SharedPreferences chart = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                SharedPreferences.Editor editor = chart.edit();
                Gson gson = new Gson();
                String json = gson.toJson(chartList); //json형태로 sharedpreferences에 저장
                editor.putString("chartList",json);
                editor.commit();
            }
        });
    }


    //Fragment thread start
    private void initFragment() {
        FragmentThread thread = new FragmentThread();
        thread.start();
    }

    //Thread 동작
    private class FragmentThread extends Thread{
        public void run(){
            //데이터 불러오기
            readData(new MyCallBack() {
                @Override
                public void onCallback(ArrayList<String> sectionList, HashMap<String, ArrayList<PriceData>> itemList, String left) {
                    fragmentManager = getSupportFragmentManager();
                    transaction = fragmentManager.beginTransaction();

                    if(sectionList.isEmpty() && itemList.isEmpty()){ //데이터가 없을 때 (초기 화면)
                        FragmentFirst fragmentFirst = new FragmentFirst();
                        transaction.add(R.id.frag_container, fragmentFirst); //fragmentFirst
                        transaction.commit();
                    }
                    else {
                        fragmentPrice = new FragmentPrice();

                        //bundle에 데이터 저장
                        Bundle bundle = new Bundle();
                        bundle.putStringArrayList("sectionList",sectionList);
                        bundle.putSerializable("itemList",itemList);
                        bundle.putString("left",left);
                        fragmentPrice.setArguments(bundle);

                        transaction.replace(R.id.frag_container, fragmentPrice); //fragmentPrice
                        transaction.commit();
                    }
                }

                @Override
                public void onCountryCallback(ArrayList<CountryData> countryList) {}
                @Override
                public void onChartCallback(ArrayList<PriceData> chartList) { }
            });
        }
    }

    //menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.price_menu,menu);
        return super.onCreateOptionsMenu(menu);
    }

    //뒤로가기 버튼 동작
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()){
            case android.R.id.home:{  //뒤로가기
                Intent intent = new Intent(PriceList.this, CountryList.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                return true;
            }
            case R.id.price_logout: //로그아웃 하기
            {
                signOut();
                Intent intent = new Intent(PriceList.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
                break;
            case R.id.show_profile: //프로필 보기 및 수정
            {
                Intent intent = new Intent(PriceList.this, ProfileSet.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                break;
            }
            case R.id.delete_travel: //여행 나라 삭제
            {
                //db경로 설정
                //collection-document-collection-document-collection
                //상위 문서를 삭제해도 하위에 있는 모든 컬레션과 문서들이 삭제 되지 않음.
                //반복문으로 지워줘야 함
                FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                DocumentReference df = db.collection("users").document(firebaseUser.getUid());
                DocumentReference sf = df.collection("countries").document(documentId);
                //"Price" collection의 모든 문서를 읽음
                sf.collection("Price").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if(task.isSuccessful()){
                            for(QueryDocumentSnapshot documentSnapshot : task.getResult()){
                                documentSnapshot.getReference().delete(); //하위 문서를 for루프로 모든 문서를 삭제 해야 함 => 상위 문서 삭제 해도, 하위 컬렉션, 하위 문서 삭제 x
                            }
                        }
                    }
                });
                //모든 하위 문서 삭제 후,
                sf.delete(); //상위 문서 삭제
                Intent intent = new Intent(PriceList.this, CountryList.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                break;
            }
            case R.id.chart: //차트
            {
                SharedPreferences chart = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                if(chart.getString("chartList","").equals("[]")){ //데이터가 존재하지 않을 때
                    Toast.makeText(getApplicationContext(), "지출금액이 없습니다", Toast.LENGTH_SHORT).show();
                }
                else {
                    Intent intent = new Intent(PriceList.this,Chart.class);
                    startActivity(intent);
                    break;
                }

            }

        }
        return super.onOptionsItemSelected(item);
    }

    //안드로이드 기본 버튼으로 뒤로가기를 눌렀을 때,
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode){
            case KeyEvent.KEYCODE_BACK:{
                Intent intent = new Intent(PriceList.this, CountryList.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                return true;
            }
        }
        return false;
    }

    //로그아웃
    private void signOut() {
        firebaseAuth.signOut();
        //구글 연동한 로그인의 로그아웃을 위함.
        //없으면 구글계정 로그아웃 X
        GoogleSignIn.getClient(this, GoogleSignInOptions.DEFAULT_SIGN_IN).signOut();
    }


    //각 날짜를 클릭 했을 때 동작 => 각각의 날짜에 해당하는 데이터를 보여줌
    private OnCardItemClickListener listener = new OnCardItemClickListener() {
        @Override
        public void OnItemClick(CountryAdapter.CustomViewHolder holder, View view, int position) { }

        @Override
        public void OnDateItemClick(DateAdapter.CustomViewHolder holder, View view, int position) {
            //데이터 불러오기
            readData(new MyCallBack() {
                @Override
                public void onCallback(ArrayList<String> sectionList, HashMap<String, ArrayList<PriceData>> itemList, String left) {
                    DateData data = dateAdapter.getItem(position);
                    //비교를 위한 데이터 형태 변환 => ex) 2021-10-20
                    String click_date = data.getItem_year()+"-"+data.getItem_month()+"-"+data.getItem_date();
                    Log.i("알림 데이터 확인",click_date);
                    fragmentManager = getSupportFragmentManager();
                    transaction = fragmentManager.beginTransaction();

                    //클릭한 날짜가 sectionList에 없으면 emptyFragment <= 클릭한 날짜에 데이터가 없음
                    if(!sectionList.contains(click_date)){
                        FragmentEmpty fragmentEmpty = new FragmentEmpty();
                        Bundle bundle = new Bundle();

                        for(int j=0; j<dates.size(); j++){
                            if(click_date.equals(dates.get(j))){ //section 헤더만 표시하기위해 2개의 데이터만 저장
                                bundle.putInt("day",j+1);  //Day1
                                bundle.putString("date",click_date); //2021-10-10
                            }
                        }
                        fragmentEmpty.setArguments(bundle);
                        transaction.replace(R.id.frag_container, fragmentEmpty); //fragmentEmpty
                        transaction.commit();
                    }
                    else{
                        //비교 후 데이터를 fragment에 넘겨줌 <= 클릭한 날짜의 데이터가 있음
                        for(int i=0; i<sectionList.size(); i++){
                            if(click_date.equals(sectionList.get(i))){ //클릭한 날짜와 sectionList의 날짜들을 비교 -> 같으면,
                                //recyclerview에 필요한 ArrayList와 HashMap을 새로 생성
                                ArrayList<String> click_section = new ArrayList<>();
                                ArrayList<PriceData> click_price = new ArrayList<>();
                                HashMap<String, ArrayList<PriceData>> click_item = new HashMap<String, ArrayList<PriceData>>();

                                click_section.add(sectionList.get(i)); //sectionList -> key

                                for(int x=0; x<itemList.get(sectionList.get(i)).size(); x++)
                                    click_price.add(itemList.get(sectionList.get(i)).get(x)); //priceList -> value
                                click_item.put(sectionList.get(i),click_price); //itemList -> hashMap

                                fragmentPrice = new FragmentPrice();

                                //bundle에 데이터 저장
                                Bundle bundle = new Bundle();
                                bundle.putStringArrayList("sectionList",click_section);
                                bundle.putSerializable("itemList",click_item);
                                bundle.putString("left",left);
                                fragmentPrice.setArguments(bundle);

                                transaction.replace(R.id.frag_container, fragmentPrice); //fragmentPrice
                                transaction.commit();
                            }
                        }
                    }
                }
                @Override
                public void onCountryCallback(ArrayList<CountryData> countryList) {}
                @Override
                public void onChartCallback(ArrayList<PriceData> chartList) { }
            });
        }
    };

    //날짜와 날짜 사이의 day를 구하는 메소드
    private ArrayList<String> addList(String s1, String s2) {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd"); //날짜 형식 지정
        try {
            //Data 타입으로 변경
            Date start = df.parse(s1);
            Date finish = df.parse(s2);

            while(start.compareTo(finish) <=0 ){ //start~finish
                dates.add(df.format(start));
                Calendar c = Calendar.getInstance();
                c.setTime(start); //시작 날짜를 calendar 객체에 set
                c.add(Calendar.DAY_OF_MONTH, 1);
                start = c.getTime();
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return dates;
    }
}
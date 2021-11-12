package com.example.travelrecord;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Currency;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class CountryList extends AppCompatActivity{
    private ArrayList<CountryData> countryList = new ArrayList<>(); //여행 목록 List => Adapter에서 필요
    private CountryData countryData;  // 데이터 객체
    private CountryAdapter countryAdapter;  //어댑터
    private RecyclerView recyclerView;
    private LinearLayoutManager linearLayoutManager;
    private Button btn_mtravel; //여행 목록 추가 버튼
    private FirebaseAuth firebaseAuth; //파이어베이스 인증 처리
    private FirebaseFirestore db; //firestore
    private SharedPreferences id; //기본 정보들을 저장하기위한 변수
    private ArrayList<CountryList.Item> rateList = new ArrayList<>(); //환율 정보 List

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //상태바 제거
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.list_country);

        //view 초기화
        initView();


    }

    //Firestore로부터 데이터를 읽음 => 비동기 => CallBack 함수 사용
    public void readData(MyCallBack myCallBack){
        //초기화
        firebaseAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();

        //db에 있는 내용 및 어댑터 연결
        //사용자 정보까지의 경로 -> collection-document
        DocumentReference df = db.collection("users").document(firebaseUser.getUid());

        //데이터를 저장할 경로 -> collection-document-collection-document
        //"countries" collection의 모든 문서를 읽음
        df.collection("countries").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if(task.isSuccessful()){
                    countryList.clear();
                    //로그인한 사용자의 countries라는 collection의 모든 document(나라) 반복
                    for(QueryDocumentSnapshot document : task.getResult()){
                        //각 document의 데이터를 받아서 해당하는 key값의 field값을 string형으로 각각 받아옴
                        String countryName = document.getData().get("countryName").toString();
                        String imgUri = document.getData().get("imgUri").toString();
                        String memo = document.getData().get("memo").toString();
                        String startDate = document.getData().get("startDate").toString();
                        String finishDate = document.getData().get("finishDate").toString();
                        String budget = document.getData().get("budget").toString();
                        String ISO = document.getData().get("iso").toString();
                        String background = document.getData().get("background").toString();
                        String documentId = document.getData().get("documentId").toString();


                        countryData = new CountryData(countryName,imgUri,memo,startDate,finishDate,budget,ISO,background,documentId);
                        countryList.add(countryData); //리스트에 추가 for 어댑터연결을 위해서
                    }
                    Collections.sort(countryList, new SortByDate()); //정렬 -> 시작 날짜순
                    myCallBack.onCountryCallback(countryList); //callback
                }
                else {
                    Log.d("알림","Error getting documents: "+ task.getException());
                }
            }
        });
    }

    //초기화
    private void initView() {
        //Toolbar setting
        Toolbar toolbar = findViewById(R.id.country_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);


        //리사이클러뷰 설정
        recyclerView = (RecyclerView) findViewById(R.id.rv);
        recyclerView.setHasFixedSize(true); //리사이클러뷰 성능 강화
        linearLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setItemViewCacheSize(5);

        ReadThread readThread = new ReadThread();
        readThread.start(); //recyclerview에 set


        //새 여행 목록 만들기 설정
        btn_mtravel = findViewById(R.id.btn_mtravel);
        btn_mtravel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(CountryList.this, SearchList.class);
                startActivity(intent);
            }
        });


        //최신 환율 정보 불러오기
        CurrencyJson currencyJson = new CurrencyJson();
        try {
            rateList = currencyJson.execute().get(); //파싱된 json data를 rateList에 저장 -> ISO 3166(2자리) : ISO 4217(국가통화코드)
        } catch (ExecutionException e) {   //ex) US : USD
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    //Thread
    private class ReadThread extends Thread{
        @Override
        public void run() {
            readData(new MyCallBack() {
                @Override
                public void onCallback(ArrayList<String> sectionList, HashMap<String, ArrayList<PriceData>> itemList, String budget) { }

                @Override
                public void onCountryCallback(ArrayList<CountryData> countryList) {
                    RecyclerView.ItemAnimator animator = recyclerView.getItemAnimator();
                    if (animator instanceof SimpleItemAnimator) {
                        ((SimpleItemAnimator) animator).setSupportsChangeAnimations(false);
                    }

                    countryAdapter = new CountryAdapter(countryList,getApplicationContext());
                    countryAdapter.setHasStableIds(true); //성능 향상
                    recyclerView.setAdapter(countryAdapter); //Adapter 연결
                    countryAdapter.notifyDataSetChanged(); //Adapter update
                    countryAdapter.setItemClickListenenr(listener); //카드뷰를 클릭했을 때 이벤트
                }

                @Override
                public void onChartCallback(ArrayList<PriceData> chartList) {

                }
            });
        }
    }

    //menu -> toolbar
    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.country_menu,menu);
        return super.onCreateOptionsMenu(menu);
    }

    //뒤로가기 동작 & menu안에 있는 item들의 동작
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()){
            case android.R.id.home:{
                Intent intent = new Intent(CountryList.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); //스택에 중복된 activity있으면 그 위에 value 삭제 -> activity 중복 방지
                startActivity(intent);
                signOut(); //logout
                return true;
            }
            case R.id.country_logout:
            {
                //로그아웃 하기
                signOut();
                Intent intent = new Intent(CountryList.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
                break;

        }
        return super.onOptionsItemSelected(item);
    }

    //키보드 뒤로가기 눌렀을 때
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode){
            case KeyEvent.KEYCODE_BACK:{
                Intent intent = new Intent(CountryList.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                signOut();
                return true;
            }
        }
        return false;
    }

    private void signOut() {
        firebaseAuth.signOut();
        //구글 연동한 로그인의 로그아웃을 위함.
        //없으면 구글계정 로그아웃 X
        GoogleSignIn.getClient(this, GoogleSignInOptions.DEFAULT_SIGN_IN).signOut();
    }


    //여행목록 아이템 클릭 시 동작
    private OnCardItemClickListener listener = new OnCardItemClickListener() {
        //@RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        public void OnItemClick(CountryAdapter.CustomViewHolder holder, View view, int position) {
            //아이템 클릭 시, 어댑터에서 해당 아이템의 CountryData객체 가지고 오기
            CountryData item = countryAdapter.getItem(position);

            String currency_num=null, currency_kr=null;

            for(int i=0; i<rateList.size(); i++){
                if(rateList.get(i).getIso().equals(item.getISO())){ //다른 나라 환율
                    currency_num = rateList.get(i).getCurrency();

                }
                if(rateList.get(i).getIso().equals("KRW")){  //원화 환율
                    currency_kr = rateList.get(i).getCurrency();

                }
            }

            //클릭 시, 해당하는 목록, 즉 해당하는 document의 Id값을 sharedPreferneces에 저장
            // => for set Profile
            id = getSharedPreferences("id",Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = id.edit();
            editor.remove("documentId");
            editor.putString("documentId",item.getDocumentId()); //각 나라의 unique한 ID

            //SharedPreferences에 저장 -> 모든 activity에서 db를 부르지 않고, 다음에 필요한 정보만 미리 저장
            editor.putString("countryName",item.getCountryName());
            editor.putString("imgUri",item.getImgUri());
            editor.putString("memo",item.getMemo());
            editor.putString("startDate",item.getStartDate());
            editor.putString("finishDate",item.getFinishDate());
            editor.putString("budget",item.getBudget());
            editor.putString("iso",item.getISO());
            editor.putString("currency",currency_num);
            editor.putString("currency_kr",currency_kr);
            editor.putString("background",item.getBackground());
            editor.commit();


            //예산 설정이 안되어 있으면 profileset으로 intent
            if(item.getBudget() == ""){
                Intent intent = new Intent(CountryList.this,ProfileSet.class);
                startActivity(intent);
            }
            else{ //예산 설정이 되어있으면 priceList로 intent
                Intent intent = new Intent(CountryList.this,PriceList.class);
                startActivity(intent);
            }
        }

        @Override
        public void OnDateItemClick(DateAdapter.CustomViewHolder holder, View view, int position) { }
    };


    //날짜 순으로 정렬
    private class SortByDate implements Comparator<CountryData>{
        @Override
        public int compare(CountryData t1, CountryData t2) {
            return t1.getStartDate().compareTo(t2.getStartDate())*(-1);
        }
    }

    //최근 환율 정보를 받아옴
    public class CurrencyJson extends AsyncTask<String, Void, ArrayList<CountryList.Item>> {
        private String request = "http://api.exchangeratesapi.io/v1/latest?access_key=1afbfc335ae95c303def00a09a4aeea5&format=1";
        private String jsonData;
        private ArrayList<com.example.travelrecord.CountryList.Item> list = new ArrayList<>();
        private ArrayList<String> ratesKeyList = new ArrayList<>();
        private String iso, rate;

        @Override
        protected ArrayList<com.example.travelrecord.CountryList.Item> doInBackground(String... strings) {
            Log.d("파싱이 시작됩니다.", "파싱 시작");
            try {
                URL url = new URL(request); //url 객체 생성
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET"); //get방식
                conn.setRequestProperty("Content-type", "application/json");
                Log.i("Response code: ", conn.getResponseCode() + "");
                BufferedReader rd; //Buffer에 데이터 저장
                if (conn.getResponseCode() >= 200 && conn.getResponseCode() <= 300) { //응답이 정상이면면
                    rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                } else {
                    rd = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                }
                StringBuffer sb = new StringBuffer();
                String line;
                while ((line = rd.readLine()) != null) { //한줄 씩 StringBuffer에 저장
                    sb.append(line);
                }
                jsonData = sb.toString(); //저장된 데이터를 String형으로
                rd.close();
                conn.disconnect();

                //데이터 가공 -> for ArrayList
                JSONObject jsonObject = new JSONObject(jsonData); //String을 JSONObject객체로
                String rateValue = jsonObject.getString("rates"); //key가 rates에 해당하는 값들 저장
                JSONObject rateObject = new JSONObject(rateValue); //위에 있는 value들을 따로 JSONObject객체로 생성
                Iterator i = rateObject.keys(); //json데이터에서 key값들만 추출
                while (i.hasNext()) {
                    String b = i.next().toString();
                    ratesKeyList.add(b); //Key List
                }

                for (int j = 0; j < ratesKeyList.size(); j++) {
                    com.example.travelrecord.CountryList.Item item = new com.example.travelrecord.CountryList.Item();

                    iso = ratesKeyList.get(j); //key 값이 국가통화코드
                    rate = rateObject.getString(iso); //value 값은 환율 -> 유로기준

                    item.setIso(iso);
                    item.setCurrency(rate);

                    list.add(item);
                }


            } catch (Exception e) {
                e.printStackTrace();
            }
            return list;
        }
    }
    public class Item {
        private String iso;
        private String currency;

        public String getIso() {
            return iso;
        }

        public void setIso(String iso) {
            this.iso = iso;
        }

        public String getCurrency() {
            return currency;
        }

        public void setCurrency(String currency) {
            this.currency = currency;
        }
    }
}

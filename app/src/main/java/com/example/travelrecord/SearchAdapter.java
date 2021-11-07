package com.example.travelrecord;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.Adapter;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.ListResult;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Random;

public class SearchAdapter extends Adapter<SearchAdapter.CustomViewHolder> implements Filterable {

    private ArrayList<SearchData> unFilteredList; //필터링 되지 않은 리스트
    private ArrayList<SearchData> filteredList;  //필터링 된 리스트
    private Context context;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore db;
    private SharedPreferences id;

    public SearchAdapter(ArrayList<SearchData> list, Context context){
        this.unFilteredList = list; //필터링 되지 않은 리스트
        this.filteredList = list;  //필터링 된 리스트
        this.context = context;
    }


    //뷰홀더가 새로 만들어지는 시점에 이 메소드가 호출
    //아이템 뷰를 위해 뷰 홀더 객체를 생성하여 리턴
    @NonNull
    @Override
    public CustomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //각 아이템을 위해 정의한 XML 레이아웃을 이용해 뷰 객체를 만들어줌
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.search_item, parent, false);
        CustomViewHolder holder = new CustomViewHolder(view);

        return holder;
    }

    //뷰홀더 객체가 만들어 질 때와 재사용 될 때 자동으로 호출
    //뷰 객체는 기존의 것을 그대로 사용, 데이터만 바꿔줌 -> 그 뷰홀더에 현재 아이템에 맞는 데이터만 설정
    //position에 해당하는 데이터를 뷰홀더의 아이템뷰에 표시함
    @Override
    public void onBindViewHolder(@NonNull CustomViewHolder holder, @SuppressLint("RecyclerView") int position) {

        //바인딩
        //국기 사진 Glide
        Glide.with(holder.itemView)
                .load(filteredList.get(position).getImgUrl()).override(70,50)
                .into(holder.iv_country);

        //나라이름
        holder.tv_countryname.setText(filteredList.get(position).getCountryName());


        //아이템 뷰의 라디오버튼이 클릭 되었을 때, 다음 activity로 이동
        holder.rg_country.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                if(i == R.id.rb_country){

                    //커버 사진을 위한 랜덤으로 사진 선정
                    pickCover();

                    //초기화
                    firebaseAuth = FirebaseAuth.getInstance();
                    db = FirebaseFirestore.getInstance();


                    //받아온 countryName을 기준으로 db에 데이터 생성
                    // -> 중복을 허용해야 함으로 document는 나라이름으로 생성하지않고 자동으로 생성하게 만듦
                    FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                    DocumentReference df = db.collection("users").document(firebaseUser.getUid());
                    DocumentReference sf = df.collection("countries").document();

                    //잠깐 저장해 놓은 랜덤으로 받아온 배경화면 이미지파일의 경로를 변수 background에 저장
                    id = context.getSharedPreferences("id",Context.MODE_PRIVATE);
                    String background = id.getString("background","");

                    //DB에 저장을 위해 CountryData 객체에 set
                    CountryData country = new CountryData();
                    country.setCountryName(filteredList.get(position).getCountryName());
                    country.setImgUri(filteredList.get(position).getImgUrl());
                    country.setISO(filteredList.get(position).getCurrency_code());
                    country.setMemo("");
                    country.setBudget("");
                    country.setStartDate("");
                    country.setFinishDate("");
                    country.setBackground(background);
                    country.setDocumentId(sf.getId());

                    sf.set(country); //set


                    //DB메 저장이 끝난 후, CountryList로 intent
                    Intent intent = new Intent(context,CountryList.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    context.startActivity(intent);
                }
            }
        });
    }

    //커버사진을 고르기위한 method
    private void pickCover() {

        ArrayList<String> backgroundList = new ArrayList<>();
        for(int i=1;i<31;i++){ //기본 커버사진은 drawable 폴더에 country_bg(숫자) 형태로 되어있음
            backgroundList.add("country_bg"+i);
        }

        //배경화면을 랜덤하게 설정해줌
        Random random = new Random();
        int num = random.nextInt(backgroundList.size());
        String background = backgroundList.get(num+1); //랜덤하게 받아온 배경화면

        id = context.getSharedPreferences("id",Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = id.edit();
        editor.remove("background"); //background에 저장된 값을 remove
        editor.putString("background",background);
        editor.commit();
    }

    @Override
    public int getItemCount() {
        return filteredList.size();
    }

    //검색어 필터링을 위한 메소드
    @Override
    public Filter getFilter() {
        return new Filter(){
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {

                //받아온 텍스트를 String 형태로 변환
                String str = constraint.toString();

                //변환된 스트링이 비어있으면
                if(str.isEmpty()){
                    filteredList = unFilteredList;
                }
                else {  //아니면 하나하나 비교해서 필터링
                    ArrayList<SearchData> filteringList = new ArrayList<>();
                    for(SearchData data : unFilteredList){
                        if(data.getCountryName().contains(str)){ //입력 받아온 텍스트가 리스트의 나라이름에 포함되어 있으면
                            filteringList.add(data); //필터링 중인 리스트에 필터링 되지 않은 리스트를 add
                        }
                    }
                    filteredList = filteringList;
                }
                FilterResults filterResults = new FilterResults();
                filterResults.values = filteredList;
                return filterResults; //필터링이 끝난 데이터를 반환
            }

            //리사이클러뷰 업데이트 작업
            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                filteredList = (ArrayList<SearchData>)filterResults.values;
                notifyDataSetChanged();
            }
        };
    }


    //아이템 뷰를 저장하는 뷰홀더 클래스
    public class CustomViewHolder extends RecyclerView.ViewHolder{
        private ImageView iv_country;
        private TextView tv_countryname;
        private RadioGroup rg_country;
        private RadioButton rb_country;


        public CustomViewHolder(@NonNull View itemView) {
            super(itemView);
            iv_country = (ImageView) itemView.findViewById(R.id.iv_country);
            tv_countryname = (TextView) itemView.findViewById(R.id.tv_countryname);
            rg_country = (RadioGroup) itemView.findViewById(R.id.rg_country);
            rb_country = (RadioButton) itemView.findViewById(R.id.rb_country);
        }
    }
}

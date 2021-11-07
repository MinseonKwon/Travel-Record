package com.example.travelrecord;


import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;

public class CountryAdapter extends RecyclerView.Adapter<CountryAdapter.CustomViewHolder> implements OnCardItemClickListener{

    private ArrayList<CountryData> arrayList;
    private Context context;
    private OnCardItemClickListener listenr;

    public CountryAdapter(ArrayList<CountryData> arrayList, Context context){
        this.arrayList = arrayList;
        this.context = context;
    }


    //리스트 뷰가 생성될 때의 생명주기
    //아이템 뷰를 위한 뷰홀더 객체를 생성하여 리턴
    @NonNull
    @Override
    public CustomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        //레이아웃 XML파일을 View 객체로 만들기 위해서 LayoutInflater내의 inflater매서드를 이용
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item,null);
        CustomViewHolder holder = new CustomViewHolder(view);
        //View 객체에 미리 view의 정보를 담은 후, 참조하고 싶을 때, view라는 변수를 통해 참조하기 위함.
        //inflate(view를 만들고 싶은 레이아웃 파일의 id, 생성될 view의 parent, true일 경우, root의 자식 view로 자동으로 추가됨)

        return holder;
    }

    //실제 데이터가 추가되었을 때의 생명주기
    //position에 해당하는 데이터를 뷰홀더의 아이템뷰에 표시
    @Override
    public void onBindViewHolder(CustomViewHolder holder, @SuppressLint("RecyclerView") int position) {
        //아이템 뷰에 각각의 position에 해당하는 값을 set 시킴.
        holder.tv_citem.setText(arrayList.get(position).getCountryName());


        //카드뷰 안에 있는 ImageView에 position에 해당하는 value에 따라 glide => 국기 이미지 로드
        Glide.with(holder.cardView)
                .load(arrayList.get(position).getImgUri()).override(70,50)
                .into(holder.iv_country_img);


        //카드뷰 배경 설정
        String background = arrayList.get(position).getBackground();

        //이미지 위의 데이터가 잘보일 수 있게 setColorFilter
        holder.cardview_background.setColorFilter(Color.parseColor("#BDBDBD"),PorterDuff.Mode.MULTIPLY);

        String str = background.substring(0,7);
        if(!str.equals("country")){ //커버사진이 drawable 폴더가 아닌 storage에 있을 경우,
            FirebaseStorage storage = FirebaseStorage.getInstance();
            StorageReference storageRef = storage.getReference();
            StorageReference pathRef = storageRef.child("coverList/"+background);

            Glide.with(holder.cardView)
                    .load(pathRef)
                    .into(holder.cardview_background);
        }else{  //초기 생성 배경 & 변경이 안됬을 때
            int iResId = context.getResources().getIdentifier(background,"drawable", context.getPackageName());
            Glide.with(holder.cardView)
                    .load(iResId)
                    .into(holder.cardview_background);
        }


        //여행 시작 날짜와 마지막 날짜 setText
        String sdate = arrayList.get(position).getStartDate();
        String fdate = arrayList.get(position).getFinishDate();
        if(sdate!="" && fdate!="")  //시작 날짜와 마지막 날짜 모두 O
            holder.tv_set_date.setText(sdate+ " ~ " +fdate);
        else
            holder.tv_set_date.setText("날짜를 설정하세요");


        //예산을 setText
        String budget = arrayList.get(position).getBudget();
        if(budget!="") {//예산 있으면
            DecimalFormat decimalFormat = new DecimalFormat("###,###.##"); //format 설정
            double budget_result = Double.parseDouble(budget);
            holder.tv_bitem.setText(arrayList.get(position).getISO() + " " + decimalFormat.format(budget_result));
        }
        else
            holder.tv_bitem.setText(arrayList.get(position).getISO()+" 0");

    }

    //전체 데이터 개수 리턴
    @Override
    public int getItemCount() {
        return arrayList.size();
    }


    //for setHasStablesId
    @Override
    public long getItemId(int position){
        return arrayList.get(position).hashCode();
    }

    @Override
    public void OnItemClick(CustomViewHolder holder, View view, int position) {
        if(listenr != null){
            listenr.OnItemClick(holder,view,position);
        }
    }

    @Override
    public void OnDateItemClick(DateAdapter.CustomViewHolder holder, View view, int position) {

    }

    //외부에서 리스너를 선정할 수 있는 메소드 추가
    public void setItemClickListenenr(OnCardItemClickListener listenr){
        this.listenr = listenr;
    }


    //포지션에 해당하는 아이템 객체를 불러오는 함수
    public CountryData getItem(int position){
        return arrayList.get(position);
    }



    //아이템 뷰를 저장하는 뷰홀더 클래스
    public class CustomViewHolder extends RecyclerView.ViewHolder {
        private TextView tv_citem;
        private ImageView iv_country_img;
        private TextView tv_set_date;
        private TextView tv_bitem;
        private CardView cardView;
        private ImageView cardview_background;

        public CustomViewHolder(View itemView){
            super(itemView);
            this.tv_citem = (TextView) itemView.findViewById(R.id.tv_citem);
            this.iv_country_img = (ImageView) itemView.findViewById(R.id.iv_country_img);
            this.tv_set_date = (TextView) itemView.findViewById(R.id.tv_set_date);
            this.tv_bitem = (TextView) itemView.findViewById(R.id.tv_bitem);
            this.cardView = (CardView) itemView.findViewById(R.id.cardview);
            this.cardview_background = (ImageView) itemView.findViewById(R.id.cardview_background);

            //어댑터가 아니라 activity에서 지정 포지션에 해당하는 클릭 이벤트를 위해
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int position = getAdapterPosition();
                    if(listenr != null){
                        listenr.OnItemClick(CustomViewHolder.this,view,position); }
                }
            });
        }
    }
}

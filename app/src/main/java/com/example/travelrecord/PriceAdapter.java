package com.example.travelrecord;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.afollestad.sectionedrecyclerview.SectionedRecyclerViewAdapter;
import com.bumptech.glide.Glide;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Currency;
import java.util.HashMap;

public class PriceAdapter extends SectionedRecyclerViewAdapter<PriceAdapter.ViewHolder> {

    private Activity activity;
    private ArrayList<String> sectionList; //hashMap의 키값
    private HashMap<String, ArrayList<PriceData>> itemList = new HashMap<>(); //바인딩에 필요한 데이터
    private SharedPreferences price; //다음 동작을 위해 필요한 정보 저장
    private SharedPreferences id; //이전에 저장했던 정보

    //create constructor
    public PriceAdapter(Activity activity, ArrayList<String> sectionList, HashMap<String, ArrayList<PriceData>> itemList){
        this.activity = activity;
        this.sectionList = sectionList;
        this.itemList = itemList;
        notifyDataSetChanged();
    }

    @Override
    public int getSectionCount() {
        return sectionList.size();
    }

    @Override
    public int getItemCount(int section) {
        return itemList.get(sectionList.get(section)).size();
    }

    //Section -> Header 바인딩
    @Override
    public void onBindHeaderViewHolder(PriceAdapter.ViewHolder viewHolder, int i) {
        //Set section value
        viewHolder.price_tv_day.setText(itemList.get(sectionList.get(i)).get(0).getPrice_day()); //Day1
        viewHolder.price_tv_date.setText(sectionList.get(i)); //2021-10-10
    }

    //실제 데이터 바인딩
    @Override
    public void onBindViewHolder(PriceAdapter.ViewHolder viewHolder, int i, int i1, int i2) {
        //item value 초기화
        PriceData sItem = itemList.get(sectionList.get(i)).get(i1);

        //카테고리 아이콘 -> 아이콘은 drawable 폴더에
        int iResId = activity.getResources().getIdentifier(sItem.getPrice_icon(),"drawable", activity.getPackageName());
        Glide.with(viewHolder.itemView)
                .load(iResId)
                .into(viewHolder.price_icon);

        //카테고리 항목명
        viewHolder.price_item.setText(sItem.getPrice_item());

        //지출 금액
        DecimalFormat decimalFormat = new DecimalFormat("###,###.##");
        double budget_result = Double.parseDouble(sItem.getPrice_spend());
        viewHolder.price_spend.setText(sItem.getCurrency_sign()+" "+ decimalFormat.format(budget_result));

        //지출금액 영역 아닌 부분 -> 수정,삭제 & 사진, 메모 보여주는 activity
        viewHolder.to_edit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //다음 activity에 필요한 정보 저장 -> for 빠른 로드
                price = activity.getSharedPreferences("price", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = price.edit();
                editor.putString("documentTime",sItem.getPrice_current_time());
                editor.putString("item",sItem.getPrice_item());
                editor.putString("picture",sItem.getPrice_picture());
                editor.putString("memo",sItem.getPrice_memo());
                editor.putString("spend",sItem.getPrice_spend());
                editor.putString("category",sItem.getPrice_category());
                editor.putString("icon",sItem.getPrice_icon());
                editor.putString("currentTime",sItem.getPrice_add_time());
                editor.commit();

                Intent intent = new Intent(activity,PriceEdit.class);
                activity.startActivity(intent);
            }
        });

        id = activity.getSharedPreferences("id",Context.MODE_PRIVATE);
        String iso = id.getString("iso","");

        //각 아이템의 금액 부분을 클릭 시, 환율에 따라 금액 변환
        //지출금액 영역 클릭
        if(!iso.equals("KRW")) { //여행나라가 대한민국이 아닐때만 클릭 허용
            viewHolder.to_exchange.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Currency currencykr = Currency.getInstance("KRW"); //원화 기호
                    String exchange,exchange_kr;

                    //원화 -> 다른나라 통화 : 초기상태
                    if (String.valueOf(viewHolder.price_spend.getText().toString().charAt(0)).equals(currencykr.getSymbol())) {
                        viewHolder.price_spend.setText(sItem.getCurrency_sign()+" "+ decimalFormat.format(budget_result));
                    }
                    else { // 다른나라 통화 ->  원화 환전
                        exchange = id.getString("currency","");
                        exchange_kr = id.getString("currency_kr","");
                        NumberFormat numberFormat = NumberFormat.getCurrencyInstance();

                        if(exchange.equals("1")){ //유로일 때,
                            double result = Double.parseDouble(exchange_kr) * Double.parseDouble(sItem.getPrice_spend());
                            int r_result = (int)Math.round(result);
                            viewHolder.price_spend.setText(numberFormat.format(r_result));
                        }
                        else{
                            double result = ((1 / Double.parseDouble(exchange))*Double.parseDouble(exchange_kr)) * Double.parseDouble(sItem.getPrice_spend());
                            int r_result = (int)Math.round(result);
                            viewHolder.price_spend.setText(numberFormat.format(r_result));
                        }
                    }
                }
            });
        }
    }

    @Override
    public int getItemViewType(int section, int relativePosition, int absolutePosition) {
        //check condition
        if(section == 1){
            //section이 1이면
            return 0;
        }
        //Return all three position
        return super.getItemViewType(section, relativePosition, absolutePosition);
    }

    @NonNull
    @Override
    public PriceAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //Default layout
        int layout;

        //check condition
        if(viewType == VIEW_TYPE_HEADER){
            //view type이 header면
            layout = R.layout.item_category;
        }
        else{
            //view type이 item이면 -> 실제 데이터(지출금액)
            layout = R.layout.price_item;
        }
        //view 초기화
        View view = LayoutInflater.from(parent.getContext()).inflate(layout,parent,false);

        //return viewholder
        return new ViewHolder(view);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        protected ImageView price_icon;
        protected TextView price_item;
        protected TextView price_spend;
        protected TextView price_tv_day;
        protected TextView price_tv_date;
        protected LinearLayout to_edit;
        protected LinearLayout to_exchange;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            this.price_icon = (ImageView) itemView.findViewById(R.id.price_icon);
            this.price_item = (TextView)  itemView.findViewById(R.id.price_item);
            this.price_spend = (TextView) itemView.findViewById(R.id.price_spend);
            this.price_tv_day = (TextView) itemView.findViewById(R.id.price_tv_day);
            this.price_tv_date = (TextView) itemView.findViewById(R.id.price_tv_date);
            this.to_edit = (LinearLayout) itemView.findViewById(R.id.to_edit);
            this.to_exchange = (LinearLayout) itemView.findViewById(R.id.to_exchange);

        }
    }
}

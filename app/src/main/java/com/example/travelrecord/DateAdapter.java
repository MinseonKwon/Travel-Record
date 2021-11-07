package com.example.travelrecord;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class DateAdapter extends RecyclerView.Adapter<DateAdapter.CustomViewHolder> implements OnCardItemClickListener{

    private ArrayList<DateData> arrayList;
    private Context context;
    private OnCardItemClickListener listenr;

    public DateAdapter(ArrayList<DateData> arrayList, Context context) {
        this.arrayList = arrayList;
        this.context = context;
    }

    //리스트뷰가 생성될 때의 생명주기
    @NonNull
    @Override
    public CustomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.date_item,parent,false);
        CustomViewHolder holder = new CustomViewHolder(view);

        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull CustomViewHolder holder, int position) {
        holder.item_week.setText(arrayList.get(position).getItem_week()); //week
        holder.item_date.setText(arrayList.get(position).getItem_date());  //day
        holder.item_month.setText(arrayList.get(position).getItem_month()+"월");  //month
    }

    @Override
    public int getItemCount() {
        return (null != arrayList ? arrayList.size():0);
    }

    @Override
    public void OnItemClick(CountryAdapter.CustomViewHolder holder, View view, int position) {
    }

    @Override
    public void OnDateItemClick(CustomViewHolder holder, View view, int position) {
        if(listenr != null){
            listenr.OnDateItemClick(holder,view,position);
        }
    }

    //외부에서 리스너를 선정할 수 있는 메소드 추가
    public void setItemClickListenenr(OnCardItemClickListener listenr){
        this.listenr = listenr;
    }


    //포지션에 해당하는 아이템 객체를 불러오는 함수
    public DateData getItem(int position){
        return arrayList.get(position);
    }



    public class CustomViewHolder extends RecyclerView.ViewHolder {

        protected TextView item_week;
        protected TextView item_date;
        protected TextView item_month;

        public CustomViewHolder(@NonNull View itemView) {
            super(itemView);

            item_week = (TextView) itemView.findViewById(R.id.item_week);
            item_date = (TextView) itemView.findViewById(R.id.item_date);
            item_month = (TextView) itemView.findViewById(R.id.item_month);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int position = getAdapterPosition();
                    if(listenr != null){
                        listenr.OnDateItemClick(CustomViewHolder.this,view,position); }
                }
            });
        }
    }
}

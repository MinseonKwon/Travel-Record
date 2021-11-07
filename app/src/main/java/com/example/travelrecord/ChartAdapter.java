package com.example.travelrecord;



import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Currency;

public class ChartAdapter extends RecyclerView.Adapter<ChartAdapter.CustomViewHolder> {

    private ArrayList<PriceData> arrayList;
    private Context context;
    private SharedPreferences id;
    private double spend=0;

    public ChartAdapter(ArrayList<PriceData> arrayList, Context context){
        this.arrayList = arrayList;
        this.context = context;
    }


    //리스트 뷰가 생성될 때의 생명주기
    //아이템 뷰를 위한 뷰홀더 객체를 생성하여 리턴
    @NonNull
    @Override
    public CustomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        //레이아웃 XML파일을 View 객체로 만들기 위해서 LayoutInflater내의 inflater매서드를 이용
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.chart_item,null);
        CustomViewHolder holder = new CustomViewHolder(view);
        //View 객체에 미리 view의 정보를 담은 후, 참조하고 싶을 때, view라는 변수를 통해 참조하기 위함.
        //inflate(view를 만들고 싶은 레이아웃 파일의 id, 생성될 view의 parent, true일 경우, root의 자식 view로 자동으로 추가됨)

        return holder;
    }

    //실제 데이터가 추가되었을 때의 생명주기
    //position에 해당하는 데이터를 뷰홀더의 아이템뷰에 표시
    @Override
    public void onBindViewHolder(CustomViewHolder holder, @SuppressLint("RecyclerView") int position) {
        //카테고리 아이콘 배경 set
        int iResIdB = context.getResources().getIdentifier(arrayList.get(position).getChart_background(),"drawable", context.getPackageName());
        holder.chart_icon.setBackgroundResource(iResIdB);

        //카테고리 아이콘
        int iResId = context.getResources().getIdentifier(arrayList.get(position).getPrice_icon()+"w","drawable", context.getPackageName());
        Glide.with(holder.itemView)
                .load(iResId)
                .into(holder.chart_icon);

        //카테고리 이름
        holder.chart_category.setText(arrayList.get(position).getPrice_category());

        //지출 금액
        DecimalFormat decimalFormat = new DecimalFormat("###,###.##");
        spend = Double.parseDouble(arrayList.get(position).getPrice_spend());
        holder.chart_spend.setText(arrayList.get(position).getCurrency_sign()+" "+ decimalFormat.format(spend));


        //지출금액 클릭 시 => 환전
        id = context.getSharedPreferences("id",Context.MODE_PRIVATE);
        String iso = id.getString("iso","");

        if(!iso.equals("KRW")) {
            holder.chart_spend.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Currency currencykr = Currency.getInstance("KRW");
                    String exchange,exchange_kr;
                    if (String.valueOf(holder.chart_spend.getText().toString().charAt(0)).equals(currencykr.getSymbol())) {
                        holder.chart_spend.setText(arrayList.get(position).getCurrency_sign()+" "+ decimalFormat.format(spend));
                    } else {
                        exchange = id.getString("currency","");
                        exchange_kr = id.getString("currency_kr","");
                        NumberFormat numberFormat = NumberFormat.getCurrencyInstance();

                        if(exchange.equals("1")){
                            double result = Double.parseDouble(exchange_kr) * spend;
                            int r_result = (int)Math.round(result);
                            holder.chart_spend.setText(numberFormat.format(r_result));
                        }
                        else{
                            double result = ((1 / Double.parseDouble(exchange))*Double.parseDouble(exchange_kr)) * spend;
                            int r_result = (int)Math.round(result);
                            holder.chart_spend.setText(numberFormat.format(r_result));
                        }
                    }
                }
            });
        }
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


    //아이템 뷰를 저장하는 뷰홀더 클래스
    public class CustomViewHolder extends RecyclerView.ViewHolder {
        protected ImageView chart_icon;
        protected TextView chart_category;
        protected TextView chart_spend;

        public CustomViewHolder(View itemView){
            super(itemView);
            this.chart_icon = (ImageView) itemView.findViewById(R.id.chart_icon);
            this.chart_category = (TextView)  itemView.findViewById(R.id.chart_category);
            this.chart_spend = (TextView) itemView.findViewById(R.id.chart_spend);
        }
    }
}

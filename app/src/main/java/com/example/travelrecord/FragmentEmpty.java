package com.example.travelrecord;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.HashMap;

//각 날짜마다의 기록이 없을 때의 fragment in PriceList
public class FragmentEmpty extends Fragment {

    private int day;
    private String date;
    private TextView empty_day;
    private TextView empty_date;

    public FragmentEmpty(){}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.empty_fragment,container,false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if(getArguments() != null){
            //전달한 key 값 -> 상단에 데이터가 나타나야함 => Day1  2021-10-10
            day = getArguments().getInt("day");
            date = getArguments().getString("date");
        }

        empty_day = view.findViewById(R.id.empty_day);
        empty_date = view.findViewById(R.id.empty_date);

        empty_day.setText("Day"+day);
        empty_date.setText(date);
    }
}

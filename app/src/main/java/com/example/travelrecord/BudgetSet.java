package com.example.travelrecord;

import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.material.textfield.TextInputEditText;

import java.lang.reflect.Array;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Currency;
import java.util.Stack;

public class BudgetSet extends AppCompatActivity {
    private ImageView iv_country_img; //각 나라의 국기
    private TextView tv_budget_country_name; //각 나라의 이름
    private TextView tv_cal; //입력한 계산식 표시
    private TextView all_budget; //환전 전 입력 금액
    private TextView exchange_budget; //환전된 금액
    private TextView tv_iso; // 다른 나라 ISO
    private TextView et_currency; //다른 나라 통화로 된 금액
    private TextView et_won; //환율에 따라 변환된 krw
    private Button btn_cancel; //취소
    private Button btn_save; //저장


    private Button btn[] = new Button[10]; //계산기 숫자(오름차순) + 소수점
    private Button btn_opt[] = new Button[4]; //연산자(/,*,-,+)
    private Button btn_delete; //삭제
    private Button btn_point; //소수점
    private Stack<String> operatorStack; //연산자를 위한 스택
    private ArrayList<String> infixList; //중위 표기
    private ArrayList<String> postfixList;  //후위 표기
    private ArrayList<Integer> checkList; //중복제거를 위한 리스트 => 1은 숫자, 2는 소수점, 0은 연산자 입력시 add

    private SharedPreferences id; //처음 여행 목록을 선택했을 때의 그 여행하는 나라의 작은 정보들이 들어있음
    private String countryName, imgUri,iso; //UI에 load에 필요한 변수 => 나라이름, 국기 url, 국가통화코드
    private String budget_num, exchange, exchange_kr; //최종 계산 결과 => 그냥 숫자만(다른 기호 없이), 각 여행 나라의 환율(유로 기준), 우리나라 환율
    private String compare="";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.set_budget);

        iv_country_img = findViewById(R.id.iv_country_img);
        tv_budget_country_name = findViewById(R.id.tv_budget_country_name);
        tv_cal = findViewById(R.id.tv_cal);
        all_budget = findViewById(R.id.all_budget);


        //국기와 나라이름 받아옴 (SharedPreferences => "id")
        id = getSharedPreferences("id",MODE_PRIVATE);
        countryName = id.getString("countryName","");
        imgUri = id.getString("imgUri","");
        iso = id.getString("iso","");
        String budget = id.getString("budget","");

        Currency currency = Currency.getInstance(iso); //받아온 국가통화 코드를 기준으로 currency 객체 생성
        DecimalFormat decimalFormat = new DecimalFormat("###,###.##");
        if(budget != "") { //수정시, 예산이 있으면 예산 표시
            double budget_result = Double.parseDouble(budget);
            all_budget.setText(currency.getSymbol()+" "+ decimalFormat.format(budget_result));
            compare = all_budget.getText().toString();
        }
        else{ //처음 예산 설정시
            all_budget.setText(currency.getSymbol()+" 0.0"); //currency에서 지정한 통화 기호 생성
        }

        //국기 이미지 Glide
        Glide.with(this)
                .load(imgUri).override(70,50)
                .into(iv_country_img);

        //나라 이름 set
        tv_budget_country_name.setText(countryName);

        //해당 나라 ISO 설정
        tv_iso = findViewById(R.id.tv_iso);
        tv_iso.setText(iso);


        //환율
        //각 나라의 환율에 따른 우리나라의 환율 표시 => ex) $1 = ₩1,200
        exchange_budget = findViewById(R.id.exchange_budget); //계산할 때의 환율 변화 => 실시간
        et_currency = findViewById(R.id.et_currency); //다른 나라 통화
        et_won = findViewById(R.id.et_won);  // 우리나라 통화
        exchange = id.getString("currency","");
        exchange_kr = id.getString("currency_kr","");

        et_currency.setText(currency.getSymbol() + " 1");
        if(iso.equals("KRW")){
            et_won.setText(currency.getSymbol() + " 1"); //우리나라 통화 일때,
        }
        else {
            NumberFormat numberFormat = NumberFormat.getCurrencyInstance();
            double result = (1 / Double.parseDouble(exchange)) * Double.parseDouble(exchange_kr); //유로 기준이라 변환이 필요함
            int r_result = (int)Math.round(result); //반올림
            et_won.setText(numberFormat.format(r_result));
        }

        //계산기->예산 입력 기능
        //초기화
        cal_init();
        //버튼 클릭 이벤트
        cal_initListener();


        //취소
        btn_cancel = findViewById(R.id.btn_cancel);
        btn_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        //저장 -> 클릭 시, 잠시 SharedPreferences에 저장
        btn_save = findViewById(R.id.btn_save);
        btn_save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!all_budget.getText().toString().equals(currency.getSymbol()+" 0.0")){ //초기값, 즉 예산이 0이 아닐 때만 저장 허용
                    if(compare.equals(all_budget.getText().toString())){ //초기에 비교를 위해 설정된 값과 같으면 => 예산이 변하지 않았으니, 그냥 finish
                        finish();
                    }
                    id = getSharedPreferences("id",MODE_PRIVATE);
                    SharedPreferences.Editor editor = id.edit();
                    editor.remove("budget");
                    editor.putString("budget",budget_num); //예산을 budget에 저장 => profileSet에서 저장할 때, db에 저장
                    editor.commit();

                    setResult(RESULT_OK);
                    finish();
                }
                else {
                    Toast.makeText(getApplicationContext(), "예산을 입력하세요", Toast.LENGTH_SHORT).show();
                }

            }
        });




    }

    private void cal_init() {
        //숫자
        btn[0] = findViewById(R.id.btn_num0);
        btn[1] = findViewById(R.id.btn_num1);
        btn[2] = findViewById(R.id.btn_num2);
        btn[3] = findViewById(R.id.btn_num3);
        btn[4] = findViewById(R.id.btn_num4);
        btn[5] = findViewById(R.id.btn_num5);
        btn[6] = findViewById(R.id.btn_num6);
        btn[7] = findViewById(R.id.btn_num7);
        btn[8] = findViewById(R.id.btn_num8);
        btn[9] = findViewById(R.id.btn_num9);

        //연산자
        btn_opt[0] = findViewById(R.id.btn_div);
        btn_opt[1] = findViewById(R.id.btn_mul);
        btn_opt[2] = findViewById(R.id.btn_sub);
        btn_opt[3] = findViewById(R.id.btn_add);

        //삭제
        btn_delete = findViewById(R.id.btn_delete);
        //소수점
        btn_point = findViewById(R.id.btn_point);


        operatorStack = new Stack<>(); //연산자 리스트 초기화
        infixList = new ArrayList<>(); //전위로 표기된 계산식 리스트 초기화
        postfixList = new ArrayList<>();  //후위로 표기된 계산식 리스트 초기화
        checkList = new ArrayList<>();  //여러 중복들이나 제한을 위한 리스트 초기화

    }

    private void cal_initListener() {
        //숫자
        for(Button i : btn){
            i.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    checkList.add(1); //숫자 클릭 시, =>1 추가
                    tv_cal.append(i.getText().toString()); //계산식을 set

                    //결과
                    result(); //=> 버튼(숫자)을 누를 때마다 계산 결과 반환 ==> (따로 '='이 없음)

                }
            });
        }

        //연산자
        for(Button i: btn_opt){
            i.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(checkList.isEmpty()){  //연산자가 첫번째로 오는 것 방지
                        Log.i("no","첫번째 X");
                    }
                    else if(checkList.get(checkList.size()-1)==0 || checkList.get(checkList.size()-1)==2){ //연산자의 중복과 소수점 다음에 오는 것 방지
                        Log.i("no","중복 or 소수점 x");
                    }
                    else{
                        tv_cal.append(" "+i.getText().toString()+" ");
                        checkList.add(0);
                    }
                }
            });
        }

        //소수점
        btn_point.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(checkList.isEmpty()){
                    Log.i("no","첫번째 x"); //소수점이 첫번째로 오는 것 방지
                }
                else if(checkList.get(checkList.size()-1)==0 || checkList.get(checkList.size()-1)==2){ //연산자 앞 or 소수점 연속 두번 x
                    Log.i("no","중복 or 소수점 x");
                }
                else if(pointCheck(checkList)){ //한 숫자에 여러개의 소수점 방지
                    Log.i("no","한 숫자에 소수점 여러개 x");
                }
                else{
                    tv_cal.append(btn_point.getText().toString());
                    checkList.add(2);
                }

            }
        });

        //삭제
        btn_delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(tv_cal.length() !=0 ){
                    checkList.remove(checkList.size()-1);
                    String[] ex = tv_cal.getText().toString().split(" ");
                    ArrayList<String> li = new ArrayList<>();
                    Collections.addAll(li,ex);
                    li.remove(li.size()-1); //문자 하나 삭제
                    //마지막 연산자일 때, " " 추가
                    if(li.size() > 0 && !isNumber(li.get(li.size()-1)))
                        li.add(li.remove(li.size()-1)+" "); //연산자는 마지막의 " "가 있기 때문에 한번더 삭제후, " "추가
                    tv_cal.setText(TextUtils.join(" ",li));
                }
            }
        });
    }

    //한 숫자에 소수점 여러개 오는거 막는 메소드
    public boolean pointCheck(ArrayList<Integer> checkList){
        for(int i=checkList.size()-2; i>=0 ;i--){ //앞서, 마지막 값은 비교 했기 때문에 그 앞부터 비교
            int check = checkList.get(i);
            if(check == 2) //소수점을 만나면 true
                return true;
            if(check == 0) //연산자를 만나면 false
                return false;
        }
        return false;
    }

    //연산자 가중치(우선순위 -> /*-+)
    public int getWeight(String operator){
        int weight = 0;
        switch(operator){
            case "÷":
            case "×":
                weight = 3;
                break;
            case "-":
            case "+":
                weight = 1;
                break;
        }
        return weight;
    }

    //숫자 판별
    private boolean isNumber(String str) {
        boolean result = true;
        try{
            Double.parseDouble(str);
        }catch (NumberFormatException e){
            result = false;
        }
        return result;
    }

    //중위->후위 표기
    public void infixToPostfix(){
        String text = tv_cal.getText().toString();
        Collections.addAll(infixList,text.split(" "));
        for(String item : infixList){
            //피연산자
            if(isNumber(item)) //value가 숫자면,
                postfixList.add(item); //postfixList에 추가
            else{ //연산자가 오면
                if(operatorStack.isEmpty())
                    operatorStack.push(item); //연산자 스택에 추가
                else{
                    if(getWeight(operatorStack.peek()) >= getWeight(item)) //연산자 우선순위 비교
                        postfixList.add(operatorStack.pop()); //최근 추가된 값을 add
                    operatorStack.push(item);
                }
            }
        }
        //for 루프 끝난 후,
        while(!operatorStack.isEmpty()) //스택에 값이 남았으면 다 postfixList에 순서대로 add
            postfixList.add(operatorStack.pop());
    }

    //계산
    public String calculator(String num1, String num2, String op){
        double first = Double.parseDouble(num1);
        double second = Double.parseDouble(num2);
        double result = 0.0;

        switch (op){
            case "÷":
                result = first / second;
                break;
            case "×":
                result = first * second;
                break;
            case "-":
                result = first - second;
                break;
            case "+":
                result = first + second;
                break;
        }
        return String.valueOf(result);
    }

    //최종 결과
    public void result(){
        int i=0;
        infixToPostfix();
        while(postfixList.size() != 1){ //postfixList가 하나 남을 때까지 반복
            if(!isNumber(postfixList.get(i))){ //remove해서 지워지므로 i-2번째가 반복
                postfixList.add(i-2, calculator(postfixList.remove(i-2),postfixList.remove(i-2),postfixList.remove(i-2))); //계산
                i=-1;
            }
            i++;
        }
        id = getSharedPreferences("id",MODE_PRIVATE);
        String iso = id.getString("iso","");
        Currency currency = Currency.getInstance(iso);

        budget_num = postfixList.remove(0); //계산된 결과값 => 숫자만으로 따로 저장

        DecimalFormat decimalFormat = new DecimalFormat("###,###.##"); //format 설정
        double budget_result = Double.parseDouble(budget_num);
        all_budget.setText(currency.getSymbol()+" "+ decimalFormat.format(budget_result)); //결과값 set

        //환율로 변화된 값을 setText
        exchange = id.getString("currency",""); //다른 나라 환율
        exchange_kr = id.getString("currency_kr",""); //우리 나라 환율
        if(exchange.equals("1")){ //유로이면,
            double result = Double.parseDouble(exchange_kr) * Double.parseDouble(budget_num);
            NumberFormat numberFormat = NumberFormat.getCurrencyInstance();
            int r_result = (int)Math.round(result);
            exchange_budget.setText(numberFormat.format(r_result)); //결과 값 밑에 환율에 따라 변환 된 값 set
        }
        else if(iso.equals("KRW")){ //원화일 때,
            exchange_budget.setText(""); //=>표시 x
        }
        else{
            double result = ((1 / Double.parseDouble(exchange))*Double.parseDouble(exchange_kr)) * Double.parseDouble(budget_num);
            NumberFormat numberFormat = NumberFormat.getCurrencyInstance();
            int r_result = (int)Math.round(result);
            exchange_budget.setText(numberFormat.format(r_result)); //결과 값 밑에 환율에 따라 변환 된 값 set
        }

        infixList.clear(); //계산식을 비워줌

    }
}
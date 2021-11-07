package com.example.travelrecord;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.app.Dialog;
import android.app.appsearch.PackageIdentifier;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.TedPermission;
import com.lakue.lakuepopupactivity.PopupActivity;
import com.lakue.lakuepopupactivity.PopupGravity;
import com.lakue.lakuepopupactivity.PopupResult;
import com.lakue.lakuepopupactivity.PopupType;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Currency;
import java.util.Date;
import java.util.Stack;

public class PriceAdd extends AppCompatActivity {
    private ImageView iv_country_img; //각 나라의 국기
    private TextView tv_budget_country_name; //각 나라의 이름
    private TextView tv_cal; //계산식이 나타나는 뷰
    private Button btn_cancel; //취소
    private Button btn_save; //저장
    private Button btn[] = new Button[10]; //계산기 숫자(오름차순) + 소수점
    private Button btn_opt[] = new Button[4]; //연산자(/,*,-,+)
    private Button btn_delete; //삭제
    private Button btn_point; //소수점
    private Stack<String> operatorStack; //연산자를 위한 스택
    private ArrayList<String> infixList; //중위 표기
    private ArrayList<String> postfixList;  //후위 표기
    private ArrayList<Integer> checkList; //중복제거
    private String budget_num,exchange, exchange_kr;
    private String init_num;

    private SharedPreferences id; //countryList에서 부터온 정보
    private String country_name, imgUri, documentId,iso; //기본 UI setting => 나라 이름, 국기 이미지, DB경로를 위한 id, 해당국가 통화코드
    private ArrayList<String> dates = new ArrayList<>(); //날짜 설정을 위한 리스트

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore db;
    private ImageView price_add_item; //항목 선택 이미지 뷰
    private ImageView price_add_album; //앨범 선택 이미지 뷰
    private ImageView price_add_memo;  //메모 입력 이미지 뷰


    //DB에 담을 정보
    private SharedPreferences price; //priceList에서 저장할 정보
    private String icon_category; //식비나 교통같은 카테고리의 아이콘 담는 변수
    private String category;      //각 카테고리의 이름
    private String memo;          //간단한 메모
    private String picture = null;       //사진
    private String day;              //여행 일자를 기록
    private String item;          //지출한 금액의 항목 => like 외식, 캐리어...
    private String spend;         //지출 금액
    private String time;
    private TextView all_budget; //환전 전 입력 금액
    private TextView exchange_budget; //환전된 금액
    private TextView current_time; //현재 시각 => Price collection의 documentId

    //사진 추가를 위한 변수
    private Uri imgCUri, phohoURI, albumURI; //사진 경로
    private String mCurrentPhotoPath;
    private static final int FROM_CAMERA = 0;  //카메라 사진 찍기
    private static final int FROM_ALBUM = 1;   //앨범에서 선택
    private static final int SHOW_PICTURE = 3;  //편집시, 사진이 있을 때, 보여주기
    private boolean delete_check = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_price);

        //view 초기화
        initView();

        //계산기->예산 입력 기능
        //초기화
        cal_init();
        //버튼 클릭 이벤트
        cal_initListener();
    }

    private void initView() {
        //add할 지출금액의 날짜를 정하는 메뉴 설정 <= Dialog
        current_time = findViewById(R.id.price_add_current_time);
        current_time.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //입력할 날짜를 선택
                AlertDialog.Builder ad = new AlertDialog.Builder(PriceAdd.this);
                ad.setTitle(" 날짜를 선택하세요");
                ad.setIcon(R.drawable.select_date);
                final String[] dateArray = new String[dates.size()+1];
                dateArray[0] = "Current Time"; //현재 입력 날짜를 기본으로 설정
                for(int j=1; j<dateArray.length; j++){
                    dateArray[j] = dates.get(j-1); //리스트에 default 값을 추가함
                }
                ad.setItems(dateArray, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        current_time.setText(dateArray[i]); //클릭 시, 원하는 날짜 선택 가능
                    }
                });

                ad.setPositiveButton("닫기",null);
                ad.show();
            }
        });

        //국기와 나라이름 받아옴
        iv_country_img = findViewById(R.id.iv_country_img);
        tv_budget_country_name = findViewById(R.id.tv_budget_country_name);
        tv_cal = findViewById(R.id.tv_cal);
        all_budget = findViewById(R.id.all_budget);
        exchange_budget = findViewById(R.id.exchange_budget);
        price_add_item = findViewById(R.id.price_add_item);
        price_add_album = findViewById(R.id.price_add_album);
        price_add_memo = findViewById(R.id.price_add_memo);

        //국기와 나라이름,날짜들, unique document ID 받아옴
        id = getSharedPreferences("id",MODE_PRIVATE);
        country_name = id.getString("countryName",""); //나라 이름
        imgUri = id.getString("imgUri","");  //국기 사진
        documentId = id.getString("documentId",""); //나라 document ID
        iso = id.getString("iso","");  //국가 통화 코드
        String allDates = id.getString("dates",""); //dates리스트를 "," 구분으로 저장되어 있었음

        //,으로 구분된 String을 dates라는 ArrayList에 저장
        String[] list = allDates.split(",");
        for(int i=0; i<list.length; i++){
            dates.add(list[i]); //날짜 설정을 위한 데이터
        }

        //국기 이미지 Glide
        Glide.with(this)
                .load(imgUri).override(70,50)
                .into(iv_country_img);

        //나라 이름 set
        tv_budget_country_name.setText(country_name);


        //각 카테고리 이미지 버튼 set
        //각각의 카테고리 ImageView를 클릭했을 때, category변수에 저장
        findViewById(R.id.food).setOnClickListener(categoryClick);
        findViewById(R.id.transport).setOnClickListener(categoryClick);
        findViewById(R.id.sightseeing).setOnClickListener(categoryClick);
        findViewById(R.id.stay).setOnClickListener(categoryClick);
        findViewById(R.id.shopping).setOnClickListener(categoryClick);
        findViewById(R.id.etc).setOnClickListener(categoryClick);


        //<--- 변경 및 입력에 필요한 변수 초기화 --->
        //앞서 간단히 저장해 놓았던 데이터
        price = getSharedPreferences("price",MODE_PRIVATE);
        spend = price.getString("spend",null);
        item = price.getString("item",null);
        memo = price.getString("memo",null);
        picture = price.getString("picture",null);
        category = price.getString("category",null);
        icon_category = price.getString("icon",null);
        time = price.getString("currentTime",null);

        if(time != null){
            current_time.setText(time);
        }


        Currency currency = Currency.getInstance(iso); //해당 국가 통화 기호 설정을 이해 currency 객체 생성
        DecimalFormat decimalFormat = new DecimalFormat("###,###.##");
        if(spend != null) { //편집 시, 해당하는 지출금액을 setText
            budget_num = spend;
            double budget_result = Double.parseDouble(spend);
            all_budget.setText(currency.getSymbol()+" "+ decimalFormat.format(budget_result));
        }
        else{ //처음 지출 금액 입력 시
            all_budget.setText(currency.getSymbol()+" 0.0");
            init_num = all_budget.getText().toString();
        }

        //항목 입력 => 필수
        price_add_item.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                itemDialog();
            }
        });


        //앨범 선택
        price_add_album.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(picture != null){
                    Intent intent = new Intent(PriceAdd.this, showPicture.class);
                    startActivityForResult(intent,SHOW_PICTURE);
                }
                else{
                    //카메라 권한 획득 => TedPermission 라이브러리
                    PermissionListener permissionListener = new PermissionListener() {
                        @Override
                        public void onPermissionGranted() {
                            Toast.makeText(PriceAdd.this, "Permission Granted", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onPermissionDenied(ArrayList<String> deniedPermissions) {
                            Toast.makeText(PriceAdd.this, "Permission Denied\n"+deniedPermissions.toString(), Toast.LENGTH_SHORT).show();
                        }
                    };

                    TedPermission.with(getApplicationContext())
                            .setPermissionListener(permissionListener)
                            .setDeniedMessage("[설정] > [권한] 에서 권한을 변경하세요")
                            .setPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.CAMERA)
                            .check();
                    cameraDialog();
                }
            }
        });



        //메모 입력
        price_add_memo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                memoDialog();
            }
        });

        //취소
        btn_cancel = findViewById(R.id.btn_cancel);
        btn_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        //저장 -> 클릭 시, db에 저장
        btn_save = findViewById(R.id.btn_save);
        btn_save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(price.getString("documentTime",null) != null) {
                    updateDB(); //편집 시, db에 반영 (update)
                }
                else {
                    saveInDB(); //처음 금액 추가 시, db에 반영
                }

            }
        });
    }

    //icon_category 각각의 버튼을 클릭 했을 때, 동작 => 카테고리(아이콘) 선택
    private View.OnClickListener categoryClick = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            switch (view.getId()){
                case R.id.food: { //식비
                    icon_category = "eat";
                    category = "식비";
                    view.setClickable(true);
                    break;
                }
                case R.id.transport:{ //교통
                    icon_category = "transport";
                    category = "교통";
                    view.setClickable(true);
                    break;
                }
                case R.id.sightseeing:{ //관광
                    icon_category = "sightseeing";
                    category = "관광";
                    view.setClickable(true);
                    break;
                }
                case R.id.stay:{  //숙박
                    icon_category = "stay";
                    category = "숙박";
                    view.setClickable(true);
                    break;
                }
                case R.id.shopping:{  //쇼핑
                    icon_category = "shopping";
                    category = "쇼핑";
                    view.setClickable(true);
                    break;
                }
                case R.id.etc:{  //기타
                    icon_category = "etc";
                    category = "기타";
                    view.setClickable(true);
                    break;
                }
            }
        }
    };

    //항목명 입력 다이얼로그 => ex) 버거킹, 오렌지 립밤....
    private void itemDialog() {
        AlertDialog.Builder ad = new AlertDialog.Builder(PriceAdd.this);
        ad.setTitle("항목");
        ad.setMessage("항목을 입력해 주세요");

        //다이어로그 안에다 editText 추가
        EditText et = new EditText(PriceAdd.this);
        //다이어로그 안의
        //EditText의 여백과 크기 조정을 위해
        //FrameLayout 설정
        FrameLayout frame = new FrameLayout(PriceAdd.this);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = getResources().getDimensionPixelSize(R.dimen.dialog_margin); //여백 설정
        params.rightMargin = getResources().getDimensionPixelSize(R.dimen.dialog_margin);
        et.setLayoutParams(params);
        //글자 수 제한
        et.setFilters(new InputFilter[]{new InputFilter.LengthFilter(8)});
        if(item != null){  //편집 시, 처음 입력 한 항목명 set
            et.setText(item);
        }
        frame.addView(et);
        ad.setView(frame);

        //확인 버튼
        ad.setPositiveButton("확인", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String result = et.getText().toString();
                //버튼에 내용 set
                item = result;
                dialogInterface.dismiss(); //모든 작업이 끝난 후 다이어로그 창 종료
            }
        });

        //취소 버튼
        ad.setNegativeButton("취소", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                item="";
                dialogInterface.dismiss();
            }
        });
        ad.show();
    }

    //카메라와 앨범 다이얼로그
    private void cameraDialog() {
        AlertDialog.Builder ad = new AlertDialog.Builder(PriceAdd.this);
        ad.setTitle("사진 업로드").setIcon(R.drawable.check).setCancelable(false);
        //사진 촬영 클릭
        ad.setPositiveButton("사진촬영", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Log.v("알림","다이얼로그 > 사진촬영 선택");
                //사진 촬영 클릭
                takePhotho();
            }
        });


        //앨범선택 클릭
        ad.setNeutralButton("앨범선택", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Log.v("알림","다이얼로그 > 앨범선택 선택");
                //앨범에서 선택
                selectAlbum();
            }
        });

        //취소 클릭
        ad.setNegativeButton("취소          ", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Log.v("알림","다이얼로그 > 취소 선택");
                //취소 클릭 && 다이얼로그 닫기
                dialogInterface.cancel();
            }
        });
        AlertDialog alertDialog = ad.create();
        alertDialog.show();;
    }

    //사진 찍기 클릭 메소드
    private void takePhotho() {
        //촬영 후 이미지 가져옴
        String state = Environment.getExternalStorageState();

        if(Environment.MEDIA_MOUNTED.equals(state)){ //외부 저장소 사용 가능 확인
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if(intent.resolveActivity(getPackageManager())!=null) { //intent를 처리 할 카메라 액티비티가 있는지 확인
                File photoFile = null; //촬영한 사진을 저장할 파일 생성
                try {
                    photoFile = createImageFile(); //이미지 파일 create후, 파일 저장
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (photoFile != null) { //파일 생성 완료 시,
                    Log.v("알림 phothoFile", photoFile.toString());
                    Uri providerURI = FileProvider.getUriForFile(getApplicationContext(), getPackageName(), photoFile); //파일 uri 가져오기
                    imgCUri = providerURI; //촬영한 사진 파일 uri => imgCUri
                    Log.v("알림 imgUri", imgCUri.toString());
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, providerURI); //intent에  uri 담기
                    if(intent.resolveActivity(getPackageManager())!=null){
                        startActivityForResult(intent, FROM_CAMERA); //intent 실행
                    }

                }
            }
        }
        else{
            Log.v("알림", "저장공간에 접근 불가능");
            return;
        }
    }

    //이미지 파일 만드는 함수
    private File createImageFile() throws  IOException{
        String imgFileName = System.currentTimeMillis()+".jpg"; //현재 시각을 기준으로 파일 이름 생성
        File imageFile = null;
        File storageDir = new File(Environment.getExternalStorageDirectory()+"/Pictures","ireh"); //파일 생성할 경로

        if (!storageDir.exists()){ //만약에 경로가 존재 하지 않는 다면,
            Log.v("알림", "storageDir 존재 x"+ storageDir.toString());
            storageDir.mkdirs(); //새롭게 지정된 디렉토리 생성
        }
        else{
            Log.v("알림","storageDir 존재함" + storageDir.toString());
        }
        imageFile = new File(storageDir,imgFileName); //생성된 파일 이름에 지정된 경로에 이미지 파일 생성
        mCurrentPhotoPath = imageFile.getAbsolutePath(); //이미지 파일의 경로 저장
        Log.v("알림 imgFile",imageFile.toString());

        return imageFile; //이미지 파일 반환
    }

    //앨범 선택 클릭 메소드
    private void selectAlbum() {
        //앨범에서 이미지 가져옴

        //앨범 열기
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType(MediaStore.Images.Media.CONTENT_TYPE);

        intent.setType("image/*"); //모든 이미지 타입

        startActivityForResult(intent,FROM_ALBUM); //intent 실행
    }

    //해당 이미지 로컬 폴더에 저장
    public void galleryAddPic(){
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        //해당 경로에 있는 파일을 객체화 => 새로 파일을 만듦
        File f = new File(mCurrentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        sendBroadcast(mediaScanIntent);
        Toast.makeText(this, "사진이 저장되었습니다.", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode != RESULT_OK){
            return;
        }

        switch (requestCode){
            case FROM_ALBUM: //앨범에서 가져오기
            {
                if(data.getData()!=null){
                    File albumFile = null;
                    try {
                        albumFile = createImageFile();
                        phohoURI = data.getData(); //앨범에서 선택한 이미지 uri 저장
                        albumURI = Uri.fromFile(albumFile);
                        galleryAddPic(); //해당 이미지를 로컬 폴더에 저장
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    picture = String.valueOf(phohoURI); //나중에 수정을 위해 따로 변수에 string형으로 이미지 uri 저장


                    Log.i("알림 photoURI", String.valueOf(phohoURI));
                }
                break;
            }
            case FROM_CAMERA: //카메라 촬영
            {
                Log.v("알림","FROM_CAMERA 처리");
                galleryAddPic(); //해당 이미지를 로컬 폴더에 저장


                picture = String.valueOf(imgCUri);

                Log.i("알림 imgUri", String.valueOf(imgCUri));
                break;
            }
            case SHOW_PICTURE: //사진 미리 보기를 눌렀을 때의 결과값
            {
                delete_check = true;
            }
        }
    }

    //메모 입력 다이얼로그 => 사진과 함께 길게 입력할 수 있는 메모장
    private void memoDialog() {
        AlertDialog.Builder ad = new AlertDialog.Builder(PriceAdd.this);
        ad.setTitle("Memo");
        ad.setMessage("메모를 입력해 주세요");

        //다이어로그 안에다 editText 추가
        EditText et = new EditText(PriceAdd.this);
        //다이어로그 안의
        //EditText의 여백과 크기 조정을 위해
        //FrameLayout 설정
        FrameLayout frame = new FrameLayout(PriceAdd.this);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = getResources().getDimensionPixelSize(R.dimen.dialog_margin); 
        params.rightMargin = getResources().getDimensionPixelSize(R.dimen.dialog_margin);  
        params.height = getResources().getDimensionPixelSize(R.dimen.dialog_height); 
        et.setLayoutParams(params);
        if(memo != null){ //편집 시, 메모가 입력되어 있으면,
            et.setText(memo);  //메모 setText
        }
        frame.addView(et);
        ad.setView(frame);

        //확인 버튼
        ad.setPositiveButton("확인", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String result = et.getText().toString();
                //버튼에 내용 set
                memo = result;
                dialogInterface.dismiss(); //모든 작업이 끝난 후 다이어로그 창 종료
            }
        });

        //취소 버튼
        ad.setNegativeButton("취소", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                memo = null;
                dialogInterface.dismiss();
            }
        });
        ad.show();
    }

    //저장 눌렀을 때 db에 저장하는 메소드
    private void saveInDB() {
        //초기화
        firebaseAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        //db에 데이터 반영
        //firestore 경로 -> collection-document-collection-document
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        DocumentReference df = db.collection("users").document(firebaseUser.getUid());
        DocumentReference sf = df.collection("countries").document(documentId); //db에서 해당 나라 경로


        //해당 나라의 통화 기호
        id = getSharedPreferences("id",MODE_PRIVATE);
        String iso = id.getString("iso",""); //통화 코드
        Currency currency = Currency.getInstance(iso);


        //고유의 document id는 getTime으로 생성
        //각 document의 field는 category, icon, item, spend, exchange, memo, picture, day
        //PriceData 객체 사용
        //현재 시간 알아내기
        SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd");

        long now = System.currentTimeMillis(); //현재 시간
        Date date = new Date(now);
        String documentTime = sdf1.format(date); //for unique document id

        //day 설정
        Date standardDate = null; //기준 date
        Date compareDate = null;  //비교 date


        try {
            standardDate = sdf2.parse(dates.get(0)); //기준 날짜(첫째날)
            if(current_time.getText().toString().equals("Current Time")) { //currentTime이면, => 문자열이므로, 변환 필요
                compareDate = sdf2.parse(documentTime.substring(0, 10));  //입력시, 시간을 비교 date에 저장
            }
            else {
                compareDate = sdf2.parse(current_time.getText().toString());
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        //두 날짜 간의 차이 일 수
        long diffDays = (compareDate.getTime()-standardDate.getTime())/1000/(24*60*60);
        if(diffDays <0 ){
            day = "R"; //여행 날짜 시작 전에 입력 했으면, 준비 비용으로 처리
        }
        else{
            day = "Day"+((int) diffDays + 1);
        }

        //사진 storage에 업로드
        if(picture != null){
            FirebaseStorage storage = FirebaseStorage.getInstance(); //storage instance를 만들고,
            StorageReference storageRef = storage.getReference();  //storage 참조
            String cu = firebaseUser.getUid();  //사용자 기준
            String filename = cu+"_"+System.currentTimeMillis();  //파일 이름 생성
            Uri file = Uri.parse(picture); //이미지 uri

            //업로드 할 파일 경로 설정
            StorageReference riversRef = storageRef.child("travelPicture/"+filename);
            //업로드 객체 생성
            UploadTask uploadTask = riversRef.putFile(file);

            uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    Toast.makeText(getApplicationContext(), "이미지 업로드 완료", Toast.LENGTH_SHORT).show();
                }
            });
            picture = filename; //업로드한 사진 이름 picture에 저장 <- 카메라 처리에서 picture는 갤러리나 촬영에서 선택한 이미지의 uri 였음.
        }


        //DB에 저장하기위해 데이터를 PriceData객체를 이용하여 set 후, db에 저장
        PriceData insertData = new PriceData();
        if(current_time.getText().toString().equals("Current Time")) { //데이터가 Current Time이면 현재 날짜로 바꿔서 데이터 Set
            insertData.setPrice_add_time(documentTime.substring(0, 10));
        }
        else {
            insertData.setPrice_add_time(current_time.getText().toString());
        }
        insertData.setPrice_category(category); //카테고리
        insertData.setPrice_icon(icon_category); //카테고리 아이콘
        insertData.setPrice_spend(budget_num);   //지출 금액
        insertData.setCurrency_sign(currency.getSymbol()); //통화 기호
        insertData.setPrice_memo(memo);  //메모
        insertData.setPrice_item(item);  //카테고리 항목명
        insertData.setPrice_picture(picture);  //사진
        insertData.setPrice_day(day);   //날짜 ex)Day1
        insertData.setPrice_current_time(documentTime); //현재시간 -> unique document ID

        //카테고리 icon, 항목명, day, 지출금액은 필수 입력 항목
        if(icon_category!=null && all_budget.getText()!=init_num && item!=null){
            //정해진 경로에 위에 set한 데이터 저장
            //경로 -> collection-document-collection-document-collection-document (데이터 구조 완료)
            sf.collection("Price").document(documentTime).set(insertData);
            Intent intent = new Intent(PriceAdd.this,PriceList.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }
        else{
            Toast.makeText(getApplicationContext(), "항목명, 아이콘, 금액을 모두 입력하세요", Toast.LENGTH_SHORT).show();
        }
    }

    //수정시, DB 갱신
    public void updateDB(){
        //초기화
        firebaseAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        //db에 데이터 반영
        //firestore 경로 -> collection-document-collection-document
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        DocumentReference df = db.collection("users").document(firebaseUser.getUid());
        DocumentReference sf = df.collection("countries").document(documentId);

        price = getSharedPreferences("price",MODE_PRIVATE);
        String documentTime = price.getString("documentTime",null); //for unique document id


        //고유의 document id는 getTime으로 생성
        //각 document의 field는 category, icon, item, spend, exchange, memo, picture, day
        //PriceData 객체 사용
        //현재 시간 알아내기
        SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd");

        //day 설정
        Date standardDate = null; //기준 date
        Date compareDate = null;  //비교 date


        try {
            standardDate = sdf2.parse(dates.get(0));
            if(current_time.getText().toString().equals("Current Time")) { //currentTime이면, => 문자열이므로, 변환 필요
                compareDate = sdf2.parse(documentTime.substring(0, 10));  //입력시, 시간을 비교 date에 저장
            }
            else {
                compareDate = sdf2.parse(current_time.getText().toString());
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        //두 날짜 간의 차이 일 수
        long diffDays = (compareDate.getTime()-standardDate.getTime())/1000/(24*60*60);
        if(diffDays <0 ){
            day = "R"; //여행 날짜 시작 전에 입력 했으면, 준비 비용으로 처리
        }
        else{
            day = "Day"+((int) diffDays + 1);
        }


        if(budget_num != spend){ //입력값 변경시
            spend = budget_num;
        }

        //사진 삭제 시,
        if(delete_check == true){
            //storage 참조
            FirebaseStorage storage = FirebaseStorage.getInstance();
            StorageReference storageRef = storage.getReference();
            StorageReference desertRef = storageRef.child("travelPicture/"+picture);
            desertRef.delete(); //storage에서 삭제
            picture = null;
        }


        //카테고리 icon, 항목명, day, 지출금액은 필수 입력 항목
        if(icon_category!=null && spend!=null && item!=null){
            sf.collection("Price").document(documentTime).update("price_spend",spend); //지출 금액
            sf.collection("Price").document(documentTime).update("price_category",category); //카테고리
            sf.collection("Price").document(documentTime).update("price_icon",icon_category); //카테고리 아이콘
            sf.collection("Price").document(documentTime).update("price_memo",memo);  //메모
            sf.collection("Price").document(documentTime).update("price_picture",picture);  //사진
            sf.collection("Price").document(documentTime).update("price_item",item);  //항목명
            if(current_time.getText().toString().equals("Current Time")) {
                sf.collection("Price").document(documentTime).update("price_add_time",documentTime.substring(0, 10));  //날짜
            }
            else {
                sf.collection("Price").document(documentTime).update("price_add_time",current_time.getText().toString());  //날짜
            }

            sf.collection("Price").document(documentTime).update("price_day",day);  //날짜
            
            Intent intent = new Intent(PriceAdd.this, PriceList.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }
        else{
            Toast.makeText(getApplicationContext(), "항목명, 아이콘, 금액을 모두 입력하세요", Toast.LENGTH_SHORT).show();
        }
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


        operatorStack = new Stack<>(); //연산자 stack
        infixList = new ArrayList<>();  //중위 표기의 계산식 리스트
        postfixList = new ArrayList<>();  //후위 표기 계산식 리스트
        checkList = new ArrayList<>();   //중복 방지 or 제한을 위한 체크 리스트

    }


    private void cal_initListener() {
        //숫자
        for(Button i : btn){
            i.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    checkList.add(1);  //숫자 입력 시, 1 추가
                    tv_cal.append(i.getText().toString());

                    //결과
                    result(); //결과 값을 숫자 입력 할 때마다 반환

                }
            });
        }

        //연산자
        for(Button i: btn_opt){
            i.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(checkList.isEmpty()){ //연산자가 첫번째에 오지 못하게 제한
                        Log.i("no","첫번째 X");
                    }
                    else if(checkList.get(checkList.size()-1)==0 || checkList.get(checkList.size()-1)==2){ //연산자가 중복이나 소수점 다음에 오는 것 제한
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
                if(checkList.isEmpty()){ //소수점이 첫번째에 오지 못하게 제한
                    Log.i("no","첫번째 x");
                }
                else if(checkList.get(checkList.size()-1)==0 || checkList.get(checkList.size()-1)==2){  //소수점이 연산자 다음에 오거나 소수점 중복 제한
                    Log.i("no","중복 or 소수점 x");
                }
                else if(pointCheck(checkList)){  //한 숫자에 여러 소수점 오는 것 제한
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
                        li.add(li.remove(li.size()-1)+" ");  //마지막 연산자는 " "가 존재 => 한번 더 remove 후, " " 추가
                    tv_cal.setText(TextUtils.join(" ",li));
                }
            }
        });
    }

    //한 숫자에 소수점 여러개 오는거 막는 메소드
    public boolean pointCheck(ArrayList<Integer> checkList){
        for(int i=checkList.size()-2; i>=0 ;i--){ //앞서, 마지막 자리까지 확인 완료 => 마지막에서 앞 순서 부터 체크
            int check = checkList.get(i);
            if(check == 2)  //소수점이면 
                return true;
            if(check == 0) //연산자 일때,
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
            if(isNumber(item)) //숫자가 오면
                postfixList.add(String.valueOf(Double.parseDouble(item)));  //postfixList에 추가
            else{
                if(operatorStack.isEmpty()) //스택이 비었으면
                    operatorStack.push(item); //추가
                else{
                    if(getWeight(operatorStack.peek()) >= getWeight(item)) //연산자 우선순위 비교
                        postfixList.add(operatorStack.pop()); //최근 추가된 value를 add
                    operatorStack.push(item);
                }
            }
        }
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
        while(postfixList.size() != 1){  //postfixList가 하나 남을 때까지 반복
            if(!isNumber(postfixList.get(i))){ //remove해서 지워지므로 i-2번째가 반복
                postfixList.add(i-2, calculator(postfixList.remove(i-2),postfixList.remove(i-2),postfixList.remove(i-2)));
                i=-1;
            }
            i++;
        }
        //for Currency Symbol
        id = getSharedPreferences("id",MODE_PRIVATE);
        String iso = id.getString("iso","");
        Currency currency = Currency.getInstance(iso);

        budget_num = postfixList.remove(0); //계산된 결과값

        DecimalFormat decimalFormat = new DecimalFormat("###,###.##");
        double budget_result = Double.parseDouble(budget_num);
        all_budget.setText(currency.getSymbol()+" "+ decimalFormat.format(budget_result));

        //환율로 변화된 값을 setText
        exchange = id.getString("currency","");  //다른 나라 환율
        exchange_kr = id.getString("currency_kr","");  //우리나라 환율
        NumberFormat numberFormat = NumberFormat.getCurrencyInstance();
        if(exchange.equals("1")){  //유로 일 때,
            double result = Double.parseDouble(exchange_kr) * Double.parseDouble(budget_num);
            int r_result = (int)Math.round(result);
            exchange_budget.setText(numberFormat.format(r_result));
        }
        else if(iso.equals("KRW")){  //원화 일 때,
            exchange_budget.setText(""); // 따로 표시 안함
        }
        else{
            double result = ((1 / Double.parseDouble(exchange))*Double.parseDouble(exchange_kr)) * Double.parseDouble(budget_num);
            int r_result = (int)Math.round(result);
            exchange_budget.setText(numberFormat.format(r_result));
        }

        infixList.clear();

    }
}
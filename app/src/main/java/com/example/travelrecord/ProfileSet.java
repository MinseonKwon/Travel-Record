package com.example.travelrecord;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.TedPermission;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Currency;
import java.util.Date;
import java.util.EventListener;


public class ProfileSet extends AppCompatActivity {
    private ImageView iv_cover; //커버 사진
    private Button btn_picture; //커버 사진 변경 버튼
    private ImageButton ib_cancel; //취소버튼
    private ImageButton ib_save;  //저장버튼
    private TextView tv_memo;  //메모 
    private ImageView iv_country; //국기 사진
    private TextView tv_country_name; //나라 이름
    private Button btn_start_date; //시작날짜 입력
    private Button btn_finish_date; //종료날짜 입력
    private Button btn_budget; //예산 설정 버튼

    //for save시, DB에 update
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore db;

    //사진 커버 변경 변수
    private Uri imgCUri, phohoURI, albumURI; //카메라 촬영 파일 uri, 갤러리 파일 uri
    private String mCurrentPhotoPath; //경로
    private static final int FROM_CAMERA = 0;
    private static final int FROM_ALBUM = 1;
    private static final int FROM_BUDGET = 2;
    private String filename="";

    //기본 ui를 위한 변수
    private SharedPreferences id; //처음 ui load시 필요한 정보가 있음
    private String countryName,imgUri,memo,budget,ISO,background,documentId; //나라이름, 국기,메모,예산,국가통화코드,커버사진,document ID
    private String startDate=""; //시작날짜
    private String finishDate=""; //종료날짜


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //상태바 제거
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.set_profile);

        //각각의 위젯들 초기화
        initView();

    }

    private void initView() {
        iv_cover = findViewById(R.id.iv_cover);
        btn_picture = findViewById(R.id.btn_picture);
        ib_cancel = findViewById(R.id.ib_cancel);
        ib_save = findViewById(R.id.ib_save);
        tv_memo = findViewById(R.id.tv_memo);
        iv_country = findViewById(R.id.iv_country);
        tv_country_name = findViewById(R.id.tv_country_name);
        btn_start_date = findViewById(R.id.btn_start_date);
        btn_finish_date = findViewById(R.id.btn_finish_date);
        btn_budget = findViewById(R.id.btn_budget);


        //앞 activity에서 필요한 값을 받음
        id = getSharedPreferences("id",MODE_PRIVATE);
        countryName = id.getString("countryName","");
        imgUri = id.getString("imgUri","");
        memo = id.getString("memo","");
        budget = id.getString("budget","");
        ISO = id.getString("iso","");
        background = id.getString("background","");
        documentId = id.getString("documentId","");
        startDate = id.getString("startDate","");
        finishDate = id.getString("finishDate","");


        //Firebase Storage 참조
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();
        StorageReference pathRef = storageRef.child("coverList/"+background); //파일 경로

        //커버사진 Glide
        String str = background.substring(0,7);
        if(!str.equals("country")){ //커버사진을 profileSet에서 변경했을 경우 -> 기본 커버사진은 모두 country로 시작
            Glide.with(this)
                    .load(pathRef)
                    .into(iv_cover);
        }else{ //기본 커버사진 로드
            int iResId = getResources().getIdentifier(background,"drawable", getPackageName());
            Glide.with(getApplicationContext())
                    .load(iResId).override(1200,800)
                    .into(iv_cover);
        }


        //memo set
        if(memo != "")
            tv_memo.setText(memo);

        //국기 사진 glide
        Glide.with(getApplicationContext())
                .load(imgUri).override(70,50)
                .into(iv_country);

        //나라 이름 set
        tv_country_name.setText(countryName);

        //예산 설정
        Currency currency = Currency.getInstance(ISO);
        if(budget != ""){ //프로필 수정 시, 예산 초기 상태
            btn_budget.setText(currency.getSymbol() + " "+budget);
        }


        //취소 버튼
        ib_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });


        //저장 버튼
        ib_save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //DB에 저장할 데이터를 위한 임시 변수들을 선언
                id = getSharedPreferences("id",MODE_PRIVATE);
                memo = tv_memo.getText().toString();
                //for 커버사진 변경(if)  =>  다시 background를 부름
                background = id.getString("background","");
                //BudgetSet에서 저장한 예산을 불러옴
                budget = id.getString("budget","");


                //초기화 (for DB)
                firebaseAuth = FirebaseAuth.getInstance();
                FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                db = FirebaseFirestore.getInstance();

                String str = background.substring(0,7);
                //사진 storage에 업로드
                if(!str.equals("country")){ //커버사진을 profileSet에서 변경했을 경우
                    if(filename != ""){
                        FirebaseStorage storage = FirebaseStorage.getInstance(); //storage instance를 만들고,
                        StorageReference storageRef = storage.getReference();  //storage 참조
                        String cu = firebaseUser.getUid();  //사용자 기준
                        String name = cu+filename;
                        Uri file = Uri.parse(background); //이미지 uri

                        //업로드 할 파일 경로 설정
                        StorageReference riversRef = storageRef.child("coverList/"+name);
                        //업로드 객체 생성
                        UploadTask uploadTask = riversRef.putFile(file);

                        uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                Toast.makeText(getApplicationContext(), "이미지 업로드 완료", Toast.LENGTH_SHORT).show();
                            }
                        });
                        background = name;
                    }
                }

                //시작 날짜와 끝날짜, 메모, {배경(커버)화면->이미지} uri, 예산 필드 저장(업데이트)
                DocumentReference df = db.collection("users").document(firebaseUser.getUid());
                df.collection("countries").document(documentId)
                        .update("startDate",startDate);
                df.collection("countries").document(documentId)
                        .update("finishDate",finishDate);
                df.collection("countries").document(documentId)
                        .update("memo",memo);
                df.collection("countries").document(documentId)
                        .update("background",background);
                df.collection("countries").document(documentId)
                        .update("budget",budget);

                //시작날짜, 종료날짜, 커버사진, 예산은 필수 항목
                if(startDate!="" && finishDate!="" && background!="" && budget!=""){
                    Intent intent = new Intent(ProfileSet.this, PriceList.class);
                    SharedPreferences.Editor editor = id.edit();
                    editor.putString("startDate",startDate);
                    editor.putString("finishDate",finishDate);
                    editor.putString("memo",memo);
                    editor.putString("background",background);
                    editor.putString("budget",budget);
                    editor.commit();
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                }
                else {
                    Toast.makeText(getApplicationContext(), "모두 입력해 주세요", Toast.LENGTH_SHORT).show();
                }
            }
        });


        //커버 사진 변경
        //사진 변경 버튼 클릭 시, 앨범선택, 사진촬영, 취소 다이얼로그 생성
        btn_picture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //카메라 권한 획득 => TedPermission 라이브러리
                PermissionListener permissionListener = new PermissionListener() {
                    @Override
                    public void onPermissionGranted() {
                        Toast.makeText(ProfileSet.this, "Permission Granted", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onPermissionDenied(ArrayList<String> deniedPermissions) {
                        Toast.makeText(ProfileSet.this, "Permission Denied\n"+deniedPermissions.toString(), Toast.LENGTH_SHORT).show();
                    }
                };

                TedPermission.with(getApplicationContext())
                        .setPermissionListener(permissionListener)
                        .setDeniedMessage("[설정] > [권한] 에서 권한을 변경하세요")
                        .setPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.CAMERA)
                        .check();
                makeDialog();
            }
        });

        //메모 입력 이벤트
        tv_memo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                memo();
            }
        });
        tv_memo.setMovementMethod(new ScrollingMovementMethod()); //스크롤

        //시작 날짜 set
        if(startDate != "")
            btn_start_date.setText(startDate);
        //마지막 날짜 set
        if(finishDate != "")
            btn_finish_date.setText(finishDate);


        //여행 시작 날짜 선택
        if(btn_start_date.getText().toString() != startDate){ //프로필 보기에서 날짜 변경 허용 x
            btn_start_date.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    startDatePick();
                }
            });
        }

        //여행 마지막 날짜 선택
        if(btn_finish_date.getText().toString() != finishDate){ //프로필 보기에서 날짜 변경 허용 x
            btn_finish_date.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    finishDatePick();
                }
            });
        }



        //예산 설정 이벤트
        btn_budget.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ProfileSet.this, BudgetSet.class);
                startActivityForResult(intent,FROM_BUDGET);
            }
        });
    }



    //앨범선택, 사진촬영, 취소 다이어로그 생성 메소드
    private void makeDialog() {
        AlertDialog.Builder ad = new AlertDialog.Builder(ProfileSet.this);
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
                        startActivityForResult(intent, FROM_CAMERA);
                    }
                }
            }
            else{
                Log.v("알림", "저장공간에 접근 불가능");
                return;
            }
        }
    }

    //이미지 파일 생성 함수
    private File createImageFile() throws  IOException{
        String imgFileName = System.currentTimeMillis()+".jpg"; //현재 시각을 기준으로 파일 이름 생성
        File imageFile = null;
        File storageDir = new File(Environment.getExternalStorageDirectory()+"/Pictures","ireh"); //파일 생성할 경로

        if (!storageDir.exists()){ //만약에 경로가 존재 하지 않는 다면,
            Log.v("알림", "storageDir 존재 x"+ storageDir.toString());
            storageDir.mkdirs(); //새롭게 지정된 디렉토리 생성
        }
        Log.v("알림","storageDir 존재함" + storageDir.toString()); 
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

        startActivityForResult(intent,FROM_ALBUM);
    }

    //해당 이미지를 로컬 폴더에 저장
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

        id = getSharedPreferences("id",MODE_PRIVATE);
        SharedPreferences.Editor editor = id.edit();

        if(resultCode != RESULT_OK){
            return;
        }

        switch (requestCode){
            case FROM_ALBUM: //앨범에서 가져오기
            {
                if(data.getData()!=null){
                    try {
                        File albumFile = null;
                        albumFile = createImageFile();

                        phohoURI = data.getData(); //앨범에서 선택한 이미지 uri 저장
                        albumURI = Uri.fromFile(albumFile);

                        //해당 이미지를 로컬 폴더에 저장
                        galleryAddPic();

                        //앨범에서 가져온 이미지를 background에 저장
                        //for 여행목록 배경을 바꾸기 위해서
                        editor.remove("background");
                        editor.putString("background", String.valueOf(phohoURI));
                        editor.commit();

                        filename = "_"+System.currentTimeMillis();  //파일 이름 생성 -> 사진을 가져오는 작업이 마무리 될때의 시각

                        Log.i("알림 photoURI", String.valueOf(phohoURI));
                        iv_cover.setImageURI(phohoURI);
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.v("알림","앨범에서 가져오기 에러");
                    }
                }
                break;
            }
            case FROM_CAMERA: //카메라 촬영
            {
                Log.v("알림","FROM_CAMERA 처리");
                //해당 이미지를 로컬 폴더에 저장
                galleryAddPic();

                //카메라로 촬영한 이미지를 background에 저장
                //for 여행목록 배경을 바꾸기 위해서
                editor.remove("background");
                editor.putString("background", String.valueOf(imgCUri));
                editor.commit();

                filename = "_"+System.currentTimeMillis();  //파일 이름 생성 -> 사진을 가져오는 작업이 마무리 될때의 시각

                Log.i("알림 imgUri", String.valueOf(imgCUri));
                iv_cover.setImageURI(imgCUri);
                break;
            }
            case FROM_BUDGET: //BudgetSet
            {
                budget = id.getString("budget","");
                Currency currency = Currency.getInstance(ISO);
                btn_budget.setText(currency.getSymbol() +" "+budget); //예산 설정 후, 설정된 예산 setText
                break;
            }
        }
    }


    //간단한 메모를 위한
    //EditText를 이용한
    //Dialog 팝업창
    private void memo() {
        AlertDialog.Builder ad = new AlertDialog.Builder(ProfileSet.this);
        ad.setTitle("Memo");
        ad.setMessage("메모를 입력해 주세요");

        //다이어로그 안에다 editText 추가
        EditText et = new EditText(ProfileSet.this);
        //다이어로그 안의
        //EditText의 여백과 크기 조정을 위해
        //FrameLayout 설정
        FrameLayout frame = new FrameLayout(ProfileSet.this);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = getResources().getDimensionPixelSize(R.dimen.dialog_margin);
        params.rightMargin = getResources().getDimensionPixelSize(R.dimen.dialog_margin);
        params.height = getResources().getDimensionPixelSize(R.dimen.dialog_height);
        et.setLayoutParams(params);
        frame.addView(et);
        ad.setView(frame);
        String s = tv_memo.getText().toString(); //텍스트뷰에 데이터가 있는 경우
        et.setText(s);                           //메모장에 보이게 하기위한 setter

        //확인 버튼
        ad.setPositiveButton("확인", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String result = et.getText().toString();
                //버튼에 내용 set
                tv_memo.setText(result);
                dialogInterface.dismiss(); //모든 작업이 끝난 후 다이어로그 창 종료
            }
        });

        //취소 버튼
        ad.setNegativeButton("취소", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        ad.show();
    }

    //시작 날짜 선택 메소드
    //calendar형 Dialog
    private void startDatePick() {
        Calendar calendar = Calendar.getInstance();
        int nYear = calendar.get(Calendar.YEAR);
        int nMonth = calendar.get(Calendar.MONTH);
        int nDay = calendar.get(Calendar.DAY_OF_MONTH);

        //캘린더 안의 날짜를 눌러서 확인을 눌렀을 때
        DatePickerDialog.OnDateSetListener mDateSetListener =
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker datePicker, int year, int month, int day) {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd"); //날짜 format 설정
                        calendar.set(year,month,day); //클릭한 날짜 calendar에 set
                        startDate = sdf.format(calendar.getTime()); //db저장을 위해 변수 startDate에 저장
                    }
                };
        mDateSetListener.toString();
        DatePickerDialog dialog = new DatePickerDialog(this, android.R.style.Theme_DeviceDefault_Light_Dialog, mDateSetListener,nYear,nMonth,nDay);

        dialog.show();;
    }

    //마지막 날짜 선택 메소드
    //calendar형 Dialog
    private void finishDatePick() {
        Calendar calendar = Calendar.getInstance();
        int nYear = calendar.get(Calendar.YEAR);
        int nMonth = calendar.get(Calendar.MONTH);
        int nDay = calendar.get(Calendar.DAY_OF_MONTH);

        //캘린더 안의 날짜를 눌러서 확인을 눌렀을 때
        DatePickerDialog.OnDateSetListener mDateSetListener =
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker datePicker, int year, int month, int day) {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd"); //날짜 format 설정
                        calendar.set(year,month,day); //클릭한 날짜 calendar에 set
                        finishDate = sdf.format(calendar.getTime()); //db저장을 위해 변수 finishDate에 저장
                    }
                };

        DatePickerDialog dialog = new DatePickerDialog(this, android.R.style.Theme_DeviceDefault_Light_Dialog, mDateSetListener,nYear,nMonth,nDay);

        dialog.show();;

    }
}
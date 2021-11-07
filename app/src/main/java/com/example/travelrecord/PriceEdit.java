package com.example.travelrecord;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.module.AppGlideModule;
import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.TedPermission;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class PriceEdit extends AppCompatActivity {
    private SharedPreferences price,id;
    private String documentTime, item, picture, memo, documentId; //각각 경로의 위한 id들과 로드 될 카테고리 항목명, 사진, 메모
    private TextView edit_item, edit_memo;  //항목명, 메모
    private Button edit_price, delete_price; //수정, 삭제
    private ImageView edit_picture;  //사진

    private FirebaseAuth firebaseAuth = FirebaseAuth.getInstance(); //파이어베이스 인증 처리
    private FirebaseFirestore db = FirebaseFirestore.getInstance(); //firestore
    private FirebaseStorage storage; //firestorage

    //사진 추가를 위한 변수 -> 지출 금액 입력에서 따로 사진을 추가 못한 경우
    private Uri imgCUri, phohoURI, albumURI;
    private String mCurrentPhotoPath;
    private static final int FROM_CAMERA = 0;
    private static final int FROM_ALBUM = 1;
    private boolean check = false; //사진을 선택 했는지 check
    private boolean m_check = false; //메모를 입력 했는지 check


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit_price);
        //상태바 제거
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        //View 초기화
        initView();
    }

    private void initView() {
        //sharedPreferences 받아오기
        price = getSharedPreferences("price",MODE_PRIVATE);
        documentTime = price.getString("documentTime",""); //document id(3rd)
        item = price.getString("item",""); //항목명
        picture = price.getString("picture",null); //사진
        memo = price.getString("memo",null);  //메모

        id = getSharedPreferences("id",MODE_PRIVATE);
        documentId = id.getString("documentId","");

        //Toolbar setting
        Toolbar toolbar = findViewById(R.id.price_edit_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        //Toolbar title set
        TextView title = toolbar.findViewById(R.id.toolbar_time_title);
        title.setText(documentTime); //입력시간을 toolbar title에 set

        edit_item = findViewById(R.id.edit_item);
        edit_picture = findViewById(R.id.edit_picture);
        edit_memo = findViewById(R.id.edit_memo);
        edit_price = findViewById(R.id.edit_price);
        delete_price = findViewById(R.id.delete_price);

        //사진이 이미 설정된 경우 -> storage에서 이미지 파일 불러와서 glide
        if (picture != null) {
            storage = FirebaseStorage.getInstance();
            StorageReference storageReference = storage.getReference().child("travelPicture/"+picture); //travelPicture라는 폴더의 picture라는 파일이름
            edit_picture.getLayoutParams().height = 1000;
            Glide.with(getApplicationContext())
                    .load(storageReference).centerCrop()
                    .into(edit_picture);
        }
        else { //사진 설정이 되어있지 않았던 경우, 사진 추가 가능
            edit_picture.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //카메라 권한 획득 => TedPermission 라이브러리
                    PermissionListener permissionListener = new PermissionListener() {
                        @Override
                        public void onPermissionGranted() {
                            Toast.makeText(PriceEdit.this, "Permission Granted", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onPermissionDenied(ArrayList<String> deniedPermissions) {
                            Toast.makeText(PriceEdit.this, "Permission Denied\n" + deniedPermissions.toString(), Toast.LENGTH_SHORT).show();
                        }
                    };

                    TedPermission.with(getApplicationContext())
                            .setPermissionListener(permissionListener)
                            .setDeniedMessage("[설정] > [권한] 에서 권한을 변경하세요")
                            .setPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA)
                            .check();
                    cameraDialog(); //카메라 dialog
                }
            });
        }

        //항목 setText
        edit_item.setText(item);

        //메모
        if(memo != null){ //메모가 설정 되어 있을 때,
            edit_memo.setText(memo);
        }
        else{ //아닐 때, 메모 설정 가능
            edit_memo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    memoDialog();
                }
            });
        }

        //편집하기 버튼
        edit_price.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(PriceEdit.this, PriceAdd.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
        });

        //삭제하기 버튼
        delete_price.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Storage 참조
                FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                storage = FirebaseStorage.getInstance();
                StorageReference storageRef = storage.getReference();
                //지울 파일 경로 설정
                StorageReference riversRef = storageRef.child("travelPicture/"+picture);

                if(picture != null){
                    riversRef.delete(); //storage에서 이미지 삭제
                }

                //db경로 설정
                DocumentReference df = db.collection("users").document(firebaseUser.getUid());
                DocumentReference sf = df.collection("countries").document(documentId);
                sf.collection("Price").document(documentTime).delete(); //해당 문서 삭제 -> 해당 아이템 삭제
                Intent intent = new Intent(PriceEdit.this, PriceList.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
        });

    }

    //menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.edit_price_menu,menu);
        return super.onCreateOptionsMenu(menu);
    }

    //뒤로가기 버튼 동작
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()){
            case android.R.id.home:{  //뒤로가기
                finish();
                return true;
            }
            case R.id.save_edit:{ //save 버튼을 눌러야 미리 설정해 놓은 사진&메모를 db에 저장 가능
                if(picture != null &&  check == true){ //사진 데이터가 있고, 사진에 uri가 저장되어 있을때 -> 갤러리나 촬영으로 사진을 불러왔을 때
                    //storage에 업로드
                    FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                    String cu = firebaseUser.getUid();
                    String filename = cu+"_"+System.currentTimeMillis();
                    storage = FirebaseStorage.getInstance();
                    StorageReference storageRef = storage.getReference();
                    //업로드 할 파일 경로 설정
                    StorageReference riversRef = storageRef.child("travelPicture/"+filename);
                    //업로드 객체 생성
                    UploadTask uploadTask = riversRef.putFile(Uri.parse(picture));
                    uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) { //성공 시,
                            Toast.makeText(PriceEdit.this, "이미지 업로드 완료", Toast.LENGTH_SHORT).show();
                        }
                    });
                    picture = filename; //업로드한 파일 이름 저장

                    //DB 사진 정보 update
                    DocumentReference df = db.collection("users").document(firebaseUser.getUid());
                    DocumentReference sf = df.collection("countries").document(documentId);
                    sf.collection("Price").document(documentTime).update("price_picture",picture);

                }
                if(edit_memo.getText().toString() != null && m_check == true){ //메모 데이터가 존재하고, 메모데이터를 저장 했을 때(memoDialog)
                    //DB 메모 정보 update
                    FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                    DocumentReference df = db.collection("users").document(firebaseUser.getUid());
                    DocumentReference sf = df.collection("countries").document(documentId);
                    sf.collection("Price").document(documentTime).update("price_memo",edit_memo.getText().toString());
                }
                Intent intent = new Intent(PriceEdit.this, PriceList.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }

        }
        return super.onOptionsItemSelected(item);
    }

    //카메라와 앨범 다이얼로그
    private void cameraDialog() {
        AlertDialog.Builder ad = new AlertDialog.Builder(PriceEdit.this);
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

    private File createImageFile() throws  IOException{
        String imgFileName = System.currentTimeMillis()+".jpg"; //현재 시각을 기준으로 파일 이름 생성
        File imageFile = null;
        File storageDir = new File(Environment.getExternalStorageDirectory()+"/Pictures","ireh"); //파일 생성 경로

        if (!storageDir.exists()){ //만약에 경로가 존재 하지 않는 다면,
            Log.v("알림", "storageDir 존재 x"+ storageDir.toString());
            storageDir.mkdirs();
        }
        Log.v("알림","storageDir 존재함" + storageDir.toString());
        imageFile = new File(storageDir,imgFileName); //생성된 파일 이름에 지정된 경로에 이미지 파일 생성
        mCurrentPhotoPath = imageFile.getAbsolutePath(); //이미지 파일의 경로 저장
        Log.v("알림 imgFile",imageFile.toString());

        return imageFile;
    }

    //앨범 선택 클릭 메소드
    private void selectAlbum() {
        //앨범에서 이미지 가져옴

        //앨범 열기
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType(MediaStore.Images.Media.CONTENT_TYPE);

        intent.setType("image/*"); //모든 이미지 타입으로

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

                        phohoURI = data.getData(); //앨범에서 선택한 uri
                        albumURI = Uri.fromFile(albumFile);

                        //해당 이미지를 로컬 폴더에 저장
                        galleryAddPic();

                        picture = String.valueOf(phohoURI);

                        //사진 glide
                        edit_picture.getLayoutParams().height = 1000;
                        Glide.with(getApplicationContext())
                                .load(phohoURI).centerCrop()
                                .into(edit_picture);
                        Log.i("알림 photoURI", String.valueOf(phohoURI));

                        check = true; //사진 설정 check

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
                galleryAddPic();

                picture = String.valueOf(imgCUri);

                //사진 glide
                edit_picture.getLayoutParams().height = 1000;
                Glide.with(getApplicationContext())
                        .load(imgCUri).centerCrop()
                        .into(edit_picture);
                Log.i("알림 imgUri", String.valueOf(imgCUri));

                check = true; //사진 설정 check

                break;
            }
        }
    }
    //메모 입력 다이얼로그 => 사진과 함께 길게 입력할 수 있는 메모장
    private void memoDialog() {
        AlertDialog.Builder ad = new AlertDialog.Builder(PriceEdit.this);
        ad.setTitle("Memo");
        ad.setMessage("메모를 입력해 주세요");

        //다이어로그 안에다 editText 추가
        EditText et = new EditText(PriceEdit.this);
        //다이어로그 안의
        //EditText의 여백과 크기 조정을 위해
        //FrameLayout 설정
        FrameLayout frame = new FrameLayout(PriceEdit.this);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = getResources().getDimensionPixelSize(R.dimen.dialog_margin);
        params.rightMargin = getResources().getDimensionPixelSize(R.dimen.dialog_margin);
        params.height = getResources().getDimensionPixelSize(R.dimen.dialog_height);
        et.setLayoutParams(params);
        frame.addView(et);
        ad.setView(frame);

        //확인 버튼
        ad.setPositiveButton("확인", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String result = et.getText().toString();

                //버튼에 내용 set
                edit_memo.setText(result);
                m_check = true; //메모 설정 check

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
}

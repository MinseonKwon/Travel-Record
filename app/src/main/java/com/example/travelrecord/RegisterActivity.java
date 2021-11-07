package com.example.travelrecord;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class RegisterActivity extends AppCompatActivity {

    private FirebaseAuth mFirebaseAuth; //파이어베이스 인증 처리
    private FirebaseFirestore db; //cloude firestore 데이터베이스
    private TextInputLayout tl_name, tl_emailId, tl_password, tl_password_confirm; //회원가입 입력 필드
    private Button btn_register; //회원가입 버튼
    private String strName,strEmail,strPwd; //결과값




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        //초기화. 연동
        mFirebaseAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        tl_name = findViewById(R.id.tl_name); //이름
        tl_emailId = findViewById(R.id.tl_emailId); //이메일 아이디
        tl_password = findViewById(R.id.tl_password); //비밀번호
        btn_register = findViewById(R.id.btn_register); //회원가입 완료 버튼


        btn_register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //회원가입 처리 시작
                //각각의 텍스트에 입력되어 있는 정보를 변수에 저장
                strName = tl_name.getEditText().getText().toString();
                strEmail = tl_emailId.getEditText().getText().toString();
                strPwd = tl_password.getEditText().getText().toString();

                Log.i("name",strName);
                Log.i("id",strEmail);
                Log.i("pwd",strPwd);

                if(strName!=null && strEmail!=null && strPwd!=null ){ //모든 칸을 채워야 함
                    //Firebase Auth 진행
                    mFirebaseAuth.createUserWithEmailAndPassword(strEmail,strPwd).addOnCompleteListener(RegisterActivity.this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            //회원가입이 이루어졌을 때의 처리
                            //task는 회원가입 처리 후의 결과값
                            if(task.isSuccessful()){
                                //현재 로그인 된 유저를 가지고 오는 변수
                                FirebaseUser firebaseUser = mFirebaseAuth.getCurrentUser();
                                UserAccount account = new UserAccount();
                                //로그인 된 정보를 저장
                                account.setIdToken(firebaseUser.getUid());
                                account.setName(strName);
                                account.setEmailId(firebaseUser.getEmail());
                                account.setPassword(strPwd);

                                //데이터 베이스 TravelRecord에 데이터 삽입
                                //set은 database에 삽입
                                //Uid를 키값으로 UserAccount의 데이터를 set
                                //collection-document
                                db.collection("users").document(firebaseUser.getUid()).set(account);

                                Toast.makeText(RegisterActivity.this, "회원가입 완료", Toast.LENGTH_SHORT).show();
                                finish();
                            }
                            else{
                                Toast.makeText(RegisterActivity.this, "회원가입 실패", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
                else{
                    Toast.makeText(RegisterActivity.this, "모두 입력하세요", Toast.LENGTH_SHORT).show();
                }
                
            }
        });
    }
}

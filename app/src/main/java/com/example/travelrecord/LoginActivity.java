package com.example.travelrecord;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.firebase.ui.auth.data.model.User;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;


public class LoginActivity extends AppCompatActivity {
    private EditText et_emailId; //이메일 아이디
    private EditText et_password; //비밀번호
    private Button btn_login;  //로그인 버튼
    private Button btn_register;  //회원가입 버튼
    private FirebaseAuth mFirebaseAuth; //파이어베이스 인증 처리
    private FirebaseFirestore db; //cloude firestore 데이터베이스
    private SignInButton google_button; //구글 로그인 버튼
    private static final int REQ_SIGN_GOOGLE = 1000; //구글 로그인 결과 코드
    private GoogleSignInClient googleSignInClient; //구글 API 클라이언트


    @Override
    protected void onCreate(Bundle savedInstanceState) { //앱 실행 시 수행
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        //초기화. 연동
        mFirebaseAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();


        //구글 signIn 버튼 이용 시, 옵션 세팅
        //구글 로그인을 통해 앱에서 요구하는 사용자의 데이터를 요청
        //DEFAULT_SIGN_IN은 사용자의 기본적인 정보를 얻을 수 있는 매개변수
        GoogleSignInOptions googleSignInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();



        //googlesignInOptions를 통해 가져온 클라이언트 정보를 담는 GoogleSignInClient 객체
        googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions);


        google_button = findViewById(R.id.google_button);
        //구글 로그인 버튼 클릭 시 수행행
        google_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Google 로그인
                Intent signInIntent = googleSignInClient.getSignInIntent();
                startActivityForResult(signInIntent, REQ_SIGN_GOOGLE);
            }
        });


        et_emailId = findViewById(R.id.tl_emailId);
        et_password = findViewById(R.id.tl_password);
        btn_login = findViewById(R.id.btn_login);
        btn_register = findViewById(R.id.btn_register);


        //일반 로그인 요청
        btn_login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String strEmail = et_emailId.getText().toString();
                String strPwd = et_password.getText().toString();

                mFirebaseAuth.signInWithEmailAndPassword(strEmail, strPwd).addOnCompleteListener(LoginActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            //로그인 성공
                            Intent intent = new Intent(LoginActivity.this, CountryList.class);
                            startActivity(intent);
                            finish(); //현재 액티비티 파괴
                        } else {
                            Toast.makeText(LoginActivity.this, "로그인 실패", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });

        //회원가입 버튼을 눌렀을 때
        btn_register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //회원가입 화면으로 이동
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });

    }

    //Google 로그인 결과 수신
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //구글 로그인 버튼 응답
        if (requestCode == REQ_SIGN_GOOGLE) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class); //구글계정 데이터
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                Log.d(LoginActivity.class.getSimpleName(),"onActivity result() -ApiException: "+e);
                Toast.makeText(LoginActivity.this, "failed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    //결과에 따른 auth 처리
    //사용자가 정상적으로 로그인 한 후에 GoogleSignInAccount 개체에서 ID 토큰을 가져와서
    //Firebase 사용자 인증 정보로 교환하고, FireBase 사용자 인증 정보를 사용해서 Firebase에 인증
    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        mFirebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) { //로그인 성공 시,
                            UserAccount user = new UserAccount(); 
                            user.setIdToken(account.getIdToken()); //구글 계정에 대한 간단한 정보 저장
                            user.setEmailId(account.getId());
                            user.setName(account.getGivenName());
                            //firestore에 구글연동 로그인 계정 데이터 추가
                            db.collection("users").document(mFirebaseAuth.getCurrentUser().getUid()).set(user);

                            Toast.makeText(LoginActivity.this, "로그인 성공", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(getApplicationContext(), CountryList.class);
                            startActivity(intent);
                        } else { //로그인 실패 시,
                            Toast.makeText(LoginActivity.this, "로그인 실패", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}

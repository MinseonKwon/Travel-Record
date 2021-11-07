package com.example.travelrecord;

import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class showPicture extends AppCompatActivity {
    private Button edit_cancel, edit_delete;
    private ImageView show_image;
    private SharedPreferences price;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_picture);

        getImage();

        edit_cancel = findViewById(R.id.edit_cancel);
        edit_delete = findViewById(R.id.edit_delete);

        //닫기 클릭 시,
        edit_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });

        //삭제 클릭 시,
        edit_delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setResult(RESULT_OK);
                finish();
            }
        });
    }

    private void getImage() {
        show_image = findViewById(R.id.show_image);

        price = getSharedPreferences("price",MODE_PRIVATE);
        String picture = price.getString("picture","");

        //storage 참조
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();
        StorageReference pathRef = storageRef.child("travelPicture/"+picture);

        //사진 Glide
        Glide.with(showPicture.this)
                .load(pathRef)
                .into(show_image);
    }
}
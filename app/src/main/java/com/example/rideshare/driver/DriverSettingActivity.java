package com.example.rideshare.driver;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.rideshare.R;
import com.example.rideshare.customer.CustomerSettingActivity;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class DriverSettingActivity extends AppCompatActivity {

    private EditText name, phone, car;
    private Button confirm, back;
    private FirebaseAuth auth;
    private DatabaseReference databaseReferenceDriver;
    private String userId;
    private String databaseUser, databasePhone, databaseImage, databaseCar;
    private ImageView customerImage;
    private Uri resultUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_setting);
        name = findViewById(R.id.userName);
        phone = findViewById(R.id.userPhone);
        confirm = findViewById(R.id.cConfirm);
        back = findViewById(R.id.cBack);
        customerImage=findViewById(R.id.customerImage);
        car=findViewById(R.id.car);

        customerImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent,1);
            }
        });

        auth = FirebaseAuth.getInstance();
        userId = auth.getCurrentUser().getUid();

        databaseReferenceDriver = FirebaseDatabase.getInstance()
                .getReference().child("Users").child("Riders").child(userId);
        getUserInfo();
        confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveUserInformation();
                finish();
            }
        });

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                return;
            }
        });
    }

    private void getUserInfo() {
        databaseReferenceDriver.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.getChildrenCount() > 0) {
                    Map<String, Object> map = (Map<String, Object>) snapshot.getValue();
                    if (map.get("name") != null) {
                        databaseUser = map.get("name").toString();
                        name.setText(databaseUser);
                    }
                    if (map.get("car") != null) {
                        databaseCar = map.get("car").toString();
                        car.setText(databaseCar);
                    }
                    if (map.get("phone") != null) {
                        databasePhone = map.get("phone").toString();
                        phone.setText(databasePhone);
                    }
                    if (map.get("profileImageUrl") != null) {
                        databaseImage = map.get("profileImageUrl").toString();
                        Glide.with(getApplication()).load(databaseImage).into(customerImage);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(DriverSettingActivity.this, "" + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveUserInformation() {
        databaseUser = name.getText().toString();
        databasePhone = phone.getText().toString();
        databaseCar=car.getText().toString();
        Map userInfo = new HashMap();
        userInfo.put("name", databaseUser);
        userInfo.put("phone", databasePhone);
        userInfo.put("car", databaseCar);
        databaseReferenceDriver.updateChildren(userInfo);
        if (resultUri!=null){
            StorageReference filePath= FirebaseStorage.getInstance().getReference().child("profile_images").child(userId);
            Bitmap bitmap=null;
            try {
                bitmap= MediaStore.Images.Media.getBitmap(getApplication().getContentResolver(),resultUri);
            } catch (IOException e) {
                e.printStackTrace();
            }
            //compress images
            ByteArrayOutputStream baos=new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG,20,baos);
            byte [] data=baos.toByteArray();
            UploadTask uploadTask=filePath.putBytes(data);
            uploadTask.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(DriverSettingActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
//                    finish();
                }
            });
            uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    if (taskSnapshot.getMetadata() != null) {
                        if (taskSnapshot.getMetadata().getReference() != null) {
                            Task<Uri> result = taskSnapshot.getStorage().getDownloadUrl();
                            result.addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    String imageUrl = uri.toString();
                                    Map newImage=new HashMap();
                                    newImage.put("profileImageUrl", imageUrl);
                                    databaseReferenceDriver.updateChildren(newImage);
//                                    finish();
                                    return;
                                }
                            });
                        }
                    }else {
//                        finish();
                    }
                }
            });
        }


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode==1 && resultCode== Activity.RESULT_OK){
            final Uri imageUri=data.getData();
            resultUri=imageUri;
            customerImage.setImageURI(resultUri);

        }
    }
}


package com.example.rideshare.customer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.rideshare.R;
import com.example.rideshare.driver.DriverLoginActivity;
import com.example.rideshare.driver.MapActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class CustomerLoginActivity extends AppCompatActivity {

   private EditText email, password;
   private Button login, regsiter;
    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener firebaseAuthListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_login);

        email=findViewById(R.id.etcemail);
        password=findViewById(R.id.etcpassword);
        login=findViewById(R.id.clogin);
        regsiter=findViewById(R.id.cregister);

        firebaseAuth=FirebaseAuth.getInstance();
        firebaseAuthListener= new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user=FirebaseAuth.getInstance().getCurrentUser();
                if(user!=null){
                    System.out.println(user.getEmail());
                    Intent intent= new Intent(CustomerLoginActivity.this, CustomerMapActivity.class);
                    startActivity(intent);
                    finish();
                    return;
                }
            }
        };

        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String lemail= email.getText().toString();
                final String lpassword= password.getText().toString();
                firebaseAuth.signInWithEmailAndPassword(lemail,lpassword).addOnCompleteListener(CustomerLoginActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (!task.isSuccessful()){
//                            System.out.println(task.getResult());
                            Toast.makeText(CustomerLoginActivity.this, "Login error", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });

        regsiter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String Remail= email.getText().toString();
                final String Rpassword= password.getText().toString();
                firebaseAuth.createUserWithEmailAndPassword(Remail, Rpassword).addOnCompleteListener(CustomerLoginActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (!task.isSuccessful()){
                            System.out.println(task.toString());
                            Toast.makeText(CustomerLoginActivity.this, "Signup error", Toast.LENGTH_SHORT).show();
                        }else{
                            String user_id= firebaseAuth.getCurrentUser().getUid();
                            DatabaseReference current_user_db= FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(user_id);
                            current_user_db.setValue(true);
                            Toast.makeText(CustomerLoginActivity.this, "Register Success", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        firebaseAuth.addAuthStateListener(firebaseAuthListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        firebaseAuth.removeAuthStateListener(firebaseAuthListener);
    }
}

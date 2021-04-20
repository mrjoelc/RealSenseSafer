package com.example.testrealsense;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.WindowManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;

public class SplashActivity extends AppCompatActivity {

    private static int SPLASH_TIME_OUT = 1000;
    public static HashMap<String, Float> objectDict;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //getObjectsListFromFirebase();

        Intent homeIntent = new Intent(SplashActivity.this, MainActivity.class);
        startActivity(homeIntent);
        finish();

    }

    /*public void getObjectsListFromFirebase(){
        String path = "config/objectsToDetect";
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(path);
        objectDict = new HashMap<>();
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                objectDict = (HashMap) snapshot.getValue();
                if(snapshot.exists()) {
                    System.out.print("Data ON Firebase: ");
                    System.out.println(snapshot.getValue());
                }else System.out.println("Data ON Firebase: NULL");
                Intent homeIntent = new Intent(SplashActivity.this, MainActivity.class);
                homeIntent.putExtra("DICT", objectDict);
                startActivity(homeIntent);
                finish();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }*/
}
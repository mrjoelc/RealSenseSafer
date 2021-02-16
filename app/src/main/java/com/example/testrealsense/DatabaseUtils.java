package com.example.testrealsense;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;

public class DatabaseUtils {
    private static final String TAG = "librs capture example";
    private static Context mContext;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;


    public DatabaseUtils(Activity activity){
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mContext = activity;
        signInAnonymous();
    }

    public FirebaseUser getFireBaseUser(){
        return mAuth.getCurrentUser();
    }

    public String getUserUID(){
        return mAuth.getCurrentUser().getUid();
    }

    public void writeTooCloseDistanceLog(float distance, String object){
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String millisInString  = dateFormat.format(new Date());
        Map<String, String> map = new HashMap<String,String>();
        map.put("distance", String.valueOf(distance));
        map.put("object", object);
        mDatabase.child("users").child(getUserUID()).child(millisInString).setValue(map);
    }

    private void signInAnonymous(){
        mAuth.signInAnonymously()
                .addOnCompleteListener((Activity) mContext, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInAnonymously:success " + mAuth.getCurrentUser().getUid());
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInAnonymously:failure", task.getException());

                        }
                    }
                });
    }

}

package com.example.testrealsense;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;

import com.example.testrealsense.Helper.DetectableObjectsAdapter;
import com.example.testrealsense.Helper.LogAdapter;

import java.util.ArrayList;
import java.util.List;

public class DetectableListActivity extends AppCompatActivity {

    List<DetectableObject> list;
    List<DetectableObject> selected_list;
    RecyclerView detectableObjects;
    RecyclerView selectedDetectableObjects;
    DetectableObjectsAdapter detectableObjectsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detectable_list);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // showing the back button in action bar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        detectableObjects = findViewById(R.id.rw_objects);
        selectedDetectableObjects = findViewById(R.id.selected_objects);

        list = new ArrayList();
        selected_list = new ArrayList();

        for (int i=0; i<6; i++) {
            DetectableObject d_o;
            d_o = new DetectableObject(i+1, false, "object"+i);
            list.add(d_o);
        }


        detectableObjectsAdapter = new DetectableObjectsAdapter(DetectableListActivity.this, (ArrayList<DetectableObject>) list);
        detectableObjects.setAdapter(detectableObjectsAdapter);
        detectableObjects.setLayoutManager(new LinearLayoutManager(this));



        detectableObjectsAdapter = new DetectableObjectsAdapter(DetectableListActivity.this, (ArrayList<DetectableObject>) selected_list);
        selectedDetectableObjects.setAdapter(detectableObjectsAdapter);
        selectedDetectableObjects.setLayoutManager(new LinearLayoutManager(this));

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
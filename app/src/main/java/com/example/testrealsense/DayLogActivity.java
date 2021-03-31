package com.example.testrealsense;

import android.content.Intent;
import android.os.Bundle;

import com.example.testrealsense.Helper.LogAdapter;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.MenuItem;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class DayLogActivity extends AppCompatActivity {

    List<SimpleLog> list;
    RecyclerView logs;
    LogAdapter logAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_day_log);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Intent i = getIntent();
        list = (ArrayList<SimpleLog>) i.getSerializableExtra("LIST");
        String day = i.getStringExtra("DAY");

        toolbar.setTitle(day);

        logs = findViewById(R.id.logs);

        System.out.println(list.get(0).getDatastamp());
        logAdapter = new LogAdapter(DayLogActivity.this, (ArrayList<SimpleLog>) list);
        logs.setAdapter(logAdapter);

        logs.setLayoutManager(new LinearLayoutManager(this));

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


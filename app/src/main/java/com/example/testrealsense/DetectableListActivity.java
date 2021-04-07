package com.example.testrealsense;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import com.example.testrealsense.Helper.DetectableObjectsAdapter;
import com.example.testrealsense.Helper.Utils;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DetectableListActivity extends AppCompatActivity implements DetectableObjectsAdapter.AdapterCallback {

    List<DetectableObject> selected_list;
    RecyclerView selectedDetectableObjects;
    DetectableObjectsAdapter detectableObjectsAdapter;
    HashMap<String, Float> objectDictSelected;
    HashMap<String, Float> objectDictUnselected;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detectable_list);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // showing the back button in action bar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        selectedDetectableObjects = findViewById(R.id.selected_objects);

        selected_list = new ArrayList<>();


        Intent intent = getIntent();
        objectDictSelected = (HashMap<String, Float>) intent.getSerializableExtra("DICT");

        takeObjectDict();
        for (HashMap.Entry<String, Float> obj : objectDictUnselected.entrySet()) {
            DetectableObject d_o;
            d_o = new DetectableObject(obj.getKey(), obj.getValue(),false);
            selected_list.add(0,d_o);
        }

        for (HashMap.Entry<String, Float> obj : objectDictSelected.entrySet()) {
            DetectableObject d_o;
            d_o = new DetectableObject(obj.getKey(), obj.getValue(),true);
            selected_list.add(0,d_o);
        }


        detectableObjectsAdapter = new DetectableObjectsAdapter(this, this,selected_list);
        selectedDetectableObjects.setAdapter(detectableObjectsAdapter);
        selectedDetectableObjects.setLayoutManager(new LinearLayoutManager(this));
    }

    void takeObjectDict(){
        /** prelievo  oggetti e distanze critiche da file json **/
        try {
            objectDictUnselected = Utils.jsonToMap(this, false);
        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }
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

    @Override
    public void onItemClicked() {
        selectedDetectableObjects.getLayoutManager().removeAllViews();
    }
}
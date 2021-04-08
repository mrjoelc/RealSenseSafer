package com.example.testrealsense;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;

import com.example.testrealsense.Helper.DetectableObjectsAdapter;
import com.example.testrealsense.Helper.Utils;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

public class DetectableListActivity extends AppCompatActivity implements DetectableObjectsAdapter.AdapterCallback {

    List<DetectableObject> selected_list;
    RecyclerView selectedDetectableObjects;
    DetectableObjectsAdapter detectableObjectsAdapter;
    HashMap<String, Float> objectDictSelected;
    HashMap<String, Float> objectDictUnselected;

    EditText searchBar;
    Button searchButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detectable_list);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // showing the back button in action bar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        searchBar = findViewById(R.id.search_bar);
        searchButton = findViewById(R.id.search_button);

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

        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {

                filter(text.toString());
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                // TODO Auto-generated method stub
            }

            @Override
            public void afterTextChanged(Editable s) {

                // filter your list from your input
                filter(s.toString());
                //you can use runnable postDelayed like 500 ms to delay search text
            }
        });
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

    void filter(String text){
        List<DetectableObject> temp = new ArrayList();
        for(DetectableObject d: selected_list){
            //or use .equal(text) with you want equal match
            //use .toLowerCase() for better matches

            //key insensitive search
            if(Pattern.compile(Pattern.quote(text), Pattern.CASE_INSENSITIVE).matcher(d.getName()).find()){
                temp.add(d);
            }
        }
        //update recyclerview
        detectableObjectsAdapter.updateList(temp);
    }


}
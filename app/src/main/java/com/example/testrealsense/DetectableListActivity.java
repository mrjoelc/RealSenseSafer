package com.example.testrealsense;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
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

    List<DetectableObject> objectsListToDetect;
    RecyclerView selectedDetectableObjects;
    DetectableObjectsAdapter detectableObjectsAdapter;
    HashMap<String, Float> objectDictSelected;
    HashMap<String, Float> objectDictUnselected;
    DetectableObject d_o;
    Boolean isSelected;

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

        selectedDetectableObjects = findViewById(R.id.selected_objects);
        searchBar = findViewById(R.id.search_bar);
        searchButton = findViewById(R.id.search_button);

        objectsListToDetect = new ArrayList<>();


        //Intent intent = getIntent();
        //objectDictSelected = (HashMap<String, Float>) intent.getSerializableExtra("DICT");

        takeObjectDict();
        for (HashMap.Entry<String, Float> obj : objectDictUnselected.entrySet()) {
            isSelected=false;
            if ( MainActivity.objectDict!=null && MainActivity.objectDict.containsKey(obj.getKey())){
                isSelected=true;
            }
            d_o = new DetectableObject(obj.getKey(), obj.getValue(),isSelected);
            if (isSelected) {
                objectsListToDetect.add(0, d_o);
            } else {
                objectsListToDetect.add(d_o);
            }
        }

        /*for (HashMap.Entry<String, Float> obj : objectDictSelected.entrySet()) {
            DetectableObject d_o;
            d_o = new DetectableObject(obj.getKey(), obj.getValue(),true);
            objectsListToDetect.add(0,d_o);
        }*/

        detectableObjectsAdapter = new DetectableObjectsAdapter(this, this, objectsListToDetect);
        selectedDetectableObjects.setAdapter(detectableObjectsAdapter);
        selectedDetectableObjects.setLayoutManager(new LinearLayoutManager(this));

        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {

                filter(text.toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {
                filter(editable.toString());
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                // TODO Auto-generated method stub
            }

        });

        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
            }
        });
    }

    void filter(String text){
        List<DetectableObject> temp = new ArrayList();
        for(DetectableObject d: objectsListToDetect){
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

    void takeObjectDict(){
        /** prelievo  oggetti e distanze critiche da file json **/
        try {
            objectDictUnselected = Utils.jsonToMap(this);
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
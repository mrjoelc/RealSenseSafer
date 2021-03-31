package com.example.testrealsense;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.google.firebase.database.DataSnapshot;

import java.io.Serializable;
import java.util.ArrayList;

public class BarChartActivity extends AppCompatActivity {
    String[] monthItems = {"01","02","03","04","05","06","07","08","09","10","11","12"};
    String[] daysItems = {"01","02","03","04","05","06","07","08","09","10","11","12","13","14","15","16","17","18","19","20","21","22","23","24","25","26","27","28","29","30","31"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_barchart);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        Spinner monthSpinner = (Spinner) findViewById(R.id.month_spinner);
        Spinner yearSpinner = (Spinner) findViewById(R.id.year_spinner);
        //String monthSelected = monthSpinner.getSelectedItem().toString();
        //String yearSelected = yearSpinner.getSelectedItem().toString();

        setSupportActionBar(toolbar);

        // showing the back button in action bar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        BarChart barChart = findViewById(R.id.barChart);

        String[] cd = Utils.getCurrentDay();


        yearSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                animateCurrentData(cd[0], cd[1], barChart);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // your code here
            }

        });

        monthSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                System.out.println(position);
                animateCurrentData(cd[0],monthItems[position] , barChart);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // your code here
            }

        });

        monthSpinner.setSelection(Integer.parseInt(cd[1])-1);
        //animateCurrentData(cd[0], cd[1], barChart);




    }

    public BarChart barChartConfig(ArrayList<BarEntry> days, BarChart barChart){
        BarDataSet barDataSet = new BarDataSet(days, "days");
        barDataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return "" + ((int) value);
            }
        });

        barDataSet.setColors(Color.parseColor("#FFA500"));
        barDataSet.setValueTextColor(Color.BLACK);
        barDataSet.setValueTextSize(16f);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTH_SIDED);
        xAxis.setDrawLabels(true);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return daysItems[(int) value % daysItems.length];
            }
        });
        xAxis.setGranularityEnabled(true);
        xAxis.setGranularity(1f); // only intervals of 1 day

        barChart.getLegend().setEnabled(false);
        BarData barData = new BarData(barDataSet);
        barChart.setFitBars(true);
        barChart.setData(barData);
        barChart.getDescription().setText("Minimum distances exceeded");
        //barChart.getDescription().setText("bar chart example");
        barChart.animateY(300);
        barChart.setTouchEnabled(true);
        return barChart;
    }

    public void animateCurrentData(String year, String month, BarChart barChart){
        ArrayList<BarEntry> days = new ArrayList<>();
        DatabaseUtils.getDataLogFromFirebaseYM(year, month, new CallbackFirebaseData() {
            @Override
            public void onCallback(DataSnapshot dataSnapshot) {
                dataSnapshot.getChildrenCount();
                for (DataSnapshot child: dataSnapshot.getChildren()) {
                    days.add(new BarEntry(Float.parseFloat(child.getKey()), (int)child.getChildrenCount()));
                }
                barChartConfig(days, barChart).setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
                    @Override
                    public void onValueSelected(Entry e, Highlight h) {
                        int x= (int) e.getX();
                        System.out.println("Bin Selezionato: " + x);
                        Intent i=new Intent(BarChartActivity.this,DayLogActivity.class);
                        ArrayList<SimpleLog> simpleLogList = new ArrayList<SimpleLog>();
                        for (DataSnapshot child: dataSnapshot.child(String.valueOf(x)).getChildren()) {
                            simpleLogList.add(new SimpleLog(child.getKey(),
                                                            String.valueOf(child.child("distance").getValue()),
                                                            String.valueOf(child.child("object").getValue())));
                        }
                        i.putExtra("LIST", (Serializable) simpleLogList);
                        startActivity(i);
                    }

                    @Override
                    public void onNothingSelected() {

                    }
                });
            }
        });

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
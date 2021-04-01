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
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.google.firebase.database.DataSnapshot;

import java.io.Serializable;
import java.util.ArrayList;

public class BarChartActivity extends AppCompatActivity {
    String[] monthItems = {"01","02","03","04","05","06","07","08","09","10","11","12"};
    String[] daysItems = {"01","02","03","04","05","06","07","08","09","10","11","12","13","14","15","16","17","18","19","20","21","22","23","24","25","26","27","28","29","30","31"};
    BarChart barChart;
    BarDataSet set1;
    BarData data;
    private static final int MAX_X_VALUE = 31;
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

        barChart = findViewById(R.id.barChart);

        String[] cd = Utils.getCurrentDay();
        createChartData(cd[0], cd[1]);



        yearSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
               // animateCurrentData(cd[0], cd[1], barChart);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // your code here
            }

        });

        monthSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                //animateCurrentData(cd[0],monthItems[position] , barChart);
                createChartData(cd[0],monthItems[position]);

            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // your code here
            }

        });

        monthSpinner.setSelection(Integer.parseInt(cd[1])-1);
        createChartData(cd[0],cd[1]);




    }

    private void configureChartAppearance() {
        barChart.setFitBars(true);
        barChart.getDescription().setEnabled(true);
        barChart.getDescription().setText("Minimum distances exceeded");
        barChart.animateY(300);
        barChart.setTouchEnabled(true);

        set1.setColors(Color.parseColor("#FFA500"));

        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTH_SIDED);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return daysItems[(int) value];
            }
        });

        YAxis axisLeft = barChart.getAxisLeft();
        axisLeft.setGranularity(1f);
        axisLeft.setAxisMinimum(0);

        YAxis axisRight = barChart.getAxisRight();
        axisRight.setGranularity(1f);
        axisRight.setAxisMinimum(0);
    }

    private void createChartData(String year, String month) {
        ArrayList<BarEntry> values = new ArrayList<>();
        DatabaseUtils.getDataLogFromFirebaseYM(year, month, new CallbackFirebaseData() {
            @Override
            public void onCallback(DataSnapshot monthDS) {
                for (DataSnapshot dayDS: monthDS.getChildren()){
                    for (int i = 0; i < MAX_X_VALUE; i++) {
                        if (dayDS.getKey().equals(daysItems[i])) {
                            values.add(new BarEntry(i, (int)dayDS.getChildrenCount()));
                        }else values.add(new BarEntry(i, 0));
                    }
                    barChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
                        @Override
                        public void onValueSelected(Entry e, Highlight h) {
                            String x=  daysItems[(int) e.getX()];
                            System.out.println("Bin Selezionato: " + x);
                            ArrayList<SimpleLog> simpleLogList = new ArrayList<SimpleLog>();
                            for (DataSnapshot child: monthDS.child(String.valueOf(x)).getChildren()) {
                                simpleLogList.add(new SimpleLog(child.getKey(),
                                        String.valueOf(child.child("distance").getValue()),
                                        String.valueOf(child.child("object").getValue())));
                            }

                            Intent i=new Intent(BarChartActivity.this,DayLogActivity.class);
                            i.putExtra("LIST", (Serializable) simpleLogList);
                            i.putExtra("DAY", x);
                            i.putExtra("MONTH", monthDS.getKey());
                            startActivity(i);
                        }

                        @Override
                        public void onNothingSelected() {}
                    });
                }
                set1 = new BarDataSet(values, "days");

                ArrayList<IBarDataSet> dataSets = new ArrayList<>();
                dataSets.add(set1);

                data = new BarData(dataSets);
                configureChartAppearance();
                prepareChartData(data);
            }
        });
    }

    private void prepareChartData(BarData data) {
        data.setValueTextSize(12f);
        barChart.setData(data);
        barChart.invalidate();
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
package com.example.testrealsense;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.graphics.Color;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Spinner;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.firebase.database.DataSnapshot;

import java.util.ArrayList;

public class BarChartActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_barchart);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        Spinner monthSpinner = (Spinner) findViewById(R.id.month_spinner);
        Spinner yearSpinner = (Spinner) findViewById(R.id.year_spinner);

        String monthSelected = monthSpinner.getSelectedItem().toString();
        String yearSelected = yearSpinner.getSelectedItem().toString();

        setSupportActionBar(toolbar);

        // showing the back button in action bar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        BarChart barChart = findViewById(R.id.barChart);

        animateCurrentMonth(barChart);


    }

    public void animateCurrentMonth(BarChart barChart){
        ArrayList<BarEntry> visitors = new ArrayList<>();
        String[] d = Utils.getCurrentDay();
        DatabaseUtils.getCurrentMonthDataCount(new CallbackFirebaseData() {
            @Override
            public void onCallback(DataSnapshot dataSnapshot) {
                dataSnapshot.getChildrenCount();
                for (DataSnapshot child: dataSnapshot.getChildren()) {
                    visitors.add(new BarEntry(Float.parseFloat(child.getKey()), child.getChildrenCount()));
                }
                BarDataSet barDataSet = new BarDataSet(visitors, "visitors");
                barDataSet.setColors(ColorTemplate.MATERIAL_COLORS);
                barDataSet.setValueTextColor(Color.BLACK);
                barDataSet.setValueTextSize(16f);

                BarData barData = new BarData(barDataSet);

                barChart.setFitBars(true);
                barChart.setData(barData);
                //barChart.getDescription().setText("bar chart example");
                barChart.animateY(2000);
                System.out.println(dataSnapshot.getChildrenCount());
            }
        });

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
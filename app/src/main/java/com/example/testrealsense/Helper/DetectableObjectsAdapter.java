package com.example.testrealsense.Helper;

import android.content.Context;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.testrealsense.DetectableListActivity;
import com.example.testrealsense.DetectableObject;
import com.example.testrealsense.MainActivity;
import com.example.testrealsense.R;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public class DetectableObjectsAdapter extends RecyclerView.Adapter<DetectableObjectsAdapter.MyViewHolder>{

    /*String name;
    int distance ;
    boolean status;*/


    Context context;
    List<DetectableObject> objectList;
    AdapterCallback callback;

    public void updateList(List<DetectableObject> list){
        objectList = list;
        notifyDataSetChanged();
    }


    public interface AdapterCallback{
        void onItemClicked();
    }

    public DetectableObjectsAdapter(Context c,  AdapterCallback callback, List<DetectableObject> objectList) {
        this.callback = callback;
        this.context = c;
        this.objectList = objectList;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        return new MyViewHolder(LayoutInflater.from(context).inflate(R.layout.detectable_item, viewGroup, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder myViewHolder, final int i) {

        myViewHolder.status.setOnCheckedChangeListener(null);


        DetectableObject detecObj = objectList.get(i);

        myViewHolder.name.setText(objectList.get(myViewHolder.getAdapterPosition()).getName());
        myViewHolder.distance.setText(String.valueOf(objectList.get(myViewHolder.getAdapterPosition()).getDistance()));


        if(objectList.get(myViewHolder.getAdapterPosition()).getStatus()){
            myViewHolder.status.setChecked(true);
        }
        else{
            myViewHolder.status.setChecked(false);
        }


        if (MainActivity.objectDict != null) {
            myViewHolder.status.setChecked(MainActivity.objectDict.containsKey(detecObj.getName()));
        }else{
            myViewHolder.status.setChecked(false);
        }


        if (myViewHolder.status.isChecked())
            myViewHolder.distance_counter.setVisibility(View.VISIBLE);
        else
            myViewHolder.distance_counter.setVisibility(View.GONE);

        if (detecObj.getStatus()) {
            myViewHolder.status.setChecked(true);
            myViewHolder.distance_counter.setVisibility(View.VISIBLE);
        }


        myViewHolder.status.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    detecObj.setStatus(true);
                    myViewHolder.distance_counter.setVisibility(View.VISIBLE);
                    DatabaseUtils.writeNewObjectToDetect(detecObj.getName(), Float.parseFloat(String.valueOf(detecObj.getDistance())));

                    int fromPosition = i;
                    int toPosition = 0;

                    // update data array
                    DetectableObject item = objectList.get(fromPosition);
                    objectList.remove(fromPosition);
                    objectList.add(toPosition, item);

                    if(callback != null) {
                        callback.onItemClicked();
                    }
                }
                else {
                    detecObj.setStatus(false);
                    myViewHolder.distance_counter.setVisibility(View.GONE);
                    DatabaseUtils.removeObjectToDetect(detecObj.getName());

                    int fromPosition = i;
                    int toPosition = objectList.size()-1;

                    DetectableObject item = objectList.get(fromPosition);
                    objectList.remove(fromPosition);
                    objectList.add(toPosition, item);

                    if(callback != null) {
                        callback.onItemClicked();
                    }

                }
            }
        });

        myViewHolder.distance.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                float value = 0.0f;
                value += Float.parseFloat(v.getText().toString());
                if (value<0 || value>9) value=0;
                detecObj.setDistance(value);
                myViewHolder.distance.setText(String.valueOf(value));
                DatabaseUtils.updateNewObjectToDetectDistance(detecObj.getName(), value);
                //System.out.println(value);
                return true;

            }
        });





        myViewHolder.minus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (detecObj.getDistance() > 0) {
                    float value = detecObj.getDistance()-1;
                    if (value<0) value=0;
                    detecObj.setDistance(value);
                    myViewHolder.distance.setText(String.valueOf(value));
                    DatabaseUtils.updateNewObjectToDetectDistance(detecObj.getName(), value);
                }

            }

        });
        myViewHolder.plus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (detecObj.getDistance() < 9) {
                    float value = detecObj.getDistance()+1;
                    if (value>9) value=9;
                    detecObj.setDistance(value);
                    myViewHolder.distance.setText(String.valueOf(value));
                    DatabaseUtils.updateNewObjectToDetectDistance(detecObj.getName(), value);

                }
               /* if (distance < 9) {
                    distance = distance+1;
                    myViewHolder.distance.setText(String.valueOf(distance));
                }*/
            }
        });



    }

    @Override
    public int getItemCount() {
        return objectList.size();
    }



    static class MyViewHolder extends RecyclerView.ViewHolder {

        TextView name;
        EditText distance;
        CheckBox status;
        RelativeLayout distance_counter;
        ImageView minus, plus;



        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            name = (TextView) itemView.findViewById(R.id.detectable_objectname);
            distance = (EditText) itemView.findViewById(R.id.detectable_distance);
            status = (CheckBox) itemView.findViewById(R.id.detectableCheckBox);
            distance_counter = itemView.findViewById(R.id.distance_counter);
            minus = itemView.findViewById(R.id.minus);
            plus = itemView.findViewById(R.id.plus);

        }
    }
}

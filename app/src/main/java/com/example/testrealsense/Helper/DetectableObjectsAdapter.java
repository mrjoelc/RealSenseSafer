package com.example.testrealsense.Helper;

import android.content.Context;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.testrealsense.DetectableObject;
import com.example.testrealsense.Detector;
import com.example.testrealsense.MainActivity;
import com.example.testrealsense.R;
import com.example.testrealsense.SimpleLog;
import com.example.testrealsense.Utils;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.mlkit.vision.common.InputImage;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;

public class DetectableObjectsAdapter extends RecyclerView.Adapter<DetectableObjectsAdapter.MyViewHolder> {

    /*String name;
    int distance ;
    boolean status;*/


    Context context;
    ArrayList<DetectableObject> objectList;


    public DetectableObjectsAdapter(Context c, ArrayList<DetectableObject> objectList) {
        context = c;
        this.objectList = objectList;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        return new MyViewHolder(LayoutInflater.from(context).inflate(R.layout.detectable_item, viewGroup, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder myViewHolder, final int i) {

        DetectableObject dectobj = objectList.get(i);

        /*name = objectList.get(i).getName();
        distance = objectList.get(i).getDistance();
        status = objectList.get(i).getStatus();*/

        myViewHolder.name.setText(dectobj.getName());
        myViewHolder.distance.setText(String.valueOf(dectobj.getDistance()));

        /*if(dectobj.getStatus())
            myViewHolder.status.setChecked(true);
        else
            myViewHolder.status.setChecked(false);*/

        if (myViewHolder.status.isChecked())
            myViewHolder.distance_counter.setVisibility(View.VISIBLE);
        else
            myViewHolder.distance_counter.setVisibility(View.GONE);

        myViewHolder.status.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    dectobj.getStatus(true);
                    myViewHolder.distance_counter.setVisibility(View.VISIBLE);
                }
                else {
                    dectobj.getStatus(false);
                    myViewHolder.distance_counter.setVisibility(View.GONE);
                }
            }
        });

        myViewHolder.distance.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                return false;
            }
        });


        myViewHolder.minus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (dectobj.getDistance() > 0) {dectobj.setDistance(dectobj.getDistance()-1);
                    myViewHolder.distance.setText(String.valueOf(dectobj.getDistance()));

                }

            }
        });
        myViewHolder.plus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
<<<<<<< Updated upstream
                if (dectobj.getDistance() < 9) {dectobj.setDistance(dectobj.getDistance()+1);}
                myViewHolder.distance.setText(String.valueOf(dectobj.getDistance()));
=======
                if (distance < 9) {
                    distance = distance+1;
                    myViewHolder.distance.setText(String.valueOf(distance));
                }
>>>>>>> Stashed changes
            }
        });


    }

    @Override
    public int getItemCount() {
        return objectList.size();
    }

    class MyViewHolder extends RecyclerView.ViewHolder {

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

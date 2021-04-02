package com.example.testrealsense.Helper;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.testrealsense.R;
import com.example.testrealsense.SimpleLog;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;

public class LogAdapter extends RecyclerView.Adapter<LogAdapter.MyViewHolder> {

    Context context;
    ArrayList<SimpleLog> logs;


    public LogAdapter(Context c, ArrayList<SimpleLog> logs) {
        context = c;
        this.logs = logs;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        return new MyViewHolder(LayoutInflater.from(context).inflate(R.layout.item_log, viewGroup, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder myViewHolder, final int i) {
        final String time = logs.get(i).getDatastamp();
        final String object = logs.get(i).getObject();
        final String distance = logs.get(i).getDistance();

        myViewHolder.time.setText(time);
        myViewHolder.object.setText(object);
        myViewHolder.distance.setText(distance +"cm");


    }

    @Override
    public int getItemCount() {
        return logs.size();
    }

    class MyViewHolder extends RecyclerView.ViewHolder {

        TextView object, distance, time;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            object = (TextView) itemView.findViewById(R.id.object);
            distance = (TextView) itemView.findViewById(R.id.distance);
            time = (TextView) itemView.findViewById(R.id.time);

        }
    }
}

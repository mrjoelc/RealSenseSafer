package com.example.testrealsense;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;


import com.example.testrealsense.Helper.DatabaseUtils;
import com.example.testrealsense.Helper.GraphicOverlay;
import com.example.testrealsense.Helper.Utils;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.intel.realsense.librealsense.DeviceList;
import com.intel.realsense.librealsense.DeviceListener;
import com.intel.realsense.librealsense.RsContext;

import org.tensorflow.lite.support.image.TensorImage;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;


public class MainActivity extends AppCompatActivity{
    private static final String TAG = "librs capture example";
    private static final int PERMISSIONS_REQUEST_CAMERA = 0;

    //bottomsheet
    private BottomSheetBehavior<LinearLayout> sheetBehavior;
    private LinearLayout bottomSheetLayout;
    protected ImageView bottomSheetArrowImageView;
    private LinearLayout gestureLayout;
    public static BottomsheetC bs;

    private boolean mPermissionsGranted = false;

    Button barChartButton;
    Button sendLogToFirebaseButton;
    Switch loadLocalSwitch;

    private Context mAppContext;
    private TextView mBackGroundText;

    private RsContext mRsContext;

    private static final int PERMISSIONS_REQUEST_WRITE = 1;

    GraphicOverlay graphicOverlay;
    TextView fps;
    TextView msDetection;
    TextView depthResolution;
    TextView rgbResolution;
    TextView Nthread_text;
    Spinner modelML_spinner;
    Spinner distance_spinner;
    Spinner computation_spinner;

    ImageView img1;
    ImageView Nthread_plus;
    ImageView Nthread_minus;

    Button detectableObjectButton;

    Bitmap imgBM;
    Bitmap img;
    TensorImage image;

    Detector detector;
    StreamDetection stream_detection;

    static Boolean firstStartModel = true;
    static Boolean firstStartComputation = true;
    static Boolean firstStartDistance = true;

    public static HashMap<String, Float> objectDict;

    boolean jsonAvaiable = true;

    DatabaseUtils databaseUtils;

    @SuppressLint("ResourceType")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Window window = this.getWindow();
        window.setStatusBarColor(Color.parseColor("#ffaa12"));

        mAppContext = getApplicationContext();
        mBackGroundText = findViewById(R.id.connectCameraText);

        barChartButton = findViewById(R.id.barchartButton);
        loadLocalSwitch = findViewById(R.id.localImageswitch);
        sendLogToFirebaseButton = findViewById(R.id.sendLogToFirebase);
        img1 = findViewById(R.id.screen_view);
        graphicOverlay = findViewById(R.id.graphicOverlay);


        //bottomsheet
        gestureLayout = findViewById(R.id.gesture_layout);
        bottomSheetLayout = findViewById(R.id.bottom_sheet_layout);
        sheetBehavior = BottomSheetBehavior.from(bottomSheetLayout);
        bottomSheetArrowImageView = findViewById(R.id.bottom_sheet_arrow);
        //bottomsheetContent
        fps = findViewById(R.id.fpsValue);
        msDetection = findViewById(R.id.detectionTime);
        depthResolution = findViewById(R.id.depthResolution);
        rgbResolution = findViewById(R.id.rgbResolution);
        modelML_spinner = findViewById(R.id.modelML_spinner);
        distance_spinner = findViewById(R.id.distance_spinner);
        computation_spinner = findViewById(R.id.computation_spinner);
        Nthread_text = findViewById(R.id.Nthread_value);
        Nthread_plus = findViewById(R.id.Nthread_plus);
        Nthread_minus = findViewById(R.id.Nthread_minus);
        detectableObjectButton = findViewById(R.id.detectableobjectButton);

        databaseUtils = new DatabaseUtils(this);

        bs = new BottomsheetC(this,sheetBehavior, bottomSheetLayout, bottomSheetArrowImageView, gestureLayout);
        bs.setContentBottomSheet(fps,
                                msDetection,
                                depthResolution,
                                rgbResolution,
                                modelML_spinner,
                                distance_spinner,
                                computation_spinner,
                                Nthread_text,
                                Nthread_minus,
                                Nthread_plus,
                                detectableObjectButton);

        //WriteLogcat wl = new WriteLogcat();
        getModelFromFirebase();
        getObjectsListFromFirebase();
        getComputationTypeFromFirebase();

        bsListeners();

        //stream_detection = new StreamDetection(img1,graphicOverlay,distanceView,fps, msDetection, this, objectDict, databaseUtils);
        stream_detection = new StreamDetection(img1,graphicOverlay,bs, this, objectDict, databaseUtils);

        checkPermission();
        mPermissionsGranted = true;

        barChartButtonListener();
        getNThreadsFromFirebase();
        sendLogButtonListener();
        loadLocalImageButtonListener();
        detectableObjectButtonListener();
    }

    @Override
    protected void onStart() {
        super.onStart();
        startDetection();

    }

    void startDetection(){
        if (loadLocalSwitch.isChecked()) {
            graphicOverlay.clear();
            showConnectLabel(false);
            imgBM = Utils.loadBitmapFromAssets(MainActivity.this, "img/image1.jpg");
            img1.setImageBitmap(imgBM);
            image = TensorImage.fromBitmap(imgBM);

            if (bs.getComputation_spinner().getSelectedItem().toString().equals("local")) {
                detector = new Detector(MainActivity.this, graphicOverlay, objectDict, bs);
                detector.setImageToDetect(image);
                detector.startDetection();
            }
        }
    }

    public void bsListeners() {
        bs.Nthread_plus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int value = Integer.parseInt(bs.Nthread_value.getText().toString())+1;
                if(value<=9) {
                    bs.Nthread_value.setText(String.valueOf(value));
                    DatabaseUtils.writeNthreads(value);
                    startDetection();
                }
            }
        });

        bs.Nthread_minus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int value = Integer.parseInt(bs.Nthread_value.getText().toString())-1;
                if(value>0) {
                    bs.Nthread_value.setText(String.valueOf(value));
                    DatabaseUtils.writeNthreads(value);
                    startDetection();
                }
            }
        });

        bs.getModelML_spinner().setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                if (!firstStartModel) {
                    System.out.println("Selected Model for detection: " + bs.getModelML_spinner().getSelectedItem().toString());
                    DatabaseUtils.writeModel(bs.getModelML_spinner().getSelectedItem().toString());
                    startDetection();
                }
                else firstStartModel = false;

            }
            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // your code here
            }
        });

        bs.getComputation_spinner().setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                checkIfRemote();
                if (!firstStartComputation) {
                    System.out.println("Selected computation Type: " + bs.getComputation_spinner().getSelectedItem().toString());
                    DatabaseUtils.writeComputationType(bs.getComputation_spinner().getSelectedItem().toString());
                    if (position==0) startDetection();
                } else firstStartComputation = false;
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        bs.getDistance_spinner().setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!firstStartDistance) {
                    System.out.println("Selected distance Type: " + bs.getDistance_spinner().getSelectedItem().toString());
                    DatabaseUtils.writeDistanceType(bs.getDistance_spinner().getSelectedItem().toString());
                    startDetection();
                } else firstStartDistance = false;
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    void checkIfRemote(){
        if(!bs.getComputation_spinner().getSelectedItem().toString().equals("local")){
            bs.Nthread_minus.setEnabled(false);
            bs.Nthread_plus.setEnabled(false);
            bs.Nthread_value.setTextColor(Color.GRAY);
            bs.modelML_spinner.setEnabled(false);
            bs.distance_spinner.setEnabled(false);
        }else{
            bs.Nthread_minus.setEnabled(true);
            bs.Nthread_plus.setEnabled(true);
            bs.Nthread_value.setTextColor(Color.BLACK);
            bs.modelML_spinner.setEnabled(true);
            bs.distance_spinner.setEnabled(true);
        }
    }


    public static void getObjectsListFromFirebase(){
        String path = "config/objectsToDetect";
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(path);
        objectDict = new HashMap<>();
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
               objectDict = (HashMap) snapshot.getValue();
               if(snapshot.exists()) {
                   System.out.print("Data ON Firebase: ");
                   System.out.println(snapshot.getValue());
               }else System.out.println("Data ON Firebase: NULL");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    public void getComputationTypeFromFirebase(){
        String path = "config/computation";
        List<String> computation = Arrays.asList(getResources().getStringArray(R.array.computation));
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(path);
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()) {
                    bs.getComputation_spinner().setSelection(computation.indexOf(snapshot.getValue()));
                }else System.out.println("Data ON Firebase: NULL");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    public void getModelFromFirebase(){
        String path = "config/model";
        List<String> models = Arrays.asList(getResources().getStringArray(R.array.models));
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(path);
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
               bs.getModelML_spinner().setSelection(models.indexOf(snapshot.getValue()));
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

    public void getNThreadsFromFirebase(){
        String path = "config/nThreads";
        int thread_number = 4;
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(path);
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                bs.Nthread_value.setText(String.valueOf(snapshot.getValue()));
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

    void loadLocalImageButtonListener(){
        loadLocalSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    startDetection();
                }
                else {
                    graphicOverlay.clear();
                    img = Utils.loadBitmapFromAssets(MainActivity.this, "img/no_image.png");
                    img1.setImageBitmap(img);
                    showConnectLabel(true);
                }
            }
        });
    }

    void sendLogButtonListener(){
        sendLogToFirebaseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                databaseUtils.writeTooCloseDistanceLog((float) 0.3,"OggettoPROVA");
                //databaseUtils.writeNewObjectToDetect("Bottle", 0.65f);
            }
        });
    }

    void barChartButtonListener(){
        barChartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i=new Intent(MainActivity.this,BarChartActivity.class);
                startActivity(i);
            }
        });
    }

    void detectableObjectButtonListener(){
        bs.getDetectableObjectButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i=new Intent(MainActivity.this,DetectableListActivity.class);
                i.putExtra("DICT", objectDict);
                startActivity(i);
            }
        });
    }

    /*void takeObjectDict(){
        try {
            objectDict = Utils.jsonToMap(this);
        } catch (JSONException e) {
            e.printStackTrace();
            jsonAvaiable = false;
        } catch (IOException e) {
            e.printStackTrace();
            jsonAvaiable = false;
        }
    }*/

    void checkPermission(){
        /*ANDROID 9 PERMISSIONS*/
        if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.O && ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSIONS_REQUEST_CAMERA);
            return;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
            return;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSIONS_REQUEST_CAMERA);
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
            return;
        }

        mPermissionsGranted = true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(mPermissionsGranted) {
            init();
        }
        else
            Log.e(TAG, "missing permissions");
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mRsContext != null)
            mRsContext.close();
        stream_detection.stop2();
        //mColorizer.close();
        //mPipeline.close();
    }

    private void init(){
        //RsContext.init must be called once in the application lifetime before any interaction with physical RealSense devices.
        //For multi activities applications use the application context instead of the activity context
        RsContext.init(mAppContext);

        //Register to notifications regarding RealSense devices attach/detach events via the DeviceListener.
        mRsContext = new RsContext();
        mRsContext.setDevicesChangedCallback(mListener);
        try(DeviceList dl = mRsContext.queryDevices()){
            if(dl.getDeviceCount() > 0) {
                showConnectLabel(false);
                if (jsonAvaiable) {
                    stream_detection.start();
                }
                else {
                    Toast.makeText(this, "json not found", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    /** se la camera non è connessa, mostra il messaggio di richiesta collegamento **/
    private void showConnectLabel(final boolean state){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mBackGroundText.setVisibility(state ? View.VISIBLE : View.GONE);

            }
        });
    }


    /** listener collegamento device **/
    private DeviceListener mListener = new DeviceListener() {
        @Override
        public void onDeviceAttach() {
            showConnectLabel(false);
            init();
        }

        @Override
        public void onDeviceDetach() {
            showConnectLabel(true);
            stream_detection.stop2();
        }
    };

}


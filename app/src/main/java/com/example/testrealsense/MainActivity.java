package com.example.testrealsense;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;


import com.example.testrealsense.Helper.ObjectGraphics;
import com.example.testrealsense.Helper.GraphicOverlay;
import com.example.testrealsense.Helper.TextOverlay;
import com.example.testrealsense.Helper.WriteLogcat;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.mlkit.common.model.LocalModel;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.objects.DetectedObject;
import com.google.mlkit.vision.objects.ObjectDetection;
import com.google.mlkit.vision.objects.ObjectDetector;
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions;
import com.intel.realsense.librealsense.Align;
import com.intel.realsense.librealsense.Colorizer;
import com.intel.realsense.librealsense.Config;
import com.intel.realsense.librealsense.DepthFrame;
import com.intel.realsense.librealsense.DeviceList;
import com.intel.realsense.librealsense.DeviceListener;
import com.intel.realsense.librealsense.Extension;
import com.intel.realsense.librealsense.Frame;
import com.intel.realsense.librealsense.FrameSet;
import com.intel.realsense.librealsense.Pipeline;
import com.intel.realsense.librealsense.PipelineProfile;
import com.intel.realsense.librealsense.RsContext;
import com.intel.realsense.librealsense.StreamType;
import com.intel.realsense.librealsense.VideoFrame;

import org.json.JSONException;

import static com.example.testrealsense.Utils.*;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
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
    BottomsheetC bs;

    private boolean mPermissionsGranted = false;

    Button barChartButton;
    Button sendLogToFirebaseButton;
    Button loadLocalButton;

    private Context mAppContext;
    private TextView mBackGroundText;

    private RsContext mRsContext;

    private static final int PERMISSIONS_REQUEST_WRITE = 1;

    GraphicOverlay graphicOverlay;
    TextView distanceView;
    TextView fps;
    TextView msDetection;
    TextView depthResolution;
    TextView rgbResolution;
    Spinner modelML_spinner;
    Spinner distance_spinner;
    Spinner computation_spinner;

    ImageView img1;

    StreamDetection stream_detection;

    HashMap<String, Float> objectDict;

    boolean jsonAvaiable = true;

    DatabaseUtils databaseUtils;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mAppContext = getApplicationContext();
        mBackGroundText = findViewById(R.id.connectCameraText);

        barChartButton = findViewById(R.id.barchartButton);
        loadLocalButton = findViewById(R.id.localImageButton);
        sendLogToFirebaseButton = findViewById(R.id.sendLogToFirebase);
        img1 = findViewById(R.id.screen_view);
        graphicOverlay = findViewById(R.id.graphicOverlay);


        //bottomsheet
        gestureLayout = findViewById(R.id.gesture_layout);
        bottomSheetLayout = findViewById(R.id.bottom_sheet_layout);
        sheetBehavior = BottomSheetBehavior.from(bottomSheetLayout);
        bottomSheetArrowImageView = findViewById(R.id.bottom_sheet_arrow);
        //bottomsheetContent
        fps = findViewById(R.id.fps);
        msDetection = findViewById(R.id.detectionTime);
        depthResolution = findViewById(R.id.depthResolution);
        rgbResolution = findViewById(R.id.rgbResolution);
        modelML_spinner = findViewById(R.id.modelML_spinner);
        distance_spinner = findViewById(R.id.distance_spinner);
        computation_spinner = findViewById(R.id.computation_spinner);
        //distanceView = findViewById(R.id.distanceTextView);

        bs = new BottomsheetC(sheetBehavior, bottomSheetLayout, bottomSheetArrowImageView, gestureLayout);
        bs.setContentBottomSheet(fps,msDetection,depthResolution,rgbResolution, modelML_spinner, distance_spinner, computation_spinner);

        bs.getModelML_spinner().setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                System.out.println(position);
                //mBackGroundText.setText(bs.getModelML_spinner().getSelectedItem().toString());

            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // your code here
            }

        });

        //WriteLogcat wl = new WriteLogcat();
        takeObjectDict();

        databaseUtils = new DatabaseUtils(this);

        //stream_detection = new StreamDetection(img1,graphicOverlay,distanceView,fps, msDetection, this, objectDict, databaseUtils);
        stream_detection = new StreamDetection(img1,graphicOverlay,bs, this, objectDict, databaseUtils);

        checkPermission();
        mPermissionsGranted = true;

        barChartButtonListener();
        sendLogButtonListener();
        loadLocalImageButtonListener();
    }

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

    void loadLocalImageButtonListener(){
        loadLocalButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Bitmap imgBM = Utils.loadBitmapFromAssets(MainActivity.this, "img/image1.jpg");
                img1.setImageBitmap(imgBM);
            }
        });
    }

    void sendLogButtonListener(){
        sendLogToFirebaseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                databaseUtils.writeTooCloseDistanceLog((float) 0.3,"OggettoPROVA");
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

    void takeObjectDict(){
        /** prelievo  oggetti e distanze critiche da file json **/
        try {
            objectDict = Utils.jsonToMap(this);
        } catch (JSONException e) {
            e.printStackTrace();
            jsonAvaiable = false;
        } catch (IOException e) {
            e.printStackTrace();
            jsonAvaiable = false;
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


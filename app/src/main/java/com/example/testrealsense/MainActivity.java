package com.example.testrealsense;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;


import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
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
import com.intel.realsense.librealsense.GLRsSurfaceView;
import com.intel.realsense.librealsense.Pipeline;
import com.intel.realsense.librealsense.PipelineProfile;
import com.intel.realsense.librealsense.RsContext;
import com.intel.realsense.librealsense.StreamFormat;
import com.intel.realsense.librealsense.StreamType;
import com.intel.realsense.librealsense.VideoFrame;

import static com.example.testrealsense.ImageUtils.*;
import static com.example.testrealsense.DrawView.*;


import java.text.DecimalFormat;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "librs capture example";
    private static final int PERMISSIONS_REQUEST_CAMERA = 0;

    private boolean mPermissionsGranted = false;


    private Context mAppContext;
    private TextView mBackGroundText;
    private GLRsSurfaceView mGLSurfaceView;
    private GLRsSurfaceView mGLSurfaceViewDepth;
    private boolean mIsStreaming = false;
    private final Handler mHandler = new Handler();

    private Pipeline mPipeline;
    private Colorizer mColorizer;
    private RsContext mRsContext;

    private Bitmap realsenseBM;

    private Align mAlign = new Align(StreamType.DEPTH);


    DrawView drawView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        mAppContext = getApplicationContext();
        mBackGroundText = findViewById(R.id.connectCameraText);
        mGLSurfaceView = findViewById(R.id.glSurfaceView);
        mGLSurfaceView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

        mGLSurfaceViewDepth = findViewById(R.id.glSurfaceViewDepth);
        mGLSurfaceViewDepth.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

        // Android 9 also requires camera permissions
        if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.O &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSIONS_REQUEST_CAMERA);
            return;
        }

        mPermissionsGranted = true;

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSIONS_REQUEST_CAMERA);
            return;
        }
        mPermissionsGranted = true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mGLSurfaceView.close();
        mGLSurfaceViewDepth.close();
        mPipeline.close();
        mColorizer.close();
    }


    @Override
    protected void onResume() {
        super.onResume();
        if(mPermissionsGranted)
            init();
        else
            Log.e(TAG, "missing permissions");
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mRsContext != null)
            mRsContext.close();
        stop();
        mColorizer.close();
        mPipeline.close();
    }

    private void init(){
        //RsContext.init must be called once in the application lifetime before any interaction with physical RealSense devices.
        //For multi activities applications use the application context instead of the activity context
        RsContext.init(mAppContext);

        //Register to notifications regarding RealSense devices attach/detach events via the DeviceListener.
        mRsContext = new RsContext();
        mRsContext.setDevicesChangedCallback(mListener);

        mPipeline = new Pipeline();
        mColorizer = new Colorizer();

        try(DeviceList dl = mRsContext.queryDevices()){
            if(dl.getDeviceCount() > 0) {
                showConnectLabel(false);
                start();
            }
        }
    }

    private void showConnectLabel(final boolean state){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mBackGroundText.setVisibility(state ? View.VISIBLE : View.GONE);
            }
        });
    }

    private DeviceListener mListener = new DeviceListener() {
        @Override
        public void onDeviceAttach() {
            showConnectLabel(false);
        }

        @Override
        public void onDeviceDetach() {
            showConnectLabel(true);
            stop();
        }
    };

    Runnable mStreaming = new Runnable() {
        final DecimalFormat df = new DecimalFormat("#.##");
        @Override
        public void run() {
            try {
                try (FrameSet frames = mPipeline.waitForFrames()) {
                    try (FrameSet p = frames.applyFilter(mAlign)) {

                        try (Frame d = p.first(StreamType.DEPTH)) {
                            DepthFrame depth = d.as(Extension.DEPTH_FRAME);
                            //do something with depth
                            final float deptValue = depth.getDistance(depth.getWidth() / 2, depth.getHeight() / 2);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    TextView textView = findViewById(R.id.distanceTextView);
                                    textView.setText("Distance: " + df.format(deptValue));
                                }
                            });


                        }
                        try (Frame c = p.first(StreamType.COLOR)) {
                            VideoFrame color = c.as(Extension.VIDEO_FRAME);
                            //do something with color
                            int c_size = color.getDataSize();
                            int c_height = color.getHeight();
                            int c_width = color.getWidth();
                            byte[] c_data = new byte[c_size];
                            color.getData(c_data);
                            final int len = c_data.length;
                            if (c_data.length != 0) {
                                realsenseBM = rgb2Bitmap(c_data, c_width, c_height);
                                LocalModel localModel =
                                        new LocalModel.Builder()
                                                .setAssetFilePath("model.tflite")
                                                .build();
                                InputImage image = InputImage.fromBitmap(realsenseBM, 0);
                                // saveBitmap(realsenseBM,"realsense.png");
                                CustomObjectDetectorOptions customObjectDetectorOptions =
                                        new CustomObjectDetectorOptions.Builder(localModel)
                                                .setDetectorMode(CustomObjectDetectorOptions.SINGLE_IMAGE_MODE)
                                                .enableClassification()
                                                .setClassificationConfidenceThreshold(0.5f)
                                                .setMaxPerObjectLabelCount(3)
                                                .build();

                                ObjectDetector objectDetector = ObjectDetection.getClient(customObjectDetectorOptions);
                                objectDetector
                                        .process(image)
                                        .addOnSuccessListener(
                                                new OnSuccessListener<List<DetectedObject>>() {
                                                    @Override
                                                    public void onSuccess(List<DetectedObject> detectedObjects) {
                                                        for (DetectedObject detectedObject : detectedObjects) {
                                                            Rect boundingBox = detectedObject.getBoundingBox();
                                                            Integer trackingId = detectedObject.getTrackingId();
                                                            for (DetectedObject.Label label : detectedObject.getLabels()) {
                                                                String text = label.getText();
                                                                TextView textView = findViewById(R.id.labelTextView);
                                                                textView.setText(text);


                                                            }
                                                        }

                                                    }
                                                })
                                        .addOnFailureListener(
                                                new OnFailureListener() {
                                                    @Override
                                                    public void onFailure(@NonNull Exception e) {
                                                        TextView textView = findViewById(R.id.labelTextView);
                                                        textView.setText(e.getMessage());
                                                    }
                                                });


                                //mySurfaceView.setBitmap(realsenseBM);
                                //  detectedObject.getLabels().get(0).getText()

                                Log.d(TAG, "onCaptureData: " + c_data.length);
                                Log.d(TAG, "transform byte to bitmap successfully\n");
                            }
                            mGLSurfaceView.upload(c);
                        }

                    }
                }
                mHandler.post(mStreaming);
                } catch (Exception e) {
                    Log.e(TAG, "streaming, error: " + e.getMessage());
                }

            };
            /*try {
                try (FrameSet frames = mPipeline.waitForFrames()) {
                    try (Frame f = frames.first(StreamType.COLOR)){
                        VideoFrame color = f.as(Extension.VIDEO_FRAME);
                        int c_size= color.getDataSize();
                        int c_height = color.getHeight();
                        int c_width = color.getWidth();
                        byte[] c_data = new  byte[c_size];
                        color.getData(c_data);
                        final int len = c_data.length;
                        if(c_data.length !=0) {
                            realsenseBM = rgb2Bitmap(c_data,c_width,c_height);
                            LocalModel localModel =
                                    new LocalModel.Builder()
                                            .setAssetFilePath("model.tflite")
                                            .build();
                            InputImage image = InputImage.fromBitmap(realsenseBM,0);
                            // saveBitmap(realsenseBM,"realsense.png");
                            CustomObjectDetectorOptions customObjectDetectorOptions =
                                    new CustomObjectDetectorOptions.Builder(localModel)
                                            .setDetectorMode(CustomObjectDetectorOptions.SINGLE_IMAGE_MODE)
                                            .enableClassification()
                                            .setClassificationConfidenceThreshold(0.5f)
                                            .setMaxPerObjectLabelCount(3)
                                            .build();

                            ObjectDetector objectDetector =  ObjectDetection.getClient(customObjectDetectorOptions);
                            objectDetector
                                    .process(image)
                                    .addOnSuccessListener(
                                            new OnSuccessListener<List<DetectedObject>>() {
                                                @Override
                                                public void onSuccess(List<DetectedObject> detectedObjects) {
                                                    for (DetectedObject detectedObject : detectedObjects) {
                                                        Rect boundingBox = detectedObject.getBoundingBox();
                                                        Integer trackingId = detectedObject.getTrackingId();
                                                        for (DetectedObject.Label label : detectedObject.getLabels()) {
                                                            String text = label.getText();
                                                            TextView textView = findViewById(R.id.labelTextView);
                                                            textView.setText(text);
                                                        }
                                                    }

                                                }
                                            })
                                    .addOnFailureListener(
                                            new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    TextView textView = findViewById(R.id.labelTextView);
                                                    textView.setText(e.getMessage());
                                                }
                                            });


                            //mySurfaceView.setBitmap(realsenseBM);
                            //  detectedObject.getLabels().get(0).getText()

                            Log.d(TAG, "onCaptureData: " + c_data.length);
                            Log.d(TAG,"transform byte to bitmap successfully\n");

                        }else{
                            Log.d(TAG,"fail to load color data\n");
                        }
                    }
                    try (Frame f = frames.first(StreamType.DEPTH))
                    {
                        DepthFrame depth = f.as(Extension.DEPTH_FRAME);
                        final float deptValue = depth.getDistance(depth.getWidth()/2, depth.getHeight()/2);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                TextView textView = findViewById(R.id.distanceTextView);
                                textView.setText("Distance: " + df.format(deptValue));
                            }
                        });
                    }
                    try (FrameSet processed = frames.applyFilter(mColorizer)) {
                        mGLSurfaceView.upload(processed);
                    }
                }
                mHandler.post(mStreaming);
            }
            catch (Exception e) {
                Log.e(TAG, "streaming, error: " + e.getMessage());
            }
        }*/
    };


    private void configAndStart() throws Exception {
        try(Config config  = new Config())
        {
            config.enableStream(StreamType.DEPTH, 640, 480);
            config.enableStream(StreamType.COLOR,640, 480);
            // try statement needed here to release resources allocated by the Pipeline:start() method
            try(PipelineProfile pp = mPipeline.start(config)){}
        }
    }

    private synchronized void start() {
        if(mIsStreaming)
            return;
        try{
            Log.d(TAG, "try start streaming");
            mGLSurfaceView.clear();
            mGLSurfaceViewDepth.clear();
            configAndStart();
            mIsStreaming = true;
            mHandler.post(mStreaming);
            Log.d(TAG, "streaming started successfully");
        } catch (Exception e) {
            Log.d(TAG, "failed to start streaming");
        }
    }

    private synchronized void stop() {
        if(!mIsStreaming)
            return;
        try {
            Log.d(TAG, "try stop streaming");
            mIsStreaming = false;
            mHandler.removeCallbacks(mStreaming);
            mPipeline.stop();
            mGLSurfaceView.clear();
            mGLSurfaceViewDepth.clear();
            Log.d(TAG, "streaming stopped successfully");
        } catch (Exception e) {
            mPipeline = null;
            mColorizer.close();
            Log.d(TAG, "failed to stop streaming");
        }
    }
}

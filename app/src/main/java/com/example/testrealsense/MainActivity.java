package com.example.testrealsense;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import com.example.testrealsense.Helper.ObjectGraphics;
import com.example.testrealsense.Helper.GraphicOverlay;
import com.example.testrealsense.Helper.*;
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
import com.intel.realsense.librealsense.Pipeline;
import com.intel.realsense.librealsense.PipelineProfile;
import com.intel.realsense.librealsense.RsContext;
import com.intel.realsense.librealsense.StreamType;
import com.intel.realsense.librealsense.VideoFrame;

import static com.example.testrealsense.ImageUtils.*;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

interface Callback {

    void myResponseCallback(List<DetectedObject> detectedObjects);


}
/*
class CallbackImpl implements Callback {
    @Override
    public void myResponseCallback(List<DetectedObject> detectedObjects) {

    }
}*/



public class MainActivity extends AppCompatActivity {
    private static final String TAG = "librs capture example";
    private static final int PERMISSIONS_REQUEST_CAMERA = 0;

    private boolean mPermissionsGranted = false;
    //MyCallBack callBack;

    private Context mAppContext;
    private TextView mBackGroundText;
    //private GLRsSurfaceView mGLSurfaceView;
    //private GLRsSurfaceView mGLSurfaceViewDepth;
    private boolean mIsStreaming = false;
    private final Handler mHandler = new Handler();

    private Pipeline mPipeline;
    private Colorizer mColorizer;
    private RsContext mRsContext;

    private static final int PERMISSIONS_REQUEST_WRITE = 1;

    private Align mAlign = new Align(StreamType.COLOR);

    private Bitmap realsenseBM;
    private Bitmap realsenseBMD;

    private MySurfaceView mySurfaceView;
    private MySurfaceView mySurfaceViewDepth;

    GraphicOverlay graphicOverlay;
    TextView distanceView;

    LocalModel localModel;

    ImageView img1;
/*
    long currentTime;
    long previousTime;
    long deltaTime;
    long aproxFps;*/


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        WriteLogcat log = new WriteLogcat();

        img1 = findViewById(R.id.screen_view);

        graphicOverlay = findViewById(R.id.graphicOverlay);

        distanceView = findViewById(R.id.distanceTextView);

        mAppContext = getApplicationContext();
        mBackGroundText = findViewById(R.id.connectCameraText);

       /* mySurfaceView = new MySurfaceView(this, findViewById(R.id.screen_view));
        mySurfaceView.setAspectRatio(640, 480);
        Bitmap mBitmap = Bitmap.createBitmap(640, 480, Bitmap.Config.ARGB_8888);
        mySurfaceView.setBitmap(mBitmap);
        mySurfaceView.run();

        mySurfaceViewDepth = new MySurfaceView(this, findViewById(R.id.screenD_view));
        mySurfaceViewDepth.setAspectRatio(640, 480);
        mySurfaceViewDepth.setBitmap(mBitmap);
        mySurfaceViewDepth.run();*/

        // Android 9 also requires camera permissions
        if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.O &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSIONS_REQUEST_CAMERA);
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
            return;
        }

        mPermissionsGranted = true;

        localModel = new LocalModel.Builder()
                        .setAssetFilePath("model.tflite")
                        .build();



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
        //mGLSurfaceView.close();
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
            }else {
                runWithoutCamera();
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

    private Bitmap loadBitmapFromAssets(String path) {
        showConnectLabel(false);
        InputStream bitmap = null;
        try {
            bitmap=getAssets().open(path);
            Bitmap bit= BitmapFactory.decodeStream(bitmap);
            return bit;
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        return null;
    }


    private void runWithoutCamera(){
        Bitmap bitmap = loadBitmapFromAssets("multi2.jpg");

        if( bitmap.getWidth() == 640 && bitmap.getHeight() == 480 ){
            Toast.makeText(this, "bitmap with right dimensions: " + bitmap.getWidth() + "x" + bitmap.getHeight(), Toast.LENGTH_SHORT).show();
            InputImage image = InputImage.fromBitmap(bitmap, 0);
            CustomObjectDetectorOptions customObjectDetectorOptions =
                    new CustomObjectDetectorOptions.Builder(localModel)
                            .setDetectorMode(CustomObjectDetectorOptions.SINGLE_IMAGE_MODE)
                            .enableMultipleObjects()
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
                                    graphicOverlay.clear();
                                    if (detectedObjects.size() > 0) {

                                        for (DetectedObject detectedObject : detectedObjects) {
                                            ObjectGraphics drawBoundingBoxLabel = new ObjectGraphics(detectedObject, graphicOverlay, image.getWidth(),0);
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

        }else
            Toast.makeText(this, "WRONG INPUT DIMENSIONS", Toast.LENGTH_SHORT).show();

        //mySurfaceView.setBitmap(bitmap);
        img1.setImageBitmap(bitmap);
    }

    public void getDepth(final Callback callBack) {
    }


    Runnable mStreaming = new Runnable() {
        int count = 0;
        final DecimalFormat df = new DecimalFormat("#.##");

        @Override
        public void run() {
            try {
                try (FrameSet frames = mPipeline.waitForFrames()) {
                    try (FrameSet processed = frames.applyFilter(mAlign)) { // align qua
                        try (Frame depthFrame = processed.first(StreamType.DEPTH)) {
                            try (Frame colorFrame = processed.first(StreamType.COLOR)) {

                                VideoFrame videoFrame = colorFrame.as(Extension.VIDEO_FRAME);
                                DepthFrame depth = depthFrame.as(Extension.DEPTH_FRAME);
                                DepthFrame depthColorized = depthFrame.as(Extension.DEPTH_FRAME); // colorizer qua

                                /** CALLBACK **/
                                //callback.myResponseCallback(depth);

                                int c_size = videoFrame.getDataSize();
                                int c_height = videoFrame.getHeight();
                                int c_width = videoFrame.getWidth();
                                byte[] c_data = new byte[c_size];
                                videoFrame.getData(c_data);

                                /*int d_size = depthColorized.getDataSize();
                                int d_height = depthColorized.getHeight();
                                int d_width = depthColorized.getWidth();
                                byte[] d_data = new byte[d_size];
                                depthColorized.getData(d_data);*/
                                //ImageView img2 = findViewById(R.id.screenD_view);


                                if (c_data.length != 0 ) {
                                    //Bitmap realsenseBMD = loadBitmapFromAssets();
                                    realsenseBM = rgb2Bitmap(c_data, c_width, c_height);
                                    InputImage image = InputImage.fromBitmap(realsenseBM, 0);
                                    CustomObjectDetectorOptions customObjectDetectorOptions =
                                            new CustomObjectDetectorOptions.Builder(localModel)
                                                    .setDetectorMode(CustomObjectDetectorOptions.SINGLE_IMAGE_MODE)
                                                    .enableMultipleObjects()
                                                    .enableClassification()
                                                    .setClassificationConfidenceThreshold(0.5f)
                                                    .setMaxPerObjectLabelCount(1)
                                                    .build();



                                    Callback callback = new Callback() {
                                        @Override
                                        public void myResponseCallback(List<DetectedObject> detectedObjects) {
                                            float depthValue = 0;
                                            for (DetectedObject detectedObject : detectedObjects) {
                                                try  {
                                                    depthValue = depth.getDistance(detectedObject.getBoundingBox().centerX(), detectedObject.getBoundingBox().centerY());

                                                }catch (Exception e) {
                                                }
                                                ObjectGraphics drawBoundingBoxLabel = new ObjectGraphics(detectedObject, graphicOverlay, image.getWidth(), depthValue);
                                                drawBoundingBoxLabel.drawBoundingBoxAndLabel();
                                            }
                                        }
                                    };

                                    if (count % 3 == 0) {

                                        ObjectDetector objectDetector = ObjectDetection.getClient(customObjectDetectorOptions);
                                        objectDetector
                                                .process(image)
                                                .addOnSuccessListener(
                                                        new OnSuccessListener<List<DetectedObject>>() {
                                                            @Override
                                                            public void onSuccess(List<DetectedObject> detectedObjects) {
                                                                graphicOverlay.clear();
                                                                if (detectedObjects.size() > 0) {

                                                                    callback.myResponseCallback(detectedObjects);
                                                                    /*for (DetectedObject detectedObject : detectedObjects) {
                                                                        float depthValue = 0;
                                                                        try  {
                                                                            depthValue = depth.getDistance(detectedObject.getBoundingBox().centerX(), detectedObject.getBoundingBox().centerY());

                                                                        }catch (Exception e) {
                                                                        }
                                                                        ObjectGraphics drawBoundingBoxLabel = new ObjectGraphics(detectedObject, graphicOverlay, image.getWidth(), depthValue);
                                                                        drawBoundingBoxLabel.drawBoundingBoxAndLabel();
                                                                    }*/
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

                                        try {
                                            float depthValue2 = depth.getDistance(depth.getWidth() / 2, depth.getHeight() / 2);
                                            distanceView.setText("distance: " + String.valueOf(depthValue2));

                                        } catch (Exception e) {
                                            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                                        }

                                        //getDepth(callback);

                                        /*if (depth.getDataSize() != 0) {
                                            Toast.makeText(getApplicationContext(), depth.getDataSize(), Toast.LENGTH_SHORT).show();
                                        }
                                        else {
                                            Toast.makeText(getApplicationContext(), "null", Toast.LENGTH_SHORT).show();
                                        }*/

                                    }

                                    //mySurfaceView.setBitmap(realsenseBM);
                                    //mySurfaceViewDepth.setBitmap(realsenseBMD);

                                    img1.setImageBitmap(realsenseBM);
                                    //img2.setImageBitmap(realsenseBMD);
                                    count++;
                                }
                            }
                        }
                    }
                }
                mHandler.post(mStreaming);
            }
            catch (Exception e) {
                Log.e(TAG, "streaming, error: " + e.getMessage());
            }
        }
    };

    private void configAndStart() throws Exception {
        try(Config config  = new Config())
        {
            config.enableStream(StreamType.DEPTH,640, 480);
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
            //mGLSurfaceView.clear();
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
           // mGLSurfaceView.clear();
            Log.d(TAG, "streaming stopped successfully");
        } catch (Exception e) {
            Log.d(TAG, "failed to stop streaming");
        }
    }
}

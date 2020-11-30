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
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import com.example.testrealsense.Helper.ObjectGraphics;
import com.example.testrealsense.Helper.GraphicOverlay;
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
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;


public class MainActivity extends AppCompatActivity implements  OnSuccessListener<List<DetectedObject>> {
    private static final String TAG = "librs capture example";
    private static final int PERMISSIONS_REQUEST_CAMERA = 0;

    private boolean mPermissionsGranted = false;

    private Context mAppContext;
    private TextView mBackGroundText;
    private boolean mIsStreaming = false;
    private final Handler mHandler = new Handler();

    private Pipeline mPipeline;
    private Colorizer mColorizer;
    private RsContext mRsContext;

    private static final int PERMISSIONS_REQUEST_WRITE = 1;

    private Align mAlign = new Align(StreamType.COLOR);

    private Bitmap realsenseBM;
    private Bitmap realsenseBMD;

    GraphicOverlay graphicOverlay;
    TextView distanceView;
    TextView fps;

    LocalModel localModel;

    ImageView img1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        img1 = findViewById(R.id.screen_view);

        graphicOverlay = findViewById(R.id.graphicOverlay);

        distanceView = findViewById(R.id.distanceTextView);
        fps = findViewById(R.id.fpsTextView);

        mAppContext = getApplicationContext();
        mBackGroundText = findViewById(R.id.connectCameraText);

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
                        .setAssetFilePath("lite-model_object_detection_mobile_object_labeler_v1_1.tflite")
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
            init();
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
                                    System.out.println(e.getMessage());
                                }
                            });

        }else
            Toast.makeText(this, "WRONG INPUT DIMENSIONS", Toast.LENGTH_SHORT).show();

        img1.setImageBitmap(bitmap);
    }

    @Override
    public void onSuccess(List<DetectedObject> detectedObjects) {
        /*graphicOverlay.clear();
        if (detectedObjects.size() > 0) {
            System.out.println("Depth Listener: " + depth.getDataSize());
            for (DetectedObject detectedObject : detectedObjects) {
                float depthValue = 0;
                try  {
                    depthValue = depth.getDistance(detectedObject.getBoundingBox().centerX(), detectedObject.getBoundingBox().centerY());

                }catch (Exception e) {
                }
                ObjectGraphics drawBoundingBoxLabel = new ObjectGraphics(detectedObject, graphicOverlay, image.getWidth(), depthValue);
                drawBoundingBoxLabel.drawBoundingBoxAndLabel();
            }
            //printFPS();
        }*/
    }

    Runnable mStreaming = new Runnable() {
        int count = 0;
        final DecimalFormat df = new DecimalFormat("#.##");
        //MyHandler handler = new MyHandler();
        long currentTime;
        long previousTime = 0;
        long deltaTime = 0;
        long aproxFps = 0;

        List<DetectedObject> detectedObjects_;

        void printFPS() {
            currentTime = Calendar.getInstance().getTimeInMillis();
            deltaTime = currentTime - previousTime;
            aproxFps = 1000/deltaTime;
            previousTime = currentTime;
            fps.setText("FPS detection: " + String.valueOf(aproxFps));
        }




        @Override
        public void run() {
            try {
                try (FrameSet frames = mPipeline.waitForFrames()) {
                    try (FrameSet processed = frames.applyFilter(mAlign)) { // align qua
                        try (Frame depthFrame = processed.first(StreamType.DEPTH)) {
                            try (Frame colorFrame = processed.first(StreamType.COLOR)) {

                                VideoFrame videoFrame = colorFrame.as(Extension.VIDEO_FRAME);
                                //DepthFrame depthColorized = depthFrame.as(Extension.DEPTH_FRAME); // colorizer qua

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

                                    if (count % 2 == 0) {
                                        DepthFrame depth = depthFrame.as(Extension.DEPTH_FRAME);
                                        System.out.println("Depth: " +depth.getDataSize());
                                        ObjectDetector objectDetector = ObjectDetection.getClient(customObjectDetectorOptions);
                                        objectDetector
                                                .process(image)
                                                .addOnSuccessListener(MainActivity.this::onSuccess)
                                                .addOnFailureListener(
                                                        new OnFailureListener() {
                                                            @Override
                                                            public void onFailure(@NonNull Exception e) {
                                                                System.out.println(e.getMessage());
                                                            }
                                                        });

                                        try {
                                            float depthValue2 = depth.getDistance(depth.getWidth() / 2, depth.getHeight() / 2);
                                            distanceView.setText("distance from center: " + String.valueOf(depthValue2));

                                        } catch (Exception e) {
                                            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                                        }


                                    }
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
            Log.d(TAG, "streaming stopped successfully");
        } catch (Exception e) {
            Log.d(TAG, "failed to stop streaming");
        }
    }
}


package com.example.testrealsense;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;



import com.example.testrealsense.Helper.GraphicOverlay;
import com.example.testrealsense.Helper.RectOverlay;
import com.example.testrealsense.Helper.TextOverlay;
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



import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "librs capture example";
    private static final int PERMISSIONS_REQUEST_CAMERA = 0;

    private boolean mPermissionsGranted = false;

    private Context mAppContext;
    private TextView mBackGroundText;
    //private GLRsSurfaceView mGLSurfaceView;
    //private GLRsSurfaceView mGLSurfaceViewDepth;
    private boolean mIsStreaming = false;
    private final Handler mHandler = new Handler();

    private Pipeline mPipeline;
    private Colorizer mColorizer;
    private RsContext mRsContext;

    private Align mAlign = new Align(StreamType.COLOR);

    private Bitmap realsenseBM;

    private MySurfaceView mySurfaceView;

    GraphicOverlay graphicOverlay;

    LocalModel localModel;

/*
    long currentTime;
    long previousTime;
    long deltaTime;
    long aproxFps;*/


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //previousTime = 0;

        graphicOverlay = findViewById(R.id.graphicOverlay);

        mAppContext = getApplicationContext();
        mBackGroundText = findViewById(R.id.connectCameraText);

        mySurfaceView = new MySurfaceView(this);
        mySurfaceView.setAspectRatio(640, 480);
        Bitmap mBitmap = Bitmap.createBitmap(640, 480, Bitmap.Config.ARGB_8888);
        mySurfaceView.setBitmap(mBitmap);
        mySurfaceView.run();

        // Android 9 also requires camera permissions
        if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.O &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSIONS_REQUEST_CAMERA);
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

    private float translateX(float x, float scaleX, float offsetX) {
        return (x * scaleX) - offsetX;
    };

    private float translateY(float y, float scaleY, float offsetY) {
        return (y * scaleY) - offsetY;
    };

    Runnable mStreaming = new Runnable() {
        int count = 0;
        final DecimalFormat df = new DecimalFormat("#.##");

        @Override
        public void run() {
            try {
                try (FrameSet frames = mPipeline.waitForFrames()) {
                    try (FrameSet processed = frames.applyFilter(mAlign)) {
                        try (Frame depthFrame = processed.first(StreamType.DEPTH)) {
                            try (Frame colorFrame = processed.first(StreamType.COLOR)) {

                                VideoFrame videoFrame = colorFrame.as(Extension.VIDEO_FRAME);
                                DepthFrame depth = depthFrame.as(Extension.DEPTH_FRAME);

                                int c_size = videoFrame.getDataSize();
                                int c_height = videoFrame.getHeight();
                                int c_width = videoFrame.getWidth();
                                byte[] c_data = new byte[c_size];
                                videoFrame.getData(c_data);

                                if (c_data.length != 0) {
                                    realsenseBM = rgb2Bitmap(c_data, c_width, c_height);
                                    InputImage image = InputImage.fromBitmap(realsenseBM, 0);
                                    CustomObjectDetectorOptions customObjectDetectorOptions =
                                            new CustomObjectDetectorOptions.Builder(localModel)
                                                    .setDetectorMode(CustomObjectDetectorOptions.SINGLE_IMAGE_MODE)
                                                    .enableMultipleObjects()
                                                    .enableClassification()
                                                    .setClassificationConfidenceThreshold(0.5f)
                                                    .setMaxPerObjectLabelCount(3)
                                                    .build();
                                    if (count % 3 == 0) {
                                        List<Rect> objectsPositions = new ArrayList<Rect>();

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
                                                                        //Determina punti traslati BoundingBox

                                                                        float scaleX = (float) graphicOverlay.getHeight() / image.getWidth();
                                                                        float scaleY = (float) graphicOverlay.getWidth() / image.getHeight();
                                                                        float offsetX = 0;
                                                                        float offsetY = 0;
                                                                        float pre_left = translateX(detectedObject.getBoundingBox().left, scaleX, offsetX);
                                                                        float pre_top = translateY(detectedObject.getBoundingBox().top, scaleY, offsetY);
                                                                        float pre_right = translateX(detectedObject.getBoundingBox().right, scaleX, offsetX);
                                                                        float pre_bottom = translateY(detectedObject.getBoundingBox().bottom, scaleY, offsetY);

                                                                        RectF boundingBox = new RectF(pre_left, pre_top, pre_right, pre_bottom);

                                                                        //Integer trackingId = detectedObject.getTrackingId();
                                                                        RectOverlay rectOverlay = new RectOverlay(graphicOverlay, boundingBox);
                                                                        graphicOverlay.add(rectOverlay);

                                                                        objectsPositions.add(detectedObject.getBoundingBox());

                                                                        /** DA SISTEMARE **/
                                                                        for (DetectedObject.Label l : detectedObject.getLabels()) {
                                                                            String objectName = l.getText();
                                                                            System.out.println("----Oggetto riconosciuto: " + objectName + "----");
                                                                            TextOverlay textOverlay = new TextOverlay(graphicOverlay, objectName, pre_left, pre_bottom);
                                                                            graphicOverlay.add(textOverlay);
                                                                        }
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

                                        float deptValue2 = depth.getDistance(depth.getWidth() / 2, depth.getHeight() / 2);
                                        TextView textView = findViewById(R.id.distanceTextView);
                                        textView.setText("Distance: " + df.format(deptValue2));
                                        /*if (objectsPositions.size() >0) {
                                            Toast.makeText(getApplicationContext(), "CIAO", Toast.LENGTH_LONG).show();
                                            for (Rect rect : objectsPositions) {
                                                float scaleX = (float) graphicOverlay.getHeight() / image.getWidth();
                                                float scaleY = (float) graphicOverlay.getWidth() / image.getHeight();
                                                float offsetX = rect.width() ;
                                                float offsetY = rect.height() / 2.1f;
                                                float pre_left = translateX(rect.centerX(), scaleX, offsetX);
                                                float pre_top = translateY(rect.centerY(), scaleY, offsetY);

                                                float deptValue = depth.getDistance((int)pre_left, (int)pre_top);
                                                TextOverlay depthOverlay = new TextOverlay(graphicOverlay, df.format(deptValue), pre_left, pre_top);
                                                graphicOverlay.add(depthOverlay);

                                            }

                                        }*/
                                    }

                                    mySurfaceView.setBitmap(realsenseBM);
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

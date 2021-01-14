package com.example.testrealsense;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.testrealsense.Helper.GraphicOverlay;
import com.example.testrealsense.Helper.ObjectGraphics;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
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
import com.intel.realsense.librealsense.Extension;
import com.intel.realsense.librealsense.Frame;
import com.intel.realsense.librealsense.FrameSet;
import com.intel.realsense.librealsense.Pipeline;
import com.intel.realsense.librealsense.PipelineProfile;
import com.intel.realsense.librealsense.RsContext;
import com.intel.realsense.librealsense.StreamType;
import com.intel.realsense.librealsense.VideoFrame;

import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;

import static com.example.testrealsense.Utils.rgb2Bitmap;


public class StreamDetection extends Thread{
    private static final String TAG = "librs capture example";
    private int count = 0;
    private final DecimalFormat df = new DecimalFormat("#.##");

    private boolean mIsStreaming = false;
    private final Handler mHandler = new Handler();

    private Pipeline mPipeline;
    private Colorizer mColorizer;
    private RsContext mRsContext;
    private Align mAlign = new Align(StreamType.COLOR);

    private LocalModel localModel;
    ObjectDetector objectDetector;

    private Bitmap realsenseBM;
    private ImageView img1;
    InputImage image;

    private TextView distanceView;
    GraphicOverlay graphicOverlay;
    ObjectGraphics drawBoundingBoxLabel;
    TextView fps;

    DepthFrame depth;
    float depthValue;
    VideoFrame videoFrame;

    int c_size;
    int c_height;
    int c_width;
    byte[] c_data;

    long currentTime;
    long previousTime = 0;
    long deltaTime = 0;
    long aproxFps = 0;

    static Context context;

    final MediaPlayer mp;

    HashMap<String, Float> objectDict;

    CustomObjectDetectorOptions customObjectDetectorOptions;



    public StreamDetection(ImageView img1, GraphicOverlay graphicOverlay, TextView distanceView, TextView fps, Context context, HashMap<String, Float> objectDict) {
        localModel = new LocalModel.Builder()
                .setAssetFilePath("lite-model_object_detection_mobile_object_labeler_v1_1.tflite")
                .build();
        customObjectDetectorOptions =
                new CustomObjectDetectorOptions.Builder(localModel)
                        .setDetectorMode(CustomObjectDetectorOptions.SINGLE_IMAGE_MODE)
                        .enableMultipleObjects()
                        .enableClassification()
                        .setClassificationConfidenceThreshold(0.5f)
                        .setMaxPerObjectLabelCount(1)
                        .build();

        this.img1 = img1;
        this.graphicOverlay = graphicOverlay;
        this.distanceView = distanceView;
        this.fps = fps;
        this.context = context;
        this.objectDict = objectDict;

        mPipeline = new Pipeline();
        mColorizer = new Colorizer();


        //objectDict = new HashMap<String, Float>();
        //objectDict.put("Person", (float) 0.7);


        mp = MediaPlayer.create( context, R.raw.alert_attention);
    }

    @Override
    public void run() {
        super.run();
        try {
            try (FrameSet frames = mPipeline.waitForFrames()) {
                try (FrameSet processed = frames.applyFilter(mAlign)) {
                    try ( Frame depthFrame = processed.first(StreamType.DEPTH)) {
                        try (Frame colorFrame = processed.first(StreamType.COLOR)) {

                            videoFrame = colorFrame.as(Extension.VIDEO_FRAME);

                            c_size = videoFrame.getDataSize();
                            c_height = videoFrame.getHeight();
                            c_width = videoFrame.getWidth();
                            c_data = new byte[c_size];
                            videoFrame.getData(c_data);


                            if (c_data.length != 0 ) {
                                //Bitmap realsenseBMD = loadBitmapFromAssets();
                                realsenseBM = rgb2Bitmap(c_data, c_width, c_height);
                                image = InputImage.fromBitmap(realsenseBM, 0);

                                objectDetector = ObjectDetection.getClient(customObjectDetectorOptions);
                                depth = depthFrame.clone().as(Extension.DEPTH_FRAME);

                                objectDetector.process(image).addOnCompleteListener(new OnCompleteListener<List<DetectedObject>>() {
                                    @Override
                                    public void onComplete(@NonNull Task<List<DetectedObject>> task) {
                                        graphicOverlay.clear();
                                        List<DetectedObject> detectedObjects = task.getResult();
                                        for (DetectedObject detectedObject : detectedObjects) {


                                            if (detectedObject.getLabels().size() > 0  && objectDict.containsKey(detectedObject.getLabels().get(0).getText())) {
                                                depthValue = -1;
                                                boolean alarm = false;
                                                depthValue = depth.getDistance(detectedObject.getBoundingBox().centerX(), detectedObject.getBoundingBox().centerY());
                                                if (depthValue < objectDict.get(detectedObject.getLabels().get(0).getText())) {
                                                    System.out.println("SOUNA NOTIFICA");
                                                    mp.start();
                                                    alarm = true;
                                                }
                                                drawBoundingBoxLabel = new ObjectGraphics(detectedObject, graphicOverlay, image.getWidth(), depthValue, alarm);
                                                drawBoundingBoxLabel.drawBoundingBoxAndLabel();
                                            }

                                        }
                                        depth.close();
                                        printFPS();
                                        img1.setImageBitmap(realsenseBM);
                                        depthFrame.close();
                                        mHandler.post(StreamDetection.this::run);
                                    }
                                });
                            }
                        }
                    }
                }
            }
        }
        catch (Exception e) {
            Log.e(TAG, "streaming, error: " + e.getMessage());
        }
    }

    private void configAndStart() throws Exception {
        try(Config config  = new Config())
        {
            config.enableStream(StreamType.DEPTH,640, 480);
            config.enableStream(StreamType.COLOR,640, 480);
            // try statement needed here to release resources allocated by the Pipeline:start() method
            try(PipelineProfile pp = mPipeline.start(config)){}
        }
    }

    public synchronized void start() {
        if(mIsStreaming)
            return;
        try{
            Log.d(TAG, "try start streaming");
            configAndStart();
            mIsStreaming = true;
            mHandler.post(this); // ? o this::run
            Log.d(TAG, "streaming started successfully");
        } catch (Exception e) {
            Log.d(TAG, "failed to start streaming");
        }
    }

    public synchronized void stop2() {
        if(!mIsStreaming)
            return;
        try {
            Log.d(TAG, "try stop streaming");
            mIsStreaming = false;
            mHandler.removeCallbacks(this);
            mPipeline.stop();
            mPipeline.close();
            Log.d(TAG, "streaming stopped successfully");
        } catch (Exception e) {
            Log.d(TAG, "failed to stop streaming");
        }
    }

    void printFPS() {
        currentTime = Calendar.getInstance().getTimeInMillis();
        deltaTime = currentTime - previousTime;
        aproxFps = 1000/deltaTime;
        previousTime = currentTime;
        fps.setText("FPS detection: " + String.valueOf(aproxFps));
    }



}

package com.example.testrealsense;

import android.graphics.Bitmap;
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

import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.List;

import androidx.annotation.NonNull;

import static com.example.testrealsense.ImageUtils.rgb2Bitmap;

class DepthHandle{
    DepthFrame depth;

    // Create monitor
    volatile Object lock = new Object();
    // Create signal for resource sharing
    volatile Boolean readyToPrint = false;


    public DepthHandle(Frame depthFrame) {
        synchronized(lock) {
            depth = depthFrame.as(Extension.DEPTH_FRAME);
            System.out.println("insideLock DepthDataSize: " + depth.getDataSize());
            readyToPrint=true;
            //depthFrame.close();
            lock.notify();
        }
    }

    // Waits for a wordToPrint to be set
    public float getDistance(int x, int y) throws InterruptedException {
        synchronized(lock) {
            while(!readyToPrint) {
                lock.wait();
            }
            System.out.println("The DepthSize" + depth.getDistance(x, y));
            readyToPrint=false;
            float depthDistance = depth.getDistance(x, y);
            //depth.close();
            return depthDistance;
        }
    }

    public void close() {
        depth.close();
    }
}


public class MyRunnable extends Thread implements OnSuccessListener<List<DetectedObject>> {
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

    private Bitmap realsenseBM;
    private ImageView img1;
    InputImage image;
    //DepthFrame depth;
    Frame depthFrame_clone;

    private TextView distanceView;
    GraphicOverlay graphicOverlay;
    TextView fps;

    DepthHandle dh;
    DepthFrame depth;

    // Create monitor
    volatile Object lock = new Object();
    // Create signal for resource sharing
    volatile Boolean ready = true;

    long currentTime;
    long previousTime = 0;
    long deltaTime = 0;
    long aproxFps = 0;

    private Object mutex = new Object();

    CustomObjectDetectorOptions customObjectDetectorOptions;

    public MyRunnable(ImageView img1, GraphicOverlay graphicOverlay, TextView distanceView, TextView fps ){
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

        mPipeline = new Pipeline();
        mColorizer = new Colorizer();
    }

    @Override
    public void onSuccess(List<DetectedObject> detectedObjects) {
        graphicOverlay.clear();
        if (detectedObjects.size() > 0) {
            //System.out.println("Depth Listener: " + depth.getDataSize());

            synchronized(lock) {
                while(ready) {
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                for (DetectedObject detectedObject : detectedObjects) {
                    float depthValue = 0;
                    //depthValue = depth.getDistance(detectedObject.getBoundingBox().centerX(), detectedObject.getBoundingBox().centerY());
                    try {
                        System.out.println("insideListener DepthDataSize: " + depth.getDataSize());
                        depthValue = depth.getDistance(detectedObject.getBoundingBox().centerX(), detectedObject.getBoundingBox().centerY());
                    } catch (Exception e) {

                    }

                    ObjectGraphics drawBoundingBoxLabel = new ObjectGraphics(detectedObject, graphicOverlay, image.getWidth(), depthValue);
                    drawBoundingBoxLabel.drawBoundingBoxAndLabel();
                }
                ready=true;
                depth.close();
                lock.notify();

            }
            //System.out.println("PROVA: " + prova + " DEPTH: " + depth);
            printFPS();
        }
    }

    @Override
    public void run() {
        super.run();
        try {
            try (FrameSet frames = mPipeline.waitForFrames()) {
                try (FrameSet processed = frames.applyFilter(mAlign)) { // align qua
                    try ( Frame depthFrame = processed.first(StreamType.DEPTH)) {
                        try (Frame colorFrame = processed.first(StreamType.COLOR)) {
                            //depth = depthFrame.as(Extension.DEPTH_FRAME);
                            //prova = (DepthFrame) depth.clone().as(Extension.DEPTH_FRAME);

                            VideoFrame videoFrame = colorFrame.as(Extension.VIDEO_FRAME);

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
                                image = InputImage.fromBitmap(realsenseBM, 0);

                                depth = depthFrame.as(Extension.DEPTH_FRAME);
                                System.out.println("insideLock DepthDataSize: " + depth.getDataSize());

                                //System.out.println("Depth: " + depth.getDataSize());
                                ObjectDetector objectDetector = ObjectDetection.getClient(customObjectDetectorOptions);

                                Task<List<DetectedObject>> task = objectDetector.process(image);

                                synchronized(lock) {
                                    while (!task.isComplete()) {
                                        System.out.println("detection...");
                                    }
                                    graphicOverlay.clear();
                                    List<DetectedObject> detectedObjects = task.getResult();
                                    for (DetectedObject detectedObject : detectedObjects) {
                                        float depthValue = 0;
                                        depthValue = depth.getDistance(detectedObject.getBoundingBox().centerX(), detectedObject.getBoundingBox().centerY());

                                        ObjectGraphics drawBoundingBoxLabel = new ObjectGraphics(detectedObject, graphicOverlay, image.getWidth(), depthValue);
                                        drawBoundingBoxLabel.drawBoundingBoxAndLabel();
                                    }

                                    printFPS();
                                }

                                /*
                                try {
                                    float depthValue2 = depth.getDistance(depth.getWidth() / 2, depth.getHeight() / 2);
                                    distanceView.setText("distance from center: " + String.valueOf(depthValue2));

                                } catch (Exception e) {
                                    //Toast.makeText(, e.getMessage(), Toast.LENGTH_SHORT).show();
                                }*/

                                img1.setImageBitmap(realsenseBM);
                                //img2.setImageBitmap(realsenseBMD);
                                count++;
                            }
                        }
                    }
                }
            }
            mHandler.post(this);
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

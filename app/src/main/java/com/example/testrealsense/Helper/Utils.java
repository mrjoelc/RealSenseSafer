package com.example.testrealsense.Helper;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.RectF;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.RequiresApi;

import org.json.JSONException;
import org.json.JSONObject;
import org.tensorflow.lite.task.vision.detector.Detection;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.Buffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Stream;

public class Utils {

    public static String[] getCurrentDay(){
        SimpleDateFormat year = new SimpleDateFormat("yyyy");
        SimpleDateFormat month = new SimpleDateFormat("MM");
        SimpleDateFormat day = new SimpleDateFormat("dd");

        // String millisInString  = dateFormat.format(new Date());
        String yearInString  = year.format(new Date());
        String monthInString  = month.format(new Date());
        String dayInString  = day.format(new Date());

        String[] date = new String[]{yearInString, monthInString, dayInString};
        return date;
    }

    public static void saveBitmap(final Bitmap bitmap, final String filename) {
        final String ROOT =
                Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "RealSense";
        final File myDir = new File(ROOT);

        if (!myDir.mkdirs()) {
            Log.i("Bitmap", "Make dir failed");
        }

        final File file = new File(myDir, filename);
        if (file.exists()) {
            file.delete();
        }
        try {
            final FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 99, out);
            out.flush();
            out.close();
        } catch (final Exception e) {
            Log.e("Bitmap", "Save Bitmap Excetion!");
        }
    }

    public static Bitmap loadBitmapFromAssets(Context context, String path) {
        InputStream bitmap = null;
        try {
            bitmap= context.getAssets().open(path);
            Bitmap bit = BitmapFactory.decodeStream(bitmap);
            return bit;
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        return null;
    }

    public static void getFiles() {

        /*File f = new File("models");
        f.mkdirs();
        File[] file = f.listFiles();
        ArrayList<String> arrayFiles = new ArrayList<String>();
        if (file.length == 0)
            System.out.println(false);
        else {
            for (int i=0; i<file.length; i++)
                arrayFiles.add(file[i].getName());
        }
        System.out.println(arrayFiles);*/

    }

    public static float[] getScaledBoundingBox(Detection detectedObject, float scaleFactor){
        float left = detectedObject.getBoundingBox().left * scaleFactor ;
        float top = detectedObject.getBoundingBox().top * scaleFactor;
        float right = detectedObject.getBoundingBox().right * scaleFactor;
        float bottom = detectedObject.getBoundingBox().bottom * scaleFactor;
        return new float[]{(int) left, (int) top, (int) right, (int) bottom};
    }

    public static float calculateScaleFactor(GraphicOverlay graphicOverlay, int imageWidth){
        return (float) graphicOverlay.getWidth() / imageWidth;
    }

    public static void pointsInsideRect(RectF points){
        List<Float> l = new ArrayList<>();
        for(float j=points.top; j<points.bottom; j++){
            for (float i=points.left; i<points.right; i++){
            }
        }
    }

    public static Bitmap rgb2Bitmap(byte[] data, int width, int height) {
        int[] colors = convertByteToColor(data);    //取RGB值转换为int数组
        if (colors == null) {
            return null;
        }

        Bitmap bmp = Bitmap.createBitmap(colors, 0, width, width, height,
                Bitmap.Config.ARGB_8888);
        return bmp;
    }

    public static int convertByteToInt(byte data) {

        int heightBit = (int) ((data >> 4) & 0x0F);
        int lowBit = (int) (0x0F & data);
        return heightBit * 16 + lowBit;
    }


    public static int[] convertByteToColor(byte[] data) {
        int size = data.length;
        if (size == 0) {
            return null;
        }

        int arg = 0;
        if (size % 3 != 0) {
            arg = 1;
        }

        int[] color = new int[size / 3 + arg];
        int red, green, blue;
        int colorLen = color.length;
        if (arg == 0) {
            for (int i = 0; i < colorLen; ++i) {
                red = convertByteToInt(data[i * 3]);
                green = convertByteToInt(data[i * 3 + 1]);
                blue = convertByteToInt(data[i * 3 + 2]);

                // 获取RGB分量值通过按位或生成int的像素值
                color[i] = (red << 16) | (green << 8) | blue | 0xFF000000;
            }
        } else {
            for (int i = 0; i < colorLen - 1; ++i) {
                red = convertByteToInt(data[i * 3]);
                green = convertByteToInt(data[i * 3 + 1]);
                blue = convertByteToInt(data[i * 3 + 2]);
                color[i] = (red << 16) | (green << 8) | blue | 0xFF000000;
            }

            color[colorLen - 1] = 0xFF000000;
        }

        return color;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static HashMap TXTToMap(Context context, String filename) throws JSONException, IOException {
        AssetManager manager = context.getAssets();
        InputStream file = manager.open("dict/"+filename);
        HashMap<String, Float> map = new HashMap<String, Float>();

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(file));


        String text;
        while ((text = reader.readLine()) != null) {
            map.put(text, 0.0f);

            // do something with the line
        }


        /*
        InputStream is;
        BufferedReader reader;
        File file = new File ("/src/main/assets/dict/dict.txt");

        if (file.exists()) {
            is = new FileInputStream(file);
            reader = new BufferedReader(new InputStreamReader(is));
            String line = reader.readLine();
            while(line != null){
                line = reader.readLine();
                System.out.println(line);
                map.put(line, 0.0f);
            }
        }*/


        /*Scanner sc=new Scanner(file);

        while(sc.hasNextLine()) {
            System.out.println(sc.nextLine());
            map.put(sc.nextLine(), 0.0f);//returns the line that was skipped
        }
        sc.close();     //closes the scanner*/

        System.out.println(map);

        return map;
    }

    public static HashMap jsonToMap(Context context, String path) throws JSONException, IOException {
        AssetManager manager = context.getAssets();
        InputStream file;
        file = manager.open(path);
        byte[] formArray = new byte[file.available()];
        file.read(formArray);
        file.close();

        String t = new String(formArray);

        HashMap<String, Float> map = new HashMap<String, Float>();
        JSONObject jObject = new JSONObject(t);
        Iterator<?> keys = jObject.keys();

        while( keys.hasNext() ){
            String key = (String)keys.next();
            Float value = Float.parseFloat(jObject.getString(key));
            map.put(key, value);

        }

        System.out.println("json : "+ jObject);
        System.out.println("map : "+ map);

        return map;
    }


}

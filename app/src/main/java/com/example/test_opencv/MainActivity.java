package com.example.test_opencv;

import android.Manifest;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mohammedalaa.gifloading.LoadingView;

import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static org.opencv.imgproc.Imgproc.findContours;

public class MainActivity extends AppCompatActivity {

    public static final int MULTIPLE_PERMISSIONS = 10; // code you want.
    private static ProgressDialogFragment progressDialogFragment;

    static {
        OpenCVLoader.initDebug();
    }

    Uri imageUri;
    String imageurl;
    boolean labelSet = false;
    Bitmap bitmapArg;
    Bitmap rawCroppedImage;
    Point[] points;
    String[] permissions = new String[]{
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
    };
    FloatingActionButton fab;
    LoadingView loadingView;
    long start;
    long end;
    String imageName = "";
    String imageExtension = "jpg";
    private ImageView imageView;
    private Button transformBtn;
    private Bitmap transformed;
    private Bitmap mrz;
    private Bitmap edged;
    private Bitmap perspectiveChanged;
    private int resultW;
    private int resultH;
    static SimpleDateFormat formatter;

    public static String ConvertBitmapToString(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
        byte[] bytes = byteArrayOutputStream.toByteArray();
        String base64Image = Base64.encodeToString(bytes, Base64.NO_WRAP);
        return base64Image;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        appendLog("\n\n\n**********************NID Scanner app has started**********************\n\n");
        imageView = (ImageView) findViewById(R.id.image_view);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        imageView = (ImageView) findViewById(R.id.image_view);
        transformBtn = (Button) findViewById(R.id.transform_btn);
        loadingView = (LoadingView) findViewById(R.id.loading_view);

        transformBtn.setVisibility(View.INVISIBLE);

        imageView = (ImageView) findViewById(R.id.image_view);

        fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (checkPermissions()) {
                    selectImage(MainActivity.this);
                }
            }
        });

        transformBtn.setOnClickListener(new View.OnClickListener() {

            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onClick(View v) {
                loadingView.showLoading();
                loadingView.bringToFront();
                labelSet = true;
                if (labelSet)
                    transformBtn.setText(R.string.processing);
                else
                    transformBtn.setText(R.string.transform_image);

                transformBtn.setEnabled(false);
                fab.setVisibility(View.INVISIBLE);
                BitmapDrawable drawable = (BitmapDrawable) imageView.getDrawable();
                bitmapArg = drawable.getBitmap();

                OpenCVLoader.initDebug();

//                step1();
//                step2();
//                step3();
            }
        });
    }

    public Bitmap RotateBitmap(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, false);
    }

    private boolean checkPermissions() {
        int result;
        List<String> listPermissionsNeeded = new ArrayList<>();
        for (String p : permissions) {
            result = ContextCompat.checkSelfPermission(MainActivity.this, p);
            if (result != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(p);
            }
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), MULTIPLE_PERMISSIONS);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissionsList[], int[] grantResults) {
        switch (requestCode) {
            case MULTIPLE_PERMISSIONS: {
                if (grantResults.length > 0) {
                    String permissionsDenied = "";
                    for (String per : permissionsList) {
                        if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                            permissionsDenied += "\n" + per;

                        }

                    }
                }
                return;
            }
        }
    }

    private void selectImage(Context context) {
        final CharSequence[] options = {"Take Photo", "Cancel"};

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Choose your profile picture");

        builder.setItems(options, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int item) {

                if (options[item].equals("Take Photo")) {
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.CAMERA},
                            1);

                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            1);

                    ContentValues values = new ContentValues();
                    values.put(MediaStore.Images.Media.TITLE, "New Picture");
                    values.put(MediaStore.Images.Media.DESCRIPTION, "From your Camera");

                    imageUri = getContentResolver().insert(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                    Intent takePicture = new Intent(MainActivity.this, CameraActivity.class);
//                    takePicture.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
//                    startActivityForResult(takePicture, 0);
//                    Intent takePicture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
//                    takePicture.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                    appendLog("CAMERA started");
                    startActivityForResult(takePicture, 0);
                } else if (options[item].equals("Cancel")) {
                    dialog.dismiss();
                }
            }
        });
        builder.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != RESULT_CANCELED) {
            switch (requestCode) {
                case 0:
                    start = new Timestamp(System.currentTimeMillis()).getTime();
                    appendLog("IMAGE captured");
//                    Log.d("IMG_PATH", data.toString());
                    if (resultCode == RESULT_OK && data != null) {
                        Bitmap thumbnail = null;
                        try {
                            imageurl = data.getStringExtra("imagePath");
                            if(imageurl != null) {
                                appendLog("IMAGE saved");
                            }
                            appendLog("Starting NID portion crop");
                            cropRawImage();
                            appendLog("Finished NID portion crop");
                            loadingView.showLoading();
                            loadingView.bringToFront();
                            OpenCVLoader.initDebug();
                            appendLog("Starting Edge detection");
                            step1();
                            appendLog("Finished Edge detection");
                            appendLog("Starting Perspective transformation");
                            step2();
                            appendLog("Finishes Perspective transformation");
                            appendLog("Starting MRZ portion crop");
                            step3();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    break;
            }
        }
    }

    private void cropRawImage() {
        showProgressDialog(getResources().getString(R.string.loading));
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    appendLog("Converting Bitmap to Mat");
                    Bitmap bMap = BitmapFactory.decodeFile(imageurl);
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inJustDecodeBounds = true;

                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    bMap.compress(Bitmap.CompressFormat.JPEG, 100, stream);


                    Mat matImage = new Mat();
                    Utils.bitmapToMat(bMap, matImage);
                    appendLog("Converted to Mat");
                    //center
//                    int x0 = bMap.getWidth()/2;
                    int y0 = bMap.getWidth() / 2;
//                    int dx = bMap.getWidth()/2;
                    int dy = (int) ((Double.valueOf(bMap.getWidth()) * 0.6) / 2);

                    Rect roi = new Rect(0, y0 - dy, bMap.getWidth(), y0);
                    Mat cropped = new Mat(matImage, roi);
                    appendLog("Mat obj cropped");
                    transformed = Bitmap.createBitmap(bMap.getWidth(), y0, Bitmap.Config.ARGB_8888);
                    Utils.matToBitmap(cropped, transformed);
                    appendLog("Converted to Bitmap");
                    bitmapArg = transformed;
                    imageName = "crop";
                    convertBitMapToImage();
                } catch (Exception e) {
                    dismissDialog();
                    e.printStackTrace();
                }

                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (Build.VERSION.SDK_INT >= 29)
                            imageView.setImageBitmap(RotateBitmap(transformed, 90));
                        else
                            imageView.setImageBitmap(transformed);
//                        transformBtn.setVisibility(View.VISIBLE);
                        dismissDialog();
                    }
                });
            }
        });
    }

    private void step1() {

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    transformed = detectEdges(transformed);
                    imageName = "edged";
                    convertBitMapToImage();
                } catch (final OutOfMemoryError e) {
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            bitmapArg = transformed;
//                            imageView.setImageBitmap(bitmapArg);
                            e.printStackTrace();
                        }
                    });
                }
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        imageView.setImageBitmap(transformed);
                    }
                });
            }
        });
    }

    private void step2() {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    perspectiveChanged = applyPerspectiveTransform(edged);
                    transformed = perspectiveChanged;
                    imageName = "perspective";
                    convertBitMapToImage();
                } catch (final OutOfMemoryError e) {
                    e.printStackTrace();
                }
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        imageView.setImageBitmap(perspectiveChanged);
                    }
                });
            }
        });
    }

    private void step3() {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    mrz = cropMrz(perspectiveChanged);
                    appendLog("Finished MRZ portion crop");
                    transformed = mrz;
                    imageName = "mrz";
                    convertBitMapToImage();
                } catch (final OutOfMemoryError e) {
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            bitmapArg = transformed;
                            imageView.setImageBitmap(bitmapArg);
                            e.printStackTrace();
                        }
                    });
                }
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        imageView.setImageBitmap(mrz);
                        loadingView.hideLoading();
                        callApi();
                        labelSet = true;
                        if (labelSet)
                            transformBtn.setText(R.string.transform_image);
                        else
                            transformBtn.setText(R.string.processing);

                        transformBtn.setEnabled(true);
                        transformBtn.setVisibility(View.INVISIBLE);
                        fab.setVisibility(View.VISIBLE);
                    }
                });
            }
        });
    }

    private Bitmap detectEdges(Bitmap bitmap) {
        appendLog("Converting Bitmap to Mat");
        Mat rgba = new Mat();
        Utils.bitmapToMat(bitmap, rgba);
        appendLog("Converted to Mat");

        appendLog("Defining color depth");
        Mat edges = new Mat(rgba.size(), CvType.CV_8UC1);
        appendLog("Applying gray scaling");
        Imgproc.cvtColor(rgba, edges, Imgproc.COLOR_RGB2GRAY, 4);
        appendLog("Applying Canny edge detection");
        Imgproc.Canny(edges, edges, 80, 100);
        appendLog("Applying Gaussian Blur");
        Imgproc.GaussianBlur(edges, edges, new Size(5, 5), 0);

        appendLog("Converting Mat to Bitmap");
        edged = Bitmap.createBitmap(edges.cols(), edges.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(edges, edged);
        appendLog("Converted to Bitmap");

        appendLog("Starting contour finding");
        Mat matImage = new Mat();
        Utils.bitmapToMat(bitmapArg, matImage);

        // find the contours
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        findContours(edges, contours, edges, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

        /*
         *
         * previous code
         *
         */
        if (contours == null) {
            Log.w("Contours", "Can't find target contour, aborting...");
            return null;
        }
        Log.d("Contours", "contour found!");

        double maxVal = 0;
        int maxValIdx = 0;
        for (int contourIdx = 0; contourIdx < contours.size(); contourIdx++) {
            double contourArea = Imgproc.contourArea(contours.get(contourIdx));
            if (maxVal < contourArea) {
                maxVal = contourArea;
                maxValIdx = contourIdx;
            }
        }

        Mat duply = matImage.clone();

        final MatOfPoint biggest = contours.get(maxValIdx);
        List<Point> corners = getCornersFromPoints(biggest.toList());
        System.out.println("corner size " + corners.size());
        appendLog("Finished contour finding");

        appendLog("Drawing Edge");
        for (Point corner : corners) {
            Imgproc.drawMarker(duply, corner, new Scalar(0, 191, 255, 255), 0, 20, 5);
        }

        Imgproc.drawContours(duply, contours, maxValIdx, new Scalar(124, 252, 0, 255), 7);
        appendLog("Drawing Edge finished");

        appendLog("Sorting points for Perspective transform");
        //         Sort points
        points = new MatOfPoint(contours.get(maxValIdx)).toArray();
        points = new SortPointArray(points).sort();
        Log.d("Points", "Points: " + Arrays.toString(points));

        appendLog("Converting Mat to Bitmap");
        Bitmap resultBitmap = Bitmap.createBitmap(duply.cols(), duply.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(duply, resultBitmap);
        appendLog("Converted to Bitmap");
        return resultBitmap;
    }

    private Bitmap applyPerspectiveTransform(Bitmap newBitmap) {
//        BitmapFactory.Options options = new BitmapFactory.Options();
//        options.inJustDecodeBounds = true;
//
//        ByteArrayOutputStream stream = new ByteArrayOutputStream();
//        bitmapArg.compress(Bitmap.CompressFormat.JPEG, 100, stream);
//        newBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        appendLog("Converting Bitmap to Mat");
        Mat matImage = new Mat();
        Utils.bitmapToMat(bitmapArg, matImage);
        appendLog("Converted to Mat");
//
//        Mat outputMat = new Mat();
//        Utils.bitmapToMat(newBitmap, outputMat);
        appendLog("Transforming perspective");
        final TransformPerspective transformPerspective = new TransformPerspective(
                points, matImage);
        final Mat transformed = transformPerspective.transform();

        appendLog("Perspective transformation completed");
        // With the transformed points, now convert the image to gray scale
        // and threshold it to give it the paper effect
        appendLog("Resizing transformed Mat");
        Size transformedSize = new Size(1420,900);;
        Mat resized = new Mat();
        Imgproc.resize(transformed, resized, transformedSize );
        resultW = (int) transformedSize.width;

        resultH = (int) transformedSize.height;

        final Mat result = new Mat(resultH, resultW, CvType.CV_8UC4);
        resized.convertTo(result, CvType.CV_8UC4);

        appendLog("Resizing transformed Mat completed");

        appendLog("Converting Mat to Bitmap");
        final Bitmap bitmapImg = Bitmap.createBitmap(resultW, resultH, Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(result, bitmapImg);
        appendLog("Converted to Bitmap");
        return bitmapImg;
    }

    public Bitmap cropMrz(Bitmap bitmap) {
        appendLog("Cropping Mrz portion from Bitmap");
        Bitmap bitmapImg = Bitmap.createBitmap(bitmap, 0, resultH/2, resultW, resultH/2);

        appendLog("Converting Bitmap to Mat");
        Mat matImage = new Mat();
        Utils.bitmapToMat(bitmapImg, matImage);
        appendLog("Converted to Mat");

        appendLog("Applying B&W filter");
        Imgproc.cvtColor(matImage, matImage, Imgproc.COLOR_BGRA2GRAY);
        appendLog("Applying adaptive thresh holding");
        Imgproc.adaptiveThreshold(matImage, matImage, 255,
                Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 75, 25);

        appendLog("Converting Mat to Bitmap");
        Utils.matToBitmap(matImage, bitmapImg);
        appendLog("Converted to Bitmap");
        return bitmapImg;
    }

    public void convertBitMapToImage() {

        String filename = imageName + new Timestamp(System.currentTimeMillis()).getTime() + "." + imageExtension;
        appendLog("Starting Saving image: " + filename);
        File sd = new File(Environment.getExternalStorageDirectory() + "/" + "nid_scanner");
        File dest = new File(sd, filename);
        Log.d("ImagePath", dest.getPath());
        try {
            FileOutputStream out = new FileOutputStream(dest);
            transformed.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        appendLog("Image saved: " + filename);
    }

    //method to convert the selected image to base64 encoded string

    private List<Point> getCornersFromPoints(final List<Point> points) {
        double minX = 0;
        double minY = 0;
        double maxX = 0;
        double maxY = 0;

        for (Point point : points) {
            double x = point.x;
            double y = point.y;

            if (minX == 0 || x < minX) {
                minX = x;
            }
            if (minY == 0 || y < minY) {
                minY = y;
            }
            if (maxX == 0 || x > maxX) {
                maxX = x;
            }
            if (maxY == 0 || y > maxY) {
                maxY = y;
            }
        }

        List<Point> corners = new ArrayList<>(4);
        corners.add(new Point(minX, minY));
        corners.add(new Point(minX, maxY));
        corners.add(new Point(maxX, minY));
        corners.add(new Point(maxX, maxY));

        return corners;
    }

    public void callApi() {
        showProgressDialog(getResources().getString(R.string.loading));
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                appendLog("Preparing image to send in server");
                try {
                    String postUrl = "http://192.168.0.108:8000/v1/api/nidscan/";
                    String postBody = null;

                    postBody = "{" +
                            "    \"image\": \"" + ConvertBitmapToString(mrz) + "\"\n" +
                            "}";

                    Log.e("str ---> ", postBody);
                    Object resJson = new JSONObject();
                    resJson = postJson(postUrl, postBody);

                    JSONObject json = new JSONObject(resJson.toString());
                    appendLog("Server response received");

//                    checkDigit(json);

                    if (checkDigit(json)) {
                        appendLog("Showing extracted MRZ data");
                        onSuccessResponse(json);
                    } else {
                        appendLog("Extracted MRZ data is not valid");
                        onFailureResponse();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    dismissDialog();
                }
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dismissDialog();
                    }
                });
            }
        });
    }

    public Object postJson(String url, String jsonStr) {

        final MediaType mediaType
                = MediaType.parse("application/json");

        OkHttpClient httpClient = new OkHttpClient();

        //post json using okhttp
        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(mediaType, jsonStr))
                .build();
        Response response = null;
        try {
            response = httpClient.newCall(request).execute();
            if (response.isSuccessful()) {
                Log.d("TAG:", "Got response from server for JSON post using OkHttp ");
//                Log.d("MRZDATA", response.body().string());
                return response.body().string();
            }

        } catch (IOException e) {
            Log.e("TAG", "error in getting response for json post request okhttp");
        }
        return null;
    }

    public void onSuccessResponse(JSONObject response) {
        dismissDialog();
        end = new Timestamp(System.currentTimeMillis()).getTime();

        Log.d("TOTAL_TIME", (end - start) / 1000 + " second(s)");
        appendLog("TOTAL_TIME: " + (end - start) / 1000 + " second(s)");
        String msg = "";

        try {
            JSONObject dataObj = new JSONObject(response.toString());
            Intent intent = new Intent(MainActivity.this, MrzResponseActivity.class);
            intent.putExtra("json", dataObj.toString());
            appendLog("\n\n\n********************** Completed **********************\n\n");
            startActivityForResult(intent, 200);
//                MainActivity.this.finish();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onFailureResponse() {
        this.runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(getApplicationContext(), "Try again!", Toast.LENGTH_LONG).show();
                appendLog("\n\n\n**********************Failed**********************\n\n");
//                imageView.setImageBitmap(null);
            }
        });

    }

    private boolean checkDigit(JSONObject json) {
        try {
            boolean valid = MrzCheckDigit.validateMrz(json.get("date_of_birth").toString(), json.get("check_date_of_birth").toString())
                    && MrzCheckDigit.validateMrz(json.get("expiration_date").toString(), json.get("check_expiration_date").toString());
            return valid;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void logLargeString(String content) {
        if (content.length() > 3000) {
            Log.d("mi", content.substring(0, 3000));
            logLargeString(content.substring(3000));
        } else {
            Log.d("mi", content);
        }
    }


    protected synchronized void showProgressDialog(String message) {
        if (progressDialogFragment != null && progressDialogFragment.isVisible()) {
            // Before creating another loading dialog, close all opened loading dialogs (if any)
            progressDialogFragment.dismissAllowingStateLoss();
        }
        progressDialogFragment = null;
        progressDialogFragment = new ProgressDialogFragment(message);
        FragmentManager fm = getFragmentManager();
        progressDialogFragment.show(fm, ProgressDialogFragment.class.toString());
    }

    protected synchronized void dismissDialog() {
        progressDialogFragment.dismissAllowingStateLoss();
    }

    private static String getTimeStampString() {
        Date date = new Date();
        return formatter.format(new Timestamp(date.getTime()));
    }

    public static void appendLog(String text)
    {
        Log.d("LOGGING", text);
        File logFile = new File(Environment.getExternalStorageDirectory() + "/" + "nid_scanner", "log.txt");
        if (!logFile.exists())
        {
            try
            {
                logFile.createNewFile();
            }
            catch (IOException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        try
        {
            //BufferedWriter for performance, true to set append to file flag
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
            buf.append(getTimeStampString() + "\t" + text + "\n");
            buf.newLine();
            buf.close();
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
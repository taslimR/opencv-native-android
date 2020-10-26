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
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Camera;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mohammedalaa.gifloading.LoadingView;

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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static org.opencv.imgproc.Imgproc.findContours;


public class MainActivity extends AppCompatActivity {

    Uri imageUri;
    String imageurl;
    private ImageView imageView;
    private Button transformBtn;
    boolean labelSet = false;
    Bitmap bitmapArg;

    Camera camera;

    Point[] points;

    private static ProgressDialogFragment progressDialogFragment;

    public static final int MULTIPLE_PERMISSIONS = 10; // code you want.

    String[] permissions= new String[]{
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA,
//            Manifest.permission.ACCESS_COARSE_LOCATION,
//            Manifest.permission.ACCESS_FINE_LOCATION
    };


//    public Mat matImage;

//    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
//        @Override
//        public void onManagerConnected(int status) {
//            switch (status) {
//                case LoaderCallbackInterface.SUCCESS:
//                {
//                    Log.i("OpenCV", "OpenCV loaded successfully");
//                    matImage = new Mat();
//                } break;
//                default:
//                {
//                    super.onManagerConnected(status);
//                } break;
//            }
//        }
//    };

    static {
        OpenCVLoader.initDebug();
    }

    private Bitmap transformed;
    private Bitmap mrz;
    private Bitmap edged;
    private int resultW;
    private int resultH;

    FloatingActionButton fab;

    LoadingView loadingView;

    private SurfaceView cameraPreview;
    private RelativeLayout overlay;
    private LayoutInflater controlInflater;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        imageView = (ImageView) findViewById(R.id.image_view);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        imageView = (ImageView) findViewById(R.id.image_view);
        transformBtn = (Button) findViewById(R.id.transform_btn);
        loadingView= (LoadingView) findViewById(R.id.loading_view);

        // Optional: Hide the status bar at the top of the window
//        requestWindowFeature(Window.FEATURE_NO_TITLE);
//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // Set the content view and get references to our views
//        setContentView(R.layout.content_image_capture);
//        cameraPreview = (SurfaceView) findViewById(R.id.camera_preview);
//        overlay = (RelativeLayout) findViewById(R.id.overlay);


        transformBtn.setVisibility(View.INVISIBLE);
//        selectImage(MainActivity.this);
//        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);

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

//                showProgressDialog(getResources().getString(R.string.loading));
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

                step1();
                step2();
                step3();
            }
        });
    }

    public Bitmap RotateBitmap(Bitmap source, float angle)
    {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, false);
    }

//    @Override
//    public void onWindowFocusChanged(boolean hasFocus) {
//        super.onWindowFocusChanged(hasFocus);
//
//        // Get the preview size
//        int previewWidth = cameraPreview.getMeasuredWidth(),
//                previewHeight = cameraPreview.getMeasuredHeight();
//
//        // Set the height of the overlay so that it makes the preview a square
//        RelativeLayout.LayoutParams overlayParams = (RelativeLayout.LayoutParams) overlay.getLayoutParams();
//        overlayParams.height = previewHeight - previewWidth;
//        overlay.setLayoutParams(overlayParams);
//    }

    private  boolean checkPermissions() {
        int result;
        List<String> listPermissionsNeeded = new ArrayList<>();
        for (String p:permissions) {
            result = ContextCompat.checkSelfPermission(MainActivity.this,p);
            if (result != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(p);
            }
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]),MULTIPLE_PERMISSIONS );
            return false;
        }
        return true;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String permissionsList[], int[] grantResults) {
        switch (requestCode) {
            case MULTIPLE_PERMISSIONS:{
                if (grantResults.length > 0) {
                    String permissionsDenied = "";
                    for (String per : permissionsList) {
                        if(grantResults[0] == PackageManager.PERMISSION_DENIED){
                            permissionsDenied += "\n" + per;

                        }

                    }
                    // Show permissionsDenied
//                    updateViews();
                }
                return;
            }
        }
    }

    private void selectImage(Context context) {
        final CharSequence[] options = { "Take Photo", "Cancel" };

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
//                    Camera.Parameters parameters = camera.getParameters();
//                    parameters.set("orientation", "portrait");
//                    camera.setParameters(parameters);

                    ContentValues values = new ContentValues();
                    values.put(MediaStore.Images.Media.TITLE, "New Picture");
                    values.put(MediaStore.Images.Media.DESCRIPTION, "From your Camera");
//                    values.put();

                    imageUri = getContentResolver().insert(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                    Intent takePicture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    takePicture.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                    startActivityForResult(takePicture, 0);


//                } else if (options[item].equals("Choose from Gallery")) {
//                    ActivityCompat.requestPermissions(MainActivity.this,
//                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
//                            1);
//                    ActivityCompat.requestPermissions(MainActivity.this,
//                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
//                            1);
//                    Intent pickPhoto = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
//                    startActivityForResult(pickPhoto , 1);

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
                    if (resultCode == RESULT_OK && data != null) {
                        Bitmap thumbnail = null;
                        try {
                            thumbnail = MediaStore.Images.Media.getBitmap(
                                    getContentResolver(), imageUri);

                            imageView.setImageBitmap(thumbnail);
                            imageurl = getRealPathFromURI(imageUri);
//                            imageView.setImageBitmap(RotateBitmap(BitmapFactory.decodeFile(imageurl), 90));
                            if(Build.VERSION.SDK_INT >= 29)
                                imageView.setImageBitmap(RotateBitmap(BitmapFactory.decodeFile(imageurl), 90));
                            else
                                imageView.setImageBitmap(BitmapFactory.decodeFile(imageurl));
                            transformBtn.setVisibility(View.VISIBLE);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    break;
                case 1:
                    if (resultCode == RESULT_OK && data != null) {
                        Uri selectedImage = data.getData();
                        String[] filePathColumn = {MediaStore.Images.Media.DATA};
                        if (selectedImage != null) {
                            Cursor cursor = getContentResolver().query(selectedImage,
                                    filePathColumn, null, null, null);
                            if (cursor != null) {
                                cursor.moveToFirst();

                                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                                String picturePath = cursor.getString(columnIndex);
//                                if(Build.VERSION.SDK_INT >= 29)
//                                    imageView.setImageBitmap(RotateBitmap(BitmapFactory.decodeFile(picturePath), 90));
//                                else
                                imageView.setImageBitmap(BitmapFactory.decodeFile(picturePath));
                                cursor.close();
                            }
                            transformBtn.setVisibility(View.VISIBLE);
                        }

                    }
                    break;
            }
        }
    }

    public String getRealPathFromURI(Uri contentUri) {
        String[] proj = { MediaStore.Images.Media.DATA };
        Cursor cursor = managedQuery(contentUri, proj, null, null, null);
        int column_index = cursor
                .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }

    private void step1() {
//        showProgressDialog(getResources().getString(R.string.loading));
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    transformed = detectEdges(bitmapArg);
                } catch (final OutOfMemoryError e) {
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            transformed = bitmapArg;
                            imageView.setImageBitmap(bitmapArg);
                            e.printStackTrace();
//                            dismissDialog();
                        }
                    });
                }
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        imageView.setImageBitmap(transformed);
//                        dismissDialog();
                    }
                });
            }
        });
    }

    private void step2() {
//        showProgressDialog(getResources().getString(R.string.loading));
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    transformed = applyPerspectiveTransform(edged);
                } catch (final OutOfMemoryError e) {
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            transformed = bitmapArg;
                            imageView.setImageBitmap(bitmapArg);
                            e.printStackTrace();
//                            dismissDialog();
                        }
                    });
                }
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        imageView.setImageBitmap(transformed);
//                        dismissDialog();
                    }
                });
            }
        });
    }


    private void step3() {
//        showProgressDialog(getResources().getString(R.string.loading));
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    mrz = cropMrz(transformed);
                } catch (final OutOfMemoryError e) {
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            transformed = bitmapArg;
                            imageView.setImageBitmap(bitmapArg);
                            e.printStackTrace();
//                            dismissDialog();
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
//                        dismissDialog();
                    }
                });
            }
        });
    }


    private Bitmap detectEdges(Bitmap bitmap) {
        Mat rgba = new Mat();
        Utils.bitmapToMat(bitmap, rgba);

        Mat edges = new Mat(rgba.size(), CvType.CV_8UC1);
        Imgproc.cvtColor(rgba, edges, Imgproc.COLOR_RGB2GRAY, 4);
        Imgproc.Canny(edges, edges, 80, 100);
        Imgproc.GaussianBlur(edges,edges,new Size(5,5),0);

        edged = Bitmap.createBitmap(edges.cols(), edges.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(edges, edged);

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmapArg.compress(Bitmap.CompressFormat.PNG, 100, stream);
//        newBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);

        Mat matImage = new Mat();
        Utils.bitmapToMat(bitmapArg, matImage);

//        Mat outputMat = new Mat();
//        Utils.bitmapToMat(newBitmap, outputMat);

        Imgproc.GaussianBlur(edges, edges, new Size(5, 5), 5);

//        Imgproc.cvtColor(edges, edges, Imgproc.COLOR_BGR2GRAY);

        // find the contours
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        findContours(edges, contours, edges, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

        /*
         *
         * previous code
         *
         */
        final Mat target = new GetTargetContour(contours).target();
        if (contours == null) {
            Log.w("Contours", "Can't find target contour, aborting...");
            return null;
        }
        Log.d("Contours", "Target contour found!");


        double maxVal = 0;
        int maxValIdx = 0;
        for (int contourIdx = 0; contourIdx < contours.size(); contourIdx++)
        {
            double contourArea = Imgproc.contourArea(contours.get(contourIdx));
            if (maxVal < contourArea)
            {
                maxVal = contourArea;
                maxValIdx = contourIdx;
            }
        }

        Mat duply = matImage.clone();

        final MatOfPoint biggest = contours.get(maxValIdx);
        List<Point> corners = getCornersFromPoints(biggest.toList());
        System.out.println("corner size " + corners.size());
        for (Point corner : corners) {
            Imgproc.drawMarker(duply, corner, new Scalar(0,191,255, 255), 0, 20, 5);
        }

        Imgproc.drawContours(duply, contours, maxValIdx, new Scalar(124,252,0, 255), 7);

        //         Sort points
        points = new MatOfPoint(contours.get(maxValIdx)).toArray();
        points = new SortPointArray(points).sort();
        Log.d("Points", "Points: " + Arrays.toString(points));

        Bitmap resultBitmap = Bitmap.createBitmap(duply.cols(), duply.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(duply, resultBitmap);

        return resultBitmap;
    }

    private Bitmap applyPerspectiveTransform(Bitmap newBitmap) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmapArg.compress(Bitmap.CompressFormat.PNG, 100, stream);
        newBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);

        Mat matImage = new Mat();
        Utils.bitmapToMat(bitmapArg, matImage);

        Mat outputMat = new Mat();
        Utils.bitmapToMat(newBitmap, outputMat);

        final TransformPerspective transformPerspective = new TransformPerspective(
                points, matImage);
        final Mat transformed = transformPerspective.transform();
        // With the transformed points, now convert the image to gray scale
        // and threshold it to give it the paper effect
        Size transformedSize = transformed.size();
        resultW = (int) transformedSize.width;
        resultH = (int) transformedSize.height;

        final Mat result = new Mat(resultH, resultW, CvType.CV_8UC4);
        transformed.convertTo(result, CvType.CV_8UC4);

        final Bitmap bitmapImg = Bitmap.createBitmap(resultW, resultH, Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(result, bitmapImg);

//        Bitmap bitmapImg_result = Bitmap.createBitmap(resultW, resultH, Bitmap.Config.ARGB_8888);
//        Utils.matToBitmap(result, bitmapImg_result);
//        result.release();
//        String filename = "result"+ new Timestamp(System.currentTimeMillis()).getTime() +".png";
//        File sd = Environment.getExternalStorageDirectory();
//        File dest = new File(sd, filename);
//        Log.d("ImagePath", dest.getPath());
//        try {
//            FileOutputStream out = new FileOutputStream(dest);
//            bitmapImg.compress(Bitmap.CompressFormat.PNG, 100, out);
//            out.flush();
//            out.close();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
        return bitmapImg;
    }

    public Bitmap cropMrz(Bitmap bitmap) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);


        Mat matImage = new Mat();
        Utils.bitmapToMat(bitmap, matImage);

        Imgproc.cvtColor(matImage, matImage, Imgproc.COLOR_BGRA2GRAY);
        Imgproc.adaptiveThreshold(matImage, matImage, 255,
                Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 75, 25);

        Rect roi = new Rect(0, resultH/2, resultW, resultH/2);
        Mat cropped = new Mat(matImage, roi);

        final Bitmap bitmapImg = Bitmap.createBitmap(resultW, resultH/2, Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(cropped, bitmapImg);
        return bitmapImg;
    }


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


    //method to convert the selected image to base64 encoded string

    public static String ConvertBitmapToString(Bitmap bitmap){

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
        byte[] bytes = byteArrayOutputStream.toByteArray();
        String base64Image = Base64.encodeToString(bytes, Base64.NO_WRAP);
        return base64Image;
    }


    public void callApi() {
        showProgressDialog(getResources().getString(R.string.loading));
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
        try {
            String postUrl= "http://192.168.0.107:8000/v1/api/nidscan/";
            String postBody= null;

                postBody = "{" +
                        "    \"image\": \""+ConvertBitmapToString(mrz)+"\"\n" +
                        "}";

            Log.e("str ---> ", postBody);
            Object resJson = new JSONObject();
            resJson = postJson(postUrl,postBody);

            JSONObject json = new JSONObject(resJson.toString());

            if(Integer.parseInt(json.get("valid_score").toString()) > 70) {
                onSuccessResponse(json);
            } else {
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

    public Object postJson(String url, String jsonStr){

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
        String msg = "";

        try {
            JSONObject dataObj = new JSONObject(response.toString());
                Intent intent = new Intent(MainActivity.this, MrzResponseActivity.class);
                intent.putExtra("json", dataObj.toString());
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
                imageView.setImageBitmap(null);
            }
        });

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
}
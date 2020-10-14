package com.example.test_opencv;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    private ImageView imageView;
    private Button transformBtn;
    boolean labelSet = false;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        imageView = (ImageView) findViewById(R.id.image_view);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = (ImageView) findViewById(R.id.image_view);
        transformBtn = (Button) findViewById(R.id.transform_btn);
        transformBtn.setVisibility(View.INVISIBLE);
//        selectImage(MainActivity.this);
//        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);

        imageView = (ImageView) findViewById(R.id.image_view);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectImage(MainActivity.this);
            }
        });

        transformBtn.setOnClickListener(new View.OnClickListener() {

            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onClick(View v) {
                labelSet = true;
                if(labelSet)
                    transformBtn.setText(R.string.processing);
                else
                    transformBtn.setText(R.string.transform_image);

                transformBtn.setEnabled(false);
                BitmapDrawable drawable = (BitmapDrawable) imageView.getDrawable();
                Bitmap bitmap = drawable.getBitmap();
                OpenCVLoader.initDebug();
                transformImage(bitmap);
            }
        });
    }

//    @Override
//    public void onResume()
//    {
//        super.onResume();
//        if (!OpenCVLoader.initDebug()) {
//            Log.d("OpenCV", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
//            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, this, mLoaderCallback);
//        } else {
//            Log.d("OpenCV", "OpenCV library found inside package. Using it!");
//            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
//        }
//    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1:
            {
                if (grantResults.length > 0 &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, do something you want
                } else {
                    // permission denied
                    Toast.makeText(MainActivity.this, "Permission denied to read your External storage", Toast.LENGTH_SHORT).show();
                }
                return;
            }
        }
    }

    private void selectImage(Context context) {
        final CharSequence[] options = { "Take Photo", "Choose from Gallery","Cancel" };

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Choose your profile picture");

        builder.setItems(options, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int item) {

                if (options[item].equals("Take Photo")) {
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.CAMERA},
                            1);
                    Intent takePicture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(takePicture, 0);

                } else if (options[item].equals("Choose from Gallery")) {
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            1);
                    Intent pickPhoto = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(pickPhoto , 1);

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
                        Bitmap selectedImage = (Bitmap) data.getExtras().get("data");
                        imageView.setImageBitmap(selectedImage);
                        transformBtn.setVisibility(View.VISIBLE);
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


    public void transformImage(Bitmap bitmap) {

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);


        Mat matImage = new Mat();
        Utils.bitmapToMat(bitmap, matImage);

        Log.d("MatArray", matImage.width() + ", " + matImage.height() + ", " + matImage.channels());


        String filename = "pippo"+ new Timestamp(System.currentTimeMillis()).getTime() +".png";
        File sd = Environment.getExternalStorageDirectory();
        File dest = new File(sd, filename);
//        Log.d("ImagePath", dest.getPath());

        // Apply filters
        Mat grayMat = new Mat();
//        Log.d("MatArray", grayMat.toString());
        Imgproc.cvtColor(matImage, grayMat, Imgproc.COLOR_BGR2GRAY);

        final Mat cannedMat = new Mat();
//
////         Find contours from the image
//        final GetContours getContours = new GetContours(cannedMat);
//
//
//
//        final List<MatOfPoint> contours = getContours.contours();
//
//        cannedMat.release();
//
//        if (contours.isEmpty()) {
//            Log.w("Contours", "No contours found!");
//            return;
//        }
//        // Get the large contour
//        final Mat target = new GetTargetContour(contours).target();
//        if (target == null) {
//            Log.w("Contours", "Can't find target contour, aborting...");
//            return;
//        }
//        Log.d("Contours", "Target contour found!");

//        find contours

//        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(5.0, 5.0));
        Mat outputMat = new Mat();
//        Imgproc.morphologyEx(matImage, outputMat, Imgproc.MORPH_CLOSE, kernel); // fill holes
//        Imgproc.morphologyEx(outputMat, outputMat, Imgproc.MORPH_OPEN, kernel); //remove noise
//        Imgproc.dilate(outputMat, outputMat, kernel);

        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Mat hierarchy = new Mat();
        Imgproc.GaussianBlur(grayMat,outputMat,new Size(5,5),0);

        grayMat = outputMat.clone();
        Bitmap bitmapImg_gray = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(grayMat, bitmapImg_gray);
//
//        matImage.release();
//
        filename = "gray"+ new Timestamp(System.currentTimeMillis()).getTime() +".png";
        sd = Environment.getExternalStorageDirectory();
        dest = new File(sd, filename);
        Log.d("ImagePath", dest.getPath());
        try {
            FileOutputStream out = new FileOutputStream(dest);
            bitmapImg_gray.compress(Bitmap.CompressFormat.PNG, 90, out);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        grayMat.release();

        imageView.setImageBitmap(bitmapImg_gray);

        Imgproc.medianBlur(outputMat, outputMat, 5);
        Imgproc.Canny(outputMat, cannedMat, 100, 200);
        Imgproc.findContours(cannedMat, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

//        hierarchy.release();

        Bitmap bitmapImg_cannedMat = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(cannedMat, bitmapImg_cannedMat);

        filename = "canny"+ new Timestamp(System.currentTimeMillis()).getTime() +".png";
        sd = Environment.getExternalStorageDirectory();
        dest = new File(sd, filename);
        Log.d("ImagePath", dest.getPath());
        try {
            FileOutputStream out = new FileOutputStream(dest);
            bitmapImg_cannedMat.compress(Bitmap.CompressFormat.PNG, 90, out);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        cannedMat.release();

        imageView.setImageBitmap(bitmapImg_cannedMat);


//        final Mat target = new GetTargetContour(contours).target();
        if (contours == null) {
            Log.w("Contours", "Can't find target contour, aborting...");
            return;
        }
        Log.d("Contours", "Target contour found!");

        // Sort points
//        final Point[] points = new MatOfPoint(target).toArray();
//        final Point[] orderedPoints = new SortPointArray(points).sort();
//        Log.d("Points", "Points: " + Arrays.toString(orderedPoints));

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

        Imgproc.drawContours(duply, contours, maxValIdx, new Scalar(124,252,0, 255), 5);

        Bitmap bitmapImg_result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(duply, bitmapImg_result);
        duply.release();
        filename = "result"+ new Timestamp(System.currentTimeMillis()).getTime() +".png";
        sd = Environment.getExternalStorageDirectory();
        dest = new File(sd, filename);
        Log.d("ImagePath", dest.getPath());
        try {
            FileOutputStream out = new FileOutputStream(dest);
            bitmapImg_result.compress(Bitmap.CompressFormat.PNG, 90, out);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        imageView.setImageBitmap(bitmapImg_result);


//         Sort points
        Point[] points = new MatOfPoint(contours.get(maxValIdx)).toArray();
        points = new SortPointArray(points).sort();
        Log.d("Points", "Points: " + Arrays.toString(points));

        // Now apply perspective transformation
        final TransformPerspective transformPerspective = new TransformPerspective(
                points, matImage);
        final Mat transformed = transformPerspective.transform();



        // With the transformed points, now convert the image to gray scale
        // and threshold it to give it the paper effect
        Imgproc.cvtColor(transformed, transformed, Imgproc.COLOR_BGRA2GRAY);
        Imgproc.adaptiveThreshold(transformed, transformed, 251,
                Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 21, 10);

        final Size transformedSize = transformed.size();
        final int resultW = (int) transformedSize.width;
        final int resultH = (int) transformedSize.height;

        final Mat result = new Mat(resultH, resultW, CvType.CV_8UC4);
        transformed.convertTo(result, CvType.CV_8UC4);


        final Bitmap bitmapImg = Bitmap.createBitmap(resultW, resultH, Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(result, bitmapImg);
        // Release
        transformed.release();
        result.release();

        filename = "result_"+ new Timestamp(System.currentTimeMillis()).getTime() +".png";
        sd = Environment.getExternalStorageDirectory();
        dest = new File(sd, filename);
        Log.d("ImagePath", dest.getPath());
        try {
            FileOutputStream out = new FileOutputStream(dest);
            bitmapImg.compress(Bitmap.CompressFormat.PNG, 90, out);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        imageView.setImageBitmap(bitmapImg);


        transformBtn.setEnabled(true);
        labelSet = false;
        if(labelSet)
            transformBtn.setText(R.string.processing);
        else
            transformBtn.setText(R.string.transform_image);
        transformBtn.setVisibility(View.INVISIBLE);

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
}
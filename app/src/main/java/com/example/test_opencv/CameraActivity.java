package com.example.test_opencv;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Timestamp;

public class CameraActivity extends AppCompatActivity {

    Camera camera;
    FrameLayout frameLayout;
    ImageView capturedImageView;
    Button btn_capture, btn_save, btn_delete;
    static Bitmap imgBitmap;
    static String imagePath;

    ShowCamera showCamera;

    Box box;

    File picture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        frameLayout = (FrameLayout) findViewById(R.id.frame_layout);
        capturedImageView = (ImageView) findViewById(R.id.captured_image_view);
        capturedImageView.setVisibility(View.INVISIBLE);
        openCamera();
        btn_capture = (Button) findViewById(R.id.btn_capture);
        btn_save = (Button) findViewById(R.id.btn_save);
        btn_save.setVisibility(View.INVISIBLE);
        btn_delete = (Button) findViewById(R.id.btn_delete);
        btn_delete.setVisibility(View.INVISIBLE);
        btn_capture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                captureImage();
            }
        });

        btn_save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("imagePath", imagePath);
                setResult(Activity.RESULT_OK, resultIntent);
                finish();
            }
        });

        btn_delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File fdelete = new File(imagePath);
                if (fdelete.exists()) {
                    if (fdelete.delete()) {
                        MainActivity.appendLog("IMAGE is deleted");
                    } else {
                        MainActivity.appendLog("IMAGE is not deleted");
                    }
                }
                frameLayout.setVisibility(View.VISIBLE);
                btn_capture.setVisibility(View.VISIBLE);
                capturedImageView.setVisibility(View.INVISIBLE);
                btn_save.setVisibility(View.INVISIBLE);
                btn_delete.setVisibility(View.INVISIBLE);
            }
        });
    }

    private void openCamera() {
        camera = Camera.open();
        box = new Box(this);
        addContentView(box, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.FILL_PARENT));
        showCamera = new ShowCamera(this, camera);
        frameLayout.addView(showCamera);
    }

    Camera.PictureCallback mPictureCallback = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, final Camera camera) {
            picture = getOutputMediaFile();

            if(picture == null) {
                return;
            } else {
                try {
                    FileOutputStream fileOutputStream = new FileOutputStream(picture);
                    fileOutputStream.write(data);
                    fileOutputStream.close();
                    imagePath = picture.getPath();
                    ContentValues values = new ContentValues();

                    values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
                    values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                    values.put(MediaStore.MediaColumns.DATA, picture.getAbsolutePath());

                    getApplicationContext().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

                    // Code For Captured Image Save in a ImageView.
                    CameraActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            String imagePath = picture.getAbsolutePath();
                            Uri myURI = Uri.parse(imagePath);
                            frameLayout.setVisibility(View.INVISIBLE);
                            btn_capture.setVisibility(View.INVISIBLE);
                            capturedImageView.setVisibility(View.VISIBLE);
                            btn_save.setVisibility(View.VISIBLE);
                            btn_delete.setVisibility(View.VISIBLE);
                            capturedImageView.setImageURI(myURI);
                        }
                    });
                    camera.startPreview();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    private void previewImage(File picture) {

        View view = View.inflate(CameraActivity.this, R.layout.alert_dialog, null);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setNegativeButton("Close", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("imagePath", imagePath);
                        setResult(Activity.RESULT_OK, resultIntent);
                        finish();
                    }
                })
                .setView(view)
                .create();
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.show();


        ImageView imageView = (ImageView)view.findViewById(R.id.selectedImage);

        imageView.setImageBitmap(getPreview(picture));
    }

    private Bitmap getPreview(File image) {
//        final int THUMBNAIL_SIZE = 72;
//        BitmapFactory.Options bounds = new BitmapFactory.Options();
//        bounds.inJustDecodeBounds = true;
//        BitmapFactory.decodeFile(image.getPath(), bounds);
//        if ((bounds.outWidth == -1) || (bounds.outHeight == -1))
//            return null;
//
//        int originalSize = (bounds.outHeight > bounds.outWidth) ? bounds.outHeight
//                : bounds.outWidth;
//
//        BitmapFactory.Options opts = new BitmapFactory.Options();
//        opts.inSampleSize = originalSize / THUMBNAIL_SIZE;
//        return BitmapFactory.decodeFile(image.getPath(), opts);
        Bitmap bMap = BitmapFactory.decodeFile(imagePath);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bMap.compress(Bitmap.CompressFormat.JPEG, 100, stream);

        return bMap;
    }

    private File getOutputMediaFile() {
        String timeStamp = new Timestamp(System.currentTimeMillis()).getTime() + "";
        String imgFolder = Environment.getExternalStorageDirectory() + "/" + "nid_scanner";
        File f=new File(imgFolder);
        if(!f.exists()) {
            if(!f.mkdir()){
                Toast.makeText(this, imgFolder +" can't be created.", Toast.LENGTH_SHORT).show();

            }
            else {
                Toast.makeText(this, imgFolder + " can be created.", Toast.LENGTH_SHORT).show();
                f.mkdir();
            }
        }


        File outputFile = new File(f, "init_" + timeStamp + ".jpg");
        return outputFile;
    }

    public void captureImage() {
        if(camera != null) {
            camera.takePicture(null,null, mPictureCallback);
        }
    }
}
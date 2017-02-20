package com.smartsight.application;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;

public class SightActivity extends AppCompatActivity implements View.OnClickListener, Camera.PictureCallback {

    private Camera camera;
    private CameraPreview preview;

    private ImageView imgSnap;
    private RelativeLayout resultView;
    private ListView listAllResults;
    private ImageView btnCloseListView;
    private TextView mainResult;

    private final SightActivity thisInstance = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sight);

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            ActivityCompat.requestPermissions(SightActivity.this,
                    new String[]{Manifest.permission.CAMERA},
                    1);
        }

        // TODO: make compatible with SDK < Lollipop
    }

    // TODO: implement onPaused, onResume...

    /**
     * Creates a camera for the application.
     *
     * @return The camera instance
     */
    public static Camera newCamera() {
        Camera camera = null;

        try {
            camera = Camera.open();
            final Camera.Parameters parameters = camera.getParameters();

            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            camera.setParameters(parameters);
        } catch (Exception e) {
            Log.e("CAMERA", "No camera available");
        }

        return camera;
    }

    @Override
    public void onPictureTaken(byte[] bytes, Camera camera) {
        final File pictureFile = getOutputMediaFile(thisInstance, MEDIA_TYPE_IMAGE);

        if (pictureFile == null) {
            Log.d("Write permission", "Error creating media file, check storage permissions.");
            return;
        }

        try {
            final FileOutputStream fos = new FileOutputStream(pictureFile);
            fos.write(bytes);
            fos.close();
        } catch (FileNotFoundException e) {
            Log.d("NOFILE", "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.d("NOACCESS", "Error accessing file: " + e.getMessage());
        }

        Log.d("PATH", pictureFile.getPath());

        new UploadPicture(thisInstance, imgSnap, listAllResults, mainResult).execute(pictureFile.getPath());
    }

    @Override
    public void onClick(View view) {
        if (view == imgSnap) {
            if (imgSnap.getDrawable().getConstantState() == getResources().getDrawable(R.drawable.btn_newcamera).getConstantState()) {
                mainResult.setVisibility(View.INVISIBLE);
                resultView.setVisibility(View.INVISIBLE);
                restartCamera();
                imgSnap.setImageResource(R.mipmap.ic_launcher);
            } else {
                imgSnap.setOnClickListener(null);
                camera.takePicture(null, null, this);
            }
        } else if (view == btnCloseListView) {
            resultView.setVisibility(View.INVISIBLE);
        } else if (view == mainResult) {
            resultView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            finish();
            return;
        }

        if (requestCode == 1) {
            camera = newCamera();

            preview = new CameraPreview(this, camera);
            final FrameLayout displayPreview = (FrameLayout) findViewById(R.id.preview);
            displayPreview.addView(preview);

            imgSnap = (ImageView) findViewById(R.id.btn_sight);
            imgSnap.setOnClickListener(this);
            ActivityCompat.requestPermissions(SightActivity.this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    2);
            mainResult = (TextView) findViewById(R.id.result);
            mainResult.setOnClickListener(this);
            resultView = (RelativeLayout) findViewById(R.id.result_view);
            listAllResults = (ListView) findViewById(R.id.list_view);
            btnCloseListView = (ImageView) findViewById(R.id.btn_close);
            btnCloseListView.setOnClickListener(this);
        } else if (requestCode == 2) {
            ActivityCompat.requestPermissions(SightActivity.this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    3);
        }
    }

    /**
     * Returns the path of the new saved picture.
     *
     * @param type The file type
     * @return the path to the picture
     */
    private static File getOutputMediaFile(SightActivity activity, final int type) {
        final File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "SmartSight");

        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("MakeDir", "Failed to create directory");
                return null;
            }
        }

        File mediaFile;
        // Picture localization in Android explorer: sdcard/Pictures/SmartSight
        if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator + Helper.getConfigValue(activity, "sm_picture_filename"));
        } else {
            return null;
        }

        return mediaFile;
    }

    /**
     * Restarts preview to take a new picture.
     */
    public void restartCamera() {
        camera.startPreview();
    }

}

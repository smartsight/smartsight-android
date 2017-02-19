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

public class SightActivity extends AppCompatActivity implements View.OnClickListener {

    private Camera camera;
    private CameraPreview preview;

    private ImageView img_Sight;
    private RelativeLayout resultView;
    private ListView listView;
    private ImageView btnClose;
    private TextView result;

    private SightActivity thisInstance = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sight);

        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            ActivityCompat.requestPermissions(SightActivity.this,
                    new String[]{Manifest.permission.CAMERA},
                    1);
        }

    }

    // TODO ONPAUSED ONRESUME...

    public static Camera newCamera(){
        Camera camera = null;
        try {
            camera = Camera.open();
        }
        catch (Exception e){
            Log.e("CAMERA", "No camera available");
        }
        return camera;
    }

    private Camera.PictureCallback picture = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] bytes, Camera camera) {
            File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
            if (pictureFile == null){
                Log.d("Write permission", "Error creating media file, check storage permissions: ");
                return;
            }

            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(bytes);
                fos.close();
            } catch (FileNotFoundException e) {
                Log.d("NOFILE", "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d("NOACCESS", "Error accessing file: " + e.getMessage());
            }
            Log.d("PATH", pictureFile.getPath());
            new uploadPicture(thisInstance, img_Sight, listView, result).execute(pictureFile.getPath());
        }
    };


    @Override
    public void onClick(View view) {
        if (view == img_Sight) {
            if (img_Sight.getDrawable().getConstantState() == getResources().getDrawable(R.drawable.btn_newcamera).getConstantState()) {
                result.setVisibility(View.INVISIBLE);
                resultView.setVisibility(View.INVISIBLE);
                restartCamera();
                img_Sight.setImageResource(R.mipmap.ic_launcher);
            } else {
                img_Sight.setOnClickListener(null);
                camera.takePicture(null, null, picture);
            }
        } else if (view == btnClose) {
            resultView.setVisibility(View.INVISIBLE);
        } else if (view == result) {
            resultView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    finish();
                } else {
                    camera = newCamera();

                    preview = new CameraPreview(this, camera);
                    FrameLayout displayPreview = (FrameLayout) findViewById(R.id.preview);
                    displayPreview.addView(preview);

                    img_Sight = (ImageView) findViewById(R.id.btn_sight);
                    img_Sight.setOnClickListener(this);
                    ActivityCompat.requestPermissions(SightActivity.this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            2);
                    result = (TextView) findViewById(R.id.result);
                    result.setOnClickListener(this);
                    resultView = (RelativeLayout) findViewById(R.id.result_view);
                    listView = (ListView) findViewById(R.id.list_view);
                    btnClose = (ImageView) findViewById(R.id.btn_close);
                    btnClose.setOnClickListener(this);
                }
                break;
            case 2:
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    finish();
                } else {
                    ActivityCompat.requestPermissions(SightActivity.this,
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                            3);
                }
                break;
            case 3:
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    finish();
                }
                break;
            default:
        }
    }

    private static File getOutputMediaFile(int type){
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "SmartSight");
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                Log.d("MakeDir", "failed to create directory");
                return null;
            }
        }

        File mediaFile;
        // sdcard/Pictures/SmartSight
        if (type == MEDIA_TYPE_IMAGE){
            mediaFile = new File(mediaStorageDir.getPath() + File.separator + "pic.jpg");
        } else {
            return null;
        }

        return mediaFile;
    }

    public void restartCamera() {
        camera.startPreview();
    }
}

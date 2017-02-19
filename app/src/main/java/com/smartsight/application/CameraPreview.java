package com.smartsight.application;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceView;
import android.view.SurfaceHolder;

import java.io.IOException;

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private SurfaceHolder surfaceHolder;
    private Camera camera;

    /**
     * Constructor to link camera instantiation with preview.
     *
     * @param context Application's context
     * @param camera  source camera
     */
    public CameraPreview(Context context, Camera camera) {
        super(context);
        this.camera = camera;

        surfaceHolder = getHolder();
        surfaceHolder.addCallback(this);
    }

    /**
     * This is called immediately after the surface is first created.
     *
     * @param surfaceHolder The SurfaceHolder whose surface is being created
     */
    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        try {
            camera.setDisplayOrientation(90);
            camera.setPreviewDisplay(surfaceHolder);
            camera.startPreview();
        } catch (IOException ioe) {
            Log.e("PREVIEW", "Error create preview :" + ioe.getMessage());
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int width, int height) {
        if (surfaceHolder.getSurface() == null) {
            return;
        }

        try {
            camera.stopPreview();
        } catch (Exception e) {
            Log.e("PREVIEW", "Error change preview : " + e.getMessage());
        }

        try {
            camera.setPreviewDisplay(this.surfaceHolder);
            camera.startPreview();
        } catch (IOException ioe) {
            Log.e("PREVIEW", "Error change preview :" + ioe.getMessage());
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

    }
}

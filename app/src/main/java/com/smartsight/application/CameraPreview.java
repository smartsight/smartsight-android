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
     * @param context The context of the application
     * @param camera The source camera
     */
    public CameraPreview(Context context, Camera camera) {
        super(context);
        this.camera = camera;

        surfaceHolder = getHolder();
        surfaceHolder.addCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        try {
            camera.setDisplayOrientation(90);
            camera.setPreviewDisplay(surfaceHolder);
            camera.startPreview();
        } catch (IOException ioe) {
            Log.e("PREVIEW", "Error creating preview: " + ioe.getMessage());
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, final int i, final int width, final int height) {
        if (surfaceHolder.getSurface() == null) {
            return;
        }

        try {
            camera.stopPreview();
        } catch (Exception e) {
            Log.e("PREVIEW", "Error changing preview: " + e.getMessage());
        }

        try {
            camera.setPreviewDisplay(this.surfaceHolder);
            camera.startPreview();
        } catch (IOException ioe) {
            Log.e("PREVIEW", "Error changing preview: " + ioe.getMessage());
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

    }

}

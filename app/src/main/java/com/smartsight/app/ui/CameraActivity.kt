package com.smartsight.app.ui

import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.hardware.Camera
import android.os.Bundle
import android.support.design.widget.BottomSheetBehavior
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import com.smartsight.app.R
import com.smartsight.app.R.string.*
import com.smartsight.app.util.*
import kotlinx.android.synthetic.main.activity_camera.*
import kotlinx.android.synthetic.main.list_results.*
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.lang.ref.WeakReference

class CameraActivity : AppCompatActivity(), View.OnClickListener, Camera.PictureCallback {

    companion object {
        private val TAG = CameraActivity::class.java.simpleName
        private val CAMERA_PERMISSION = 1
        private val SNAP_TAG = R.mipmap.ic_launcher
        private val RESTART_TAG = R.drawable.ic_restart
    }

    private val mActivity = WeakReference(this@CameraActivity)
    private lateinit var cameraPreview: CameraPreview
    internal lateinit var camera: Camera
    internal lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)

        tryInitCamera()

        bottomSheetBehavior = BottomSheetBehavior.from(bottom_sheet)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

        bottomSheetBehavior.setBottomSheetCallback(object: BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                when (newState) {
                    BottomSheetBehavior.STATE_EXPANDED -> {
                        val transX = ((bottomSheet.width / 2) - (snap_image.width / 2) - 16).toFloat()
                        snap_image.animate().translationX(transX)
                    }
                    BottomSheetBehavior.STATE_HIDDEN -> {
                        snap_image.animate().translationX(0f)
                        restartView()
                    }
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                activity_main.bringChildToFront(bottom_sheet)
                activity_main.bringChildToFront(snap_image)
            }
        })
    }

    /**
     * Tries to initialize the camera depending on the current permissions.
     *
     * We call [initView] if both [Manifest.permission.CAMERA] and
     * [Manifest.permission.WRITE_EXTERNAL_STORAGE] are granted (see [hasCameraAndStoragePermission]).
     *
     * Otherwise, we ask the user for the permissions calling [ActivityCompat.requestPermissions].
     */
    private fun tryInitCamera() {
        if (hasCameraAndStoragePermission(applicationContext)) {
            initView()
        } else {
            val permissions = ArrayList<String>(2)

            if (!hasCameraPermission(applicationContext)) {
                permissions.add(Manifest.permission.CAMERA)
            }

            if (!hasStoragePermission(applicationContext)) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }

            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), CAMERA_PERMISSION)
        }
    }

    /**
     * Initializes the camera and the preview.
     *
     * An action listener is set on the [snap_image] to take pictures.
     *
     * This method is called once the correct permissions are granted.
     */
    private fun initView() {
        camera = try {
            createCamera()
        } catch (e: Exception) {
            Log.e(TAG, "Can't access camera $e")
            showToast(applicationContext, no_camera)
            return
        }

        cameraPreview = CameraPreview(this, camera)

        preview_frame.addView(cameraPreview)
        snap_image.setOnClickListener(this)
        snap_image.tag = SNAP_TAG
    }

    /**
     * Reinitializes the state of the view.
     *
     * This method does the following:
     *  - Set the [snap_image] source to its default
     *  - Reset its tag to allow the user to take another picture
     *  - Restart the camera
     */
    private fun restartView() {
        snap_image.setImageResource(SNAP_TAG)
        snap_image.tag = SNAP_TAG
        restartCamera(camera)
    }

    /**
     * Handles the click event.
     *
     * [snap_image]:
     *  [SNAP_TAG]:
     *      - Set the [snap_image] tag to [RESTART_TAG] to change its behavior
     *      - Disable the snapping feature
     *      - Take the picture
     *  [RESTART_TAG]:
     *      - Restart the view
     *      - Hide the results list
     *      Note: we don't need the reset the tag here since it's handled by [restartView]
     */
    override fun onClick(view: View) {
        when (view) {
            snap_image -> {
                if (snap_image.tag == SNAP_TAG) {
                    snap_image.tag = RESTART_TAG
                    snap_image.isEnabled = false
                    camera.takePicture(null, null, this)
                } else {
                    restartView()
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                }
            }
        }
    }

    /**
     * Creates a picture from the camera [data] and calls the [PhotoProcessor].
     *
     * This method is called when the camera has taken the picture.
     */
    override fun onPictureTaken(data: ByteArray, camera: Camera) {
        val picture = try {
            getOutputMediaFile(applicationContext)
        } catch (e: IOException) {
            Log.e(TAG, "Can't access file $e")
            showToast(applicationContext, file_error)
            return
        }

        val output = try {
            FileOutputStream(picture)
        } catch (e: FileNotFoundException) {
            Log.e(TAG, "File not found $e")
            showToast(applicationContext, file_error)
            return
        }

        var bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
        val matrix = Matrix()
        matrix.postRotate(90f)
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, stream)

        output.write(stream.toByteArray())
        output.close()

        PhotoProcessor(mActivity.get()!!).execute(picture.path)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == CAMERA_PERMISSION) {
            if (hasCameraAndStoragePermission(applicationContext)) {
                initView()
            } else {
                showToast(applicationContext, no_permission)
                finish()
            }
        }
    }

    override fun onBackPressed() {
        if (snap_image.tag == RESTART_TAG) {
            restartView()
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        } else {
            super.onBackPressed()
        }
    }

    override fun onPause() {
        super.onPause()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onResume() {
        super.onResume()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onDestroy() {
        super.onDestroy()

        try {
            destroyCamera(camera)
        } catch (e: RuntimeException) {
            Log.e(TAG, "Unable to destroy camera because hasn't been initialized $e")
        }
    }
}

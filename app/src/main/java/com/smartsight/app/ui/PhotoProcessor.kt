package com.smartsight.app.ui

import android.os.AsyncTask
import android.support.design.widget.BottomSheetBehavior
import android.util.Log
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.webkit.URLUtil
import android.widget.ArrayAdapter
import com.smartsight.app.R
import com.smartsight.app.R.string.*
import com.smartsight.app.data.SM_PHOTO_NAME
import com.smartsight.app.data.SM_SERVER_ROUTE_CLASSIFY
import com.smartsight.app.data.SM_SERVER_URL
import com.smartsight.app.util.isNetworkConnected
import com.smartsight.app.util.restartCamera
import com.smartsight.app.util.showToast
import kotlinx.android.synthetic.main.activity_camera.*
import kotlinx.android.synthetic.main.list_results.*
import okhttp3.*
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class PhotoProcessor(val activity: CameraActivity) : AsyncTask<String, String, String>() {
    companion object {
        private val TAG = PhotoProcessor::class.java.simpleName
        private val ENDPOINT = "$SM_SERVER_URL$SM_SERVER_ROUTE_CLASSIFY"
    }

    lateinit var photoPath: String

    override fun onPreExecute() {
        val animation = RotateAnimation(0.0f, 360.0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f)
        animation.duration = 2000
        animation.repeatCount = -1
        activity.snapImage.animation = animation
        activity.snapImage.startAnimation(animation)
    }

    override fun doInBackground(vararg pathnames: String): String {
        if (!URLUtil.isValidUrl(SM_SERVER_URL)) {
            Log.e(TAG, "The server URL config is wrong: $SM_SERVER_URL.\n" +
                    "See https://github.com/smartsight/smartsight-android#development-setup")
            cancel(true)
            return "$check_server_url"
        }

        if (!isNetworkConnected(activity)) {
            cancel(true)
            return "$check_connection"
        }

        photoPath = pathnames.first()

        val mediaType = MediaType.parse("image/jpeg")
        val picture = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                        "photo",
                        SM_PHOTO_NAME,
                        RequestBody.create(mediaType, File(photoPath))
                )
                .build()
        val request = Request.Builder()
                .url(ENDPOINT)
                .post(picture)
                .build()

        try {
            val client = OkHttpClient.Builder()
                    .connectTimeout(3, TimeUnit.MINUTES)
                    .writeTimeout(3, TimeUnit.MINUTES)
                    .readTimeout(3, TimeUnit.MINUTES)
                    .build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                Log.e(TAG, "Response from the server $response")
                cancel(true)
                return "$server_error"
            }

            val jsonResponse = response.body().string()

            response.close()

            return jsonResponse
        } catch (e: IOException) {
            Log.e(TAG, "No response from the server $e")
            cancel(true)
            return "$server_unreachable"
        }
    }

    override fun onPostExecute(result: String) {
        clearView()
        activity.snapImage.setImageResource(R.drawable.ic_restart)

        if (!File(photoPath).delete()) {
            Log.e(TAG, "Can't delete photo")
        }

        try {
            val response = JSONObject(result)
            val data = JSONArray(response.getString("data"))

            val firstClassification = data.getJSONObject(0)
            activity.results_first.text = "${firstClassification.getString("class")} (${firstClassification.getString("score")})"

            val predictions = mutableListOf<String>()

            for (i in 1 until data.length()) {
                val classification = data.getJSONObject(i)
                val prediction = classification.getString("class")
                val score = classification.getString("score")

                predictions.add("$prediction ($score)")
            }

            val listAdapter = ArrayAdapter(activity, android.R.layout.simple_expandable_list_item_1, predictions)
            activity.results_list.adapter = listAdapter

            activity.bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        } catch (e: JSONException) {
            Log.e(TAG, "Cannot parse JSON $e")
            restartCamera(activity.camera)
        }
    }

    override fun onCancelled(resId: String) {
        clearView()
        showToast(activity, resId.toInt())
        restartCamera(activity.camera)
    }

    private fun clearView() {
        activity.snapImage.clearAnimation()
        activity.snapImage.isEnabled = true
    }
}

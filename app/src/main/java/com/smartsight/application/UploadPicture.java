package com.smartsight.application;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class UploadPicture extends AsyncTask<String, String, String> {

    private String endpoint;
    private SightActivity sightInstance;

    private ImageView imgSnap;
    private ListView listView;
    private TextView result;

    public UploadPicture(SightActivity mainActivity, ImageView imgView, ListView listView, TextView textView) {
        this.sightInstance = mainActivity;
        this.imgSnap = imgView;
        this.listView = listView;
        this.result = textView;
        this.endpoint = "http://" +
                Helper.getConfigValue(mainActivity, "server_ip") + ":" +
                Helper.getConfigValue(mainActivity, "server_port") +
                Helper.getConfigValue(mainActivity, "server_route_classify");
    }

    /**
     * Triggers the loading animation when requesting the server.
     */
    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        final Animation animation = new RotateAnimation(0.0f, 360.0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        animation.setDuration(2000);
        animation.setRepeatCount(-1);
        animation.setFillAfter(true);

        imgSnap.setAnimation(animation);
        imgSnap.startAnimation(animation);
    }

    /**
     * Uploads the image and waits for the server's response in the background.
     *
     * @param pathnames The array of file path names
     * @return The JSON response or an error
     */
    @Override
    protected String doInBackground(String... pathnames) {
        Log.d("PATHTOSEND", pathnames[0]);

        final MediaType MEDIA_TYPE_JPG = MediaType.parse("image/jpeg");
        final RequestBody picture = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                        "file",
                        Helper.getConfigValue(sightInstance, "sm_picture_filename"),
                        RequestBody.create(MEDIA_TYPE_JPG, new File(pathnames[0]))
                )
                .build();

        final Request request = new Request.Builder()
                .url(endpoint)
                .post(picture)
                .build();

        if (!isConnected()) {
            return "connectionError";
        }

        try {
            final OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(3, TimeUnit.MINUTES)
                    .writeTimeout(3, TimeUnit.MINUTES)
                    .readTimeout(3, TimeUnit.MINUTES)
                    .build();

            final Response response = client.newCall(request).execute();

            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code: " + response);
            }

            final String jsonResponse = response.body().string();
            response.close();
            Log.d("JSON", jsonResponse);

            return jsonResponse;
        } catch (IOException ioe) {
            ioe.printStackTrace();
            return "serverError";
        }
    }

    /**
     * Handles the server's response.
     * Parses the JSON data and stops the loading animation.
     * <p>
     * Response in case of success:
     * {
     * "meta": {
     * "type": "success",
     * "code": 200
     * },
     * "data": "[
     * {\"class\": \"pizza, pizza pie\", \"score\": 0.884148},
     * {\"class\": \"butcher shop, meat market\", \"score\": 0.002444},
     * {\"class\": \"carbonara\", \"score\": 0.00208},
     * {\"class\": \"trifle\", \"score\": 0.002078},
     * {\"class\": \"pomegranate\", \"score\": 0.001326}
     * ]"
     * }
     * <p>
     * Response in case of error:
     * {
     * "error": {
     * "code": 415,
     * "message": "Unsupported Media Type (jpg, jpeg)"
     * }
     * }
     *
     * @param response The response to process
     * @see <a href="https://github.com/smartsight/smartsight-api/wiki">SmartSight API Documentation</a>
     */
    @Override
    protected void onPostExecute(final String response) {
        super.onPostExecute(response);

        switch (response) {
            case "serverError":
                Toast.makeText(sightInstance, "Server unreachable", Toast.LENGTH_LONG).show();
                sightInstance.restartCamera();
                break;

            case "connectionError":
                Toast.makeText(sightInstance, "Check your connection", Toast.LENGTH_LONG).show();
                sightInstance.restartCamera();
                break;

            default:
                try {
                    final JSONObject jsonResponse = new JSONObject(response);
                    Log.d("ARR", jsonResponse.getString("data"));

                    final JSONArray data = new JSONArray(jsonResponse.getString("data"));
                    Log.d("TAG", data.toString());

                    result.setText(data.getJSONObject(0).getString("class") + " (" + data.getJSONObject(0).getString("score") + ")");
                    result.setVisibility(View.VISIBLE);
                    imgSnap.setImageResource(R.drawable.btn_restart);

                    final ArrayList<String> scores = new ArrayList<>();

                    for (int i = 0; i < data.length(); i++) {
                        final JSONObject prediction = data.getJSONObject(i);
                        final String title = prediction.getString("class");
                        final String score = prediction.getString("score");

                        scores.add(title + " (" + score + ")");
                    }

                    final ArrayAdapter<String> listAdapter = new ArrayAdapter<>(sightInstance,
                            android.R.layout.simple_expandable_list_item_1, scores);
                    listView.setAdapter(listAdapter);
                } catch (JSONException e) {
                    e.printStackTrace();
                    sightInstance.restartCamera();
                    result.setVisibility(View.INVISIBLE);
                    imgSnap.setImageResource(R.mipmap.ic_launcher);
                }
        }

        imgSnap.setOnClickListener(sightInstance);
        imgSnap.clearAnimation();
    }

    private boolean isConnected() {
        final ConnectivityManager connectivityManager
                = (ConnectivityManager) sightInstance.getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();

        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}

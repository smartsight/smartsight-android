package com.smartsight.application;

import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

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

    private String serverURL;

    private SightActivity sightInstance;

    private ImageView imgSnap;
    private ListView listView;
    private TextView result;

    public UploadPicture(SightActivity mainAcivity, ImageView imgView, ListView listView, TextView textView) {
        this.sightInstance = mainAcivity;
        this.imgSnap = imgView;
        this.listView = listView;
        this.result = textView;
        this.serverURL = "http://" + Helper.getConfigValue(mainAcivity, "server_ip") + ":"
                + Helper.getConfigValue(mainAcivity, "server_port") + "/classify";
    }

    /**
     * Launch loading animation for uploading.
     */
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        Animation animation = new RotateAnimation(0.0f, 360.0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        animation.setDuration(3000);
        animation.setRepeatCount(-1);
        animation.setFillAfter(true);

        imgSnap.setAnimation(animation);
        imgSnap.startAnimation(animation);
    }

    /**
     * Background task that upload and wait server's response
     *
     * @param strings File path
     * @return JSON response
     */
    @Override
    protected String doInBackground(String... strings) {
        Log.d("PATHTOSEND", strings[0]);

        MediaType MEDIA_TYPE_JPG = MediaType.parse("image/jpeg");
        RequestBody postImage = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", "pic.jpg", RequestBody.create(MEDIA_TYPE_JPG, new File(strings[0])))
                .build();

        Request req = new Request.Builder()
                .url(serverURL)
                .post(postImage)
                .build();

        try {
            OkHttpClient httpClient;
            httpClient = new OkHttpClient.Builder()
                    .connectTimeout(3, TimeUnit.MINUTES)
                    .writeTimeout(3, TimeUnit.MINUTES)
                    .readTimeout(3, TimeUnit.MINUTES)
                    .build();
            Response resp = httpClient.newCall(req).execute();
            if (!resp.isSuccessful()) throw new IOException("Unexpected code " + resp);
            String jsonResp = resp.body().string();
            resp.close();
            Log.d("JSON", jsonResp);
            return jsonResp;
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return null;
    }

    /**
     * JSON parsing process & stop loading animation.
     *
     * @param s JSON datas to display
     */
    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);
        if (s == null) {
            sightInstance.restartCamera();
        } else {
            try {
                /*JSONArray arr = new JSONArray("[\n" +
                    "  {\n" +
                    "    \"class\": \"pizza\",\n" +
                    "    \"confidence\": 0.97\n" +
                    "  },\n" +
                    "  {\n" +
                    "    \"class\": \"plate\",\n" +
                    "    \"confidence\": 0.76\n" +
                    "  }\n" +
                    "]");*/
                JSONObject jsonObject = new JSONObject(s);
                Log.d("ARR", jsonObject.getString("data"));
                JSONArray arr = new JSONArray(jsonObject.getString("data"));
                Log.d("TAG", arr.toString());
                result.setText(arr.getJSONObject(0).getString("class") + " : " + arr.getJSONObject(0).getString("score"));
                result.setVisibility(View.VISIBLE);
                imgSnap.setImageResource(R.drawable.btn_newcamera);

                ArrayList<String> scores = new ArrayList<>();
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject jsonO = arr.getJSONObject(i);
                    String classe = jsonO.getString("class");
                    String score = jsonO.getString("score");
                    scores.add(classe + " : " + score);
                }

                ArrayAdapter<String> listAdapter = new ArrayAdapter<>(sightInstance,
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
}

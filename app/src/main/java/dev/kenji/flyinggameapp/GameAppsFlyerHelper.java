package dev.kenji.flyinggameapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.appsflyer.AFInAppEventParameterName;
import com.appsflyer.AFLogger;
import com.appsflyer.AppsFlyerLib;
import com.appsflyer.attribution.AppsFlyerRequestListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class GameAppsFlyerHelper {

    static final String APP_PREF = "agPrefs";
    private static final String APPSTATS = "appStatus";
    private static String gameURL = "";
    private static final String POLICYSTATUS = "policyStatus";
    static String apiResponse = "";
    static String appStatus = "";

    private static final int SPLASH_TIME_OUT = 800;
    private static final String TAG = "AppsFlyerHelper";
    private static final String AF_ID = "LQ4sUsSSSLf8FomYUjFMZ8";
    private static final String APP_ID = "5G";


    public static void init(Context context) {
        // app flay初始化
        AppsFlyerLib.getInstance().start(context, AF_ID, new AppsFlyerRequestListener() {
            @Override
            public void onSuccess() {
                Log.e(TAG, "Launch sent successfully, got 200 response code from server");
            }

            @Override
            public void onError(int i, @NonNull String s) {
                Log.e(TAG, "Launch failed to be sent:\n" + "Error code: " + i + "\n" + "Error description: " + s);
            }
        });
        AppsFlyerLib.getInstance().setDebugLog(true);
        AppsFlyerLib.getInstance().setLogLevel(AFLogger.LogLevel.DEBUG);
    }

    public static void event(Activity context, String name, String data) {
        Map<String, Object> eventValue = new HashMap<String, Object>();
        /***
         * 开启新窗口跳转
         */
        if("UserConsent".equals(name))
        {
            if (data.equals("Accepted")) {
                Log.d(TAG, "User Consent Accepted");

                RequestQueue connectAPI = Volley.newRequestQueue(context);
                JSONObject requestBody = new JSONObject();
                try {
                    requestBody.put("appid", APP_ID);
                    requestBody.put("package", context.getPackageName());
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                String endPoint = "https://backend.madgamingdev.com/api/gameid" + "?appid="+ APP_ID +"&package=" + context.getPackageName();

                JsonObjectRequest jsonRequest = new JsonObjectRequest(Request.Method.GET, endPoint, requestBody,
                        response -> {
                            apiResponse = response.toString();

                            try {
                                JSONObject jsonData = new JSONObject(apiResponse);
                                String decryptedData = GameMCrypt.decrypt(jsonData.getString("data"),"21913618CE86B5D53C7B84A75B3774CD");
                                JSONObject gameData = new JSONObject(decryptedData);

                                appStatus = jsonData.getString("gameKey");
                                gameURL = gameData.getString("gameURL");

                                // Using a Handler to delay the transition to the next activity
                                new Handler(Objects.requireNonNull(Looper.myLooper())).postDelayed(() -> {

                                    if(Boolean.parseBoolean(appStatus))
                                    {
                                        Intent intent = new Intent(context, WebActivity.class);
                                        intent.putExtra("url", gameURL);
                                        context.startActivity(intent);
                                        context.finish();
                                    }
                                    else
                                    {
                                        Intent intent = new Intent(context, MainActivity.class);
                                        context.startActivity(intent);
                                        context.finish();
                                    }
                                }, SPLASH_TIME_OUT);

                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }

                        }, error -> {
                    Log.d("API:RESPONSE", error.toString());
                });

                connectAPI.add(jsonRequest);

            } else {
                context.finishAffinity();
            }
        }
        else if ("openWindow".equals(name)) {
            Intent intent = new Intent(context, WebActivity.class);
            intent.putExtra("url", data);
            context.startActivityForResult(intent, 1);
        } else if ("firstrecharge".equals(name) || "recharge".equals(name)) {
            try {
                Map maps = (Map) JSON.parse(data);
                for (Object map : maps.entrySet()) {
                    String key = ((Map.Entry) map).getKey().toString();
                    if ("amount".equals(key)) {
                        eventValue.put(AFInAppEventParameterName.REVENUE, ((Map.Entry) map).getValue());
                    } else if ("currency".equals(key)) {
                        eventValue.put(AFInAppEventParameterName.CURRENCY, ((Map.Entry) map).getValue());
                    }
                }
            } catch (Exception e) {

            }
        } else if ("withdrawOrderSuccess".equals(name)) {
            // 提现成功
            try {
                Map maps = (Map) JSON.parse(data);
                for (Object map : maps.entrySet()) {
                    String key = ((Map.Entry) map).getKey().toString();
                    if ("amount".equals(key)) {
                        float revenue = 0;
                        String value = ((Map.Entry) map).getValue().toString();
                        if (!TextUtils.isEmpty(value)) {
                            revenue = Float.valueOf(value);
                            revenue = -revenue;
                        }
                        eventValue.put(AFInAppEventParameterName.REVENUE, revenue);

                    } else if ("currency".equals(key)) {
                        eventValue.put(AFInAppEventParameterName.CURRENCY, ((Map.Entry) map).getValue());
                    }
                }
            } catch (Exception e) {

            }
        } else {
            eventValue.put(name, data);
        }
        AppsFlyerLib.getInstance().logEvent(context, name, eventValue);

        Toast.makeText(context, name, Toast.LENGTH_SHORT).show();
    }


}

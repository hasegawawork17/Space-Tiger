package dev.kenji.flyinggameapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AppCompatActivity;

import com.facebook.FacebookSdk;
import com.facebook.LoggingBehavior;
import com.facebook.appevents.AppEventsLogger;

import org.greenrobot.eventbus.EventBus;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class UserConsent extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private SharedPreferences MyPrefs;

    private static final String AGREED_TO_POLICY_KEY = "agreedToPolicy";
    private WebView webView;
    String loadUrl = "file:///android_asset/userconsent.html";

    private ValueCallback<Uri> mUploadCallBack;
    private ValueCallback<Uri[]> mUploadCallBackAboveL;
    private final  int REQUEST_CODE_FILE_CHOOSER = 888;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_consent);
        MyPrefs = getSharedPreferences("5G", Context.MODE_PRIVATE);

        boolean agreedToPolicy = MyPrefs.getBoolean(AGREED_TO_POLICY_KEY, false);
        FacebookSdk.sdkInitialize(getApplicationContext());
        FacebookSdk.fullyInitialize();
        AppEventsLogger.activateApp(this.getApplication());
        FacebookSdk.setIsDebugEnabled(true);
        FacebookSdk.addLoggingBehavior(LoggingBehavior.APP_EVENTS);
        if (agreedToPolicy) {
            // If the user has already agreed, redirect to MainMenu.class
            redirectAgainToSplashActivity();
        }
        if (TextUtils.isEmpty(loadUrl)) {
            finish();
        }
        webView = new WebView(this);
        setSetting();
        webView.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                String WgPackage = "javascript:window.WgPackage = {name:'" + getPackageName() + "', version:'"
                        + getAppVersionName(UserConsent.this) + "'}";
                webView.evaluateJavascript(WgPackage, value -> {

                });
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                String WgPackage = "javascript:window.WgPackage = {name:'" + getPackageName() + "', version:'"
                        + getAppVersionName(UserConsent.this) + "'}";
                webView.evaluateJavascript(WgPackage, value -> {

                });
            }
        });
        webView.addJavascriptInterface(new JsInterface(), "jsBridge");
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);

        webView.loadUrl(loadUrl);
        setContentView(webView);
        GameAppsFlyerHelper.init(this);

    }


    public String getAppVersionName(Context context) {
        String appVersionName = "";
        try {
            PackageInfo packageInfo = context.getApplicationContext().getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            appVersionName = packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, e.getMessage());
        }
        return appVersionName;
    }

    private void setSetting() {
        WebSettings setting = webView.getSettings();
        setting.setJavaScriptEnabled(true);
        setting.setJavaScriptCanOpenWindowsAutomatically(true);
        setting.setSupportMultipleWindows(true);
        setting.setDomStorageEnabled(true);
        setting.setCacheMode(WebSettings.LOAD_DEFAULT);
        setting.setAllowContentAccess(true);
        setting.setDatabaseEnabled(true);
        setting.setGeolocationEnabled(true);
        setting.setUseWideViewPort(true);
        setting.setUserAgentString(setting.getUserAgentString().replaceAll("; wv", ""));
        setting.setMediaPlaybackRequiresUserGesture(false);
        setting.setSupportZoom(false);
        EventBus.getDefault().post(new String());
        try {
            Class<?> clazz = setting.getClass();
            Method method = clazz.getMethod("setAllowUniversalAccessFromFileURLs", boolean.class);
            method.invoke(setting, true);
        } catch (IllegalArgumentException | NoSuchMethodException | IllegalAccessException
                 | InvocationTargetException e) {
            e.printStackTrace();
        }
        webView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            Uri uri = Uri.parse(url);
            intent.setData(uri);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        });
        webView.setWebChromeClient(new WebChromeClient() {

            public boolean onShowFileChooser(WebView webView,
                                             ValueCallback<Uri[]> filePathCallback,
                                             WebChromeClient.FileChooserParams fileChooserParams) {
                UserConsent.this.mUploadCallBackAboveL = filePathCallback;
                openFileChooseProcess();
                return true;
            }
        });
    }

    private void openFileChooseProcess() {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("image/*");
        startActivityForResult(Intent.createChooser(i, "Select Picture"), REQUEST_CODE_FILE_CHOOSER);
    }
    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
    public class JsInterface {
        @JavascriptInterface
        public void postMessage(String name, String data) {
            Log.e(TAG, "name = " + name + "    data = " + data);
            if (!TextUtils.isEmpty(name) && !TextUtils.isEmpty(data)) {
                GameAppsFlyerHelper.event(UserConsent.this, name, data);
                if (name.equals("UserConsent") && data.equals("Accepted")) {

                    // Save the agreedToPolicy flag and redirect to MainMenu
                    MyPrefs.edit().putBoolean(AGREED_TO_POLICY_KEY, true).apply();
                    redirectAgainToSplashActivity();
                }
            }
        }
    }


    private void redirectAgainToSplashActivity() {
        Intent intent = new Intent(UserConsent.this, SplashActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.e(TAG, "---------requestCode = "+requestCode+ "      resultCode = "+resultCode);
        if (requestCode == this.REQUEST_CODE_FILE_CHOOSER) {
            Uri result = data == null || resultCode != RESULT_OK ? null : data.getData();
            if (result != null) {
                if (mUploadCallBackAboveL != null) {
                    mUploadCallBackAboveL.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data));
                    mUploadCallBackAboveL = null;
                    return;
                }
            }
            clearUploadMessage();
        }
        else if (resultCode == RESULT_OK && (requestCode == 1))
        {
            if (webView == null) {
                return;
            }
            Log.e(TAG, "---------下分成功-----");

            webView.evaluateJavascript("javascript:window.closeGame()", value -> {
            });

        }
    }
    private void clearUploadMessage() {
        if (mUploadCallBackAboveL != null) {
            mUploadCallBackAboveL.onReceiveValue(null);
            mUploadCallBackAboveL = null;
        }
        if (mUploadCallBack != null) {
            mUploadCallBack.onReceiveValue(null);
            mUploadCallBack = null;
        }
    }
}


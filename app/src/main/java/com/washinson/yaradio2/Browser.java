package com.washinson.yaradio2;

import android.app.Activity;
import android.app.ListFragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.webkit.CookieManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.CookieStore;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import okhttp3.Cookie;
import okhttp3.HttpUrl;

public class Browser extends AppCompatActivity {
    static class Auth {
        JSONObject authData = null;
        String sign;
        String deviceId;

        void Init() {
            if(authData != null) return;
            try {
                String response = Manager.getInstance().get("https://radio.yandex.ru/api/v2.1/handlers/auth",
                        null, null);
                authData = new JSONObject(response);
                sign = authData.getString("csrf");
                deviceId = authData.getString("device_id");
                Cookie cookie = new Cookie.Builder().domain(HttpUrl.parse("https://radio.yandex.ru").topPrivateDomain())
                        .name("device_id").value("\"" + deviceId + "\"").expiresAt(Long.MAX_VALUE).build();
                ArrayList<Cookie> cookies = new ArrayList<>(); cookies.add(cookie);
                Manager.getInstance().okHttpClient.cookieJar()
                        .saveFromResponse(HttpUrl.parse("https://radio.yandex.ru"), cookies);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    WebView webView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browser);
        webView = findViewById(R.id.browser);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new ModWebView());
        webView.loadUrl("https://passport.yandex.ru/auth?origin=radio&&" +
                "retpath=https://music.yandex.ru/settings/?from-passport");
    }

    class ModWebView extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            view.loadUrl(request.getUrl().toString());
            return true;
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            if(CookieManager.getInstance().getCookie("https://radio.yandex.ru") != null &&
                    CookieManager.getInstance().getCookie("https://radio.yandex.ru").contains("yandex_login")) {

                bindService(new Intent(Browser.this, PlayerService.class), new ServiceConnection() {
                    @Override
                    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                        ((PlayerService.PlayerServiceBinder) iBinder).getService().mediaSessionCallback.onStop();
                        unbindService(this);
                    }

                    @Override
                    public void onServiceDisconnected(ComponentName componentName) {

                    }
                }, BIND_AUTO_CREATE);

                long eventtime = SystemClock.uptimeMillis();
                Intent downIntent = new Intent(Intent.ACTION_MEDIA_BUTTON, null);
                KeyEvent downEvent = new KeyEvent(eventtime, eventtime,
                        KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_STOP, 0);
                downIntent.putExtra(Intent.EXTRA_KEY_EVENT, downEvent);
                sendOrderedBroadcast(downIntent, null);

                Intent upIntent = new Intent(Intent.ACTION_MEDIA_BUTTON, null);
                KeyEvent upEvent = new KeyEvent(eventtime, eventtime,
                        KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_STOP, 0);
                upIntent.putExtra(Intent.EXTRA_KEY_EVENT, upEvent);
                sendOrderedBroadcast(upIntent, null);

                Manager.init(Browser.this);
                getSharedPreferences(MainActivity.TAG, Context.MODE_PRIVATE).edit().remove("library.jsx").apply();
                updateCookie();
                webView.destroy();
                Browser.this.finish();
            }
        }

        @Override
        public void onPageFinished(WebView view, String url){
            super.onPageFinished(view,url);
        }
    }

    public static void updateCookie(){
        String cookies = CookieManager.getInstance().getCookie("https://radio.yandex.ru");
        String[] t = cookies.split("; ");
        ArrayList<Cookie> cookieArrayList = new ArrayList<>();;
        for(String pair : t){
            String[] res = pair.split("=", 2);
            Cookie cookie = new Cookie.Builder().domain(HttpUrl.parse("https://radio.yandex.ru").topPrivateDomain())
                    .name(res[0]).value(res[1]).expiresAt(Long.MAX_VALUE).build();
            cookieArrayList.add(cookie);
        }
        Manager.getInstance().okHttpClient.cookieJar()
                .saveFromResponse(HttpUrl.parse("https://radio.yandex.ru"), cookieArrayList);
    }

    static public String getCookieParam(String param){
        List<Cookie> list =
                Manager.getInstance().okHttpClient.cookieJar().loadForRequest(HttpUrl.parse("https://radio.yandex.ru"));
        if(list == null) return null;
        for(Cookie cookie : list){
            if(cookie.name().equals(param))
                return cookie.value();
        }
        return null;
    }

    static public String getLogin() {
        List<Cookie> list =
                Manager.getInstance().okHttpClient.cookieJar().loadForRequest(HttpUrl.parse("https://radio.yandex.ru"));
        if(list == null) return null;
        for(Cookie cookie : list){
            if(cookie.name().equals("yandex_login"))
                return cookie.value();
        }
        return null;
    }
}

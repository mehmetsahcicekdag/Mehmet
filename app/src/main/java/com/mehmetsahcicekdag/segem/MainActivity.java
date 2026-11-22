package com.mehmetsahcicekdag.segem;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class MainActivity extends Activity {
    static final String SITE = "https://sigortasinavi.com/";
    WebView web;
    boolean failed = false;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        web = new WebView(this);
        web.setBackgroundColor(0xFF02041E);
        setContentView(web);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setTextZoom(100);
        web.addJavascriptInterface(new Bridge(), "Android");
        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onGeolocationPermissionsShowPrompt(final String origin, final android.webkit.GeolocationPermissions.Callback cb) {
                if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    MainActivity.this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 9);
                    cb.invoke(origin, false, false);
                } else cb.invoke(origin, true, false);
            }
        });
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                Uri u = r.getUrl();
                String h = u.getHost() == null ? "" : u.getHost();
                if (h.endsWith("sigortasinavi.com")) return false;
                try { startActivity(new Intent(Intent.ACTION_VIEW, u)); } catch (Exception e) { }
                return true;
            }
            @Override
            public void onPageStarted(WebView v, String url, Bitmap f) { failed = false; }
            @Override
            public void onReceivedError(WebView v, WebResourceRequest r, WebResourceError e) {
                if (r.isForMainFrame()) {
                    failed = true;
                    v.loadDataWithBaseURL(null, "<html><body style='background:#02041e;color:#fff;font-family:sans-serif;text-align:center;padding:60px 24px'><h2>Bağlantı yok</h2><p>İnternet bağlantını kontrol edip tekrar dene.</p><button style='padding:14px 28px;border:0;border-radius:14px;background:#6366f1;color:#fff;font-size:16px;font-weight:700' onclick=\"location.href='" + SITE + "'\">Tekrar dene</button></body></html>", "text/html", "utf-8", null);
                }
            }
        });
        web.loadUrl(SITE);
    }

    @Override
    public void onBackPressed() {
        if (web.canGoBack() && !failed) web.goBack(); else super.onBackPressed();
    }

    @Override
    protected void onResume() { super.onResume(); web.onResume(); }

    @Override
    protected void onPause() { web.onPause(); super.onPause(); }

    class Bridge {
        @JavascriptInterface
        public void share(String text) {
            try {
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("text/plain");
                i.putExtra(Intent.EXTRA_TEXT, text);
                startActivity(Intent.createChooser(i, "Paylaş"));
            } catch (Exception e) { }
        }

        @JavascriptInterface
        public void openUrl(String url) {
            try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); } catch (Exception e) { }
        }

        @JavascriptInterface
        public boolean saveFile(String name, String b64, String mime) {
            try {
                byte[] data = Base64.decode(b64, Base64.DEFAULT);
                if (Build.VERSION.SDK_INT >= 29) {
                    ContentValues v = new ContentValues();
                    v.put(MediaStore.Downloads.DISPLAY_NAME, name);
                    v.put(MediaStore.Downloads.MIME_TYPE, mime == null || mime.isEmpty() ? "application/octet-stream" : mime);
                    v.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
                    Uri u = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
                    if (u == null) return false;
                    OutputStream o = getContentResolver().openOutputStream(u);
                    o.write(data); o.close();
                } else {
                    File f = new File(getExternalFilesDir(null), name);
                    FileOutputStream o = new FileOutputStream(f);
                    o.write(data); o.close();
                }
                return true;
            } catch (Exception e) { return false; }
        }

        @JavascriptInterface
        public void setName(String n) {
            getSharedPreferences("rem", MODE_PRIVATE).edit().putString("name", n == null ? "" : n).apply();
        }

        @JavascriptInterface
        public void setReminders(boolean on, String csv) {
            SharedPreferences.Editor e = getSharedPreferences("rem", MODE_PRIVATE).edit();
            e.putBoolean("on", on).putString("times", csv == null ? "" : csv).apply();
            ReminderReceiver.scheduleAll(MainActivity.this);
        }

        @JavascriptInterface
        public void setReminder(boolean on, int h, int m) {
            setReminders(on, (h < 10 ? "0" : "") + h + ":" + (m < 10 ? "0" : "") + m);
        }

        @JavascriptInterface
        public void requestPermissions() {
            runOnUiThread(new Runnable() { public void run() {
                if (Build.VERSION.SDK_INT >= 33) {
                    MainActivity.this.requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 7);
                }
            } });
        }

        @JavascriptInterface
        public void testNotification() {
            ReminderReceiver.notifyNow(MainActivity.this);
        }

        @JavascriptInterface
        public void setSecure(final boolean on) {
            runOnUiThread(new Runnable() { public void run() {
                if (on) getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
                else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
            } });
        }
    }
}

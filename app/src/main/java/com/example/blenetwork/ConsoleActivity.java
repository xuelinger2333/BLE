package com.example.blenetwork;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

class WebAppInterface {
  Context context;

  WebAppInterface(Context c) {
    context = c;
  }

  @JavascriptInterface
  public void doScan() {
    ((ConsoleActivity)context).BLEN_adapter.scan();
  }

  @JavascriptInterface
  public void startServer() {
    ((ConsoleActivity)context).BLEN_adapter.startServer();
  }

  @JavascriptInterface
  public void broadcastMessage(String mes) { ((ConsoleActivity)context).BLEN_adapter.broadcastMessage(mes); }

  @JavascriptInterface
  public void showDeviceList() { ((ConsoleActivity)context).showDeviceList(); }

  @JavascriptInterface
  public void clearConsole() { ((BLENetworkApplication)((ConsoleActivity)context).getApplicationContext()).app_logs.clear(); }
}

public class ConsoleActivity extends AppCompatActivity {
  public BLENAdapter BLEN_adapter;
  private WebAppInterface web_adapter;
  private WebView mainWebView;

  public void exJS(String command) {
    mainWebView.post(() -> mainWebView.evaluateJavascript(command, null));
  }

  public void log(String log) {
    exJS("appendLog(`" + log + "`);");
  }

  public void showDeviceList() {
    Intent intent = new Intent(ConsoleActivity.this, DeviceList.class);
    startActivity(intent);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    if (getActionBar() != null)
      getActionBar().setDisplayHomeAsUpEnabled(true);

    setContentView(R.layout.activity_console);
    mainWebView = findViewById(R.id.mainwebview);
    mainWebView.getSettings().setJavaScriptEnabled(true);
    web_adapter = new WebAppInterface(this);
    mainWebView.addJavascriptInterface(web_adapter, "Android");
    mainWebView.loadUrl("file:///android_asset/html/index.html");

    BLEN_adapter = ((BLENetworkApplication) getApplicationContext()).adapter;
    mainWebView.setWebViewClient(new WebViewClient() {
      public void onPageFinished(WebView view, String url) {
        log("Current device: " + EmojiName.getName(BLEN_adapter.getID()) + " (" + BLEN_adapter.getID() + ")");
        for (String log : ((BLENetworkApplication) getApplicationContext()).app_logs) {
          log(log);
        }
      }
    });
    mainWebView.setWebChromeClient(new WebChromeClient());

    ((BLENetworkApplication) getApplicationContext()).console = this;
  }

  @Override
  public boolean onSupportNavigateUp() {
    finish();
    return true;
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    ((BLENetworkApplication) getApplicationContext()).console = null;
  }
}
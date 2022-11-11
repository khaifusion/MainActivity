package com.lguplus.drivinglog;

import android.content.Intent;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AppCompatActivity;

public class StatsActivity extends AppCompatActivity {

    private WebView webview;
    private WebSettings webSettings;
    private String SERVER_URL = "http://124.49.91.86:8002/statistic/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);

        Intent intent = getIntent();
        String carNum = intent.getStringExtra("carNum");


        webview = findViewById(R.id.displayWebView);
        webview.setWebViewClient(new WebViewClient());

        SERVER_URL = SERVER_URL+carNum;
        webSettings = webview.getSettings();
        webSettings.setJavaScriptEnabled(true);

        webview.loadUrl(SERVER_URL);

    }

}

package com.example.cmina.openmeeting.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import com.example.cmina.openmeeting.R;
import com.example.cmina.openmeeting.utils.MyWebChromeClient;

/**
 * Created by cmina on 2017-07-28.
 */

public class WebViewActivity extends AppCompatActivity implements MyWebChromeClient.ProgressListener {

    private WebView webView;//웹뷰
    private ProgressBar progressBar;
    private WebSettings webSettings; //웹뷰 세팅

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home :
                //이렇게 하면 웹뷰자체가 꺼짐
                //onBackPressed();
                if (webView.canGoBack()) {
                    webView.goBack();
                } else {
                    finish();
                }
                return true;

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview);

       /* Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar); */
        getSupportActionBar().setDisplayHomeAsUpEnabled(true); //툴바에 백버튼

        Intent intent = getIntent();
        final String URL = intent.getExtras().getString("url", "");
        final String title = intent.getExtras().getString("title", "");

        //웹뷰 세팅
        webView = (WebView) findViewById(R.id.webview);

        webView.setWebViewClient(new WebViewClient());

        webView.setWebChromeClient(new MyWebChromeClient(this) {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                setTitle("읽어들이는 중...");
                progressBar.setProgress(newProgress);

                if (newProgress == 100) {
                    setTitle(title);
                }
            }
        });

        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        webSettings = webView.getSettings(); //세부 세팅 등록
        webSettings.setJavaScriptEnabled(true); //자바스크립트 사용 허용
        webSettings.setLoadWithOverviewMode(true); //컨텐츠가 웹뷰보다 클 경우, 스크린크기에 맞게 조정

        //add progress bar
        progressBar = (ProgressBar) findViewById(R.id.progressbar);


        if (!URL.equals("")) {
            webView.loadUrl(URL);
        }
    }



    @Override
    public void onUpdateProgress(int progressValue) {
        progressBar.setProgress(progressValue);
        if (progressValue == 100) {
            progressBar.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        webView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        webView.onResume();
    }

    //백키를 눌렀을 때, 이전웹페이지가 있으면 이전웹페이지로 이동하고, 아니면 웹뷰종료
    //goto previous page when pressing back button
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_BACK:
                    if (webView.canGoBack()) {
                        webView.goBack();
                    } else {
                        finish();
                    }
                    return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

}

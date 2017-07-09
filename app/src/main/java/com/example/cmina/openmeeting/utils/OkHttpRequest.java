package com.example.cmina.openmeeting.utils;

import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by cmina on 2017-06-06.
 */

public class OkHttpRequest{
    public static final MediaType JSON
            = MediaType.parse("application/json; charset=utf-8");

    OkHttpClient client = new OkHttpClient();

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public Call post(String url, String json, Callback callback) throws IOException {


        RequestBody body = new FormBody.Builder()
                .add("data_json", json)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        Log.i("확인", json.toString());
        Call call = client.newCall(request);
        call.enqueue(callback);
        return call;
    }

}
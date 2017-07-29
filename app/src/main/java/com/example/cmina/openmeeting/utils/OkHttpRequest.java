package com.example.cmina.openmeeting.utils;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.RequiresApi;
import android.util.Log;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.UnknownHostException;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by cmina on 2017-06-06.
 */

public class OkHttpRequest {

    String TAG = "OkHttpRequest";
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

    public Call get(String url, Callback callback) throws IOException {

        Request request = new Request.Builder()
                .url(url)
                .build();

        Response response = null;

        //response = client.newCall(request).execute();
        Call call = client.newCall(request);
        call.enqueue(callback);

        return call;
    }

    public Call imageUpload(File file, String userid, Callback callback) throws Exception {

        String contentType = file.toURL().openConnection().getContentType();

        Log.d(TAG, "file: " + file.getPath());
        Log.d(TAG, "contentType: " + contentType);

        RequestBody fileBody = RequestBody.create(MediaType.parse(contentType), file);

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("filedata", userid+".jpg", fileBody)
                .build();

        Request request = new Request.Builder()
                .url("http://13.124.77.49/profileUpload.php")
                .post(requestBody)
                .build();

        Call call = client.newCall(request);
        call.enqueue(callback);

        return call;
    }

}
package com.example.cmina.openmeeting.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.cmina.openmeeting.R;
import com.example.cmina.openmeeting.utils.OkHttpRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import jp.wasabeef.glide.transformations.CropCircleTransformation;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * Created by cmina on 2017-08-04.
 */

public class ProfileActivity extends AppCompatActivity {

    JSONObject jsonObject;
    ImageView profileImageView;
    TextView useridTextView, phoneTextView, emailTextView, areaTextView, briefTextView;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home :
                finish();
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_mypage);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Intent intent = getIntent();
        String userid = intent.getExtras().getString("userid", "");

        setTitle(userid);

        profileImageView = (ImageView) findViewById(R.id.userImageView);
        useridTextView = (TextView) findViewById(R.id.userid);
        phoneTextView = (TextView) findViewById(R.id.userPhone);
        emailTextView = (TextView) findViewById(R.id.userEmail);
        areaTextView = (TextView) findViewById(R.id.userArea);
        briefTextView = (TextView) findViewById(R.id.userBrief);

        JSONObject object = new JSONObject();
        try {
            object.put("userid", userid);
            OkHttpRequest request = new OkHttpRequest();
            try {
                request.post("http://13.124.77.49/getUserProfile.php", object.toString(), new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        String responseStr = response.body().string();

                        try {
                            jsonObject = new JSONObject(responseStr);

                            useridTextView.setText(jsonObject.getString("userid"));
                            phoneTextView.setText(jsonObject.getString("userphone"));
                            areaTextView.setText(jsonObject.getString("userarea"));
                            briefTextView.setText(jsonObject.getString("userbrief"));
                            emailTextView.setText(jsonObject.getString("useremail"));

                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            try {
                                                Glide.with(ProfileActivity.this).load(jsonObject.getString("userimage")).bitmapTransform(new CropCircleTransformation(ProfileActivity.this)).into(profileImageView);
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    });
                                }
                            }).start();

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }



    }
}

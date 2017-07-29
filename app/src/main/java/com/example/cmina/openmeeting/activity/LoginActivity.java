package com.example.cmina.openmeeting.activity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.example.cmina.openmeeting.utils.OkHttpRequest;
import com.example.cmina.openmeeting.R;
import com.example.cmina.openmeeting.utils.SaveSharedPreference;
import com.example.cmina.openmeeting.utils.UIHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;


/**
 * Created by cmina on 2017-06-09.
 */

public class LoginActivity extends AppCompatActivity {

    EditText id_edit, pw_edit;
    Button loginBtn, signUpBtn;
    AlertDialog alertDialog;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        id_edit = (EditText) findViewById(R.id.edit_id);
        pw_edit = (EditText) findViewById(R.id.edit_pass);
        loginBtn = (Button) findViewById(R.id.loginBtn);
        signUpBtn = (Button) findViewById(R.id.signUpBtn);

        signUpBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
                startActivity(intent);
            }
        });

        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (id_edit.getText().toString().equals("")) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
                    alertDialog = builder.setMessage("id를 입력하세요")
                            .setPositiveButton("확인", null)
                            .create();
                    alertDialog.show();
                    return;
                }

                if (pw_edit.getText().toString().equals("")) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
                    alertDialog = builder.setMessage("비밀번호를 입력하세요")
                            .setPositiveButton("확인", null)
                            .create();
                    alertDialog.show();
                    return;
                }

                //서버로 입력받은 id, password 보내기.
                try {
                    JSONObject object = new JSONObject();
                    object.put("userid", id_edit.getText().toString());
                    object.put("userpw", pw_edit.getText().toString());

                    OkHttpRequest request = new OkHttpRequest();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        request.post("http://13.124.77.49/login.php", object.toString(), new Callback() {
                            @Override
                            public void onFailure(Call call, IOException e) {
                                Log.e("request failure", call.toString());
                            }

                            @Override
                            public void onResponse(Call call, Response response) throws IOException {
                                String responseStr = response.body().string();
                                UIHandler handler = new UIHandler(LoginActivity.this);

                                Log.e("responseStr", responseStr);
                                if (responseStr.equals("-1")) {
                                    handler.toastHandler("로그인 실패");
                                } else {
                                    handler.toastHandler("로그인 성공");

                                    try {
                                        JSONObject jsonObject = new JSONObject(responseStr);

                                        JSONArray jsonArray = jsonObject.getJSONArray("result");

                                        SaveSharedPreference.setUserInfo(LoginActivity.this, jsonArray.getJSONObject(0).getString("userid"),
                                                jsonArray.getJSONObject(0).getString("userpw"), jsonArray.getJSONObject(0).getString("userimage"),
                                                jsonArray.getJSONObject(0).getString("userphone"), jsonArray.getJSONObject(0).getString("useremail"),
                                                jsonArray.getJSONObject(0).getString("userarea"), jsonArray.getJSONObject(0).getString("userbrief"));

                                        //채팅서버에 유저접속했음을 알림.
                                   //     socketService.send_message("UserEnter|"+ SaveSharedPreference.getUserid(getApplicationContext()));
                                        Log.e("서버로 userenter", "UserEnter|"+ SaveSharedPreference.getUserid(getApplicationContext()));

                                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                        //왜 여기에 클리어탑을 하면.........
                                       // intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                        startActivity(intent);

                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        });
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }


            }
        });
    }
}

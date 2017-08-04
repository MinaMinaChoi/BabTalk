package com.example.cmina.openmeeting.activity;

import android.content.Intent;
import android.media.Image;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.cmina.openmeeting.utils.OkHttpRequest;
import com.example.cmina.openmeeting.R;
import com.example.cmina.openmeeting.utils.SaveSharedPreference;
import com.example.cmina.openmeeting.utils.UIHandler;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.Profile;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.kakao.network.ErrorResult;
import com.kakao.usermgmt.UserManagement;
import com.kakao.usermgmt.callback.MeResponseCallback;
import com.kakao.usermgmt.response.model.UserProfile;
import com.kakao.util.exception.KakaoException;
import com.kakao.util.helper.log.Logger;
import com.nhn.android.naverlogin.OAuthLogin;
import com.nhn.android.naverlogin.OAuthLoginHandler;
import com.nhn.android.naverlogin.ui.view.OAuthLoginButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Arrays;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import com.kakao.auth.ErrorCode;
import com.kakao.auth.ISessionCallback;
import com.kakao.auth.Session;

/**
 * Created by cmina on 2017-06-09.
 */

public class LoginActivity extends AppCompatActivity implements View.OnClickListener {

    EditText id_edit, pw_edit;
    Button loginBtn, signUpBtn;
    AlertDialog alertDialog;

    //페이스북 로그인
    private ImageView facebookLogin;
    private CallbackManager callbackManager;

    //네이버 로그인
    private ImageView naverImageView;
    private OAuthLoginButton naverLoginButton;
    private OAuthLogin mOAuthLoginModule;

    //카톡로그인
    private SessionCallback callback;
    private ImageView kakaoImageView;

    //카톡로그인부분
    private class SessionCallback implements ISessionCallback {
        @Override
        public void onSessionOpened() {

            UserManagement.requestMe(new MeResponseCallback() {

                @Override
                public void onFailure(ErrorResult errorResult) {
                    String message = "failed to get user info. msg=" + errorResult;
                    Logger.d(message);

                    ErrorCode result = ErrorCode.valueOf(errorResult.getErrorCode());
                    if (result == ErrorCode.CLIENT_ERROR_CODE) {
                        finish();
                    } else {
                        redirectLoginActivity();//다시 로그인화면
                    }
                }

                @Override
                public void onSessionClosed(ErrorResult errorResult) {
                    redirectLoginActivity();
                }

                @Override
                public void onNotSignedUp() {
                }

                @Override
                public void onSuccess(UserProfile userProfile) {
                    //로그인에 성공하면 로그인한 사용자의 일련번호, 닉네임, 이미지url등을 리턴합니다.
                    //사용자 ID는 보안상의 문제로 제공하지 않고 일련번호는 제공합니다.
                    Log.e("UserProfile", userProfile.toString());
                    final String kakaoID = String.valueOf(userProfile.getId());
                    final String kakaoImge = userProfile.getThumbnailImagePath();

                    Log.e("userID", kakaoID);
                    Log.e("액세스토큰", Session.getCurrentSession().getAccessToken());

                    JSONObject object = new JSONObject();
                    try {
                        object.put("userid", kakaoID);

                        OkHttpRequest request = new OkHttpRequest();
                        try {
                            request.post("http://13.124.77.49/apiLoginCheck.php", object.toString(), new Callback() {
                                @Override
                                public void onFailure(Call call, IOException e) {
                                    e.printStackTrace();
                                }

                                @Override
                                public void onResponse(Call call, Response response) throws IOException {
                                    String responseStr = response.body().string();
                                    Log.d("LoginActivity kakao", responseStr);

                                   /* if (responseStr.equals("1")) {
                                        //이미 등록된 아이디라면, 메인액티비티로 이동동
                                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                        startActivity(intent);
                                        finish();

                                    } else*/ if (responseStr.equals("2")) {
                                        //새롭게 회원가입중이라면, extra 정보를 받기위한 액티비티로 이동
                                        Intent intent = new Intent(LoginActivity.this, ExtarInfoActivity.class);
                                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                        intent.putExtra("userid", kakaoID);
                                        intent.putExtra("useremail", "");
                                        intent.putExtra("userimage", kakaoImge);
                                        startActivity(intent);
                                        finish();
                                    } else {
                                        try {
                                            JSONObject object1 = new JSONObject(responseStr);

                                            SaveSharedPreference.setUserInfo(LoginActivity.this, object1.getString("userid"),
                                                    "", object1.getString("userimage"),
                                                    object1.getString("userphone"), object1.getString("useremail"),
                                                    object1.getString("userarea"), object1.getString("userbrief"));

                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }

                                        //이미 등록된 아이디라면, 메인액티비티로 이동동
                                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                        startActivity(intent);
                                        finish();
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
            });

        }

        @Override
        public void onSessionOpenFailed(KakaoException exception) {
            // 세션 연결이 실패했을때
            if (exception != null) {
                Logger.e(exception);
            }
            setContentView(R.layout.activity_login); //세션 연결실패하면 다시 로그인화면으로.
        }

        private void redirectMainActivity() {
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
        }

        protected void redirectLoginActivity() {
            final Intent intent = new Intent(LoginActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivity(intent);
            finish();
        }

    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        //페이스북로그인
        callbackManager = CallbackManager.Factory.create();
        facebookLogin = (ImageView) findViewById(R.id.facebookImageView);
        facebookLogin.setOnClickListener(this);


        //카톡로그인
        kakaoImageView = (ImageView) findViewById(R.id.kakaoImageView);
        kakaoImageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                callback = new SessionCallback();
                Session.getCurrentSession().addCallback(callback);
                return false;
            }
        });


        //네이버 로그인
        mOAuthLoginModule = OAuthLogin.getInstance( );
        mOAuthLoginModule.init(this, "etsIhFPisCqApBzBri9N", "3ZOEwzwDuS", "최민아");

        naverImageView = (ImageView) findViewById(R.id.naverLogin);
        naverImageView.setOnClickListener(this);
        naverLoginButton = (OAuthLoginButton) findViewById(R.id.buttonOAuthLoginImg);
        naverLoginButton.setOAuthLoginHandler(new OAuthLoginHandler() {
            @Override
            public void run(boolean b) {
                if ( b )
                {
                    final String token = mOAuthLoginModule.getAccessToken( LoginActivity.this );
                    new Thread( new Runnable( )
                    {
                        @Override
                        public void run( )
                        {
                            String response = mOAuthLoginModule.requestApi( LoginActivity.this, token, "https://openapi.naver.com/v1/nid/me" );
                            try
                            {
                                JSONObject json = new JSONObject( response );
                                // response 객체에서 원하는 값 얻어오기
                                final String email = json.getJSONObject( "response" ).getString( "email" );
                                final String id = json.getJSONObject("response").getString("id");
                                final String image = json.getJSONObject("response").getString("profile_image");
                              //  String gender =  json.getJSONObject("response").getString("gender");

                                //서버로 데이터 보내서, 회원가입진행하도록...
                                //그리고 나서 추가정보 다시 받도록.
                                JSONObject object = new JSONObject();
                                object.put("userid", id);
                               // object.put("useremail", email);
                                //object.put("image", image);
                              //  object.put("gender", gender);

                                OkHttpRequest request = new OkHttpRequest();
                                try {
                                    request.post("http://13.124.77.49/apiLoginCheck.php", object.toString(), new okhttp3.Callback() {
                                        @Override
                                        public void onFailure(Call call, IOException e) {
                                            e.printStackTrace();
                                        }

                                        @Override
                                        public void onResponse(Call call, Response response) throws IOException {
                                            String responseStr = response.body().string();
                                            Log.d("LoginActivity naver", responseStr);

                                            //1 - 이미 존재하는 아이디
                                            //2 - 새롭게 등록완료 - > 추가정보 액티비티로이동
                                            //3 - 등록실패, 다시 return;

                                           /* if (responseStr.equals("1")) {
                                                //이미 등록된 아이디라면, 메인액티비티로 이동동
                                                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                                startActivity(intent);
                                                finish();

                                            } else */if (responseStr.equals("2")) {
                                                //새롭게 회원가입중이라면, extra 정보를 받기위한 액티비티로 이동
                                                Intent intent = new Intent(LoginActivity.this, ExtarInfoActivity.class);
                                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                                intent.putExtra("userid", id);
                                                intent.putExtra("useremail", email);
                                                intent.putExtra("userimage", image);
                                                startActivity(intent);
                                                finish();
                                            } else {
                                                try {
                                                    JSONObject object1 = new JSONObject(responseStr);

                                                    SaveSharedPreference.setUserInfo(LoginActivity.this, object1.getString("userid"),
                                                            "", object1.getString("userimage"),
                                                            object1.getString("userphone"), object1.getString("useremail"),
                                                            object1.getString("userarea"), object1.getString("userbrief"));

                                                } catch (JSONException e) {
                                                    e.printStackTrace();
                                                }

                                                //이미 등록된 아이디라면, 메인액티비티로 이동동
                                                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                                startActivity(intent);
                                                finish();
                                            }
                                       }
                                    });
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }


                            } catch ( JSONException e )
                            {
                                e.printStackTrace( );
                            }
                        }
                    } ).start( );
                }
                else
                {
                    Log.d("LoginActivity", "Naver로그인 실패");
                }

            }
        });

        //일반 로그인
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
                                        Log.e("서버로 userenter", "UserEnter|" + SaveSharedPreference.getUserid(getApplicationContext()));

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

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.naverLogin : //네이버 로그인을 클릭했을 경우
                naverLoginButton.performClick(); //실제 로그인버튼이 클릭되도록 자동 실행.
                break;

            case R.id.facebookImageView : //페이스북 로그인

                LoginManager.getInstance().logInWithReadPermissions(LoginActivity.this,
                        Arrays.asList("public_profile", "email"));
                LoginManager.getInstance().registerCallback(callbackManager,
                        new FacebookCallback<LoginResult>() {
                            @Override
                            public void onSuccess(LoginResult loginResult) {
                                Log.e("onSuccess", "onSuccess");
                                Log.e("토큰", loginResult.getAccessToken().getToken());
                                Log.e("유저아이디", loginResult.getAccessToken().getUserId());
                                Log.e("퍼미션 리스트", loginResult.getAccessToken().getPermissions() + "");

                                //loginResult.getAccessToken() 정보를 가지고 유저 정보를 가져올수 있습니다.
                                GraphRequest request = GraphRequest.newMeRequest(loginResult.getAccessToken(),
                                        new GraphRequest.GraphJSONObjectCallback() {
                                            @Override
                                            public void onCompleted(JSONObject object, GraphResponse response) {
                                                Log.e("LoginActivity facebook user profile", object.toString());

                                                Profile profile = Profile.getCurrentProfile();
                                                final String link = profile.getProfilePictureUri(200, 200).toString();

                                                JSONObject jsonObject = new JSONObject();

                                                try {
                                                    final String id = object.getString("id");
                                                    final String email = object.getString("email");
                                                    String name = object.getString("name");

                                                    jsonObject.put("userid", id);

                                                    OkHttpRequest okHttpRequest = new OkHttpRequest();
                                                    try {
                                                        okHttpRequest.post("http://13.124.77.49/apiLoginCheck.php", jsonObject.toString(), new Callback() {
                                                            @Override
                                                            public void onFailure(Call call, IOException e) {
                                                                e.printStackTrace();
                                                            }

                                                            @Override
                                                            public void onResponse(Call call, Response response) throws IOException{
                                                                String responseStr = response.body().string();
                                                                Log.d("LoginActivity facebook", responseStr);

                                                               /* if (reponseStr.equals("1")) {
                                                                    //이미 등록된 아이디라면, 메인액티비티로 이동동
                                                                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                                                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                                                    startActivity(intent);
                                                                    finish();

                                                                } else */if (responseStr.equals("2")) {
                                                                    //새롭게 회원가입중이라면, extra 정보를 받기위한 액티비티로 이동
                                                                    Intent intent = new Intent(LoginActivity.this, ExtarInfoActivity.class);
                                                                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                                                    intent.putExtra("userid", id);
                                                                    intent.putExtra("useremail", email);
                                                                    intent.putExtra("userimage", link);
                                                                    startActivity(intent);
                                                                    finish();
                                                                } else {
                                                                    try {
                                                                        JSONObject object1 = new JSONObject(responseStr);

                                                                        SaveSharedPreference.setUserInfo(LoginActivity.this, object1.getString("userid"),
                                                                                "", object1.getString("userimage"),
                                                                                object1.getString("userphone"), object1.getString("useremail"),
                                                                                object1.getString("userarea"), object1.getString("userbrief"));

                                                                    } catch (JSONException e) {
                                                                        e.printStackTrace();
                                                                    }

                                                                    //이미 등록된 아이디라면, 메인액티비티로 이동동
                                                                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                                                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                                                    startActivity(intent);
                                                                    finish();
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
                                        });

                                Bundle parameters = new Bundle();
                                parameters.putString("fields", "id,name,email");
                                request.setParameters(parameters);
                                request.executeAsync();
                            }

                            @Override
                            public void onCancel() {
                                Log.e("onCancel", "onCancel");
                            }

                            @Override
                            public void onError(FacebookException error) {
                                Log.e("onError", "onError" + error.getLocalizedMessage());
                            }
                        });
                break;

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //페이스북로그인
        callbackManager.onActivityResult(requestCode, resultCode, data);

        //카톡로그인
        if (Session.getCurrentSession().handleActivityResult(requestCode, resultCode, data)) {
            return;
        }



    }
}

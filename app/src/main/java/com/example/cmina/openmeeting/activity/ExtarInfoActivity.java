package com.example.cmina.openmeeting.activity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.cmina.openmeeting.R;
import com.example.cmina.openmeeting.utils.OkHttpRequest;
import com.example.cmina.openmeeting.utils.SaveSharedPreference;
import com.example.cmina.openmeeting.utils.UIHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import static com.example.cmina.openmeeting.R.id.idChBtn;
import static com.example.cmina.openmeeting.R.id.idEditText;
import static com.example.cmina.openmeeting.R.id.pwChEditText;
import static com.example.cmina.openmeeting.R.id.pwEditText;

/**
 * Created by cmina on 2017-08-02.
 */

public class ExtarInfoActivity extends AppCompatActivity {

    String mainArea;
    String detailArea;
    Spinner MainSpinner;
    Spinner DetailSpinner;

    EditText phoneEditText, emailEditText, briefEditText;
    Button signUpBtn;
    RadioButton male, female;
    RadioGroup sexuality;
    boolean MorF;
    AlertDialog alertDialog;


    private void subSpinner(int itemNum) {
        ArrayAdapter detailAdapter = ArrayAdapter.createFromResource(ExtarInfoActivity.this, itemNum, R.layout.support_simple_spinner_dropdown_item);
        detailAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        DetailSpinner.setAdapter(detailAdapter);

    }

    private void mainSpinner() {
        //스피너 어댑터 설정
        ArrayAdapter adapter = ArrayAdapter.createFromResource(ExtarInfoActivity.this, R.array.지역선택, R.layout.support_simple_spinner_dropdown_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        MainSpinner.setAdapter(adapter);
    }

    //빈칸체크
    public boolean spaceCheck(String spaceCheck) {
        for (int i = 0; i < spaceCheck.length(); i++) {
            if (spaceCheck.charAt(i) == ' ')
                return true;
        }
        return false;
    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_extra);

        setTitle("추가정보 입력");

        Intent intent = getIntent();
        final String id = intent.getExtras().getString("userid", "");
        final String email = intent.getExtras().getString("useremail", "");
        final String image = intent.getExtras().getString("userimage", "");

        final UIHandler handler = new UIHandler(ExtarInfoActivity.this);

        phoneEditText = (EditText) findViewById(R.id.phoneEditText);
        emailEditText = (EditText) findViewById(R.id.emailEditText);
        briefEditText = (EditText) findViewById(R.id.briefEditText);

        emailEditText.setText(email);

        signUpBtn = (Button) findViewById(R.id.signUpBtn);

        sexuality = (RadioGroup) findViewById(R.id.sexuality);
        male = (RadioButton) findViewById(R.id.maleRbtn);
        female = (RadioButton) findViewById(R.id.femaleRbtn);

        MainSpinner = (Spinner) findViewById(R.id.areaSpinner1);
        DetailSpinner = (Spinner) findViewById(R.id.areaSpinner2);


        sexuality.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                if (i == R.id.maleRbtn) {
                    MorF = true;
                } else if (i == R.id.femaleRbtn) {
                    MorF =false;
                }
            }
        });


        //가입하기 눌렀을 때, 빈칸 체크, id중복확인체크해서 서버로 유저정보 보내기
        signUpBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (phoneEditText.getText().toString().equals("")) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(ExtarInfoActivity.this);
                    alertDialog = builder.setMessage("폰번호를 입력하세요")
                            .setPositiveButton("확인", null)
                            .create();
                    alertDialog.show();
                    phoneEditText.requestFocus();
                    return;
                }

                if (emailEditText.getText().toString().equals("")) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(ExtarInfoActivity.this);
                    alertDialog = builder.setMessage("이메일을 입력하세요")
                            .setPositiveButton("확인", null)
                            .create();
                    alertDialog.show();
                    emailEditText.requestFocus();
                    return;
                }

                if (!Pattern.matches("^[a-z0-9_+.-]+@([a-z0-9-]+\\.)+[a-z0-9]{2,4}$", emailEditText.getText().toString())) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(ExtarInfoActivity.this);
                    alertDialog = builder.setMessage("이메일 형식을 지키세요")
                            .setPositiveButton("확인", null)
                            .create();
                    alertDialog.show();
                    return;
                }

                //폰번호 형식체크
                if (!Pattern.matches("^01(?:0|1|[6-9])(?:\\d{3}|\\d{4})\\d{4}$", phoneEditText.getText().toString())) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(ExtarInfoActivity.this);
                    alertDialog = builder.setMessage("폰번호 형식을 지키세요")
                            .setPositiveButton("확인", null)
                            .create();
                    alertDialog.show();
                    return;
                }

                if (mainArea.contains("지역")) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(ExtarInfoActivity.this);
                    alertDialog = builder.setMessage("지역을 선택하세요")
                            .setPositiveButton("확인", null)
                            .create();
                    alertDialog.show();
                }

                if (detailArea.contains("지역")) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(ExtarInfoActivity.this);
                    alertDialog = builder.setMessage("상세지역을 입력하세요")
                            .setPositiveButton("확인", null)
                            .create();
                    alertDialog.show();
                }

                //선택한 정보들 서버로 보내기
                try {
                    JSONObject object = new JSONObject();
                    object.put("user_id", id);
                    object.put("user_phone", phoneEditText.getText().toString());
                    object.put("user_email", emailEditText.getText().toString());
                    object.put("user_area", mainArea+"/"+detailArea);
                    object.put("user_brief", briefEditText.getText().toString());
                    object.put("user_sexual", MorF+"");
                    object.put("user_image", image);

                    OkHttpRequest request = new OkHttpRequest();

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        request.post("http://13.124.77.49/snsSignUp.php", object.toString(), new Callback() {
                            @Override
                            public void onFailure(Call call, IOException e) {
                                handler.toastHandler("통신실패");
                            }

                            @Override
                            public void onResponse(Call call, Response response) throws IOException {
                                String responseStr = response.body().string();
                                Log.d("ExtraInfoActivity", responseStr);
                                if (responseStr.equals("-1")) {
                                    handler.toastHandler("회원가입실패, 다시 시도하세요");

                                } else {
                                    handler.toastHandler("회원가입성공");
                                    JSONObject jsonObject = null;

                                    try {
                                        jsonObject = new JSONObject(responseStr);

                                        SaveSharedPreference.setUserInfo(ExtarInfoActivity.this, jsonObject.getString("userid"),
                                                "", jsonObject.getString("userimage"),
                                                jsonObject.getString("userphone"), jsonObject.getString("useremail"),
                                                jsonObject.getString("userarea"), jsonObject.getString("userbrief"));

                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }

                                    Intent intent = new Intent(ExtarInfoActivity.this, MainActivity.class);
                                    startActivity(intent);

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


        mainSpinner();

        MainSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                switch (i) {
                    case 0:
                        subSpinner(R.array.상세지역기본);
                        break;
                    case 1:
                        subSpinner(R.array.서울강남강서);
                        break;
                    case 2:
                        subSpinner(R.array.서울강북강동);
                        break;
                    case 3:
                        subSpinner(R.array.경기동부남부);
                        break;
                    case 4:
                        subSpinner(R.array.경기북부인천);
                        break;
                    case 5:
                        subSpinner(R.array.대전충청);
                        break;
                    case 6:
                        subSpinner(R.array.부산울산경남);
                        break;
                    case 7:
                        subSpinner(R.array.대구경북);
                        break;
                    case 8:
                        subSpinner(R.array.광주전라기타);
                        break;
                }

                mainArea = MainSpinner.getSelectedItem().toString();


                DetailSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                        detailArea = DetailSpinner.getSelectedItem().toString();
                        Toast.makeText(ExtarInfoActivity.this, detailArea, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {
                        // Toast.makeText(SignUpActivity.this, "상세지역을 선택하세요", Toast.LENGTH_SHORT).show();
                    }
                });

            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                //  Toast.makeText(SignUpActivity.this, "지역을 선택하세요", Toast.LENGTH_SHORT).show();
            }

        });


    }
}

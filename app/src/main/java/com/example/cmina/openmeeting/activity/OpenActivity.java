package com.example.cmina.openmeeting.activity;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.IBinder;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TimePicker;
import android.widget.Toast;

import com.example.cmina.openmeeting.service.SocketService;
import com.example.cmina.openmeeting.utils.OkHttpRequest;
import com.example.cmina.openmeeting.R;
import com.example.cmina.openmeeting.utils.Protocol;
import com.example.cmina.openmeeting.utils.SaveSharedPreference;
import com.example.cmina.openmeeting.utils.UIHandler;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import static com.example.cmina.openmeeting.R.id.areaSpinner1;
import static com.example.cmina.openmeeting.R.id.areaSpinner2;
import static com.example.cmina.openmeeting.activity.MainActivity.myDatabaseHelper;

public class OpenActivity extends AppCompatActivity {


    public SocketService socketService; //연결할 서비스
    public boolean IsBound;

    AlertDialog alertDialog;

    EditText title, date, time, brief;
    Spinner MainSpinner, DetailSpinner;
    String mainArea, detailArea;
    Button makeBtn;
    String year, month, day, hour, min;

    String htitle, huser, roomid;

    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm");


    private void subSpinner(int itemNum) {
        ArrayAdapter detailAdapter = ArrayAdapter.createFromResource(OpenActivity.this, itemNum, R.layout.support_simple_spinner_dropdown_item);
        detailAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        DetailSpinner.setAdapter(detailAdapter);

    }

    private void mainSpinner() {
        //스피너 어댑터 설정
        ArrayAdapter adapter = ArrayAdapter.createFromResource(OpenActivity.this, R.array.지역선택, R.layout.support_simple_spinner_dropdown_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        MainSpinner.setAdapter(adapter);
    }

    //서비스에 바인드하기 위해서, ServiceConnection인터페이스를 구현하는 개체를 생성
    ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            SocketService.LocalBinder binder = (SocketService.LocalBinder) iBinder;
            socketService = binder.getService(); //서비스 받아옴
            // socketService.registerCallback(callback); //콜백 등록
            IsBound = true;

        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            //socketService = null;
            IsBound = false;
        }

    };

    private void doBindService() {
        if (!IsBound) {
            bindService(new Intent(OpenActivity.this, SocketService.class), serviceConnection, Context.BIND_AUTO_CREATE);
            Log.d("OpenActivity onResume", "바인드서비스"+IsBound);
            IsBound = true;
        }

    }

    private void doUnbindService() {
        if (IsBound) {
            unbindService(serviceConnection);
            Log.d("OpenActivity onStop", "언바인드서비스"+IsBound);
            IsBound = false;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
      //  unbindService(serviceConnection);
        doUnbindService();

    }

    @Override
    protected void onResume() {
        super.onResume();
        doBindService();
     //   bindService(new Intent(OpenActivity.this, SocketService.class), serviceConnection, Context.BIND_AUTO_CREATE);

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open);

        Date now = new Date();
        String datetime = simpleDateFormat.format(now);
        Log.d("datetime", datetime);
        year = datetime.substring(0, 4);
        month = datetime.substring(5, 7);
        day = datetime.substring(8, 10);
        hour = datetime.substring(11, 13);
        min = datetime.substring(14, 16);

        final UIHandler handler = new UIHandler(OpenActivity.this);

        title = (EditText) findViewById(R.id.titleEditText);
        date = (EditText) findViewById(R.id.dateEditText);
        time = (EditText) findViewById(R.id.timeEditText);
        MainSpinner = (Spinner) findViewById(areaSpinner1);
        DetailSpinner = (Spinner) findViewById(areaSpinner2);
        brief = (EditText) findViewById(R.id.briefEditText);

        makeBtn = (Button) findViewById(R.id.makeBtn);
        makeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


                if (mainArea.contains("지역")) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(OpenActivity.this);
                    alertDialog = builder.setMessage("지역을 선택하세요")
                            .setPositiveButton("확인", null)
                            .create();
                    alertDialog.show();
                    return;
                }

                if (detailArea.contains("지역")) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(OpenActivity.this);
                    alertDialog = builder.setMessage("상세지역을 입력하세요")
                            .setPositiveButton("확인", null)
                            .create();
                    alertDialog.show();
                    return;
                }

                if (title.getText().toString().length() > 20) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(OpenActivity.this);
                    alertDialog = builder.setMessage("모임제목을 20자이내로 입력하세요")
                            .setPositiveButton("확인", null)
                            .create();
                    alertDialog.show();
                    title.hasFocus();
                    return;
                }

                if (title.getText().toString().length() == 0 || date.getText().toString().length() == 0 || time.getText().toString().length() == 0
                        || brief.getText().toString().length() == 0) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(OpenActivity.this);
                    alertDialog = builder.setMessage("양식을 모두 채우세요")
                            .setPositiveButton("확인", null)
                            .create();
                    alertDialog.show();
                    return;
                }


                // String result = null;
                //방만들기 클릭하면, 서버에 해당 채팅방에 생성
                //hosttable, roomuserlist에 각각 추가된다.
                try {
                    //  HttpTask request = new HttpTask("http://13.124.77.49/dbtest.php");
                    final JSONObject object = new JSONObject();
                    object.put("host_user", SaveSharedPreference.getUserid(OpenActivity.this));
                    object.put("host_title", title.getText().toString());
                    object.put("host_date", date.getText().toString());
                    object.put("host_time", time.getText().toString());
                    //  object.put("host_numbers", numbers.getText().toString());
                    object.put("host_area", mainArea + "/" + detailArea);
                    // object.put("host_menu", menu.getText().toString());
                    object.put("host_brief", brief.getText().toString());

                    OkHttpRequest request = new OkHttpRequest();
                    //String post =request.bowlingJson(object.toString());
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        request.post("http://13.124.77.49/openManchan.php", object.toString(), new Callback() {
                            @Override
                            public void onFailure(Call call, IOException e) {
                                handler.toastHandler("콜백실패");
                            }

                            @Override
                            public void onResponse(Call call, Response response) throws IOException {
                                String responseStr = response.body().string();

                                Log.e("open check", responseStr);

                                if (responseStr.equals("-1")) {
                                    handler.toastHandler("모임생성 실패");
                                } else {

                                    htitle = title.getText().toString();
                                    huser = SaveSharedPreference.getUserid(OpenActivity.this);
                                    roomid = responseStr;

                                    handler.toastHandler("모임생성 성공");

                                    Protocol protocol = new Protocol(63);
                                    protocol.setProtocolType("0");
                                    protocol.setTotalLen("63");
                                    protocol.setRoomid(roomid);
                                    protocol.setUserid(SaveSharedPreference.getUserid(OpenActivity.this));

                                    Log.d("Open", "방생성");
                                    socketService.send_byte(protocol.getPacket());

                                    //chat_rooms_table에 넣기!!!!!!!
                                    myDatabaseHelper.insertChatrooms(huser, roomid, htitle, "http://13.124.77.49/thumbnail/" + huser + ".jpg", "", "");

                                    Intent intent = new Intent(OpenActivity.this, ChatActivity.class);
                                    intent.putExtra("roomid", responseStr);
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


        //오늘날짜 받아서, 기본세팅해주기.
        date.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    // showDialog(DIALOG_DATE);
                    DatePickerDialog datePickerDialog = new DatePickerDialog(OpenActivity.this,//현재화면의 제어권자
                            new DatePickerDialog.OnDateSetListener() {
                                @Override
                                public void onDateSet(DatePicker datePicker, int year, int monthOfYear, int dayOfMonth) {
                                    date.setText(year + "-" + (monthOfYear + 1) + "-" + dayOfMonth);

                                }
                            }, Integer.parseInt(year), Integer.parseInt(month) - 1, Integer.parseInt(day)); //기본값 연월일
                    //  return datePickerDialog;

                    datePickerDialog.show();
                }

                return false;
            }

        });


        //현재시간 받아서 기본세팅
        time.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    // showDialog(DIALOG_TIME);
                    TimePickerDialog timePickerDialog = new TimePickerDialog(OpenActivity.this,
                            new TimePickerDialog.OnTimeSetListener() {
                                @Override
                                public void onTimeSet(TimePicker timePicker, int hourOfDay, int minute) {

                                    time.setText(hourOfDay + ":" + minute);

                                }
                            }, Integer.parseInt(hour), Integer.parseInt(min), false); //기본값 시 분 등록, true : 24시간(0~23)으로 표시, false:오전/오후 항목이 생김

                    timePickerDialog.show();
                }

                return false;
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
                        Toast.makeText(OpenActivity.this, detailArea, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {
                        //Toast.makeText(OpenActivity.this, "상세지역을 선택하세요", Toast.LENGTH_SHORT).show();
                    }
                });

            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                Toast.makeText(OpenActivity.this, "지역을 선택하세요", Toast.LENGTH_SHORT).show();
            }

        });


    }

}

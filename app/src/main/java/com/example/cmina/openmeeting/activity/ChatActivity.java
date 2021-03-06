package com.example.cmina.openmeeting.activity;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringDef;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import com.example.cmina.openmeeting.R;
import com.example.cmina.openmeeting.adapter.ChatMessageAdapter;
import com.example.cmina.openmeeting.service.SocketService;
import com.example.cmina.openmeeting.utils.ChatMessage;
import com.example.cmina.openmeeting.utils.Protocol;
import com.example.cmina.openmeeting.utils.SaveSharedPreference;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import static android.R.attr.bitmap;
import static android.os.Environment.getExternalStoragePublicDirectory;

import static com.example.cmina.openmeeting.activity.MainActivity.cursor;
import static com.example.cmina.openmeeting.activity.MainActivity.myDatabaseHelper;
import static com.example.cmina.openmeeting.service.SocketService.inRoom;
import static com.example.cmina.openmeeting.utils.Protocol.PT_CHAT_IMG;
import static com.example.cmina.openmeeting.utils.Protocol.PT_CHAT_MSG;
import static com.example.cmina.openmeeting.utils.Protocol.PT_OFFSET;
import static com.example.cmina.openmeeting.utils.Protocol.cliToServer;
import static org.opencv.imgproc.Imgproc.INTER_LINEAR;

/**
 * Created by cmina on 2017-06-13.
 */

public class ChatActivity extends AppCompatActivity {

/*    //파일 다운로드 중이면
    public static boolean duringDownload;*/

    //파일 이어받기를 위한 static
    public static String realPath="";
    //유튜브 공유하기
    String youtubeUrl;

    public SocketService socketService; //연결할 서비스
    public boolean IsBound;

    final static int REQ_CODE_SELECT_IMAGE = 3001;
    final static int REQUEST_IMAGE_CAPTURE = 4001;
    final static int REQ_CODE_SELECT_MOVIE = 5001;

    Uri mCurrentPhotoPath;

    //마시멜로우 권한 설정
    static final int PERMISSION_REQUEST_CODE = 1;
    String[] PERMISSIONS = {"android.permission.CAMERA", "android.permission.WRITE_EXTERNAL_STORAGE", "android.permission.READ_EXTERNAL_STORAGE"};

    private boolean hasPermission(String[] permissions) {
        int res = 0;
        for (String perms : permissions) {
            res = checkCallingOrSelfPermission(perms);
            if (!(res == PackageManager.PERMISSION_GRANTED)) {
                return false; //퍼미션허가 안된 경우
            }
        }
        return true; //허가
    }

    //마시멜로이상 런타임퍼미션요청
    private void requestNecessaryPermissions(String[] permissions) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(permissions, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0) {
                    boolean readAccepted = grantResults[2] == PackageManager.PERMISSION_GRANTED;
                    boolean writeAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (!readAccepted && !writeAccepted && !cameraAccepted) {
                            showDialogforPermission("앱을 실행하려면 퍼미션을 허가하셔야합니다. ");
                            return; //해당 메소드를 종료
                        }
                    }
                }
                break;
        }
    }

    private void showDialogforPermission(String msg) {
        final AlertDialog.Builder myDialog = new AlertDialog.Builder(ChatActivity.this);
        myDialog.setTitle("알림");
        myDialog.setMessage(msg);
        myDialog.setCancelable(false);
        myDialog.setPositiveButton("예", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(PERMISSIONS, PERMISSION_REQUEST_CODE);
                }
            }
        });

        myDialog.setNegativeButton("아니오", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                return;
            }
        });
        myDialog.show();
    }


    //서비스에 바인드하기 위해서, ServiceConnection인터페이스를 구현하는 개체를 생성
    public ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            SocketService.LocalBinder binder = (SocketService.LocalBinder) iBinder;
            socketService = binder.getService(); //서비스 받아옴
            Log.e("ChatActivity", "서비스 바인드 성공");
            socketService.registerCallback(callback); //콜백 등록
            socketService.registerCallback2(callback2); //보내기실패한 동영상 adapter에서 삭제
            IsBound = true;

            //youtube url이 있다면...
            if (!youtubeUrl.equals("")) {
                //확실히 서비스 바인드 한 후에, 유튜브 공유!!
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                message = youtubeUrl;
                if (now_room != null && message != null) {

                    final Protocol protocol = new Protocol(message.trim().getBytes().length + 133);
                    protocol.setProtocolType(String.valueOf(PT_CHAT_MSG));
                    protocol.setTotalLen(String.valueOf(message.trim().getBytes().length + 133));
                    protocol.setRoomid(now_room);
                    protocol.setUserid(SaveSharedPreference.getUserid(getApplicationContext()));
                    protocol.setUserimg(SaveSharedPreference.getUserimage(getApplicationContext()));
                    protocol.setMsg(message);

                    Log.d("보내는 유튜브 주소", "" + message.trim().getBytes().length + "/" + now_room + "/" + SaveSharedPreference.getUserid(getApplicationContext()) + "/"
                            + SaveSharedPreference.getUserimage(getApplicationContext()) + "/" + message);

                    //서비스 바인드성공하기 전에 불려서....메시지 보내기가 안됨...
                    if (socketService != null) {
                        socketService.send_byte(protocol.getPacket());
                        Log.d("챗액티비티 소켓확인", socketService.toString());
                        message = null;
                    } else {
                        Log.e("ChatActivity", "socketService == null 메시지 보내기 실패");
                        Toast.makeText(ChatActivity.this, "메시지 보내기 실패", Toast.LENGTH_SHORT).show();
                    }

                }
            }


        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            //socketService = null;
            IsBound = false;
        }
    };


    ArrayList<ChatMessage> items = new ArrayList<ChatMessage>();
    public ChatMessageAdapter adapter;

    //채팅방 UI변수
    Button sendBtn/*, socketCloseBtn*/;
    ImageButton sendImage;
    EditText chatMessageEditText;
    ListView chatListView;

    private String message = "";
    public static String now_room = "";

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                Intent intent = new Intent(ChatActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);

                //onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    //이미지의 실제 주소 가져오기
    public static String getRealPathFromUri(Context context, Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = {MediaStore.Images.Media.DATA};
            cursor = context.getContentResolver().query(contentUri, proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    // 파일명 찾기
    public static String getName(Context context, Uri uri) {
        String[] projection = {MediaStore.Images.ImageColumns.DISPLAY_NAME};
        Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null);
        int column_index = cursor
                .getColumnIndexOrThrow(MediaStore.Images.ImageColumns.DISPLAY_NAME);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }

    //realPath에서 파일명찾기
    public static String getFileName(String pathandname){
        int start=pathandname.lastIndexOf("/");
        int end=pathandname.lastIndexOf(".");
        if(start!=-1 && end!=-1){
            return pathandname.substring(start+1);
        }else{
            return null;
        }
    }

    // uri 아이디 찾기
    public static String getUriId(Context context, Uri uri) {
        String[] projection = {MediaStore.Images.ImageColumns._ID};
        Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns._ID);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        inRoom = true;
        long fileresize = 0;
        Bitmap bitmap = null;
        byte[] filebyte = new byte[0];

        //갤러리에서 사진선택
        if (requestCode == REQ_CODE_SELECT_IMAGE && resultCode == RESULT_OK) {
            if (data != null) {
                Uri uri = data.getData();
                Log.e("image gal", uri + " now_room = " + now_room);

                //2017.08.04
                //원본파일 그대로 보내는 게 아니라, 이미지 리사이징해서 서버에 보내기
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                    Mat matInput = new Mat();
                    org.opencv.android.Utils.bitmapToMat(bitmap, matInput);

                    Mat resized = new Mat();
                    Imgproc.resize(matInput, resized, new Size(bitmap.getWidth()/4,bitmap.getHeight()/4), 0, 0, INTER_LINEAR);

                    bitmap = Bitmap.createBitmap(resized.width(), resized.height(), Bitmap.Config.ARGB_8888);
                    org.opencv.android.Utils.matToBitmap(resized, bitmap);

                    //resize한 비트맵의 사이즈...
                    filebyte = bitmapToByteArray(bitmap);
                    fileresize = filebyte.length;

                } catch (IOException e) {
                    e.printStackTrace();
                }

               /* File oFile = new File(getRealPathFromUri(ChatActivity.this, uri));
                long lFileSize = oFile.length();
                int filesize = (int) (long) lFileSize;*/

                Log.d("이미지 파일크기", "//리사이즈 이미지 크기" + fileresize);

                //이미지 파일....소켓으로 전달.
                Protocol protocol = new Protocol((int) (fileresize + 133)); //채팅프로토콜+파일사이즈 만큼의 바이트배열을 만든다!
                protocol.setProtocolType(String.valueOf(PT_CHAT_IMG));
                protocol.setRoomid(now_room);
                protocol.setUserid(SaveSharedPreference.getUserid(ChatActivity.this));
                protocol.setUserimg("http://13.124.77.49/thumbnail/"+SaveSharedPreference.getUserid(getApplicationContext())+".jpg");
                //protocol.sendImage(getRealPathFromUri(ChatActivity.this, uri));
                protocol.sendImage(filebyte);
                protocol.setTotalLen(String.valueOf(fileresize + 133));

                socketService.send_byte(protocol.getPacket());

                Log.e("이미지 보내기 확인", now_room + uri + fileresize);

            }
        }

        //카메라에서 사진찍기
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            if (data != null) {
                if (!mCurrentPhotoPath.equals("")) {
                    //2017.08.04
                    //원본파일 그대로 보내는 게 아니라, 이미지 리사이징해서 서버에 보내기
                    String photoPath = mCurrentPhotoPath.getPath();
                    Log.e("photoPath", photoPath);

                    try {
                        bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), Uri.parse(photoPath));
                        Mat matInput = new Mat();
                        org.opencv.android.Utils.bitmapToMat(bitmap, matInput);

                        Mat resized = new Mat();
                        Imgproc.resize(matInput, resized, new Size(bitmap.getWidth()/4,bitmap.getHeight()/4), 0, 0, INTER_LINEAR);

                        bitmap = Bitmap.createBitmap(resized.width(), resized.height(), Bitmap.Config.ARGB_8888);
                        org.opencv.android.Utils.matToBitmap(resized, bitmap);

                        //resize한 비트맵의 사이즈...
                        filebyte = bitmapToByteArray(bitmap);
                        fileresize = filebyte.length;

                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    File oFile = new File(photoPath);
                    long lFileSize = oFile.length();
                    int filesize = (int) (long) lFileSize;

                    Log.d("이미지 파일크기", lFileSize + "//이미지 리사이즈 " +fileresize );

                    //이미지 파일....소켓으로 전달.
                    Protocol protocol = new Protocol((int) (fileresize + 133)); //채팅프로토콜+파일사이즈 만큼의 바이트배열을 만든다!
                    protocol.setProtocolType(String.valueOf(PT_CHAT_IMG));
                    protocol.setRoomid(now_room);
                    protocol.setUserid(SaveSharedPreference.getUserid(ChatActivity.this));
                    protocol.setUserimg("http://13.124.77.49/thumbnail/"+SaveSharedPreference.getUserid(getApplicationContext())+".jpg");
                    protocol.sendImage(filebyte);
                    protocol.setTotalLen(String.valueOf(fileresize + 133));

                    socketService.send_byte(protocol.getPacket());

                    Log.e("촬영한 이미지 보내기 확인", now_room + photoPath + filesize);

                } else {
                    Log.d("파일없음", mCurrentPhotoPath + "파일없음");
                }
            }
        }


        //동영상 선택
        if (requestCode == REQ_CODE_SELECT_MOVIE && resultCode == RESULT_OK) {
            if (data != null) {
                //서버로 보내기!
                //->서버로 보내기 전에 파일이름만 먼저 보내서,
                //혹시 파일받기를 하다가 중단한 적이 있는가를 탐색.
                //uri는 저장해 두었다가
                //해당 위치에서부터 파일보내도록 하기...
                //일단 동영상 썸네일 세팅? 그 위에 프로그레스바올리기?
                Uri uri = data.getData();

         //       duringDownload = true;

                String name = getName(this, uri);
                realPath = getRealPathFromUri(ChatActivity.this, uri);

                //********************
                //수정중
                //동영상을 최초 보낼때!!!
                //setmsgid()셋팅하지 않고!!
                Protocol protocol1 = new Protocol(244);
                protocol1.setTotalLen(String.valueOf(244));
                protocol1.setProtocolType(String.valueOf(PT_OFFSET));
                protocol1.setRoomid(now_room);
                protocol1.setUserid(SaveSharedPreference.getUserid(ChatActivity.this));
                protocol1.setFileName(name);
                protocol1.setUserimg("http://13.124.77.49/thumbnail/"+SaveSharedPreference.getUserid(getApplicationContext())+".jpg");
                protocol1.setCheckType(String.valueOf(cliToServer));

                socketService.send_byte(protocol1.getPacket());

               // Log.d("동영상 서버업로드 시작", now_room + uri  + "// 파일이름 : " + name +"/offset " + offset +" roomid "+now_room +" userid "+SaveSharedPreference.getUserid(this));

               // socketService.videosendTime = System.currentTimeMillis();

                Log.e("PT_OFFSET 보냄", "실제경로 : " + realPath + "\n파일명 : " + name + "\nuri : " + uri.toString() );

                //********************

               /* File oFile = new File(path);
                long lFileSize = oFile.length();
                int filesize = (int) (long) lFileSize;

                Log.d("동영상 파일크기", lFileSize + "//" + filesize);

                Protocol protocol = new Protocol(filesize + 213); //채팅프로토콜+파일사이즈 만큼의 바이트배열을 만든다!
                protocol.setTotalLen(String.valueOf(filesize + 213));
                protocol.setProtocolType(String.valueOf(PT_CHAT_MOVIE));
                protocol.setRoomid(now_room);
                protocol.setUserid(SaveSharedPreference.getUserid(ChatActivity.this));
                protocol.setUserimg(SaveSharedPreference.getUserimage(ChatActivity.this));
                protocol.setFileName(name);
                protocol.sendVideo(getRealPathFromUri(ChatActivity.this, uri));

                socketService.send_byte(protocol.getPacket());

                Log.e("동영상 보내기 확인", now_room + uri + filesize + "// 파일이름 : "+name);

                String uriId = getUriId(this, uri);
                Log.e("###", "실제경로 : " + path + "\n파일명 : " + name + "\nuri : " + uri.toString() + "\nuri id : " + uriId);*/

            }
        }


    }

    private void doBindService() {
        if (!IsBound) {
            //갤러리 이미지를 선택해 오면서 새롭게 socket service 를 실행하니까....새로 접속이 됐구나.
            bindService(new Intent(ChatActivity.this, SocketService.class), serviceConnection, Context.BIND_AUTO_CREATE);
            Log.e("ChatActivity onResume", "바인드서비스");
            IsBound = true;
        }

    }

    private void doUnbindService() {
        if (IsBound) {
            unbindService(serviceConnection);
            Log.e("ChatActivity onStop", "언바인드서비스");
            IsBound = false;
        }
    }


    public byte[] bitmapToByteArray( Bitmap bitmap ) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream() ;
        bitmap.compress( Bitmap.CompressFormat.JPEG, 100, stream) ;
        byte[] byteArray = stream.toByteArray() ;
        return byteArray ;
    }


    @Override
    protected void onResume() {
        super.onResume();
        inRoom = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        inRoom = false;
    }

    @Override
    protected void onStop() {
        super.onStop();
        doUnbindService();
    }

    //이미지 저장 경로 파일 생성!
    private Uri createImageFile() {
        long curr = System.currentTimeMillis();
        String imageFileName = curr + ".jpg";
        //외부저장소에 저장
        File storageDir = getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

        Uri uri = Uri.fromFile(new File(storageDir, imageFileName));
        return uri;
    }

    //서비스에서 아래의 콜백함수를 호출하며, 콜백함수에서는 액티비티에서 처리할 내용입력
    public SocketService.ICallback callback = new SocketService.ICallback() {
        public void recvMsg(String roomid, String userid, String userimg, String msg, String time, int type, String msgid, String preimg,
                            String pretitle, String predesc) {
            Log.d("ChatActivity ", "adapter에 추가");

            adapter.addChatMsg(roomid, userid, userimg, msg, time, type, msgid, preimg, pretitle, predesc);
            addHandler();

          //  myDatabaseHelper.insertChatlogs(roomid, userid, userimg, msg, time, type, msgid, preimg, pretitle, predesc);

        }

    };

    //실패했던 동영상 정보 삭제하기
    public SocketService.ICallback callback2 = new SocketService.ICallback() {
        public void recvMsg(String roomid, String userid, String userimg, String msg, String time, int type, String msgid, String preimg,
                            String pretitle, String predesc) {
            Log.d("callback2", "실패한 동영상  adapter에서 삭제 "+msgid);
            adapter.delChatMsg(msgid);//최근걸 지우는게 아니라, type=4인걸...
           // addHandler();
        }
    };




    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        //채팅방액티비티에 있음
        inRoom = true;

        doBindService();

        Intent intent = getIntent();
        now_room = intent.getExtras().getString("roomid");
        String roomtitle = intent.getExtras().getString("roomtitle");
        if (intent.getExtras().getString("youtubeUrl") != null) {
            youtubeUrl = intent.getExtras().getString("youtubeUrl");
        } else {
            youtubeUrl = "";
        }

        //액션바에 백버튼만들기 위해
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        if (roomtitle == null) {
            setTitle("같이 밥먹자");
        } else {
            setTitle(roomtitle);
        }

        adapter = new ChatMessageAdapter(items, ChatActivity.this);
        sendBtn = (Button) findViewById(R.id.sendBtn);
        sendImage = (ImageButton) findViewById(R.id.sendImage);
     /*   socketCloseBtn = (Button) findViewById(R.id.socketcloseBtn);
        socketCloseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    if (socketService.is != null && socketService.os != null && socketService.socket != null) {
                        socketService.is.close();
                        socketService.os.close();
                        socketService.dis.close();
                        socketService.dos.close();
                        socketService.socket.close();
                        Log.d("임의로 소켓연결 끊음", "ChatActivity");
                        Toast.makeText(ChatActivity.this, "소켓 연결 끊음", Toast.LENGTH_SHORT).show();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });*/

        //갤러리나, 카메라
        sendImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //다이얼로그
                AlertDialog.Builder builder = new AlertDialog.Builder(ChatActivity.this);
                builder.setTitle("파일 전송");
                builder.setMessage("파일 전송하겠습니까?")
                        .setCancelable(true)
                        .setPositiveButton("갤러리", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                Intent i = new Intent(Intent.ACTION_PICK);
                                i.setType(MediaStore.Images.Media.CONTENT_TYPE);
                                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                try {
                                    startActivityForResult(i, REQ_CODE_SELECT_IMAGE);
                                } catch (android.content.ActivityNotFoundException e) {
                                    e.printStackTrace();
                                }

                            }
                        })
                        .setNeutralButton("카메라", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                if (!hasPermission(PERMISSIONS)) {
                                    requestNecessaryPermissions(PERMISSIONS);
                                } else {
                                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                                    //촬영한 이미지를 저장할 path 생성
                                    mCurrentPhotoPath = createImageFile();
                                    //EXTRA_OUTPUT을 이용해서 저장할 경로에 이미지 저장.
                                    intent.putExtra(MediaStore.EXTRA_OUTPUT, mCurrentPhotoPath);

                                    if (intent.resolveActivity(getPackageManager()) != null) {
                                        startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
                                    }
                                }

                            }
                        })
                        .setNegativeButton("동영상", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //동영상 선택
                                Intent i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
                                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

                                try {
                                    startActivityForResult(i, REQ_CODE_SELECT_MOVIE);
                                } catch (android.content.ActivityNotFoundException e) {
                                    e.printStackTrace();
                                }

                            }
                        });
                AlertDialog alertDialog = builder.create();
                alertDialog.show();

            }
        });

        if (message == "") {
            sendBtn.setEnabled(false);
        } else {
            sendBtn.setEnabled(true);
        }

        chatMessageEditText = (EditText) findViewById(R.id.chatMessageEditText);
        chatListView = (ListView) findViewById(R.id.chatListView);
        chatListView.setAdapter(adapter);
        //새로운 아이템이 추가되었을 때, 스크롤이 되도록 설정
        chatListView.setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);

        //when message is added, it makes listview to scroll last message
        adapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                chatListView.setSelection(adapter.getCount() - 1);
            }
        });


        cursor = myDatabaseHelper.getChatMsg(now_room);

        Log.e("chat msg 개수확인", "Count = " + cursor.getCount());

        while (cursor.moveToNext()) {
            //cursor.getString() : 테이블의 실제 데이터 가져옴
            //cursor.getColumnIndex() : 테이블의 해당 컬럼이름을 가져옴
            adapter.addChatMsg(cursor.getString(cursor.getColumnIndex("roomid")), cursor.getString(cursor.getColumnIndex("userid")),
                    cursor.getString(cursor.getColumnIndex("userimg")), cursor.getString(cursor.getColumnIndex("msg")),
                    cursor.getString(cursor.getColumnIndex("time")), cursor.getInt(cursor.getColumnIndex("type")),
                    cursor.getString(cursor.getColumnIndex("msgid")), cursor.getString(cursor.getColumnIndex("preimg")),
                    cursor.getString(cursor.getColumnIndex("pretitle")), cursor.getString(cursor.getColumnIndex("predesc")));

        }

        adapter.notifyDataSetChanged();

        cursor.close();


        //에딧텍스트에 변화가 생기면 불리는 콜백함수
        chatMessageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (chatMessageEditText.getText().length() != 0) {
                    sendBtn.setEnabled(true);
                    sendBtn.setBackgroundResource(R.drawable.btnround);
                } else {
                    sendBtn.setEnabled(false);
                    sendBtn.setBackgroundResource(R.drawable.btnunable);

                }
            }
        });

        //클릭했을 때, 보낼메시지가 있으면 보낸다. 클릭하고 나면 에딧텍스트 다시 빈칸으로.
        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String chatMsg = String.valueOf(chatMessageEditText.getText());
                chatMsg = chatMsg.trim(); //공백제거

                if (chatMsg.length() != 0) {
                    message = chatMsg;
                    chatMessageEditText.setText("");

                    if (now_room != null && message != null) {
                        //바로 리스트뷰에 추가하는게 아니라, 서버에 전달이 확실히 됐을 때 그 후에....
                        // Protocol protocol = new Protocol(PT_CHAT, message.trim().getBytes().length);
                        final Protocol protocol = new Protocol(message.trim().getBytes().length + 133);
                        protocol.setProtocolType(String.valueOf(PT_CHAT_MSG));
                        protocol.setTotalLen(String.valueOf(message.trim().getBytes().length + 133));
                        protocol.setRoomid(now_room);
                        protocol.setUserid(SaveSharedPreference.getUserid(getApplicationContext()));
                        protocol.setUserimg("http://13.124.77.49/thumbnail/"+SaveSharedPreference.getUserid(getApplicationContext())+".jpg");
                        protocol.setMsg(message);

                        Log.d("보내는 chat", "" + message.trim().getBytes().length + "/" + now_room + "/" + SaveSharedPreference.getUserid(getApplicationContext()) + "/"
                                + SaveSharedPreference.getUserimage(getApplicationContext()) + "/" + message);

                        if (socketService != null) {
                            socketService.send_byte(protocol.getPacket());
                            Log.d("챗액티비티 소켓확인", socketService.toString());
                            message = null;
                        } else {
                            Toast.makeText(ChatActivity.this, "메시지 보내기 실패", Toast.LENGTH_SHORT).show();
                        }
                    }
                }

            }
        });

    }


    public void addHandler() {
        new Thread(new Runnable() {
            @Override
            public void run() {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        adapter.notifyDataSetChanged();
                    }
                });
            }
        }).start();
    }
}

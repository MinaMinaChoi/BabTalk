package com.example.cmina.openmeeting.service;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.cmina.openmeeting.R;
import com.example.cmina.openmeeting.activity.ChatActivity;
import com.example.cmina.openmeeting.utils.Protocol;
import com.example.cmina.openmeeting.utils.SaveSharedPreference;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;

import jp.wasabeef.glide.transformations.CropCircleTransformation;

import static android.R.attr.path;
import static com.example.cmina.openmeeting.R.id.useridTextView;
import static com.example.cmina.openmeeting.activity.ChatActivity.now_room;

import static com.example.cmina.openmeeting.activity.MainActivity.myDatabaseHelper;
import static com.example.cmina.openmeeting.utils.Protocol.PT_CHAT_IMG;
import static com.example.cmina.openmeeting.utils.Protocol.PT_CHAT_MOVIE;
import static com.example.cmina.openmeeting.utils.Protocol.PT_CHAT_MSG;
import static com.example.cmina.openmeeting.utils.Protocol.PT_CHECK;
import static com.example.cmina.openmeeting.utils.Protocol.PT_OFFSET;
import static com.example.cmina.openmeeting.utils.Protocol.SOCKET_CHECK;

/**
 * Created by cmina on 2017-06-13.
 */

public class SocketService extends Service {

    public long sendCheckTime; //소켓연결상태 확인 메시지 보낸시간
    public long lastReadTime; //마지막으로 메시지를 읽은 시간

    public static boolean inRoom = false;

    private String ip = "13.124.77.49";
    private int port = 12345;
    public Socket socket;
    private boolean connected;

    int left_packet = 0;

    public InputStream is;
    public OutputStream os;
    public DataInputStream dis;
    public DataOutputStream dos;

    ServiceSocketThread serviceSocketThread;

    //task kill 됐을 때


    @Override
    public void onTaskRemoved(Intent rootIntent) {
        //super.onTaskRemoved 를 호출해주면, task가 종료되는 시점에서 프로세스는 재시작.
        //task는 안보이고 프로세스만 살아있는 형태, task가 종료되는 시점에서 서비스도 같이 종료시키려면 stopself()호출
        //    super.onTaskRemoved(rootIntent);
        Log.d("Socket Service", "onTaskRemoved" + rootIntent);
        stopSelf();
    }

    //서비스 이미 실행중이면 oncreate호출되지 않는다
    //아닌데...왜 이미지받고나면 socketSerivce oncreate() 시작되지?
    @Override
    public void onCreate() {
        super.onCreate();
        //소켓 연결 스레드 실행
        if (!connected) {
            serviceSocketThread = new ServiceSocketThread();
            serviceSocketThread.start();
        }
        connected = true;
        Log.d("SocketService", "onCreate" + connected);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.d("SocketService", "onStartCommand");

        return START_STICKY;

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return myBinder;
    }

    private final IBinder myBinder = new LocalBinder(); //컴포넌트에 반환되는 IBinder

    //컴포넌트에 반환해줄 IBinder를 위한 클래스
    public class LocalBinder extends Binder {
        public SocketService getService() {
            return SocketService.this;
        }
    }

    //콜백인터페이스 선언
    public interface ICallback {
        public void recvMsg(String roomid, String userimg, String userid, String msg, String time, int type); //액티비티에서 선언한 콜백함수
    }

    private ICallback mCallback;

    //액티비티에서 콜백함수를 등록하기 위함.
    public void registerCallback(ICallback callback) {
        mCallback = callback;
    }

    //소켓연결하는 스레드
    private class ServiceSocketThread extends Thread {

        @Override
        public void run() {
            connect();
        }

        private void connect() {
            try {
                socket = new Socket(ip, port);

                lastReadTime = System.currentTimeMillis();
                sendCheckTime = 0;

                Log.d("소켓연결 시도" , "lastReadTime : "+lastReadTime + "// sendCheckTime : "+sendCheckTime);

                if (socket != null) { //정상적으로 소켓 연결되었으면...

                    Log.e("socket Check", socket.toString());

                    toastHandler("소켓 연결에 성공했습니다");
                    is = socket.getInputStream();
                    dis = new DataInputStream(is);
                    os = socket.getOutputStream();
                    dos = new DataOutputStream(os);

                    //소켓접속하고 바로 userid보내기!
                    // send_byte();
                    //   Protocol protocol = new Protocol()
                    String userid = SaveSharedPreference.getUserid(getApplicationContext());
                    dos.writeUTF(userid);
                    Log.d("서버로 userid보냄", userid);


                }
            } catch (UnknownHostException e) {
                toastHandler("알 수 없는 호스트");
                e.printStackTrace();
            } catch (IOException e) {
                toastHandler("소켓 연결에 실패했습니다");
                e.printStackTrace();
            }

            //소켓연결 상태 체크 스레드
            Thread socketCheckThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    Log.d("Socket Service ", "socketCheckThread 시작");

                    Protocol protocol;
                    //마지막으로 메시지를 읽은 시간으로부터 20초가 지났으면 확인메시지 보내기
                    while (true) {
                        //읽기가 발생하기 전에 계속 일어나는 구만.
                        //제어하는 조건을 하나 더 추가해서
                        long checkTime = System.currentTimeMillis();

                        if (checkTime - lastReadTime > 10000 && lastReadTime != 0) {
                            lastReadTime = 0;
                            protocol = new Protocol(PT_CHECK, 0);
                            protocol.setTotalLen("64");
                            protocol.setProtocolType(String.valueOf(PT_CHECK));
                            protocol.setSocketCheck(String.valueOf(SOCKET_CHECK));
                            //체크 보낸시간
                            sendCheckTime = System.currentTimeMillis();
                            Log.e("소켓연결 상태 체크 sendCheckTime", sendCheckTime + "");
                            send_byte(protocol.getPacket());

                        }
                    }
                }
            });

            socketCheckThread.start();

            //받는 스레드
            final Thread receiverThread = new Thread(new Runnable() {
                //새 protocol
                Protocol protocol;
                byte[] buf;

                @Override
                public void run() {
                    Log.d("Socket Service ", "receiverThread 시작");

                    while (true) {
                        //소켓연결체크 메시지 보낸후 10초동안 응답이 없으면 끊겼다고 간주..
                        //sencCheckTime != 0 은 정상적으로 데이터를 받았을 경우를 제외시키기 위해서.
                        if (System.currentTimeMillis() - sendCheckTime > 10000 && sendCheckTime != 0) {
                            //소켓 끊김!
                            //while문 벗어나. 소켓 재연결.
                            Log.d("소켓이상 감지", "끊고 다시  " );
                            break;
                        }

                        try {
                            //받은 크기가 최소, 4바이트(32) 이상 되는지 확인하는 로직 추가하기!!
                            if (is.available() <= 0 && left_packet <= 0) { //읽을 게 없으면, 밑에 부분 실행하지 않고, 위에 while(ture) 로 가기
                                continue;
                            }

                            //마지막으로 메시지를 받은 시간.
                            lastReadTime = System.currentTimeMillis();
                            Log.d("실제 데이터 마지막으로 받은 시간 : ", ""+ lastReadTime);
                            //메시지를 받으면 sendCheckTime을 0으로 초기화.
                            sendCheckTime = 0;

                            System.out.println("인풋스트림 크기 : " + is.available());

                            //////////////////
                            //이전에 받은 패킷이 남아있다면
                            //left_packet 이 있을 때와 없을 때 아예 나눠서.,..
                            if (left_packet > 0) {

                                protocol = new Protocol(left_packet);
                                buf = protocol.getPacket();
                                Log.d("남은 패킷 길이 : ", "" + left_packet + "// buf.getPacket 의 길이" + buf.length);

                                left_packet = 0; //다시 0으로 초기화

                                //********
                                is.read(buf);

                                int total_len = Integer.parseInt(protocol.getTotalLen()); //받아야할 길이

                                int recv_len = buf.length; //받은 길이
                                int recv_cnt = 0;

                                System.out.println("인풋스트림 읽기시작 받아야할 길이 : " + total_len + "//buf.length" + recv_len);

                                if (total_len > recv_len) { //받아야할 길이가 받은 길이보다 크면,
                                    protocol.setPacket2(total_len, buf);
                                } else {
                                    protocol.setPacket(total_len, buf); //total크기의 바이트배열에다가 지금까지 받은 buf(바이트배열)을 복사하고,
                                }

                                //총길이보다 덜 받았을 때...
                                while (recv_len < total_len) {
                                    if (is.available() > 0) {

                                        buf = new byte[is.available()];
                                        System.out.println("계속 읽는 중" + is.available());

                                        recv_cnt = is.read(buf); //인풋스트림에서 읽어와서 buf바이트배열에 담는다.
                                        System.out.println("더 읽은 데이터 길이 : " + recv_cnt);

                                        //남은 길이 비교해서,
                                        //남은길이가 더 크면
                                        if ((total_len - recv_len) >= recv_cnt) {
                                            protocol.addPacket(recv_len, buf);
                                            recv_len += recv_cnt; //읽은 데이터 길이를 더한다.
                                        } else {
                                            protocol.addPacket2(total_len, recv_len, buf);
                                            recv_len += (total_len - recv_len); //읽은 데이터 길이를 더한다.

                                            left_packet = recv_cnt - (total_len - recv_len);

                                            //   left_packet = buf.length - (total_len - recv_len);
                                        }
                                        System.out.println("지금까지 읽은 데이터 길이 : " + recv_len);
                                    }
                                }
                                //*************


                            } else {
                                //버퍼에 담긴 만큼의 바이트배열 생성.
                                protocol = new Protocol(is.available());
                                buf = protocol.getPacket();

                                Log.d("left 0일 떄, 인풋스트림 길이 : ", "" + buf.length);

                                //********
                                is.read(buf);

                                int total_len = Integer.parseInt(protocol.getTotalLen()); //전체 길이
                                //   int PT_type = Integer.parseInt(protocol.getProtocolType()); //프로토콜타입

                                //만약에 이미지 전송중이면,
                                //바로바로 temp파일에 넣기!

                            /*    if (PT_type == PT_CHAT_IMG) {

                                    //다받으면.

                                } else {
                                    //나머지는 원래받던대로.
                                    //총길이만큼 다 받아서 분석.


                                }*/

                                int recv_len = buf.length; //받은 길이
                                int recv_cnt = 0;

                                System.out.println("left 0일 떄, 인풋스트림 읽기시작 받아야할 길이 : " + total_len + "//buf.length" + recv_len);

                                if (total_len > recv_len) { //받아야할 길이가 받은 길이보다 크면,
                                    protocol.setPacket2(total_len, buf);
                                } else {
                                    protocol.setPacket(total_len, buf); //total크기의 바이트배열에다가 지금까지 받은 buf(바이트배열)을 복사하고,
                                }

                                //총길이보다 덜 받았을 때...
                                while (recv_len < total_len) {
                                    if (is.available() > 0) {

                                        buf = new byte[is.available()];
                                        System.out.println("left 0일 떄, 계속 읽는 중" + is.available());

                                        recv_cnt = is.read(buf); //인풋스트림에서 읽어와서 buf바이트배열에 담는다.
                                        System.out.println("left 0일 떄, 더 읽은 데이터 길이 : " + recv_cnt);

                                        //남은 길이 비교해서,
                                        //남은길이가 더 크면
                                        if ((total_len - recv_len) >= recv_cnt) {
                                            protocol.addPacket(recv_len, buf);
                                            recv_len += recv_cnt; //읽은 데이터 길이를 더한다.
                                        } else {
                                            protocol.addPacket2(total_len, recv_len, buf);
                                            recv_len += (total_len - recv_len); //읽은 데이터 길이를 더한다.

                                            // left_packet = buf.length - (total_len - recv_len);
                                            left_packet = recv_cnt - (total_len - recv_len);
                                        }

                                        System.out.println("left 0일 떄, 지금까지 읽은 데이터 길이 : " + recv_len);
                                    }
                                }
                                //*************
                            }


                            int packetType = Integer.parseInt(protocol.getProtocolType());

                            inmessage(packetType, protocol);

                        } catch (IOException e) {

                            Log.d("서버와 연결 끊어짐", "서버와 연결 끊어졌습니다다");
                            e.printStackTrace();
                            //서버와 연결 끊어졌을 때, 네트워크 자원 릴리즈하기

                            break; //while문 정지
                        }
                    } //while 끝
                    Log.d("while 끝", "끝 확인");
                    //소켓연결 종료
                    try {
                        os.close();
                        dos.close();
                        is.close();
                        dis.close();

                        socket.close();

                        //다시 연결
                        connect();
                        Log.e("자원해제하고 다시 연결시도", "다시다시");

                    } catch (IOException e1) {
                        e1.printStackTrace();
                        Log.d("소켓을 닫는데 실패했습니다", "실패");
                    }
                }
            });

            receiverThread.start();

        }
    }

    private void inmessage(int protocoltype, Protocol protocol) {

        //받은 시간
        Date date = new Date();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd, a hh:mm");
        String time = simpleDateFormat.format(date).toString();//.substring(12);

        // String msgtype = protocol.getMsgType();
        String roomid = protocol.getRoomid().trim();
        String totalLen = protocol.getTotalLen().trim();
        // String dataLength = protocol.getDataLength().trim();
        String userid = protocol.getUserid().trim();
        int type;

        switch (protocoltype) {

            //일반 메시지 일때ㄹ
            case PT_CHAT_MSG:

                //sqlite에 넣을 msgtype
                type = 0;


                String userimg = protocol.getUserimg().trim();
                String msg = null;
                //  if (msgtype.equals(Message)) {
                type = 0;
                msg = protocol.getMsg();

                Log.e("chat 확인", "total=" + totalLen + "/" + roomid + "/" + userid + "/" + userimg + "/" + msg);

                //   }
                //여기서 sqlite에 저장해야해.
                myDatabaseHelper.insertChatlogs(SaveSharedPreference.getUserid(getApplicationContext()), roomid, userid, userimg,
                        msg, time, type);

                //실시간으로 해당 방에 있을 때는....바로바로 업데이트
                //내가 지금 있는 방이 어디인지를 파악해서,
                //그방에게 전해지는 메시지만.....adapter에 add되도록...
                if (now_room.equals(roomid) && inRoom) {   //방이름이 같고, inRoom일 때

                    // adapter.addChatMsg(roomid, userimg, userid, msg, time, type);
                    //addHandler();
                    //콜백함수로 등록된 recvMsg를 통해서, 어댑터에 채팅내용을 추가하도록...
                    mCallback.recvMsg(roomid, userimg, userid, msg, time, type);
                    Log.d("확인", roomid + userid);

                } else { //해당 방안이 아닐 경우, 노티를 띄워주기!
                    if (!userid.equals("알림")) {

                        sendNotification(userimg, userid, msg, roomid, time.substring(12));
                        showCustomToast(userid, userimg, msg);

                    }
                }


                break;

            //이미지 메시지 일때
            case PT_CHAT_IMG:

                String userimg1 = protocol.getUserimg().trim();
                String msg1 = null;

                //    if (msgtype.equals(IMG)) {
                //이미지일때
                type = 1;

                long curr = System.currentTimeMillis();  // 또는 System.nanoTime();
                //외부저장소
                msg1 = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + curr + ".jpg";

                //내부저장소. 나의 앱에서만 사용할 수 있도록
                //  msg = getFilesDir()+"/"+curr+".jpg";

                File file = new File(msg1);
                FileOutputStream fos = null;
                try {
                    fos = new FileOutputStream(file);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                BufferedOutputStream bos = new BufferedOutputStream(fos);

                try {
                    //D/skia: --- decoder->decode returned false
                    //해결을 위해서...
                    //이미지를 다 셋팅하기 전에 디코더를 닫아버리는데서 발생하는 문제.
                    Thread.sleep(1500);
                    //바이트배열을...파일에 저장하기
                    bos.write(protocol.getImg(Integer.parseInt(totalLen) - 113), 0, Integer.parseInt(totalLen) - 113);

                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        bos.flush();
                        bos.close();
                        fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
                Log.e("받은 IMG 확인", "totalLen=" + totalLen + "/" + roomid + "/" + userid + "/" + userimg1 + "/" + protocol.getImg(Integer.parseInt(totalLen.trim()) - 113));
                // }

                //여기서 sqlite에 저장해야해.
                myDatabaseHelper.insertChatlogs(SaveSharedPreference.getUserid(getApplicationContext()), roomid, userid, userimg1,
                        msg1, time, type);

                //실시간으로 해당 방에 있을 때는....바로바로 업데이트
                //내가 지금 있는 방이 어디인지를 파악해서,
                //그방에게 전해지는 메시지만.....adapter에 add되도록...
                if (now_room.equals(roomid) && inRoom) {   //방이름이 같고, inRoom일 때

                    // adapter.addChatMsg(roomid, userimg, userid, msg, time, type);
                    //addHandler();
                    //콜백함수로 등록된 recvMsg를 통해서, 어댑터에 채팅내용을 추가하도록...
                    mCallback.recvMsg(roomid, userimg1, userid, msg1, time, type);
                    Log.d("확인", roomid + userid);

                } else { //해당 방안이 아닐 경우, 노티를 띄워주기!
                    if (!userid.equals("알림")) {
                        sendNotification(userimg1, userid, "사진", roomid, time.substring(12));
                        showCustomToast(userid, userimg1, "사진");

                    }
                }

                break;

            //동영상 전송
            case PT_CHAT_MOVIE:

                String userimg2 = protocol.getUserimg().trim();
                String msg2 = null;

                //동영상일 때
                type = 2;

                long curr1 = System.currentTimeMillis();  // 또는 System.nanoTime();
                //외부저장소
                msg2 = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + curr1 + ".mp4";

                File file1 = new File(msg2);
                FileOutputStream fos1 = null;
                try {
                    fos1 = new FileOutputStream(file1);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                BufferedOutputStream bos1 = new BufferedOutputStream(fos1);

                try {
                    //D/skia: --- decoder->decode returned false
                    //해결을 위해서...
                    //이미지를 다 셋팅하기 전에 디코더를 닫아버리는데서 발생하는 문제.
                    Thread.sleep(1500);
                    //바이트배열을...파일에 저장하기
                    bos1.write(protocol.getVideo(Integer.parseInt(totalLen) - 213), 0, Integer.parseInt(totalLen) - 213);

                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        bos1.flush();
                        bos1.close();
                        fos1.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
                Log.e("동영상 확인", "totalLen=" + totalLen + "/" + roomid + "/" + userid + "/" + userimg2 + "/" + msg2);
                // }

                //여기서 sqlite에 저장해야해.
                myDatabaseHelper.insertChatlogs(SaveSharedPreference.getUserid(getApplicationContext()), roomid, userid, userimg2,
                        msg2, time, type);

                //실시간으로 해당 방에 있을 때는....바로바로 업데이트
                //내가 지금 있는 방이 어디인지를 파악해서,
                //그방에게 전해지는 메시지만.....adapter에 add되도록...
                if (now_room.equals(roomid) && inRoom) {   //방이름이 같고, inRoom일 때
                    //콜백함수로 등록된 recvMsg를 통해서, 어댑터에 채팅내용을 추가하도록...
                    mCallback.recvMsg(roomid, userimg2, userid, msg2, time, type);
                    Log.d("확인", roomid + userid);

                } else { //해당 방안이 아닐 경우, 노티를 띄워주기!
                    if (!userid.equals("알림")) {
                        sendNotification(userimg2, userid, "동영상", roomid, time.substring(12));
                        showCustomToast(userid, userimg2, "동영상");

                    }
                }

                break;

            //파일중단시 중단위치 체크해서 보내주면, 그 위치부터 파일 재전송!!!
            case PT_OFFSET:


                break;

            //소켓연결 체크용
            case PT_CHECK:

                int check = Integer.parseInt(protocol.getSocketCheck());
                if (check == SOCKET_CHECK) {
                    //소켓연결 정상
                    Log.d("소켓상태 정상", "정상" + SOCKET_CHECK);
                }
                break;
        }
    }


    public void send_byte(byte[] packet) { //바이트배열로 메시지 보내기
        try {
            //소켓연결 안되어있으면 여기서 오류 생김!!nullpointerException
            os.write(packet);
            os.flush();
        } catch (IOException e) {
            toastHandler("데이터 전송에 실패했습니다");
            e.printStackTrace();
        }
    }

    public void send_message(String str) { //서버에게 메세지를 보내는 부분...소켓 아웃풋스트림을 사용해서.
        try {
            dos.writeUTF(str);
            dos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void toastHandler(final String str) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(SocketService.this, str, Toast.LENGTH_SHORT).show();
            }
        }, 0);
    }

/*
    private void addHandler() {

        final Handler handler = new Handler(Looper.getMainLooper());
        //오래 걸리는 작업 work스레드에서 실행.
        new Thread(new Runnable() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        //work스레드내부에서 명령이 실행 되는 것이 아니라.
                        //handler.post..에 의해, 메시지 큐에 추가 되어 uithread가 차례대로 처리
                        adapter.notifyDataSetChanged();
                    }
                });
            }
        }).start();

        //이렇게 처리했을 때의 문제점!!
        //runnable의 run()메소드가, 새로운 스레드에서 실행됨.
        //만약 ui스레드와, 새로운 work 스레드에서 동시에 UI위젯에 접근해서 변경하려고 하면....에러.
*/
/*        new Thread(new Runnable() {
            @Override
            public void run() {
                adapter.notifyDataSetChanged();
            }
        }).start();

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                adapter.notifyDataSetChanged();
            }
        }, 0);*//*


    }

*/

    //노티피케이션 띄우기
    private void sendNotification(final String userimg, String userid, String msg, String roomid, String time) {

        RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.custom_noti);
        final Bitmap[] bm = {null};
        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    URL url = new URL(userimg);
                    URLConnection conn = url.openConnection();
                    conn.connect();
                    BufferedInputStream bis = new BufferedInputStream(conn.getInputStream());
                    bm[0] = BitmapFactory.decodeStream(bis);
                    bis.close();
                } catch (Exception e) {
                }

            }
        };

        thread.start();
        try {
            thread.join();
            remoteViews.setImageViewBitmap(R.id.userimageView, bm[0]);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        remoteViews.setTextViewText(useridTextView, userid);
        remoteViews.setTextViewText(R.id.msgTextView, msg);
        remoteViews.setTextViewText(R.id.recent_time, time);

        // Glide.with(getApplicationContext()).load(userimg).bitmapTransform(new CropCircleTransformation(getApplicationContext())).into();


        Intent intent = new Intent(this, ChatActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("roomid", roomid);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setContent(remoteViews)
                .setSmallIcon(R.drawable.phonepink)
                //   .setContentTitle(userid)
                //    .setContentText(msg)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());

    }


    public void showCustomToast(final String userid, final String userImg, final String msg) {

        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

                LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
                final View view = inflater.inflate(R.layout.custom_toast, null);

                ImageView imageView = (ImageView) view.findViewById(R.id.userimageView);
                TextView useridTextView = (TextView) view.findViewById(R.id.useridTextView);
                TextView msgTextView = (TextView) view.findViewById(R.id.msgTextView);

                Glide.with(getApplicationContext()).load(userImg).bitmapTransform(new CropCircleTransformation(getApplicationContext())).into(imageView);
                useridTextView.setText(userid);
                msgTextView.setText(msg);

                Toast toast = new Toast(getApplicationContext());
                toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
                toast.setDuration(Toast.LENGTH_SHORT);
                toast.setView(view);
                toast.show();

            }
        }, 0);
    }
}
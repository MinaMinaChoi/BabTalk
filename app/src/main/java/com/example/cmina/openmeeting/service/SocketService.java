package com.example.cmina.openmeeting.service;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
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
import com.example.cmina.openmeeting.utils.MyDatabaseHelper;
import com.example.cmina.openmeeting.utils.OGTag;
import com.example.cmina.openmeeting.utils.OkHttpRequest;
import com.example.cmina.openmeeting.utils.Protocol;
import com.example.cmina.openmeeting.utils.SaveSharedPreference;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jp.wasabeef.glide.transformations.CropCircleTransformation;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import static com.example.cmina.openmeeting.R.id.useridTextView;
import static com.example.cmina.openmeeting.activity.ChatActivity.getFileName;

import static com.example.cmina.openmeeting.activity.ChatActivity.now_room;

import static com.example.cmina.openmeeting.activity.ChatActivity.realPath;
import static com.example.cmina.openmeeting.activity.MainActivity.cursor;
import static com.example.cmina.openmeeting.activity.MainActivity.myDatabaseHelper;
import static com.example.cmina.openmeeting.receiver.RestartReceiver.ACTION_RESTART_SERVICE;
import static com.example.cmina.openmeeting.receiver.WifiChangeReceiver.EVENT_NETWORK_CHAGED;
import static com.example.cmina.openmeeting.utils.Protocol.PT_CHAT_IMG;
import static com.example.cmina.openmeeting.utils.Protocol.PT_CHAT_MOVIE;
import static com.example.cmina.openmeeting.utils.Protocol.PT_CHAT_MSG;
import static com.example.cmina.openmeeting.utils.Protocol.PT_CHECK;
import static com.example.cmina.openmeeting.utils.Protocol.PT_OFFSET;
import static com.example.cmina.openmeeting.utils.Protocol.SOCKET_CHECK;
import static com.example.cmina.openmeeting.utils.Protocol.cliToServer;
import static com.example.cmina.openmeeting.utils.Protocol.serverToCli;

/**
 * Created by cmina on 2017-06-13.
 */

public class SocketService extends Service {

    //wifi, 3g 변경을 인지하기 위해
    private BroadcastReceiver receiver;

    public long sendCheckTime; //소켓연결상태 확인 메시지 보낸시간
    public long lastReadTime; //마지막으로 메시지를 읽은 시간
    public long videosendTime; //비디오파일을 보낸 시간

    public boolean duringSending = false;
    public boolean duringDownload = false;

    public static boolean inRoom = false;

    private String ip = "13.124.77.49";
    private int port = 12345;
    public Socket socket;
    private boolean connected;

    int left_packet_len = 0;
    byte[] left_packet = null;

    public InputStream is;
    public OutputStream os;
    public DataInputStream dis;
    public DataOutputStream dos;

    ServiceSocketThread serviceSocketThread;


    @Override
    public void onTaskRemoved(Intent rootIntent) {
        //super.onTaskRemoved 를 호출해주면, task가 종료되는 시점에서 프로세스는 재시작.
        //task는 안보이고 프로세스만 살아있는 형태, task가 종료되는 시점에서 서비스도 같이 종료시키려면 stopself()호출
        //    super.onTaskRemoved(rootIntent);
        Log.d("Socket Service", "onTaskRemoved" + rootIntent);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    //    registerRestartAlarm(true);
        if (receiver != null) {
            unregisterReceiver(receiver);
        }
/*      if (os != null && is != null && socket != null) {
            try {
                Log.d("SocketService", "onDestroy");
                os.close();
                is.close();
                dis.close();
                dos.close();
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }*/
        Toast.makeText(this, "SocketService destroyed", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCreate() {
        super.onCreate();
      //  registerRestartAlarm(false);
        Toast.makeText(this, "SocketService 실행", Toast.LENGTH_SHORT).show();
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
        // startForeground(1, new Notification());
       /* MyNotiControl cl = new MyNotiControl(SocketService.this);
        startForeground(1, cl.getNoti());
*/
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
        public void recvMsg(String roomid, String userid, String userimg, String msg, String time, int type, String msgid,
                            String preimg, String pretitle, String predesc); //액티비티에서 선언한 콜백함수
    }


    private ICallback mCallback;
    public ICallback mCallback2;


    //액티비티에서 콜백함수를 등록하기 위함.
    public void registerCallback(ICallback callback) {
        mCallback = callback;
    }

    public void registerCallback2(ICallback callback) {
        mCallback2 = callback;
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

                Log.d("소켓연결 시도", "lastReadTime : " + lastReadTime + "// sendCheckTime : " + sendCheckTime);

                //2017.08.03 수정중
                IntentFilter intentfilter = new IntentFilter();
                intentfilter.addAction(EVENT_NETWORK_CHAGED);
                receiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        //임의로 소켓 끊기, 다시 접속하도록
                        if (os != null && is != null && socket != null) {
                            Log.d("SocketService receiver 안", "" + EVENT_NETWORK_CHAGED);

                            try {
                                os.close();
                                is.close();
                                dis.close();
                                dos.close();
                                socket.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                        }
                    }
                };
                registerReceiver(receiver, intentfilter);

                if (socket != null) { //정상적으로 소켓 연결되었으면...
                    Log.d("SocketService 소켓연결 성공", socket.toString());
                    toastHandler("소켓 연결에 성공했습니다");
                    is = socket.getInputStream();
                    dis = new DataInputStream(is);
                    os = socket.getOutputStream();
                    dos = new DataOutputStream(os);

                    //소켓접속하고 바로 userid보내기!
                    String userid = SaveSharedPreference.getUserid(getApplicationContext());

                    //바로 sqlite에 저장된 최신메시지의 msgid 서버로 보냄.
                    myDatabaseHelper = new MyDatabaseHelper(SocketService.this);
                    myDatabaseHelper.open();
                    cursor = myDatabaseHelper.getRecentMsgID();
                    cursor.moveToFirst();
                    String msgid = "0";
                    if (cursor.getCount() > 0) {
                        msgid = cursor.getString(cursor.getColumnIndex("msgid"));
                    }

                    if (msgid.equals("")) {
                        msgid = "0";
                    }

                    if (userid.equals("")) {
                        userid = "noLogin";
                    }

                    dos.writeUTF(userid + "|" + msgid);

                    Log.d("서버로 userid, msgid보냄", userid + msgid);

                }
            } catch (UnknownHostException e) {
                // toastHandler("알 수 없는 호스트");
                e.printStackTrace();
            } catch (IOException e) {
                // toastHandler("소켓 연결에 실패했습니다");
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
                        if (System.currentTimeMillis() - lastReadTime > 10000 && lastReadTime != 0) {
                            lastReadTime = 0;
                            protocol = new Protocol(64);
                            protocol.setTotalLen("64");
                            protocol.setProtocolType(String.valueOf(PT_CHECK));
                            protocol.setSocketCheck(String.valueOf(SOCKET_CHECK));
                            //체크 보낸시간
                            sendCheckTime = System.currentTimeMillis();
                            Log.d("소켓연결 상태 체크 sendCheckTime", sendCheckTime + "");
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
                        //만약 duringDownload 가 true인데 계속 데이터를 받지못한지 10초가 지났으면.....
                        long now_time = System.currentTimeMillis();
                        if (now_time - sendCheckTime > 10000 && sendCheckTime != 0) {
                            //소켓 끊김! while문 벗어나. 소켓 재연결.
                            Log.d("서버 소켓이상 감지", "끊고 다시 ");
                            //수정중
                            //여기서 만약 서버로 동영상 보내는 중이었으면 엑스표시 뜨도록...
                            if (duringSending) {
                                //sqlite와 adapter에 추가.

                                if (inRoom) {
                                    mCallback.recvMsg(now_room, SaveSharedPreference.getUserid(SocketService.this), SaveSharedPreference.getUserimage(SocketService.this), getFileName(realPath), "" + now_time, 4, "",
                                            "", "", "");
                                }

                                //정상적으로 추가
                                myDatabaseHelper.insertChatlogs(now_room, SaveSharedPreference.getUserid(SocketService.this), SaveSharedPreference.getUserimage(SocketService.this), getFileName(realPath), "" + now_time, 4, "",
                                        "", "", "");
                                Log.e("보내다가 실패", now_room + " / " + getFileName(realPath) + " / " + now_time);
                            }
                            break;
                        }

                        try {
                            //받은 크기가 최소, 4바이트(32) 이상 되는지 확인하는 로직 추가하기!!
                            if (is.available() <= 32) { //읽을 게 없으면, 밑에 부분 실행하지 않고, 위에 while(ture) 로 가기
                                continue;
                            }

                            //마지막으로 메시지를 받은 시간.
                            lastReadTime = System.currentTimeMillis();
                            Log.d("실제 데이터 마지막으로 받은 시간 : ", "" + lastReadTime);
                            //메시지를 받으면 sendCheckTime을 0으로 초기화.
                            sendCheckTime = 0;

                            System.out.println("인풋스트림 크기 : " + is.available());
                            System.out.println("left_packet 크기 : " + left_packet_len);
                            //{
                            //버퍼에 담긴 만큼의 바이트배열 생성.
                            protocol = new Protocol(is.available());
                            buf = protocol.getPacket();

                            //********
                            is.read(buf);

                            int total_len = Integer.parseInt(protocol.getTotalLen()); //전체 길이
                            int protocol_type = Integer.parseInt(protocol.getProtocolType()); //프로토콜타입
                            String roomid = protocol.getRoomid().trim();
                            String userid = protocol.getUserid().trim();

                            int recv_len = buf.length; //받은 길이
                            int recv_cnt = 0;


                            if (protocol_type == PT_CHAT_MOVIE) { //동영상파일이면 바로바로 파일에 쓰기시작!!

                                System.out.println("동영상파일 전송받는중. 바로 임시파일에 쓰기");
                                duringSending = false;
                                //읽은 바이트배열중에, 헤더부분 233 빼고

                                String userimg = protocol.getUserimg().trim();
                                String filename = protocol.getFileName().trim();
                                String tempfile = filename + ".tempfile";

                                File file = new File(SocketService.this.getFilesDir(), tempfile);

                                if (file.isFile()) {
                                    System.out.println("파일 이미 존재" + tempfile);
                                } else {
                                    System.out.println("최초파일" + tempfile);
                                }

                                FileOutputStream fos = new FileOutputStream(file, true);

                                fos.write(buf, 233, buf.length - 233);

                                while (total_len > recv_len) { //읽은 바이트길이보다 최종길이가 더 길면...
                                    //더받아서 파일이어쓰기를 해줘야하지.
                                    if (is.available() > 0) {

                                        buf = new byte[is.available()];
                                        //  System.out.println(" 계속 읽는 중" + is.available());
                                        recv_cnt = is.read(buf); //inputstream에서 읽어와서 buf바이트배열에 담는다
                                        //   System.out.println("더 읽은 데이터 길이 : " + rec_cnt);

                                        if ((total_len - recv_len) >= recv_cnt) {
                                            //  protocol.addPacket(rec_len, buf);
                                            fos.write(buf, 0, buf.length);
                                            recv_len += recv_cnt; //읽은 데이터 길이를 더한다.

                                        } else {
                                            // protocol.addPacket2(total_len, rec_len, buf);
                                            fos.write(buf, 0, total_len - recv_len);

                                            left_packet = new byte[recv_cnt - (total_len - recv_len)]; //20070
                                            left_packet_len = recv_cnt - (total_len - recv_len);
                                            System.arraycopy(buf, (total_len - recv_len), left_packet, 0, recv_cnt - (total_len - recv_len));

                                            recv_len += (total_len - recv_len); //읽은 데이터 길이를 더한다.
                                        }
                                    }
                                }

                                fos.close();

                                //다 읽었으면//tempfile 확장자 떼기
                                File fileToMove = new File(SocketService.this.getFilesDir(), filename);
                                boolean isMoved = file.renameTo(fileToMove);

                                if (isMoved) {
                                    System.out.println("파일이동 성공" + filename + "/" + fileToMove.length());

                                    //받은 시간
                                    long curr = System.currentTimeMillis();

                                    //여기에서 날짜비교를 해야겠군.
                                    //채팅방밖에서 메시지 받을 경우에도 날짜선 추가해주기 위해
                                    cursor = myDatabaseHelper.getRecentTime(roomid);
                                    cursor.moveToFirst();
                                    if (cursor.getCount() > 0) {

                                        Date date = new Date(Long.parseLong(cursor.getString(cursor.getColumnIndex("time"))));
                                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");

                                        String recenttime = simpleDateFormat.format(date).substring(0, 10);
                                        Date nowdate = new Date(curr); //새로 받은 메시지의 받은시간 long

                                        String nowtime = simpleDateFormat.format(nowdate).substring(0, 10);

                                        //Log.d("날짜선 추가", recenttime + "/nowtime " + nowtime);

                                        if (nowtime.compareTo(recenttime) > 0) {
                                            //현재시간이 최근메시지시간보다 크면, 날짜선 추가
                                            myDatabaseHelper.insertChatlogs(roomid, "알림", "", nowtime, "" + curr, 0, "" + curr, "", "", "");
                                            //mCallback이 널이 뜨네....이걸 해결해야겠구만..
                                            //그래야, 콜백함수에 insert넣는 것도 할 수 있음.
                                            if (now_room.equals(roomid) && inRoom) {
                                                mCallback.recvMsg(roomid, "알림", "", nowtime, "" + curr, 0, "" + curr, "", "", "");
                                                //Log.d("Socket Service 날짜선", roomid + userid);
                                            }
                                        }
                                    }


                                    Date date = new Date(curr);
                                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd, a hh:mm");
                                    String time = simpleDateFormat.format(date).toString();

                                    int index = filename.lastIndexOf(".");//문자열에서 탐색하는 문자열이 마지막으로 등장하는 위치에 대한 index를 반환
                                    String msgid = filename.substring(0, index);

                                    //만약 type ==4이고 time이 getmsgid()와 같으면 sqlite와 adapter를 지워주고
                                    //수정중
                                    cursor = myDatabaseHelper.getVideoInfo(protocol.getMsgId());
                                    cursor.moveToFirst();
                                    if (cursor.getCount() > 0) {
                                        //실패한 거 다시 받을 때
                                        myDatabaseHelper.removetype4(protocol.getMsgId());
                                        mCallback2.recvMsg(roomid, userid, userimg, filename, "" + curr, 2, protocol.getMsgId(), "", "", "");

                                        // mCallback.recvMsg(roomid, userid, userimg, filename, "" + curr, 2, msgid, "", "", "");
                                        Log.e("SocketService video", inRoom + "" + now_room);
                                        if (now_room.equals(roomid) && inRoom) {
                                            mCallback.recvMsg(roomid, userid, userimg, filename, "" + curr, 2, msgid, "", "", "");
                                        } else { //해당 방안이 아닐 경우, 노티를 띄워주기!
                                            if (!userid.equals("알림")) {
                                                sendNotification(userimg, userid, "동영상", roomid, time.substring(12));
                                                showCustomToast(userid, userimg, "동영상");
                                            }
                                        }
                                        //이어받기의 경우, msgid에 로컬에서 실패한 시간이 들어감.
                                        //msgid는 파일네임에서 추출! 서버에 도착한시간임.
                                        myDatabaseHelper.insertChatlogs(roomid, userid, userimg, filename, "" + curr, 2, msgid, "", "", "");

                                    } else {
                                        //최초추가
                                        //mCallback.recvMsg(roomid, userid, userimg, filename, "" + curr, 2, msgid, "", "", "");
                                        Log.e("SocketService video", inRoom + "" + now_room);
                                        if (now_room.equals(roomid) && inRoom) {
                                            mCallback.recvMsg(roomid, userid, userimg, filename, "" + curr, 2, msgid, "", "", "");
                                        } else { //해당 방안이 아닐 경우, 노티를 띄워주기!
                                            if (!userid.equals("알림")) {
                                                sendNotification(userimg, userid, "동영상", roomid, time.substring(12));
                                                showCustomToast(userid, userimg, "동영상");
                                            }
                                        }

                                        //콜백함수에다가 sqlite 추가
                                        myDatabaseHelper.insertChatlogs(roomid, userid, userimg, filename, "" + curr, 2, msgid, "", "", "");
                                    }

                                    Log.d("동영상 확인", "totalLen=" + total_len + "/" + roomid + "/" + userid + "/" + userimg + "/" + filename + "/" + protocol.getMsgId());

                                } else {
                                    System.out.println("파일이동 실패");
                                }

                            } else {
                                //원래 받던방식대로. totalLen만큼 다 받은 후 분석하도록!
                                System.out.println("인풋스트림 읽기시작 받아야할 길이 : " + total_len);

                                if (total_len >= recv_len) { //받아야할 길이가 받은 길이보다 크면,
                                    protocol.setPacket2(total_len, buf);
                                } else {
                                    protocol.setPacket(total_len, buf); //total크기의 바이트배열에다가 지금까지 받은 buf(바이트배열)을 복사하고,

                                    //남은 패킷은....?
                                    left_packet = new byte[recv_len - total_len];
                                    left_packet_len = recv_len - total_len;

                                    System.arraycopy(buf, total_len, left_packet, 0, recv_len - total_len);
                                    //남은 left_packet을 분석하는게 여기에 와야하네.
                                    //만약, 서버로부터 오는 새로운 메시지가 없다면..... 위에....
                                }

                                //수정중 2017.08.03
                                //하나의 패킷이 완성 되었으면, 바로 분석!
                                int packetType = Integer.parseInt(protocol.getProtocolType());
                                inmessage(packetType, protocol);

                                //총길이보다 덜 받았을 때...
                                while (recv_len < total_len) {
                                    if (is.available() > 0) {
                                        //마지막으로 메시지를 받은 시간.
                                        lastReadTime = System.currentTimeMillis();

                                        buf = new byte[is.available()];
                                        System.out.println(" 계속 읽는 중" + is.available());

                                        recv_cnt = is.read(buf); //인풋스트림에서 읽어와서 buf바이트배열에 담는다.

                                        //남은 길이 비교해서,
                                        //남은길이가 더 크면
                                        if ((total_len - recv_len) >= recv_cnt) {
                                            Log.d("(total_len - recv_len) >= recv_cnt)", "받아야할 길이 >= 받은 길이" + recv_cnt);
                                            protocol.addPacket(recv_len, buf);
                                            recv_len += recv_cnt; //읽은 데이터 길이를 더한다.
                                        } else {

                                            Log.d("else", "받아야할 길이 < 받은 길이" + recv_cnt);

                                            protocol.addPacket2(total_len, recv_len, buf);

                                            left_packet = new byte[recv_cnt - (total_len - recv_len)]; //20070
                                            left_packet_len = recv_cnt - (total_len - recv_len);
                                            System.arraycopy(buf, (total_len - recv_len), left_packet, 0, recv_cnt - (total_len - recv_len));

                                            recv_len += (total_len - recv_len); //읽은 데이터 길이를 더한다.
                                        }

                                        System.out.println("지금까지 읽은 데이터 길이 : " + recv_len);
                                    }
                                }


                                boolean whileCheck = false;

                                while (left_packet_len > 0 && !whileCheck) { //while문이 계속 도네..
                                    //남은 패킷이 있다면 분리해서 분석하기.
                                    //단순히 남은패킷의 길이정보만 담은 left_packet.
                                    //남은 바이트배열을 담을 것이 필요!!!
                                    protocol = new Protocol(left_packet.length);
                                    //남은 패킷을 그 길이 만큼 바이트배열을 만들어서 붙이기.
                                    protocol.setPacket(left_packet.length, left_packet);
                                    buf = protocol.getPacket();
                                    System.out.println("남은 패킷 길이" + buf.length);

                                    total_len = Integer.parseInt(protocol.getTotalLen()); //받아야할 길이

                                    recv_len = buf.length; //받은 길이
                                    recv_cnt = 0;

                                    System.out.println("while 인풋스트림 읽기시작 받아야할 길이 : " + total_len + "//buf.length" + recv_len);

                                    if (total_len >= recv_len) { //받아야할 길이가 받은 길이보다 크거나 같으면,
                                        protocol.setPacket2(total_len, buf);
                                        Log.d("while문 total_len >= recv_len", total_len + "/" + recv_len);

                                        left_packet = new byte[0];
                                        left_packet_len = 0;

                                    } else {
                                        protocol.setPacket(total_len, buf); //total크기의 바이트배열에다가 지금까지 받은 buf(바이트배열)을 복사하고,
                                        //만약 더 받았으면 나머지는 다음 패킷으로 넘기기
                                        left_packet = new byte[recv_len - total_len];
                                        left_packet_len = recv_len - total_len;

                                        System.arraycopy(buf, total_len, left_packet, 0, recv_len - total_len);

                                        //  recv_len += (total_len - recv_len); //읽은 데이터 길이를 더한다.

                                        Log.d("while문 else", total_len + "/" + recv_len);

                                    }

                                    //총길이보다 덜 받았을 때...
                                    while (recv_len < total_len) {
                                        if (is.available() > 0) {

                                            //마지막으로 메시지를 받은 시간.
                                            lastReadTime = System.currentTimeMillis();

                                            buf = new byte[is.available()];
                                            System.out.println("while계속 읽는 중" + is.available());

                                            recv_cnt = is.read(buf); //인풋스트림에서 읽어와서 buf바이트배열에 담는다.
                                            System.out.println("while더 읽은 데이터 길이 : " + recv_cnt);

                                            //남은 길이 비교해서,
                                            //남은길이가 더 크면
                                            if ((total_len - recv_len) >= recv_cnt) {
                                                protocol.addPacket(recv_len, buf);
                                                recv_len += recv_cnt; //읽은 데이터 길이를 더한다.

                                                //  left_packet = new byte[0];
                                                left_packet_len = 0;

                                                whileCheck = true;

                                            } else {
                                                protocol.addPacket2(total_len, recv_len, buf);

                                                left_packet = new byte[recv_cnt + recv_len - total_len];
                                                left_packet_len = recv_cnt + recv_len - total_len;

                                                System.arraycopy(buf, total_len - recv_len, left_packet, 0, recv_cnt + recv_len - total_len);

                                                recv_len += (total_len - recv_len); //읽은 데이터 길이를 더한다.

                                                whileCheck = false;

                                            }
                                            System.out.println("while지금까지 읽은 데이터 길이 : " + recv_len);
                                        }
                                    } //총길이만큼 받는 것!

                                    int packetType1 = Integer.parseInt(protocol.getProtocolType());
                                    //while 계속 돌아서 계속 입력 될때가 있네...
                                    inmessage(packetType1, protocol);

                                }

                            }

                        } catch (IOException e) {
                            //socket.close()하면 여기서 캐치
                            Log.d("서버와 연결 끊어짐", "서버와 연결 끊어졌습니다다");
                            e.printStackTrace();

                            //만약 서버로 동영상을 보내는 중이었으면, 엑스표시하도록....
                            //수정중
                            if (duringSending) {
                                //sqlite와 adapter에 추가.
                                //여기서 만약 서버로 동영상 보내는 중이었으면 엑스표시 뜨도록...
                                if (duringSending) {
                                    //sqlite와 adapter에 추가.

                                    if (inRoom) {
                                        mCallback.recvMsg(now_room, SaveSharedPreference.getUserid(SocketService.this), SaveSharedPreference.getUserimage(SocketService.this), getFileName(realPath), "" + now_time, 4, "" + now_time,
                                                "", "", "");
                                    }
                                    //정상적으로 추가
                                    myDatabaseHelper.insertChatlogs(now_room, SaveSharedPreference.getUserid(SocketService.this), SaveSharedPreference.getUserimage(SocketService.this), getFileName(realPath), "" + now_time, 4, "" + now_time,
                                            "", "", "");
                                    Log.e("보내다가 실패", now_room + " / " + getFileName(realPath) + " / " + now_time);
                                }
                            }

                            break; //while문 정지
                        }

                    } //while 끝
                    Log.d("while 끝", "끝 확인");
                    //소켓연결 종료
                    try {
                        if (os != null && dos != null && is != null && dis != null && socket != null) {
                            //서버와 연결 끊어졌을 때, 네트워크 자원 릴리즈하기
                            os.close();
                            dos.close();
                            is.close();
                            dis.close();
                            socket.close();

                            //다시 연결
                            connect();
                            unregisterReceiver(receiver);
                            Log.d("SocketService", "소켓연결 다시");
                        }

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
        long curr = System.currentTimeMillis();

        Date date = new Date(curr);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd, a hh:mm");
        String time = simpleDateFormat.format(date).toString();//.substring(12);

        String roomid = protocol.getRoomid().trim();
        String totalLen = protocol.getTotalLen().trim();
        String userid = protocol.getUserid().trim();
        String userimg = "";

        //    Log.d("inmessage 메소드 ", roomid +" / "+userid );

        int type;
        String msgID = "";
        String msg = "";


        //여기에서 날짜비교를 해야겠군.
        //채팅방밖에서 메시지 받을 경우에도 날짜선 추가해주기 위해
        cursor = myDatabaseHelper.getRecentTime(roomid);
        cursor.moveToFirst();
        if (cursor.getCount() > 0) {

            date = new Date(Long.parseLong(cursor.getString(cursor.getColumnIndex("time"))));
            simpleDateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");

            String recenttime = simpleDateFormat.format(date)
                    .substring(0, 10);
            Date nowdate = new Date(curr); //새로 받은 메시지의 받은시간 long
          //  Log.d("날짜선 추가", cursor.getString(cursor.getColumnIndex("time")) + "/nowtime " + curr);

            String nowtime = simpleDateFormat.format(nowdate)
                    .substring(0, 10);

          //  Log.d("날짜선 추가", recenttime + "/nowtime " + nowtime);

            if (nowtime.compareTo(recenttime) > 0) {
                //현재시간이 최근메시지시간보다 크면, 날짜선 추가
                myDatabaseHelper.insertChatlogs(roomid, "알림", "", nowtime, "" + curr, 0, "" + curr, "", "", "");
                //mCallback이 널이 뜨네....이걸 해결해야겠구만..
                //그래야, 콜백함수에 insert넣는 것도 할 수 있음.
                if (now_room.equals(roomid) && inRoom) {
                    mCallback.recvMsg(roomid, "알림", "", nowtime, "" + curr, 0, "" + curr, "", "", "");
                    //Log.d("Socket Service 날짜선", roomid + userid);
                }
            }
        }


        switch (protocoltype) {

            //일반 메시지 일때
            case PT_CHAT_MSG:
                //sqlite에 넣을 msgtype
                userimg = protocol.getUserimg().trim();
                msg = protocol.getMsg();
                msgID = protocol.getMsgId();
                type = 0;
                Log.e("받은 chat", "total=" + totalLen + "/" + roomid + "/" + userid + "/" + userimg + "/" + msg);

                //만약에 메시지의 형태가 url의 형태라면, 파싱해서 대표이미지, 타이틀, 컨텐츠내용 미리보기!!!
                String regex = "^(((http(s?))\\:\\/\\/)?)([0-9a-zA-Z\\-]+\\.)+[a-zA-Z]{2,6}(\\:[0-9]+)?(\\/\\S*)?$";
                Pattern pattern = Pattern.compile(regex);
                Matcher matcher = pattern.matcher(msg);

                String preimg = "";
                String pretitle = "";
                String predesc = "";

                if (matcher.find()) {

                    OGTag ogTag = new OGTag();
                    requestOgTag(msg, ogTag, roomid, userid, userimg, curr + "", type, msgID);
                    preimg = ogTag.getOgImageUrl();
                    pretitle = ogTag.getOgTitle();
                    predesc = ogTag.getOgDescription();

                    Log.d("SocketService ogtag preview", preimg + " / " + pretitle + " / " + predesc);

                } else {

                    Log.e("SocketService text", inRoom + "" + now_room);
                    if (now_room.equals(roomid) && inRoom) {   //방이름이 같고, inRoom일 때
                        //콜백함수로 등록된 recvMsg를 통해서, 어댑터에 채팅내용을 추가하도록...
                        mCallback.recvMsg(roomid, userid, userimg, msg, "" + curr, type, msgID, preimg, pretitle, predesc);
                        Log.d("확인", roomid + userid);

                    } else { //해당 방안이 아닐 경우, 노티를 띄워주기!
                        if (!userid.equals("알림")) {
                            sendNotification(userimg, userid, msg, roomid, time.substring(12));
                            showCustomToast(userid, userimg, msg);
                        }
                    }
                    //여기서 sqlite에 저장해야해.
                    myDatabaseHelper.insertChatlogs(roomid, userid, userimg, msg, "" + curr, type, msgID, preimg, pretitle, predesc);

                }

                break;

            //이미지 메시지 일때
            case PT_CHAT_IMG:
                type = 1;
                userimg = protocol.getUserimg().trim();
                msgID = protocol.getMsgId();
                //long curr = System.currentTimeMillis();  // 또는 System.nanoTime();

                msg = curr + ".jpg";
                FileOutputStream fileOutputStream = null;
                BufferedOutputStream bos = null;
                //내장 저장소로 저장 시도
                try {
                    fileOutputStream = openFileOutput(msg, Context.MODE_PRIVATE);
                    bos = new BufferedOutputStream(fileOutputStream);
                    bos.write(protocol.getImg(Integer.parseInt(totalLen) - 133), 0, Integer.parseInt(totalLen) - 133);

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        bos.flush();
                        bos.close();
                        fileOutputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                Log.d("받은 IMG 확인", "totalLen=" + totalLen + "/" + roomid + "/" + userid + "/" + userimg + "/" + msgID);
                Log.e("SocketService image", inRoom + "" + now_room);
                if (now_room.equals(roomid) && inRoom) {   //방이름이 같고, inRoom일 때

                    //콜백함수로 등록된 recvMsg를 통해서, 어댑터에 채팅내용을 추가하도록...
                    mCallback.recvMsg(roomid, userid, userimg, msg, "" + curr, type, msgID, "", "", "");
                    Log.d("확인", roomid + userid);

                } else { //해당 방안이 아닐 경우, 노티를 띄워주기!
                    if (!userid.equals("알림")) {
                        sendNotification(userimg, userid, "이미지", roomid, time.substring(12));
                        showCustomToast(userid, userimg, "이미지");
                    }
                }

                //여기서 sqlite에 저장해야해.
                myDatabaseHelper.insertChatlogs(roomid, userid, userimg, msg, "" + curr, type, msgID, "", "", "");

                break;

/*            //동영상 전송
            case PT_CHAT_MOVIE:

                duringDownload = false;

                userimg = protocol.getUserimg();
                msgID = protocol.getMsgId();
                msg = protocol.getFileName();
                //동영상일 때
                type = 2;

                //내부저장소로 변경해보자
                FileOutputStream fos = null;
                BufferedOutputStream bos1 = null;
                //내장 저장소로 저장 시도
                try {
                    fos = openFileOutput(msg, Context.MODE_PRIVATE);
                    bos1 = new BufferedOutputStream(fos);
                    bos1.write(protocol.getVideo(Integer.parseInt(totalLen) - 233), 0, Integer.parseInt(totalLen) - 233);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        bos1.flush();
                        bos1.close();
                        fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                //여기서 sqlite에 저장해야해.
                //애초에 sqlite 에는 들어가지 않았네. 어댑터에만 추가되었고.
                //   myDatabaseHelper.deleteChatlogs(4);
                //   myDatabaseHelper.updateChatlogs(msg, time, type, msgID);
                myDatabaseHelper.insertChatlogs(roomid, userid, userimg, msg, time, type, msgID);
                Log.d("동영상 확인", "totalLen=" + totalLen + "/" + roomid + "/" + userid + "/" + userimg + "/" + msg + "/" + msgID);
                //실시간으로 해당 방에 있을 때는....바로바로 업데이트
                //내가 지금 있는 방이 어디인지를 파악해서,
                //그방에게 전해지는 메시지만.....adapter에 add되도록...
                if (now_room.equals(roomid) && inRoom && userid.equals(SaveSharedPreference.getUserid(this))) {   //방이름이 같고, inRoom일 때
                    //내가 보낸 메시지이면, 기존의 임시섬네일 삭제하고 새롭게 어댑터추가하는 mCallback2 메소드로...
                    //콜백함수로 등록된 recvMsg를 통해서, 어댑터에 채팅내용을 추가하도록...
                    mCallback2.recvMsg(roomid, userimg, userid, msg, time, type, msgID);

                } else if (now_room.equals(roomid) && inRoom) {
                    mCallback.recvMsg(roomid, userid, userimg, msg, time, type, msgID);
                } else { //해당 방안이 아닐 경우, 노티를 띄워주기!
                    if (!userid.equals("알림")) {
                        sendNotification(userimg, userid, "동영상", roomid, time.substring(12));
                        showCustomToast(userid, userimg, "동영상");
                    }
                }

                break;*/

            //파일중단시 중단위치 체크해서 보내주면, 그 위치부터 파일 재전송!!!
            case PT_OFFSET:
                //1번으로 받으면 서버에 tempfile이 있는 위치에서부터 파일 재전송!
                //2번으로 받으면 클라이언트에 tempfile이 얼마만큼 있는지 확인 체크해서 offset위치를 전송

                int checktype = Integer.parseInt(protocol.getCheckType());

                if (checktype == cliToServer) { // 서버로 파일 보내기

                    duringSending = true;

                    int offset = Integer.parseInt(protocol.getOffSet());
                    String msgid = protocol.getMsgId();

                    if (!realPath.equals("")) { //uri가 빈값이 아니면
                        String name = getFileName(realPath);
                        String path = realPath;
                        File oFile = new File(path);
                        long lFileSize = oFile.length();
                        int filesize = (int) (long) lFileSize;

                        Log.d("clitoServer", "filesize" + filesize + " offset " + offset);

                        toastHandler(offset + "에서부터 전송");

                        Protocol protocol2 = new Protocol(filesize + 233 - offset); //채팅프로토콜+파일사이즈 만큼의 바이트배열을 만든다!
                        protocol2.setTotalLen(String.valueOf(filesize + 233 - offset));
                        protocol2.setProtocolType(String.valueOf(PT_CHAT_MOVIE));
                        protocol2.setRoomid(roomid);
                        protocol2.setUserid(userid);
                        protocol2.setUserimg(SaveSharedPreference.getUserimage(SocketService.this));
                        protocol2.setFileName(name);
                        protocol2.sendVideo(path, offset);
                        //수정중
                        protocol2.setMsgId(msgid);

                        send_byte(protocol2.getPacket());

                    }
                    //uri값을 다시 널로!
                    //   realPath = "";

                } else if (checktype == serverToCli) { //클라에 있는 템프파일 offset위치 전송.

                    duringDownload=true;

                    String filename = protocol.getFileName().trim();
                    String tempfile = filename + ".tempfile";
                    ///usr/share/nginx/html/uploads/1500598052263.mp4.tempfile

                    if (!tempfile.equals("")) { //자꾸 offset보내지는거 막기 위해서....

                        File file = new File(SocketService.this.getFilesDir(), tempfile.replace("/usr/share/nginx/html/uploads/", ""));

                        Protocol protocol1 = new Protocol(244);
                        protocol1.setTotalLen(String.valueOf(244));
                        protocol1.setRoomid(roomid);
                        protocol1.setUserid(userid);
                        protocol1.setFileName(filename);
                        protocol1.setProtocolType(String.valueOf(Protocol.PT_OFFSET));
                        protocol1.setCheckType(String.valueOf(serverToCli));

                        if (file.isFile()) {
                            //tempfile의 크기를 보내기
                            long lFileSize = file.length();

                            int filesize = (int) lFileSize;
                            System.out.println("템프파일 이미 존재" + filesize);
                            protocol1.setOffSet("" + filesize);

                        } else {
                            //offset 0으로 보내기
                            protocol1.setOffSet(String.valueOf(0));
                            System.out.println("템프파일 없음");
                        }

                        send_byte(protocol1.getPacket());
                    }

                }

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
            //toastHandler("데이터 전송에 실패했습니다");
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
    public void sendNotification(final String userimg, String userid, String msg, String roomid, String time) {

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
                .setSmallIcon(R.drawable.babtalk)
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


    private OGTag requestOgTag(final String url, final OGTag ogTag, final String roomid, final String userid, final String userimg, final String curr, final int type, final String msgID) {
        OkHttpRequest request = new OkHttpRequest();

        Date date = new Date(Long.parseLong(curr));
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd, a hh:mm");
        final String time = simpleDateFormat.format(date).toString();//.substring(12);

        try {
            request.get(url, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e("request failure", call.toString());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseStr = response.body().string();

                    Document doc = Jsoup.parse(responseStr);
                    Elements ogTags = doc.select("meta[property^=og:]");

                    if (ogTags.size() > 0) {
                        // 필요한 OGTag를 추려낸다
                        for (int i = 0; i < ogTags.size(); i++) {
                            Element tag = ogTags.get(i);

                            String text = tag.attr("property");
                            if ("og:url".equals(text)) {
                                //ret.setOgUrl(tag.attr("content"));
                            } else if ("og:image".equals(text)) {
                                ogTag.setOgImageUrl(tag.attr("content"));
                            } else if ("og:description".equals(text)) {
                                ogTag.setOgDescription(tag.attr("content"));
                            } else if ("og:title".equals(text)) {
                                ogTag.setOgTitle(tag.attr("content"));
                            }
                        }

                        Log.d("확인", ogTag.getOgImageUrl() + " / " + ogTag.getOgTitle() + " / " + ogTag.getOgDescription());


                        if (now_room.equals(roomid) && inRoom) {   //방이름이 같고, inRoom일 때
                            //콜백함수로 등록된 recvMsg를 통해서, 어댑터에 채팅내용을 추가하도록...
                            mCallback.recvMsg(roomid, userid, userimg, url, "" + curr, type, msgID, ogTag.getOgImageUrl(), ogTag.getOgTitle(), ogTag.getOgDescription());
                            Log.d("확인", roomid + userid);

                        } else { //해당 방안이 아닐 경우, 노티를 띄워주기!
                            if (!userid.equals("알림")) {
                                sendNotification(userimg, userid, url, roomid, time.substring(12));
                                showCustomToast(userid, userimg, url);
                            }
                        }
                        //여기서 sqlite에 저장해야해.
                        myDatabaseHelper.insertChatlogs(roomid, userid, userimg, url, "" + curr, type, msgID, ogTag.getOgImageUrl(), ogTag.getOgTitle(), ogTag.getOgDescription());

                        // 필요한 작업을 한다.
                        // setData(ret.getOgUrl(), ret.getOgImageUrl(), ret.getOgDescription(), ret.getOgTitle(), viewHolder);

                    } else {
                        Log.e("ogTags", "없음");

                        Elements imgs = doc.getElementsByTag("img");
                        String src = "";
                        if (imgs.size() > 0) {
                            src = imgs.get(0).attr("src");
                            Log.d("<img>태그들 중에서 첫번 째 요소", "//" + src);
                        }

                        doc.title();

                        System.out.println("Title: " + doc.title());
                        System.out.println("Meta Title: " + doc.select("meta[name=title]").attr("content"));
                        System.out.println("Meta Description: " + doc.select("meta[name=description]").attr("content"));

                        ogTag.setOgImageUrl(src);
                        ogTag.setOgDescription(doc.select("meta[name=description]").attr("content"));
                        ogTag.setOgTitle(doc.title());


                        if (now_room.equals(roomid) && inRoom) {   //방이름이 같고, inRoom일 때
                            //콜백함수로 등록된 recvMsg를 통해서, 어댑터에 채팅내용을 추가하도록...
                            mCallback.recvMsg(roomid, userid, userimg, url, "" + curr, type, msgID, ogTag.getOgImageUrl(), ogTag.getOgTitle(), ogTag.getOgDescription());
                            Log.d("확인", roomid + userid);

                        } else { //해당 방안이 아닐 경우, 노티를 띄워주기!
                            if (!userid.equals("알림")) {
                                sendNotification(userimg, userid, url, roomid, time.substring(12));
                                showCustomToast(userid, userimg, url);
                            }
                        }
                        //여기서 sqlite에 저장해야해.
                        myDatabaseHelper.insertChatlogs(roomid, userid, userimg, url, "" + curr, type, msgID, ogTag.getOgImageUrl(), ogTag.getOgTitle(), ogTag.getOgDescription());


                        // setData(url, src, doc.select("meta[name=description]").attr("content"), doc.title(), viewHolder);
                        // 여기가 더 늦게 불리기 때문에....
                        //제대로 데이터가 셋팅이 안되는 구나.
                        Log.d("확인", ogTag.getOgImageUrl() + " / " + ogTag.getOgTitle() + " / " + ogTag.getOgDescription());

                    }

                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.d("확인22", ogTag.getOgImageUrl() + " / " + ogTag.getOgTitle() + " / " + ogTag.getOgDescription());
        return ogTag;

    }


    //AlarmManager를 통해서 되살아나는 서비스
    public void registerRestartAlarm(boolean isOn) {

        Intent i = new Intent(ACTION_RESTART_SERVICE);
        sendBroadcast(i);

        PendingIntent sender = PendingIntent.getBroadcast(getApplicationContext(), 0, i, 0);
        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);

        if (isOn) {
            //10초에 한번씩...확인하는 구조.
            Toast.makeText(this, "registerRestartAlarm true", Toast.LENGTH_SHORT).show();
            am.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + 1000, 10000, sender);
        } else {
            Toast.makeText(this, "registerRestartAlarm true", Toast.LENGTH_SHORT).show();
            am.cancel(sender);
        }
    }

}

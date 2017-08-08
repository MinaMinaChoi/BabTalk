package com.example.cmina.openmeeting.utils;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import static android.R.attr.order;
import static android.R.attr.type;
import static android.icu.text.MessagePattern.ArgType.SELECT;
import static com.example.cmina.openmeeting.activity.MainActivity.cursor;

/**
 * Created by cmina on 2017-06-14.
 */

public class MyDatabaseHelper {

    private MyDatabaseOpenHelper myDatabaseOpenHelper;
    private SQLiteDatabase sqLiteDatabase;
    private static final int DATABASE_VERSION = 1;
    private static final String TAG = "MyDatabaseHelper";
    private static final String DATABASE_NAME = "babtalk.db";
    private Context context;

    public MyDatabaseHelper(Context context) {
        this.context = context;
    }

    public static final String chat_rooms_table = "chat_rooms";
    public static final String chat_logs_table = "chat_logs";
    public static final String friends_table = "friends";

    private String chat_rooms_create = "CREATE TABLE chat_rooms (_id Integer primary key autoincrement, "
            + "myid VARCHAR(20) not null, "
            + "roomid VARCHAR(20) not null, "
            + "roomtitle VARCHAR(20) not null, "
            + "imageurl VARCHAR(50), "
            + "recent_msg text, "
            + "recent_msg_time long);";

    private String chat_logs_create = "CREATE TABLE chat_logs (_id Integer primary key autoincrement, "
            + "roomid VARCHAR(20) not null, "
            //myid는 로그인한 userid에 따라서 방의 채팅내용을 가져올 때 사용. 하나의 디바이스로 user1, user2가 사용한다 치면... ==> 아니다..이럴경우 없다... 하나의 디바이스에 한 유저만 사용하도록..
           // + "myid VARCHAR(20) not null, "
            + "userid VARCHAR(20) not null, "
            + "userimg VARCHAR(50), "
            + "msg text not null, "
            + "time long not null, "
            + "msgid long not null, "
            + "preimg text, "
            + "pretitle text, "
            + "predesc text, "
            + "type Integer default 0);";

    private String friends_create = "CREATE TABLE friends (_id integer primary key autoincrement, "
            + "f_id VARCHAR(20) not null, "
            + "myid VARCHAR(20) not null, "
            + "image_url VARCHAR(50), "
            + "full_image_url text, "
            + "phone VARCHAR(20));";


    public class MyDatabaseOpenHelper extends SQLiteOpenHelper {


        public MyDatabaseOpenHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
            super(context, name, factory, version);
        }


        //필요한 테이블들을 여기서 만든다.
        @Override
        public void onCreate(SQLiteDatabase sqLiteDatabase) {

            sqLiteDatabase.execSQL(chat_logs_create);
            sqLiteDatabase.execSQL(chat_rooms_create);
            sqLiteDatabase.execSQL(friends_create);

        }

        @Override
        public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldversion, int newversion) {

            Log.w(TAG, "Upgrading database from version " + oldversion + " to " + newversion
                    + ", which will destroy all old data");
            sqLiteDatabase.execSQL("DROP TABLE IF EXISTS notes");
            onCreate(sqLiteDatabase);

        }
    }

    public MyDatabaseHelper open() {
        myDatabaseOpenHelper = new MyDatabaseOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION);
        sqLiteDatabase = myDatabaseOpenHelper.getWritableDatabase();

        return this;
    }

    public void close() {
        myDatabaseOpenHelper.close();
    }


    //테이블별로 인서트메소드 만들기.
    public long insertChatlogs(String roomid, String userid, String userimg, String msg, String created_at, Integer type, String msgid,
                               String preimg, String pretitle, String predesc) {

        ContentValues values = new ContentValues();
       // values.put("myid", myid);
        values.put("roomid", roomid);
        values.put("userid", userid);
        values.put("userimg", userimg);
        values.put("msg", msg);
        values.put("time", created_at);
        values.put("type", type);
        values.put("msgid", msgid);
        values.put("preimg", preimg);
        values.put("pretitle", pretitle);
        values.put("predesc", predesc);

        return sqLiteDatabase.insert(chat_logs_table, null, values);

    }

    //임시 비디오 섬네일지우기
    public boolean deleteChatlogs(Integer type) {
        return sqLiteDatabase.delete(chat_logs_table, "type='" + type + "'", null) > 0;
    }

    //가장 최근의 채팅로그 바꾸기. video받기 실패 type = 5로 바꾸기.

//채팅방에 참여시작하게 된 시간도 넣어야겠네....
    //그래야지, 참여이전의 메시지가 들어오지 않게끔...

    public long insertChatrooms(String myid, String roomid, String roomtitle, String imageurl, String recent_msg, String recent_msg_time) {

        ContentValues values = new ContentValues();
        values.put("myid", myid);
        values.put("roomid", roomid);
        values.put("roomtitle", roomtitle);
        values.put("imageurl", imageurl);
        values.put("recent_msg", recent_msg);
        values.put("recent_msg_time", recent_msg_time);

        return sqLiteDatabase.insert(chat_rooms_table, null, values);

    }

    public long insertFriends(String myid, String f_id, String image_url, String full_image_url, String phone) {

        ContentValues values = new ContentValues();
        values.put("myid", myid);
        values.put("f_id", f_id);
        values.put("image_url", image_url);
        values.put("full_image_url", full_image_url);
        values.put("phone", phone);

        return sqLiteDatabase.insert(friends_table, null, values);

    }


    //chat_rooms, friends 테이블은 delete 메소드도 만들고,

    //친구의 아이디 지우기
    public boolean deletefriend(String f_id) {
        return sqLiteDatabase.delete(friends_table, "f_id='" + f_id + "'", null) > 0;
    }

    //수정중
    //채팅방 지우기 ==>서버에도 해당 채팅방유저목록에서 나를 지우는 작업이 필요.
    //채팅메시지로 ~님 퇴장했습니다. 보내야하고...
    public boolean deleteChatRoom(String roomid) {
        sqLiteDatabase.delete(chat_logs_table, "roomid = '"+roomid+"'", null);
        Log.d("MyDataBaseHelper 해당방 채팅로그도 삭제", (sqLiteDatabase.delete(chat_logs_table, "roomid = '"+roomid+"'", null)>0)+"");
        return sqLiteDatabase.delete(chat_rooms_table, "roomid ='" + roomid+"'", null) > 0;
    }


    //대화내용으로 검색하는
    public Cursor getMatchMsg(String roomid, String str) {
        Cursor c = sqLiteDatabase.rawQuery("SELECT msg FROM chat_logs WHERE msg LIKE '" + str + "'%", null);
        return c;
    }

    //해당방의 제일 최근 메시지 가져오는
    public Cursor getRecentMsg(String roomid) {
        Cursor c = sqLiteDatabase.rawQuery("SELECT msg FROM chat_logs WHERE roomid='"+roomid+"' order by _id desc limit 1", null);
        return c;
    }

    //해당방의 최신메시지 시간 가져오는
    public Cursor getRecentTime(String roomid) {
        Cursor c = sqLiteDatabase.rawQuery("SELECT time FROM chat_logs WHERE roomid='"+roomid+"' order by _id desc limit 1", null);
        return c;
    }

    //해당방의 최신메시지의 타입을 가져오는
    public Cursor getRecentMsgType(String roomid) {
        Cursor cursor = sqLiteDatabase.rawQuery("SELECT type FROM chat_logs WHERE roomid='"+roomid+"' order by _id desc limit 1", null);
        return cursor;
    }

    //수정중 ==>이 메소드는 왜 안되지?
    //보내기 실패한 동영상을 보내는 중일 때, type = 4
    public Cursor removeChatType4(String msgid) {
        Cursor cursor = sqLiteDatabase.rawQuery("DELETE FROM chat_logs WHERE type = 4 and time='"+msgid+"'", null);
        Log.d("MyDatabaseHelper removeChatType4", msgid+"삭제");
        return cursor;
    }

    public boolean removetype4(String msgid) {
        Log.d("MyDatabaseHelper removetype4 ", msgid+"삭제"+ (sqLiteDatabase.delete(chat_logs_table, "type =4 and time = '"+msgid+"'", null) > 0));
        return sqLiteDatabase.delete(chat_logs_table, "type =4 and time = '"+msgid+"'", null) > 0;
    }

    //type =4이고, time이 getMsgId()와 같은
    public Cursor getVideoInfo(String msgid) {
        Cursor cursor = sqLiteDatabase.rawQuery("SELECT * FROM chat_logs WHERE type = 4 and time='"+msgid+"'", null);
        Log.d("MyDatabaseHelper getVideoInfo", msgid+"있음");
        return cursor;
    }

    //채팅로그의 최신 msgid를 가져오는
    public Cursor getRecentMsgID() {
        Cursor cursor = sqLiteDatabase.rawQuery("SELECT msgid FROM chat_logs order by msgid desc limit 1", null);
        return cursor;
    }

    //해당 방의 대화내용 전부 가져오는
    public Cursor getChatMsg(String roomid) {

        Cursor cursor = sqLiteDatabase.rawQuery("SELECT * FROM " + chat_logs_table + " WHERE roomid='" + roomid + "'", null);
        return cursor;

    }

    //친구목록 전부 가져오는
    public Cursor getFriends(String myid) {
        Cursor cursor = sqLiteDatabase.rawQuery("SELECT * FROM " + friends_table + " WHERE myid='" + myid + "' order by f_id desc", null);
        return cursor;

    }

    //참여중인 채팅방리스트 가져오는
    public Cursor getChatRooms(String myid) {
        Cursor cursor = sqLiteDatabase.rawQuery("SELECT * FROM " + chat_rooms_table + " WHERE myid='" + myid + "' order by _id desc", null);
        return cursor;
    }

    //채팅방 리스트 전부 지우기
    public void chatListDeleteAll() {
        sqLiteDatabase.delete(chat_rooms_table, null, null);
    }



}

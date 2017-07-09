package com.example.cmina.openmeeting.utils;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import static android.icu.text.MessagePattern.ArgType.SELECT;

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
            + "myid VARCHAR(20) not null, "
            + "userid VARCHAR(20) not null, "
            + "userimg VARCHAR(50), "
            + "msg text not null, "
            + "time long not null, "
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
    public long insertChatlogs(String myid, String roomid, String userid, String userimg, String msg, String created_at, Integer type) {

        ContentValues values = new ContentValues();
        values.put("myid", myid);
        values.put("roomid", roomid);
        values.put("userid", userid);
        values.put("userimg", userimg);
        values.put("msg", msg);
        values.put("time", created_at);
        values.put("type", type);

        return sqLiteDatabase.insert(chat_logs_table, null, values);

    }

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

    //채팅방 지우기
    public boolean deleteChatRoom(String roomid) {
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



    //해당 방의 대화내용 전부 가져오는
    public Cursor getChatMsg(String roomid, String myid) {

        Cursor cursor = sqLiteDatabase.rawQuery("SELECT * FROM " + chat_logs_table + " WHERE roomid='" + roomid + "' and myid='" + myid + "'", null);
        return cursor;

    }

    //친구목록 전부 가져오는
    public Cursor getFriends(String myid) {
        Cursor cursor = sqLiteDatabase.rawQuery("SELECT * FROM " + friends_table + " WHERE myid='" + myid + "' order by f_id desc", null);
        return cursor;

    }

    //채팅방 리스트 전부 가져오는
    public Cursor getChatRooms(String myid) {
        Cursor cursor = sqLiteDatabase.rawQuery("SELECT * FROM " + chat_rooms_table + " WHERE myid='" + myid + "' order by _id desc", null);
        return cursor;

    }

    //채팅방 리스트 전부 지우기
    public void chatListDeleteAll() {
        sqLiteDatabase.delete(chat_rooms_table, null, null);
    }



}
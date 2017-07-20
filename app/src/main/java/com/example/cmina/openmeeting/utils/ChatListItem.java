package com.example.cmina.openmeeting.utils;

/**
 * Created by cmina on 2017-06-14.
 */

public class ChatListItem {

    private String roomid;
    private String roomtitle;
    private String imgurl;
    private String recent_msg;
    private String recent_msg_time;
    private int recent_msg_type;

    public ChatListItem(String roomid, String roomtitle, String imgurl, String recent_msg, String recent_msg_time, int recent_msg_type) {
        this.roomid = roomid;
        this.roomtitle = roomtitle;
        this.imgurl = imgurl;
        this.recent_msg = recent_msg;
        this.recent_msg_time = recent_msg_time;
        this.recent_msg_type = recent_msg_type;
    }

    public void setRoomid(String id) {
        roomid = id;
    }

    public void setRoomtitle(String str) {
        roomtitle = str;
    }

    public void setImgurl(String str) {
        imgurl = str;
    }

    public void setRecent_msg(String str) {
        recent_msg = str;
    }

    public void setRecent_msg_time(String time) {
        recent_msg_time = time;
    }

    public void setRecent_msg_type(int type) {
        recent_msg_type = type;
    }

    public String getRoomid() {
        return roomid;
    }

    public String getRoomtitle() {
        return roomtitle;
    }

    public String getImgurl() {
        return imgurl;
    }

    public String getRecent_msg() {
        return recent_msg;
    }

    public String getRecent_msg_time() {
        return recent_msg_time;
    }

    public int getRecent_msg_type() {
        return recent_msg_type;
    }
}

package com.example.cmina.openmeeting.utils;

/**
 * Created by cmina on 2017-06-13.
 */

public class ChatMessage {

    private String roomid;
    private String userid;
    private String imgUrl;
    private String msg;
    private int msgtype;
    private String msgTime;

    public ChatMessage(String roomid, String imgUrl, String userid, String msg, String msgTime, int msgtype) {
        this.roomid = roomid;
        this.imgUrl = imgUrl;
        this.userid = userid;
        this.msg = msg;
        this.msgtype = msgtype;
        this.msgTime = msgTime;
    }

    public void setRoomid(String string) {
        roomid = string;
    }

    public String getRoomid() {
        return roomid;
    }

    public void setImgUrl(String url) {
        imgUrl = url;
    }

    public String getImgUrl() {
        return imgUrl;
    }

    public void setUserid(String id) {
        userid = id;
    }

    public String getUserid() {
        return userid;
    }

    public void setMsgtype(int type) {
        msgtype = type;
    }

    public int getMsgtype() {
        return msgtype;
    }

    public void setMsgTime(String str) {
        msgTime = str;
    }

    public String getMsgTime() {
        return msgTime;
    }

    public void setMsg(String str) {
        msg = str;
    }

    public String getMsg() {
        return msg;
    }


}

package com.example.cmina.openmeeting.utils;

/**
 * Created by cmina on 2017-06-13.
 */

public class ChatMessage {

    private String roomid;
    private String userid;
    private String profileImg;
    private String msg;
    private int msgtype;
    private String msgTime;
    private String msgid;
    private String preImg;
    private String preTitle;
    private String preDesc;

    public ChatMessage(String roomid, String userid, String profileImg, String msg, String msgTime, int msgtype, String msgid,
                       String preImg, String preTitle, String preDesc) {
        this.roomid = roomid;
        this.profileImg = profileImg;
        this.userid = userid;
        this.msg = msg;
        this.msgtype = msgtype;
        this.msgTime = msgTime;
        this.msgid = msgid;
        this.preImg = preImg;
        this.preTitle = preTitle;
        this.preDesc = preDesc;
       // this.preview = preview;
    }

    public ChatMessage() {

    }




    public void setPreImg(String preImg) {
        this.preImg = preImg;
    }

    public String getPreImg() {
        return preImg;
    }

    public void setPreTitle(String pretitle) {
        this.preTitle = pretitle;
    }

    public String getPreTitle() {
        return preTitle;
    }

    public void setPreDesc(String preDesc) {
        this.preDesc = preDesc;
    }

    public String getPreDesc() {
        return preDesc;
    }

    public void setRoomid(String string) {
        roomid = string;
    }

    public String getRoomid() {
        return roomid;
    }

    public void setProfileImg(String url) {
        profileImg = url;
    }

    public String getProfileImg() {
        return profileImg;
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

    public void setMsgid(String msgid) {
        this.msgid = msgid;
    }

    public String getMsgid() {
        return msgid;
    }


}

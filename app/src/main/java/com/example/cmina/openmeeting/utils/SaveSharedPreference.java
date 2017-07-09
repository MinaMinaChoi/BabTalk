package com.example.cmina.openmeeting.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by cmina on 2017-06-09.
 */

public class SaveSharedPreference {

    static final String USERID = "userid";
    static final String USERIMAGE = "userimage";
    static final String USERPW = "userpw";
    static final String USEREMAIL = "useremail";
    static final String USERAREA = "userarea";
    static final String USERBRIEF = "userbrief";
    static final String USERPHONE = "userphone";

    static SharedPreferences getSharedPreferences(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static void setUserInfo(Context context, String userid, String userpw, String userimage, String userphone, String useremail, String userarea, String userbrief) {
        SharedPreferences.Editor editor = getSharedPreferences(context).edit();
        editor.putString(USERID, userid);
        editor.putString(USERPW, userpw);
        editor.putString(USERIMAGE, userimage);
        editor.putString(USERPHONE, userphone);
        editor.putString(USEREMAIL, useremail);
        editor.putString(USERAREA, userarea);
        editor.putString(USERBRIEF, userbrief);
        editor.commit();
    }

    public static void setUserid(Context context, String userid) {
        SharedPreferences.Editor editor = getSharedPreferences(context).edit();
        editor.putString(USERID, userid);
        editor.commit();
    }

    public static void setUserimage(Context context, String userimage) {
        SharedPreferences.Editor editor = getSharedPreferences(context).edit();
        editor.putString(USERIMAGE, userimage);
        editor.commit();
    }

    public static void setUserpw(Context context, String userpw) {
        SharedPreferences.Editor editor = getSharedPreferences(context).edit();
        editor.putString(USERPW, userpw);
        editor.commit();
    }

    public static void setUseremail(Context context, String useremail) {
        SharedPreferences.Editor editor = getSharedPreferences(context).edit();
        editor.putString(USEREMAIL, useremail);
        editor.commit();
    }

    public static void setUserphone(Context context, String userphone) {
        SharedPreferences.Editor editor = getSharedPreferences(context).edit();
        editor.putString(USERPHONE, userphone);
        editor.commit();
    }

    public static void setUserarea(Context context, String userarea) {
        SharedPreferences.Editor editor = getSharedPreferences(context).edit();
        editor.putString(USERAREA, userarea);
        editor.commit();
    }

    public static void setUserbrief(Context context, String userbrief) {
        SharedPreferences.Editor editor = getSharedPreferences(context).edit();
        editor.putString(USERBRIEF, userbrief);
        editor.commit();
    }

    public static String getUserid(Context context) {
        return getSharedPreferences(context).getString(USERID, "");
    }

    public static String getUserpw(Context context) {
        return getSharedPreferences(context).getString(USERPW, "");
    }

    public static String getUserimage(Context context) {
        return getSharedPreferences(context).getString(USERIMAGE, "");
    }

    public static String getUserphone(Context context) {
        return getSharedPreferences(context).getString(USERPHONE, "");
    }

    public static String getUseremail(Context context) {
        return getSharedPreferences(context).getString(USEREMAIL, "");
    }

    public static String getUserarea(Context context) {
        return getSharedPreferences(context).getString(USERAREA, "");
    }

    public static String getUserbrief(Context context) {
        return getSharedPreferences(context).getString(USERBRIEF, "");
    }

    public static void clearUserInfo (Context context) {
        SharedPreferences.Editor editor = getSharedPreferences(context).edit();
        editor.clear();
        editor.commit();
    }

}

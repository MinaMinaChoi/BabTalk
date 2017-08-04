package com.example.cmina.openmeeting.utils;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.widget.RemoteViews;

import com.example.cmina.openmeeting.R;
import com.example.cmina.openmeeting.activity.MyNotiControlView;

/**
 * Created by cmina on 2017-08-03.
 */

public class MyNotiControl {

    private Context context;
    private RemoteViews rView;
    private NotificationCompat.Builder nBuilder;

    public MyNotiControl(Context parent) {
        this.context = parent;
        nBuilder = new NotificationCompat.Builder(context)
                .setContentTitle("GUNMAN SERVICE")
                .setSmallIcon(R.drawable.babtalk)
                .setPriority(Notification.PRIORITY_MIN) //요 부분이 핵심입니다. MAX가 아닌 MIN을 줘야 합니다.
                .setOngoing(true);

        rView = new RemoteViews(parent.getPackageName(), R.layout.mycontrolview); //노티바를 내렸을때 보여지는 화면입니다.

        //set the listener
        setListener(rView);
        nBuilder.setContent(rView);
    }

    public Notification getNoti() {
        return nBuilder.build();
    }

    public void setListener(RemoteViews view) {
        Intent i = new Intent(context, MyNotiControlView.class);
        i.putExtra("Foo", "bar");
        PendingIntent button = PendingIntent.getActivity(context, 0, i, 0);
        view.setOnClickPendingIntent(R.id.btn, button);
    }
}
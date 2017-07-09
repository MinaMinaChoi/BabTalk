package com.example.cmina.openmeeting.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import android.widget.Toast;

import static com.example.cmina.openmeeting.R.id.idEditText;

/**
 * Created by cmina on 2017-06-09.
 */

public class UIHandler {

    Context context;

    public UIHandler(Context context) {
        this.context = context;
    }

    public void toastHandler(final String str) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, str, Toast.LENGTH_SHORT).show();
            }
        }, 0);
    }

    public void addHandler(final String str, final TextView textView) {
        Handler handler = new Handler(Looper.getMainLooper()) ;
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                textView.setText(str);
            }
        }, 0);
    }
}

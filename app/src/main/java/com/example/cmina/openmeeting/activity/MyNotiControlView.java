package com.example.cmina.openmeeting.activity;

import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;

/**
 * Created by cmina on 2017-08-03.
 */

public class MyNotiControlView extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        String foo = (String) getIntent().getExtras().get("Foo");
        if (foo.equals("bar")) {
            //
        }
    }

}

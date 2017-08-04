package com.example.cmina.openmeeting.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

/**
 * Created by cmina on 2017-08-03.
 */

public class WifiChangeReceiver extends BroadcastReceiver {

    public static final String EVENT_NETWORK_CHAGED = "network_changed";
    private static boolean firstConnect = true;

    @Override
    public void onReceive(Context context, Intent intent) {
        final ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();
        if (activeNetInfo != null) {

            Log.d("WifiChangeReceiver", EVENT_NETWORK_CHAGED +" / "+activeNetInfo.getType());
            Intent intent1 = new Intent(EVENT_NETWORK_CHAGED);
            context.sendBroadcast(intent1);

        }

    }

}

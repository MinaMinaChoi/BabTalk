package com.example.cmina.openmeeting.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.example.cmina.openmeeting.service.SocketService;

/**
 * Created by cmina on 2017-08-03.
 * 즉, 요약하자면 1초 후부터 10초에 한번씩 RestartReceiver를 작동시키는 알람을 등록하게 됨.
 * 이 알람은 OS단에서 관리되기 때문에 TaskKiller에 영향을 받지 않음
 * RestartReceiver는 알람을 받고 SocketService를 다시 실행시키게 되는 형식.
 * 만약 TaskKiller에 의해 서비스가 죽더라도 10초안에 알람을 통해서 살아나게 되는 것.

 */

public class RestartReceiver extends BroadcastReceiver {

    static public final String ACTION_RESTART_SERVICE = "RestartReceiver.restart";

    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getAction().equals(ACTION_RESTART_SERVICE)){

            Intent i = new Intent(context, SocketService.class);
            context.startService(i);

        }
    }
}

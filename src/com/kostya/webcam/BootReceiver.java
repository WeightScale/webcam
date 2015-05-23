package com.kostya.webcam;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by Kostya on 16.05.14.
 */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        //context.startService(new Intent(context, SendDataService.class));// Запускаем сервис для передачи данных на google disk
        context.startService(new Intent(context, SMSIncomingService.class));
    }
}

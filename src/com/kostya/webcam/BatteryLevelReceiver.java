package com.kostya.webcam;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by Kostya on 30.06.14.
 */
public class BatteryLevelReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String intentAction = intent.getAction();

        if (intentAction.equalsIgnoreCase(Intent.ACTION_BATTERY_LOW)) {

        } else if (intentAction.equalsIgnoreCase(Intent.ACTION_BATTERY_OKAY)) {

        }


    }
}

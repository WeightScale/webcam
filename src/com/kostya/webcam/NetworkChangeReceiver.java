package com.kostya.webcam;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by Kostya on 20.12.2014.
 */
public class NetworkChangeReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, final Intent intent) {

        //String intentAction = intent.getAction();
        Internet.flagIsInternet = Internet.checkInternetConnection(context);
        //String status = NetworkUtil.getConnectivityStatusString(context);


    }
}

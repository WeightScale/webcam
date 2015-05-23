package com.kostya.webcam;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;

import java.io.File;

/**
 * Created by Kostya on 30.06.14.
 */
public class SMSIncomingService extends Service {
    BroadcastReceiver incomingSMSReceiver;

    final static String KEY_BODY = "body";
    final static String KEY_ADDRESS = "address";

    public static final String SMS_RECEIVED_ACTION = "android.provider.Telephony.SMS_RECEIVED";
    public static final String SMS_DELIVER_ACTION = "android.provider.Telephony.SMS_DELIVER";

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        startForeground(1, generateNotification());
        //return START_NOT_STICKY;
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        IntentFilter filter = new IntentFilter(SMS_RECEIVED_ACTION);
        filter.addAction(SMS_DELIVER_ACTION);
        incomingSMSReceiver = new IncomingSMSReceiver();
        registerReceiver(incomingSMSReceiver, filter);

        startForeground(1, generateNotification());

        Bundle bundle = new Bundle();
        //bundle.putString("body","command:take=nonstop ttt=uuu;settings:period_take=28 pic_size_height=480 pic_size_width=640 exposure=0;");
        bundle.putString(KEY_ADDRESS, "+380503285426");
        bundle.putString(KEY_BODY, "command:take=nonstop bluetooth=on get_pref=;settings:period_take=28 pic_size_height=480 pic_size_width=640 exposure=0;");
        Intent mIntent = new Intent(this, SmsService.class);
        //mIntent.putExtra("path",path);
        mIntent.putExtras(bundle);
        startService(mIntent);


    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(incomingSMSReceiver);
        stopForeground(true);
        stopSelf();
    }

    private class IncomingSMSReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null) {
                if (intent.getAction().equals(SMS_RECEIVED_ACTION)) {

                    Bundle bundle = intent.getExtras();
                    if (bundle != null) {
                        Object[] pdus = (Object[]) intent.getExtras().get("pdus");
                        SmsMessage[] messages = new SmsMessage[pdus.length];
                        StringBuilder bodyText = new StringBuilder();
                        for (int i = 0; i < pdus.length; i++) {
                            messages[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                            bodyText.append(messages[i].getMessageBody());
                        }

                        String address = messages[0].getDisplayOriginatingAddress();
                        bundle.putString(KEY_ADDRESS, address);
                        bundle.putString(KEY_BODY, bodyText.toString());
                        Intent mIntent = new Intent(context, SmsService.class);
                        mIntent.putExtras(bundle);
                        //mIntent.putExtra("path",path);
                        context.startService(mIntent);
                    }
                }
            }
        }
    }

    private Notification generateNotification() {

        //NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = new Notification(R.drawable.camera_notifi, "SMS_Control", System.currentTimeMillis());
        Intent notificationIntent = new Intent(getApplicationContext(), MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent intent = PendingIntent.getActivity(getBaseContext(), 0, notificationIntent, 0);
        notification.setLatestEventInfo(this, "Web Cam", "Получение команд", intent);
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        //notificationManager.notify(0, notification);
        return notification;
    }

}

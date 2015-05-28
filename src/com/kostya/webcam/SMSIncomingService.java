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

/** Сервис обработки входящих сообщений СМС.
 * @author Kostya.
 */
public class SMSIncomingService extends Service {
    /** Приемник сообений. */
    BroadcastReceiver incomingSMSReceiver;

    /** The constant KEY_BODY. */
    final static String KEY_BODY = "body";
    /** The constant KEY_ADDRESS.  */
    final static String KEY_ADDRESS = "address";

    /** The constant SMS_RECEIVED_ACTION. */
    public static final String SMS_RECEIVED_ACTION = "android.provider.Telephony.SMS_RECEIVED";
    /** The constant SMS_DELIVER_ACTION. */
    public static final String SMS_DELIVER_ACTION = "android.provider.Telephony.SMS_DELIVER";

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        /** Запускаем сервис в фоновом режиме */
        startForeground(1, generateNotification());
        //return START_NOT_STICKY;
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        /** Фильтер сообщений */
        IntentFilter filter = new IntentFilter(SMS_RECEIVED_ACTION);
        /** Добавить действие в фильтер */
        filter.addAction(SMS_DELIVER_ACTION);
        /** Экземпляр приемника сообщений */
        incomingSMSReceiver = new IncomingSMSReceiver();
        /** Регестрируем филтер в приемнике */
        registerReceiver(incomingSMSReceiver, filter);
        /** Запускаем сервис в фоновом режиме */
        //startForeground(1, generateNotification());

        /** для теста*/ //todo
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

    /**
     * Класс приемника входящих сообщений.
     */
    private class IncomingSMSReceiver extends BroadcastReceiver {

        /** Вызов сообщения.
         * @param context Еонтекст.
         * @param intent Цель сообщения.
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            /** Цель имеем действие */
            if (intent.getAction() != null) {
                /** Действие входящее сообщение */
                if (intent.getAction().equals(SMS_RECEIVED_ACTION)) {
                    /** Извлекаем пакет параметров */
                    Bundle bundle = intent.getExtras();
                    /** Есть пакет */
                    if (bundle != null) {
                        /** Получаем параметр PDU*/
                        Object[] pdus = (Object[]) intent.getExtras().get("pdus");
                        /** Создаем переменную для сообщений */
                        SmsMessage[] messages = new SmsMessage[pdus.length];
                        /** Создаем конструктор текста */
                        StringBuilder bodyText = new StringBuilder();
                        /** Извлекаем сообщения из PDU и сохраняем в переменную*/
                        for (int i = 0; i < pdus.length; i++) {
                            messages[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                            /** Сшиваем сообщения в один текст */
                            bodyText.append(messages[i].getMessageBody());
                        }
                        /** Получаем адресс отправителя сообщения */
                        String address = messages[0].getDisplayOriginatingAddress();
                        /** В пакет добавляем адресс отправителя*/
                        bundle.putString(KEY_ADDRESS, address);
                        /** Впакет добавляем текст сообщения */
                        bundle.putString(KEY_BODY, bodyText.toString());
                        /** Создаем цель для Сервиса обработки смс сообщений */
                        Intent mIntent = new Intent(context, SmsService.class);
                        /** Помещаем пакет в цель */
                        mIntent.putExtras(bundle);
                        //mIntent.putExtra("path",path);
                        /** Запускаем сервис с целью*/
                        context.startService(mIntent);
                    }
                }
            }
        }
    }

    /** Генератор уведомлений
     * @return Уведомление.
     */
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

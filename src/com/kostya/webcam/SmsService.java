package com.kostya.webcam;

import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.Preference;
import android.telephony.SmsManager;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

/** Сервис Обработчик СМС
 * @author Kostya
 */
public class SmsService extends Service {
    //static boolean take_single_flag = true;
    //BluetoothAdapter bluetooth = BluetoothAdapter.getDefaultAdapter();
    //BluetoothServer bluetoothServer;
    //private BroadcastReceiver broadcastReceiver; //приёмник намерений

    private final String CMD_TAKE = "take";                     //сделать фото
    private final String VALUE_TAKE_SINGLE = "single";
    private final String VALUE_TAKE_NONSTOP = "nonstop";
    private final String VALUE_TAKE_IFSEND = "ifsend";

    /** The constant CMD_GET_PREF.  */
    public final static String CMD_GET_PREF = "get_pref";
    /** The constant CMD_BLUETOOTH.  */
    public final static String CMD_BLUETOOTH = "bluetooth";


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        /** Цель имеет параметры */
        if (intent.getExtras() != null /*&& intent.getAction()!=null*/) {
            /** Получить параметр*/
            String command = intent.getExtras().getString(SMSIncomingService.KEY_BODY);
            String address = intent.getExtras().getString(SMSIncomingService.KEY_ADDRESS);
            if (command != null) {
                String[] settings, commands;
                String[] str = command.split(";");
                for (int i = 0; i < str.length; i++) {
                    str[i] = str[i].toLowerCase();
                    if (str[i].contains(getString(R.string.pref_settings))) {
                        int index = str[i].indexOf(":");
                        settings = (getString(R.string.pref_settings) + str[i].substring(index)).split(":");
                        new SettingsAsyncTask(this).execute(settings);
                    } else if (str[i].contains("command")) {
                        commands = str[i].split(":");
                        executeCommand(commands[1], address);
                    }
                }
            }
        }
        stopSelf();
        return START_NOT_STICKY;
    }

    /** Выполнить команду.
     * @param reader the reader
     * @param address the address
     * @return the boolean
     */
    boolean executeCommand(String reader, String address) {
        String[] parts = reader.split(" ", 0);
        SimpleCommandLineParser commands = new SimpleCommandLineParser(parts, "=");
        Iterator<String> iteratorCommands = commands.getKeyIterator();
        while (iteratorCommands.hasNext()) {
            String cmd = iteratorCommands.next();
            if (cmd.equals(CMD_TAKE)) {
                boolean fTake_single;
                String value = commands.getValue(CMD_TAKE);
                if (value.equalsIgnoreCase(VALUE_TAKE_SINGLE))
                    fTake_single = true;
                else if (value.equalsIgnoreCase(VALUE_TAKE_NONSTOP))
                    fTake_single = false;
                else if (value.equalsIgnoreCase(VALUE_TAKE_IFSEND))
                    fTake_single = false;
                else
                    break;
                startService(new Intent(getApplicationContext(), TakeService.class).setAction(CMD_TAKE).putExtra(getString(R.string.key_flag_take_single), fTake_single));
                //return true;
            } else if (cmd.equals(CMD_GET_PREF)) {
                //Preferences preferences = new Preferences(getSharedPreferences(Preferences.PREF_SETTINGS,Context.MODE_PRIVATE));
                SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.pref_settings), Context.MODE_PRIVATE);
                if (sharedPreferences.getAll() == null)
                    break;
                //Map<String,?> map = preferences.getSharedPreferences().getAll();
                StringBuilder stringBuilder = new StringBuilder("command:");
                for (Map.Entry<String, ?> entry : sharedPreferences.getAll().entrySet()) {
                    stringBuilder.append(" " + entry.getKey() + "=");
                    stringBuilder.append(String.valueOf(entry.getValue()));
                }
                stringBuilder.append(";");
                sendSMS(address, stringBuilder.toString());
            } else if (cmd.equals(CMD_BLUETOOTH)) {
                boolean flag_bluetooth = false;
                String value = commands.getValue(CMD_BLUETOOTH);
                if (value.equalsIgnoreCase("on"))
                    flag_bluetooth = true;
                else if (value.equalsIgnoreCase("off"))
                    flag_bluetooth = false;
                else
                    break;
                startService(new Intent(getApplicationContext(), BluetoothServer.class).setAction(CMD_BLUETOOTH).putExtra("flag_bluetooth", flag_bluetooth));
            }
        }
        return false;
    }

    private void sendSMS(String phoneNumber, String message) {
        SmsManager sms = SmsManager.getDefault();
        ArrayList<String> parts = sms.divideMessage(message);
        try {
            //sms.sendTextMessage(phoneNumber, null, message, null, null);
            sms.sendMultipartTextMessage(phoneNumber, null, parts, null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

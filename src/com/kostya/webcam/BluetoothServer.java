package com.kostya.webcam;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.IBinder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.UUID;

/**
 * Created by Kostya on 19.09.14.
 */
public class BluetoothServer extends Service {
    final BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
    private BroadcastReceiver broadcastReceiver; //приёмник намерений
    public AcceptThread acceptThread;
    static final String CAM_VERSION = "WeightScales3";
    //Context context;
    UshellTask ushellTask;

    boolean flag_connect = false;

    /*BluetoothServer(Context c, BluetoothAdapter bluetoothAdapter){
        context = c;
        adapter = bluetoothAdapter;
        ushellTask = new UshellTask(context,BluetoothServer.this);
    }*/

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (adapter != null) {

            broadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) { //обработчик Bluetooth'а
                    String action = intent.getAction();
                    if (action != null) {
                        if (action.equals(BluetoothDevice.ACTION_ACL_DISCONNECTED)) { //устройство отсоеденено
                            flag_connect = false;
                            disconnect();
                        } else if (action.equals(BluetoothDevice.ACTION_ACL_CONNECTED)) { //найдено соеденено
                            flag_connect = true;
                        } else if (action.equals("connectSocket")) {
                            connect();
                        }
                    }
                }
            };

            IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED);
            intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
            intentFilter.addAction("connectSocket");
            registerReceiver(broadcastReceiver, intentFilter);

            ushellTask = new UshellTask(getApplicationContext(), BluetoothServer.this);

        } else {
            stopSelf();
        }

        //connect();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        if (intent != null)
            if (intent.getAction() != null)
                if (intent.getAction().equals(SmsService.CMD_BLUETOOTH)) {
                    if (intent.getExtras() != null) {
                        //flag_take_single = intent.getBooleanExtra(Preferences.KEY_FLAG_TAKE_SINGLE, true);
                        boolean flag = intent.getBooleanExtra("flag_bluetooth", false);
                        if (flag)
                            connect();
                        else {
                            stopSelf();
                        }
                    }
                }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (adapter.isDiscovering())
            adapter.cancelDiscovery();
        unregisterReceiver(broadcastReceiver);
        cancelAcceptThread(false);
        disconnect();
        adapter.disable();
    }

    void connect() {
        if (!adapter.isEnabled()) {
            adapter.enable();
        }
        while (!adapter.isEnabled()) ;
        acceptThread = new AcceptThread();
        acceptThread.execute();
    }

    public void send(String str) {
        if (acceptThread != null)
            acceptThread.write(str);
    }

    void disconnect() {
        if (acceptThread != null)
            acceptThread.close();
    }

    private class AcceptThread extends AsyncTask<Void, Void, Boolean> {
        private boolean closed = true;
        BluetoothAdapter adapter;
        private BluetoothServerSocket mmServerSocket;
        BluetoothSocket socket;
        private InputStream mmInStream;
        private OutputStreamWriter mmOutputStreamWriter;
        final UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");//"00001101-0000-1000-8000-00805F9B34FB"


        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            adapter = BluetoothAdapter.getDefaultAdapter();
            closed = false;
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            try {
                adapter.cancelDiscovery();
                mmServerSocket = adapter.listenUsingRfcommWithServiceRecord("WeightScale", uuid);
                socket = mmServerSocket.accept();

                if (socket != null) {
                    mmServerSocket.close();
                    mmInStream = socket.getInputStream();
                    mmOutputStreamWriter = new OutputStreamWriter(socket.getOutputStream());

                    while (!isCancelled()) {
                        try {
                            byte byteRead = (byte) mmInStream.read();
                            ushellTask.buildCommand(byteRead);
                        } catch (IOException e) {
                            e.printStackTrace();
                            if (socket != null)
                                try {
                                    socket.close();
                                    socket = null;
                                } catch (IOException e1) {
                                    e1.printStackTrace();
                                }
                            break;
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();//todo
                if (isCancelled())
                    return false;
            }
            if (isCancelled())
                return false;
            closed = true;
            return true;
        }

        @Override
        protected void onPostExecute(Boolean b) {
            super.onPostExecute(b);
            if (b)
                sendBroadcast(new Intent("connectSocket"));
        }

        public void write(String bytes) {
            try {
                mmOutputStreamWriter.write(bytes);
                mmOutputStreamWriter.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void close() {
            try {
                if (mmServerSocket != null)
                    mmServerSocket.close();
                if (socket != null)
                    socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    void cancelAcceptThread(boolean b) {
        if (acceptThread != null) {
            acceptThread.cancel(b);
        }
    }

    /*boolean isConnected(){
        if(acceptThread.socket!=null)
            return acceptThread.socket.isConnected();
        return false;
    }*/
}

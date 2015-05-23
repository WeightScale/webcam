//Управляет соединениями (Bluetooth, Wi-Fi, мобильная сеть)
package com.kostya.webcam;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

class Internet {
    private final Context context;
    //private BroadcastReceiver broadcastReceiver;
    private TelephonyManager telephonyManager;
    private PhoneStateListener phoneStateListener;

    public final static String INTERNET_CONNECT = "internet_connect";
    public final static String INTERNET_DISCONNECT = "internet_disconnect";

    public static boolean flagIsInternet = false;

    Internet(Context c) {
        context = c;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        //context.unregisterReceiver(broadcastReceiver);
    }

    void connect() {
        //context=c;
        //final WifiManager wifi=(WifiManager)context.getSystemService(Context.WIFI_SERVICE);
        telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_DATA_CONNECTION_STATE);
        phoneStateListener = new PhoneStateListener() {
            public void onDataConnectionStateChanged(int state) {
                switch (state) {
                    case TelephonyManager.DATA_DISCONNECTED:
                        if (telephonyManager != null)
                            turnOnDataConnection(true);
                        break;
                    default:
                        break;
                }
            }
        };
        turnOnDataConnection(true);
    }

    void disconnect() {
        /*if(broadcastReceiver!=null){
            context.unregisterReceiver(broadcastReceiver);
            broadcastReceiver = null;
        }*/
        if (telephonyManager != null) {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
            telephonyManager = null;
        }

        turnOnDataConnection(false);
        //turnOnWiFiConnection(false);
    }

	/*boolean isConnected(){ //есть ли соединение?
		return ((ConnectivityManager)(context.getSystemService(Context.CONNECTIVITY_SERVICE))).getActiveNetworkInfo().isConnected();
	}*/

    boolean haveNetworkConnection() {
        boolean haveConnectedWifi = false;
        boolean haveConnectedMobile = false;

        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo[] netInfo = cm.getAllNetworkInfo();
        for (NetworkInfo ni : netInfo != null ? netInfo : new NetworkInfo[0]) {
            if (ni.getTypeName().equalsIgnoreCase("WIFI"))
                if (ni.isConnected())
                    haveConnectedWifi = true;
            if (ni.getTypeName().equalsIgnoreCase("MOBILE"))
                if (ni.isConnected())
                    haveConnectedMobile = true;
        }
        return haveConnectedWifi || haveConnectedMobile;
    }

    public boolean checkInternetConnection() {
        try {
            ConnectivityManager con_manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            return con_manager != null && con_manager.getActiveNetworkInfo() != null && con_manager.getActiveNetworkInfo().isAvailable() && con_manager.getActiveNetworkInfo().isConnected();
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean checkInternetConnection(Context cont) {
        try {
            ConnectivityManager con_manager = (ConnectivityManager) cont.getSystemService(Context.CONNECTIVITY_SERVICE);
            return con_manager != null && con_manager.getActiveNetworkInfo() != null && con_manager.getActiveNetworkInfo().isAvailable() && con_manager.getActiveNetworkInfo().isConnected();
        } catch (Exception e) {
            return false;
        }
    }

    public void turnOnWiFiConnection(boolean on) {
        WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        wifi.setWifiEnabled(on);
    }

    private boolean turnOnDataConnection(boolean on) {
        int bv = Build.VERSION.SDK_INT;
        try {
            if (bv == Build.VERSION_CODES.FROYO) {
                Method dataConnSwitchmethod;
                Class<?> telephonyManagerClass;
                Object ITelephonyStub;
                Class<?> ITelephonyClass;

                TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

                telephonyManagerClass = Class.forName(telephonyManager.getClass().getName());
                Method getITelephonyMethod = telephonyManagerClass.getDeclaredMethod("getITelephony");
                getITelephonyMethod.setAccessible(true);
                ITelephonyStub = getITelephonyMethod.invoke(telephonyManager);
                ITelephonyClass = Class.forName(ITelephonyStub.getClass().getName());

                if (on)
                    dataConnSwitchmethod = ITelephonyClass.getDeclaredMethod("enableDataConnectivity");
                else
                    dataConnSwitchmethod = ITelephonyClass.getDeclaredMethod("disableDataConnectivity");

                dataConnSwitchmethod.setAccessible(true);
                dataConnSwitchmethod.invoke(ITelephonyStub);
            } else {
                //log.i("App running on Ginger bread+");
                final ConnectivityManager conman = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                final Class<?> conmanClass = Class.forName(conman.getClass().getName());
                final Field iConnectivityManagerField = conmanClass.getDeclaredField("mService");
                iConnectivityManagerField.setAccessible(true);
                final Object iConnectivityManager = iConnectivityManagerField.get(conman);
                final Class<?> iConnectivityManagerClass = Class.forName(iConnectivityManager.getClass().getName());
                final Method setMobileDataEnabledMethod = iConnectivityManagerClass.getDeclaredMethod("setMobileDataEnabled", Boolean.TYPE);
                setMobileDataEnabledMethod.setAccessible(true);
                setMobileDataEnabledMethod.invoke(iConnectivityManager, on);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

}

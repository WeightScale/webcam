//Управляет соединениями (Bluetooth, Wi-Fi, мобильная сеть)
package com.kostya.webcam;

import android.bluetooth.BluetoothAdapter;
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

/** Класс для работы с интернет
 * @author Kostya
 */
public class Internet {
    private final Context context;
    private TelephonyManager telephonyManager;
    private PhoneStateListener phoneStateListener;

    /** The constant INTERNET_CONNECT. */
    public static final String INTERNET_CONNECT = "internet_connect";
    /** The constant INTERNET_DISCONNECT. */
    public static final String INTERNET_DISCONNECT = "internet_disconnect";

    /** The constant flagIsInternet. */
    public static boolean flagIsInternet = false;

    /**
     * Экземпляр нового класса Internet.
     * @param context контекст.
     */
    public Internet(Context context) {
        this.context = context;
    }

    /**
     * Соединение с интернет.
     */
    public void connect() {
        telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        phoneStateListener = new PhoneStateListener() {
            @Override
            public void onDataConnectionStateChanged(int state) {
                switch (state) {
                    case TelephonyManager.DATA_DISCONNECTED:
                        if (telephonyManager != null) {
                            turnOnDataConnection(true);
                        }
                        break;
                    default:
                        break;
                }
            }
        };
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_DATA_CONNECTION_STATE);


        if (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
            turnOnWiFiConnection(true);
        }
        turnOnDataConnection(true);
    }

    /**
     * Рассоединение с интернет
     */
    public void disconnect() {
        if (telephonyManager != null) {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
            telephonyManager = null;
        }

        turnOnDataConnection(false);
        turnOnWiFiConnection(false);
    }

    /**
     * Проверяем интернет.
     * @return true - есть интернет.
     */
    public static boolean isOnline() {
        try {
            Process p1 = Runtime.getRuntime().exec("ping -c 1 www.google.com");
            int returnVal = p1.waitFor();
            return returnVal == 0;
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return false;
    }

    /** Включаем / выключаем wifi connect
     * @param on true - включить.
     */
    public void turnOnWiFiConnection(boolean on) {
        WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if (wifi == null) {
            return;
        }
        wifi.setWifiEnabled(on);
        while (wifi.isWifiEnabled() != on) ;
    }

    /** Включаем / выключаем data connect
     * @param on true - включить.
     * @return true - включен.
     */
    private boolean turnOnDataConnection(boolean on) {
        try {
            int bv = Build.VERSION.SDK_INT;
            //int bv = Build.VERSION_CODES.FROYO;
            if (bv == Build.VERSION_CODES.FROYO) { //2.2

                TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

                Class<?> telephonyManagerClass = Class.forName(telephonyManager.getClass().getName());
                Method getITelephonyMethod = telephonyManagerClass.getDeclaredMethod("getITelephony");
                getITelephonyMethod.setAccessible(true);
                Object ITelephonyStub = getITelephonyMethod.invoke(telephonyManager);
                Class<?> ITelephonyClass = Class.forName(ITelephonyStub.getClass().getName());

                Method dataConnSwitchMethod = on ? ITelephonyClass.getDeclaredMethod("enableDataConnectivity") : ITelephonyClass.getDeclaredMethod("disableDataConnectivity");

                dataConnSwitchMethod.setAccessible(true);
                dataConnSwitchMethod.invoke(ITelephonyStub);
            } else if (bv <= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                //log.i("App running on Ginger bread+");
                /*final ConnectivityManager conman = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                final Class<?> conmanClass = Class.forName(conman.getClass().getName());
                final Field iConnectivityManagerField = conmanClass.getDeclaredField("mService");
                iConnectivityManagerField.setAccessible(true);
                final Object iConnectivityManager = iConnectivityManagerField.get(conman);
                final Class<?> iConnectivityManagerClass =  Class.forName(iConnectivityManager.getClass().getName());
                final Method setMobileDataEnabledMethod = iConnectivityManagerClass.getDeclaredMethod("setMobileDataEnabled", Boolean.TYPE);
                setMobileDataEnabledMethod.setAccessible(true);
                setMobileDataEnabledMethod.invoke(iConnectivityManager, on);*/

                //Cursor cursor = context.getContentResolver().query(Settings.System.CONTENT_URI, null, null,null, null);
                /*Cursor cursor = context.getContentResolver().query(Settings.System.CONTENT_URI, null, null,null, null);

                ContentQueryMap mQueryMap = new ContentQueryMap(cursor, BaseColumns._ID, true, null);
                Map<String,ContentValues> map = mQueryMap.getRows();
                ContentValues values = map.get(Settings.Secure.DATA_ROAMING);*/

                ConnectivityManager dataManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                Method setMobileDataEnabledMethod = ConnectivityManager.class.getDeclaredMethod("setMobileDataEnabled", boolean.class);
                setMobileDataEnabledMethod.setAccessible(true);
                setMobileDataEnabledMethod.invoke(dataManager, on);

                // context.startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));

                /*Intent intent=new Intent(Settings.ACTION_DATA_ROAMING_SETTINGS);
                ComponentName cn = new ComponentName("com.android.phone","com.android.phone.Settings");
                intent.setComponent(cn);
                context.startActivity(intent);*/

                //Global.getInt(context.getContentResolver(), "mobile_data");

                //((Activity) context).startActivityForResult(new Intent(android.provider.Settings.ACTION_DATA_ROAMING_SETTINGS), 0);

                //final  Intent intent=new Intent(Settings.ACTION_WIRELESS_SETTINGS);
                //intent.addCategory(Intent.CATEGORY_LAUNCHER);
                //final ComponentName cn = new ComponentName("com.android.phone","com.android.phone.Settings");
                //intent.setComponent(cn);
                //intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                //context.startActivity(intent);
            } else {
                ConnectivityManager dataManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                 /*final Method setMobileDataEnabledMethod = ConnectivityManager.class.getDeclaredMethod("setMobileDataEnabled", Boolean.TYPE);
                //Method[] setMobileDataEnabledMethod = ConnectivityManager.class.getDeclaredMethods("setMobileDataEnabled");
                setMobileDataEnabledMethod.setAccessible(on);
                setMobileDataEnabledMethod.invoke(dataManager, on);*/

                Method dataMtd = null;
                try {
                    dataMtd = ConnectivityManager.class.getDeclaredMethod("setMobileDataEnabled", boolean.class);
                } catch (SecurityException | NoSuchMethodException e) {
                    e.printStackTrace();
                }

                assert dataMtd != null;
                dataMtd.setAccessible(true);
                try {
                    dataMtd.invoke(dataManager, on);
                } catch (Exception e) {
                    e.printStackTrace();
                }


                /*TelephonyManager tm = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
                Method methodSet = Class.forName(tm.getClass().getName()).getDeclaredMethod( "setDataEnabled", Boolean.TYPE);
                methodSet.invoke(tm,on);*/

                /*TelephonyManager telephonyService = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                Method setMobileDataEnabledMethod = telephonyService.getClass().getDeclaredMethod("setDataEnabled", boolean.class);
                if (null != setMobileDataEnabledMethod)      {
                    setMobileDataEnabledMethod.invoke(telephonyService, on);
                }*/

                /*Intent intent=new Intent(Settings.ACTION_DATA_ROAMING_SETTINGS);
                ComponentName cn = new ComponentName("com.android.phone","com.android.phone.Settings");
                intent.setComponent(cn);
                context.startActivity(intent);*/

               /* Method dataMtd = ConnectivityManager.class.getDeclaredMethod("setMobileDataEnabled", boolean.class);
                dataMtd.setAccessible(on);
                dataMtd.invoke((ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE), on);*/

                /*Class[] cArg = new Class[2];
                cArg[0] = String.class;
                cArg[1] = Boolean.TYPE;
                Method setMobileDataEnabledMethod;

                setMobileDataEnabledMethod = iConnectivityManagerClass.getDeclaredMethod("setMobileDataEnabled", cArg);

                Object[] pArg = new Object[2];
                pArg[0] = getContext().getPackageName();
                pArg[1] = true;
                setMobileDataEnabledMethod.setAccessible(true);
                setMobileDataEnabledMethod.invoke(iConnectivityManager, pArg);*/
            }
            return true;
        } catch (Exception ignored) {
            Log.e("hhh", "error turning on/off data");
            return false;
        }
    }

    /*private boolean turnOnDataConnection(boolean on) {
        try{
            int bv = Build.VERSION.SDK_INT;
            if(bv == Build.VERSION_CODES.FROYO){

                TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

                Class<?> telephonyManagerClass = Class.forName(telephonyManager.getClass().getName());
                Method getITelephonyMethod = telephonyManagerClass.getDeclaredMethod("getITelephony");
                getITelephonyMethod.setAccessible(true);
                Object ITelephonyStub = getITelephonyMethod.invoke(telephonyManager);
                Class<?> ITelephonyClass = Class.forName(ITelephonyStub.getClass().getName());

                Method dataConnSwitchMethod;
                if (on)
                    dataConnSwitchMethod = ITelephonyClass.getDeclaredMethod("enableDataConnectivity");
                else
                    dataConnSwitchMethod = ITelephonyClass.getDeclaredMethod("disableDataConnectivity");

                dataConnSwitchMethod.setAccessible(true);
                dataConnSwitchMethod.invoke(ITelephonyStub);
            }
            else{
                //log.i("App running on Ginger bread+");
                final ConnectivityManager conman = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                final Class<?> conmanClass = Class.forName(conman.getClass().getName());
                final Field iConnectivityManagerField = conmanClass.getDeclaredField("mService");
                iConnectivityManagerField.setAccessible(true);
                final Object iConnectivityManager = iConnectivityManagerField.get(conman);
                final Class<?> iConnectivityManagerClass =  Class.forName(iConnectivityManager.getClass().getName());
                final Method setMobileDataEnabledMethod = iConnectivityManagerClass.getDeclaredMethod("setMobileDataEnabled", Boolean.TYPE);
                setMobileDataEnabledMethod.setAccessible(true);
                setMobileDataEnabledMethod.invoke(iConnectivityManager, on);
            }
            return true;
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch(Exception ignored){
            Log.e("hhh", "error turning on/off data");
        }
        return false;
    }*/

    /** Отослать на URL
     * @param url URL - осылки.
     * @return true - отослан.
     */
    protected static boolean send(URL url) {
        try {
            URLConnection urlConnection = url.openConnection();
            if (!(urlConnection instanceof HttpURLConnection)) {
                throw new IOException("URL is not an Http URL");
            }
            HttpURLConnection connection = (HttpURLConnection) urlConnection;
            //connection.setReadTimeout(3000);
            //connection.setConnectTimeout(3000);
            connection.connect();
            int responseCode = connection.getResponseCode();
            return responseCode == HttpURLConnection.HTTP_OK;

        } catch (MalformedURLException ignored) {
            return false;
        } catch (IOException ignored) {
            return false;
        }
    }

}

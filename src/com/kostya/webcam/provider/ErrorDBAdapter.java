package com.kostya.webcam.provider;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.telephony.TelephonyManager;

import java.text.SimpleDateFormat;
import java.util.Date;

public class ErrorDBAdapter {

    private final Context context;
    //private SQLiteDatabase db;

    public static int day;

    public static final String TABLE_ERROR = "errorTable";

    public static final String KEY_ID = "_id";
    public static final String KEY_DATE_CREATE = "dateCreate";
    public static final String KEY_NUMBER_ERROR = "numberError";
    public static final String KEY_DESCRIPTION = "description";
    public static final String KEY_NUMBER_BT = "bluetooth";

    private static final String[] All_COLUMN_ERROR_TABLE = new String[]{KEY_ID, KEY_DATE_CREATE, KEY_NUMBER_ERROR, KEY_DESCRIPTION, KEY_NUMBER_BT};

    public static final String TABLE_CREATE_ERROR = "create table "
            + TABLE_ERROR + " ("
            + KEY_ID + " integer primary key autoincrement, "
            + KEY_DATE_CREATE + " text,"
            + KEY_NUMBER_ERROR + " text,"
            + KEY_DESCRIPTION + " text,"
            + KEY_NUMBER_BT + " text );";

    static final String TABLE_ERROR_PATH = TABLE_ERROR;
    private static final Uri TABLE_ERROR_CONTENT_URI = Uri.parse("content://" + WebCamBaseProvider.AUTHORITY + "/" + TABLE_ERROR_PATH);

    public ErrorDBAdapter(Context cnt) {
        context = cnt;
    }

    public ErrorDBAdapter(Context cnt, int d) {
        context = cnt;
        day = d;
    }

    public Uri insertNewEntry(String number, String des) {
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yy HH:mm:ss");
        ContentValues newTaskValues = new ContentValues();
        TelephonyManager mngr = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        //mngr.getDeviceId();
        String bt_number = mngr.getDeviceId();
        newTaskValues.put(KEY_DATE_CREATE, sdf.format(date));
        if (bt_number != null)
            newTaskValues.put(KEY_NUMBER_BT, bt_number);
        newTaskValues.put(KEY_NUMBER_ERROR, number);
        newTaskValues.put(KEY_DESCRIPTION, des);
        return context.getContentResolver().insert(TABLE_ERROR_CONTENT_URI, newTaskValues);
    }

    boolean removeEntry(long _rowIndex) {
        return context.getContentResolver().delete(ContentUris.withAppendedId(TABLE_ERROR_CONTENT_URI, _rowIndex), null, null) > 0;
    }

    long dayDiff(Date d1, Date d2) {
        final long DAY_MILLIS = 1000 * 60 * 60 * 24;
        long day1 = d1.getTime() / DAY_MILLIS;
        long day2 = d2.getTime() / DAY_MILLIS;
        return (day1 - day2);
    }

    private String getKeyString(long _rowIndex, String key) {
        Cursor result = context.getContentResolver().query(ContentUris.withAppendedId(TABLE_ERROR_CONTENT_URI, _rowIndex), new String[]{KEY_ID, key}, null, null, null);
        if ((result.getCount() == 0) || !result.moveToFirst()) {
            result.close();
            return "";
            //throw new SQLiteException("Нет записи с номером строки" + _rowIndex);
        }
        String str = result.getString(result.getColumnIndex(key));
        result.close();
        return str;
    }

    public Cursor getAllEntries() {
        return context.getContentResolver().query(TABLE_ERROR_CONTENT_URI, All_COLUMN_ERROR_TABLE, null, null, null);
    }

    public Cursor getErrorCodeCounts(int count) {
        return context.getContentResolver().query(TABLE_ERROR_CONTENT_URI, new String[]{KEY_ID, KEY_NUMBER_ERROR}, null, null, KEY_ID + " DESC " + " LIMIT " + String.valueOf(count));
    }

    public Cursor getEntryItem(long _rowIndex) throws SQLiteException {
        Cursor result = context.getContentResolver().query(ContentUris.withAppendedId(TABLE_ERROR_CONTENT_URI, _rowIndex),
                All_COLUMN_ERROR_TABLE, null, null, null);
        if ((result.getCount() == 0) || !result.moveToFirst()) {
            //throw new SQLiteException("Нет записи с номером строки" + _rowIndex);
        }
        return result;
    }

    public boolean updateEntry(long _rowIndex, String key, int in) {
        //boolean b;
        ContentValues newValues = new ContentValues();
        newValues.put(key, in);
        return context.getContentResolver().update(ContentUris.withAppendedId(TABLE_ERROR_CONTENT_URI, _rowIndex), newValues, null, null) > 0;
    }

    public boolean updateEntry(long _rowIndex, String key, float fl) {
        ContentValues newValues = new ContentValues();
        newValues.put(key, fl);
        return context.getContentResolver().update(ContentUris.withAppendedId(TABLE_ERROR_CONTENT_URI, _rowIndex), newValues, null, null) > 0;
    }

    public boolean updateEntry(long _rowIndex, String key, String st) {
        ContentValues newValues = new ContentValues();
        newValues.put(key, st);
        return context.getContentResolver().update(ContentUris.withAppendedId(TABLE_ERROR_CONTENT_URI, _rowIndex), newValues, null, null) > 0;
    }

}

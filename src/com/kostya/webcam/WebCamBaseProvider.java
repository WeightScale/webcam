package com.kostya.webcam;

import android.content.*;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

/**
 * Created with IntelliJ IDEA.
 * User: Kostya
 * Date: 08.12.13
 * Time: 13:11
 * To change this template use File | Settings | File Templates.
 */
public class WebCamBaseProvider extends ContentProvider {

    private static final String DATABASE_NAME = "webCam.db";
    private static final int DATABASE_VERSION = 1;
    static final String AUTHORITY = "com.kostya.webcam.webCam";

    private static final int ALL_ROWS = 1;
    private static final int SINGLE_ROWS = 2;

    private static final int ERROR_LIST = 1;
    private static final int ERROR_ID = 2;
    private static final int VENDOR_LIST = 3;
    private static final int VENDOR_ID = 4;
    private static final int PREFERENCES_LIST = 5;
    private static final int PREFERENCES_ID = 6;
    private static final int TYPE_LIST = 7;
    private static final int TYPE_ID = 8;

    private static final UriMatcher uriMatcher;

    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(AUTHORITY, ErrorDBAdapter.TABLE_ERROR_PATH, ERROR_LIST);
        uriMatcher.addURI(AUTHORITY, ErrorDBAdapter.TABLE_ERROR_PATH + "/#", ERROR_ID);
        /*uriMatcher.addURI(AUTHORITY, VendorDBAdapter.TABLE_VENDOR_PATH, VENDOR_LIST);
        uriMatcher.addURI(AUTHORITY, VendorDBAdapter.TABLE_VENDOR_PATH + "/#", VENDOR_ID);
        uriMatcher.addURI(AUTHORITY, PreferencesDBAdapter.TABLE_PREFERENCES_PATH, PREFERENCES_LIST);
        uriMatcher.addURI(AUTHORITY, PreferencesDBAdapter.TABLE_PREFERENCES_PATH + "/#", PREFERENCES_ID);
        uriMatcher.addURI(AUTHORITY, TypeDBAdapter.TABLE_TYPE_PATH, TYPE_LIST);
        uriMatcher.addURI(AUTHORITY, TypeDBAdapter.TABLE_TYPE_PATH + "/#", TYPE_ID);*/
    }

    private SQLiteDatabase db;

    public void vacuum() {
        db.execSQL("VACUUM");
    }

    public WebCamBaseProvider() {

    }

    private String getTable(Uri uri) {
        switch (uriMatcher.match(uri)) {
            case ERROR_LIST:
            case ERROR_ID:
                return ErrorDBAdapter.TABLE_ERROR_PATH; // return
            /*case VENDOR_LIST: case VENDOR_ID:
                return VendorDBAdapter.TABLE_VENDOR_PATH; // return
            case PREFERENCES_LIST: case PREFERENCES_ID:
                return PreferencesDBAdapter.TABLE_PREFERENCES_PATH; // return
            case TYPE_LIST: case TYPE_ID:
                return TypeDBAdapter.TABLE_TYPE_PATH;*/ // return
            /** PROVIDE A DEFAULT CASE HERE **/
            default:
                // If the URI doesn't match any of the known patterns, throw an exception.
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public boolean onCreate() {
        DBHelper dbHelper = new DBHelper(getContext());
        db = dbHelper.getWritableDatabase();
        db.setLockingEnabled(false);
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sort) {
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        switch (uriMatcher.match(uri)) {
            case ERROR_LIST: // общий Uri
                queryBuilder.setTables(ErrorDBAdapter.TABLE_ERROR);
                break;
            case ERROR_ID: // Uri с ID
                queryBuilder.setTables(ErrorDBAdapter.TABLE_ERROR);
                queryBuilder.appendWhere(ErrorDBAdapter.KEY_ID + "=" + uri.getLastPathSegment());
                break;
            /*case VENDOR_LIST: // общий Uri
                queryBuilder.setTables(VendorDBAdapter.TABLE_VENDOR);
                break;
            case VENDOR_ID: // Uri с ID
                queryBuilder.setTables(VendorDBAdapter.TABLE_VENDOR);
                queryBuilder.appendWhere(VendorDBAdapter.KEY_ID + "=" + uri.getLastPathSegment());
                break;
            case PREFERENCES_LIST: // общий Uri
                queryBuilder.setTables(PreferencesDBAdapter.TABLE_PREFERENCES);
                break;
            case PREFERENCES_ID: // Uri с ID
                queryBuilder.setTables(PreferencesDBAdapter.TABLE_PREFERENCES);
                queryBuilder.appendWhere(PreferencesDBAdapter.KEY_ID + "=" + uri.getLastPathSegment());
                break;
            case TYPE_LIST: // общий Uri
                queryBuilder.setTables(TypeDBAdapter.TABLE_TYPE);
                break;
            case TYPE_ID: // Uri с ID
                queryBuilder.setTables(TypeDBAdapter.TABLE_TYPE);
                queryBuilder.appendWhere(TypeDBAdapter.KEY_ID + "=" + uri.getLastPathSegment());
                break;*/
            default:
                throw new IllegalArgumentException("Wrong URI: " + uri);
        }
        Cursor cursor = queryBuilder.query(db, projection, selection, selectionArgs, null, null, sort);
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        switch (uriMatcher.match(uri)) {
            case ALL_ROWS:
                return "vnd.android.cursor.dir/vnd.";
            case SINGLE_ROWS:
                return "vnd.android.cursor.item/vnd.";
        }
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {

        long rowID = db.insert(getTable(uri), null, contentValues);
        if (rowID > 0) {
            Uri resultUri = ContentUris.withAppendedId(uri, rowID);
            // уведомляем ContentResolver, что данные по адресу resultUri изменились
            getContext().getContentResolver().notifyChange(resultUri, null);
            return resultUri;
        }
        throw new SQLiteException("Ошибка добавления записи " + uri);
    }

    @Override
    public int delete(Uri uri, String where, String[] whereArg) {
        int delCount;
        String id;
        switch (uriMatcher.match(uri)) {
            case ERROR_LIST: // общий Uri
                delCount = db.delete(ErrorDBAdapter.TABLE_ERROR, where, whereArg);
                break;
            case ERROR_ID:
                id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(where))
                    where = ErrorDBAdapter.KEY_ID + " = " + id;
                else
                    where = where + " AND " + ErrorDBAdapter.KEY_ID + " = " + id;
                delCount = db.delete(ErrorDBAdapter.TABLE_ERROR, where, whereArg);
                break;
            /*case VENDOR_LIST: // общий Uri
                delCount = db.delete(VendorDBAdapter.TABLE_VENDOR,where,whereArg);
            break;
            case VENDOR_ID:
                id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(where))
                    where = VendorDBAdapter.KEY_ID + " = " + id;
                else
                    where = where + " AND " + VendorDBAdapter.KEY_ID + " = " + id;
                delCount = db.delete(VendorDBAdapter.TABLE_VENDOR,where,whereArg);
            break;
            case PREFERENCES_LIST: // общий Uri
                delCount = db.delete(PreferencesDBAdapter.TABLE_PREFERENCES,where,whereArg);
                break;
            case PREFERENCES_ID:
                id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(where))
                    where = PreferencesDBAdapter.KEY_ID + " = " + id;
                else
                    where = where + " AND " + PreferencesDBAdapter.KEY_ID + " = " + id;
                delCount = db.delete(PreferencesDBAdapter.TABLE_PREFERENCES,where,whereArg);
            break;
            case TYPE_LIST:
                delCount = db.delete(TypeDBAdapter.TABLE_TYPE,where,whereArg);
            break;
            case TYPE_ID:
                id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(where))
                    where = TypeDBAdapter.KEY_ID + " = " + id;
                else
                    where = where + " AND " + TypeDBAdapter.KEY_ID + " = " + id;
                delCount = db.delete(TypeDBAdapter.TABLE_TYPE,where,whereArg);
            break;*/
            default:
                throw new IllegalArgumentException("Wrong URI: " + uri);
        }
        db.execSQL("VACUUM");
        if (delCount > 0)
            getContext().getContentResolver().notifyChange(uri, null);
        return delCount;
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String where, String[] whereArg) {
        int updateCount;
        String id;
        switch (uriMatcher.match(uri)) {
            case ERROR_LIST: // общий Uri
                updateCount = db.update(ErrorDBAdapter.TABLE_ERROR, contentValues, where, whereArg);
                break;
            case ERROR_ID:
                id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(where))
                    where = ErrorDBAdapter.KEY_ID + " = " + id;
                else
                    where = where + " AND " + ErrorDBAdapter.KEY_ID + " = " + id;
                updateCount = db.update(ErrorDBAdapter.TABLE_ERROR, contentValues, where, whereArg);
                break;
            /*case VENDOR_LIST: // общий Uri
                updateCount = db.update(VendorDBAdapter.TABLE_VENDOR,contentValues,where,whereArg);
                break;
            case VENDOR_ID:
                id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(where))
                    where = VendorDBAdapter.KEY_ID + " = " + id;
                else
                    where = where + " AND " + VendorDBAdapter.KEY_ID + " = " + id;
                updateCount = db.update(VendorDBAdapter.TABLE_VENDOR,contentValues,where,whereArg);
                break;
            case PREFERENCES_LIST: // общий Uri
                updateCount = db.update(PreferencesDBAdapter.TABLE_PREFERENCES,contentValues,where,whereArg);
                break;
            case PREFERENCES_ID:
                id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(where))
                    where = PreferencesDBAdapter.KEY_ID + " = " + id;
                else
                    where = where + " AND " + PreferencesDBAdapter.KEY_ID + " = " + id;
                updateCount = db.update(PreferencesDBAdapter.TABLE_PREFERENCES,contentValues,where,whereArg);
                break;
            case TYPE_LIST: // общий Uri
                updateCount = db.update(TypeDBAdapter.TABLE_TYPE,contentValues,where,whereArg);
                break;
            case TYPE_ID: // Uri с ID
                id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(where))
                    where = TypeDBAdapter.KEY_ID + " = " + id;
                else
                    where = where + " AND " + TypeDBAdapter.KEY_ID + " = " + id;
                updateCount = db.update(TypeDBAdapter.TABLE_TYPE,contentValues,where,whereArg);
                break;*/
            default:
                throw new IllegalArgumentException("Wrong URI: " + uri);
        }
        if (updateCount > 0)
            getContext().getContentResolver().notifyChange(uri, null);
        return updateCount;
    }

    private class DBHelper extends SQLiteOpenHelper {

        public DBHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        public void onCreate(SQLiteDatabase db) {
            db.execSQL(ErrorDBAdapter.TABLE_CREATE_ERROR);
            /*db.execSQL(TypeDBAdapter.TABLE_CREATE_TYPE);
            db.execSQL(VendorDBAdapter.TABLE_CREATE_VENDOR);
            db.execSQL(PreferencesDBAdapter.TABLE_CREATE_PREFERENCES);*/

            //Add default record to mytable
            /*ContentValues contentValues = new ContentValues();
            Resources res = getContext().getResources();
            String[] type_records = res.getStringArray(R.array.type_array);
            for (String type_record : type_records) {
                contentValues.put(TypeDBAdapter.KEY_TYPE, type_record);
                db.insert(TypeDBAdapter.TABLE_TYPE, null, contentValues);
            }*/
        }

        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            /*if (!db.isReadOnly()) {
            // Enable foreign key constraints
                db.execSQL("PRAGMA foreign_keys=ON;");
            }*/
            //db.execSQL("alter table inpuCheckTable rename to inputCheckTable");
            db.execSQL("DROP TABLE IF EXISTS " + ErrorDBAdapter.TABLE_ERROR);
            /*db.execSQL("DROP TABLE IF EXISTS " + TypeDBAdapter.TABLE_TYPE);
            db.execSQL("DROP TABLE IF EXISTS " + VendorDBAdapter.TABLE_VENDOR);
            db.execSQL("DROP TABLE IF EXISTS " + PreferencesDBAdapter.TABLE_PREFERENCES);*/
            onCreate(db);
        }

        public SQLiteDatabase open() throws SQLiteException {
            try {
                db = this.getWritableDatabase();
            } catch (SQLiteException ex) {
                db = this.getReadableDatabase();
            }
            return db;
        }

    }

}

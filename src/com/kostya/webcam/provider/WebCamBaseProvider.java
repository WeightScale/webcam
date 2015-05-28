package com.kostya.webcam.provider;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import com.kostya.webcam.provider.ErrorDBAdapter;

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

    private static final UriMatcher uriMatcher;

    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(AUTHORITY, ErrorDBAdapter.TABLE_ERROR_PATH, ERROR_LIST);
        uriMatcher.addURI(AUTHORITY, ErrorDBAdapter.TABLE_ERROR_PATH + "/#", ERROR_ID);
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
        }

        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + ErrorDBAdapter.TABLE_ERROR);
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

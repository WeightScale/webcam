package com.kostya.webcam;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;

import java.util.Iterator;

/**
 * Created by Kostya on 10.07.14.
 */
public class SettingsAsyncTask extends AsyncTask<String, Void, Void> {
    private static boolean flag_settings = false;
    private final Context context;

    SettingsAsyncTask(Context c) {
        context = c;
    }

    public boolean executeSetting(String reader) {
        String[] parts = reader.split(" ", 0);
        SimpleCommandLineParser settings = new SimpleCommandLineParser(parts, "=");
        if (settings.containsKey("period_take")) {

        }
        if (settings.containsKey("pic_size")) {

        }
        if (settings.containsKey("exposure")) {

        }
        return true;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        flag_settings = true;
    }

    @Override
    protected synchronized Void doInBackground(String... strings) {
        if (strings.length < 2)
            return null;
        String[] parts = strings[1].split(" ", 0);
        SimpleCommandLineParser settings = new SimpleCommandLineParser(parts, "=");
        SharedPreferences sharedPreferences = context.getSharedPreferences(strings[0], Context.MODE_PRIVATE);
        Preferences preferences = new Preferences(sharedPreferences);

        Iterator<String> iterator = settings.getKeyIterator();
        while (iterator.hasNext()) {
            String key = iterator.next();
            preferences.write(key, settings.getValue(key));
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        //new WebCamService().load_parameters();
        flag_settings = false;
    }

    @Override
    protected void onProgressUpdate(Void... values) {
        super.onProgressUpdate(values);
    }
}

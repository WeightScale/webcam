//Простой класс настроек
package com.kostya.webcam;

import android.content.SharedPreferences;
import android.content.res.Resources;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

class Preferences {
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    //static final String PREF_SETTINGS = Resources.getSystem().getString(R.string.pref_settings);


    //static boolean admin=false; //возможности админа

    Preferences(SharedPreferences sp) {
        sharedPreferences = sp;
        editor = sp.edit();
    }

    void load(SharedPreferences sp) {
        sharedPreferences = sp;
        editor = sp.edit();
    }

    void write(String key, String value) {
        if (key.isEmpty() || value.isEmpty())
            return;
        editor.putString(key, value);
        editor.commit();
    }

    void write(String key, int value) {
        editor.putInt(key, value);
        editor.commit();
    }

    void write(String key, boolean value) {
        editor.putBoolean(key, value);
        editor.commit();
    }

    String read(String key, String def) {
        return sharedPreferences.getString(key, def);
    }

    boolean read(String key, boolean def) {
        return sharedPreferences.getBoolean(key, def);
    }

    int read(String key, int in) {
        return sharedPreferences.getInt(key, in);
    }

    boolean contains(String key) {
        return sharedPreferences.contains(key);
    }

    Iterator<String> getKeyIterator() {
        Map<String, ?> map = sharedPreferences.getAll();
        Set<String> keySet = map.keySet();
        if (!keySet.isEmpty())
            return keySet.iterator();
        return null;
    }

    void remove(String key) {
        editor.remove(key);
        editor.commit();
    }

    SharedPreferences getSharedPreferences() {
        return sharedPreferences;
    }
}
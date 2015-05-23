package com.kostya.webcam;

import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import com.google.android.gms.drive.DriveId;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.services.drive.DriveScopes;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by Kostya on 18.12.2014.
 */
public class ActivityGoogleDrivePreference extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    Preferences preferences;
    static final int REQUEST_ACCOUNT_PICKER = 1;
    static final int REQUEST_ACTIVITY_AUTH = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.goole_drive);
        HashMap<String, String> hashMap = (HashMap<String, String>) getIntent().getSerializableExtra("map");

        try {
            if (getIntent().getAction().equalsIgnoreCase("UserRecoverableAuthIOException")) {
                try {
                    startActivityForResult((Intent) getIntent().getExtras().get("request_authorization"), REQUEST_ACTIVITY_AUTH);
                } catch (Exception e) {
                }
            }
        } catch (Exception e) {
        }

        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
        preferences = new Preferences(getSharedPreferences(getResources().getString(R.string.pref_settings), Context.MODE_PRIVATE));
        Preference name = findPreference(getString(R.string.key_account_name));

        if (name != null) {
            try {
                name.setSummary(hashMap.get("502"));
            } catch (Exception e) {
                name.setSummary(preferences.read(getString(R.string.key_account_name), ""));
            }
            name.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    //*Установить Сервисы Google play*//*
                    GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(ActivityGoogleDrivePreference.this, Collections.singleton(DriveScopes.DRIVE));
                    startActivityForResult(credential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);
                    return false;
                }
            });
        }

        name = findPreference(getString(R.string.key_folder_id));
        if (name != null) {
            try {
                name.setSummary(hashMap.get("511"));
            } catch (Exception e) {
                name.setSummary("folder id: " + preferences.read(getString(R.string.key_folder_id), "NULL"));
            }
            name.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    preference.setSummary("folder id: " + o.toString());
                    preferences.write(preference.getKey(), o.toString());
                    startService(new Intent(ActivityGoogleDrivePreference.this, SendDataService.class));
                    return false;
                }
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_ACCOUNT_PICKER:
                if (data != null && data.getExtras() != null) {
                    String accountName = data.getExtras().getString(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        preferences.write(getString(R.string.key_account_name), accountName);
                        findPreference(getString(R.string.key_account_name)).setSummary(preferences.read(getString(R.string.key_account_name), ""));
                    }
                }
                break;
            case REQUEST_ACTIVITY_AUTH:
                if (resultCode == RESULT_OK) {
                    startService(new Intent(this, SendDataService.class));
                    onBackPressed();
                }
                break;
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}

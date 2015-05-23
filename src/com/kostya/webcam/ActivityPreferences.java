//Активность настроек
package com.kostya.webcam;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.os.Bundle;
import android.preference.*;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.services.drive.DriveScopes;

import java.util.*;

public class ActivityPreferences extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
    Preferences preferences;


    private boolean flag_change = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
        preferences = new Preferences(getSharedPreferences(getResources().getString(R.string.pref_settings), Context.MODE_PRIVATE));
        Preference name = findPreference(getString(R.string.key_settings_google_drive));

        if (name != null) {
            //name.setSummary("Folder id: "+preferences.read(getString(R.string.key_folder_id),""));
            name.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(final Preference preference) {
                    final AlertDialog.Builder dialog = new AlertDialog.Builder(ActivityPreferences.this);// R.style.AlertDialogCustom
                    dialog.setTitle("Внимание");
                    dialog.setMessage("Вы хотите изменить настройки доступа к google drive? Это может привести к неправильной работе");
                    dialog.setCancelable(false);
                    dialog.setPositiveButton("Да", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            startActivity(new Intent().setClass(getApplicationContext(), ActivityGoogleDrivePreference.class));
                            return;
                        }
                    });
                    dialog.setNegativeButton("Нет", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            return;
                        }
                    });
                    dialog.show();
                    return true;
                }
            });
            /*name.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {

                    return false;
                }
            });*/
        }

        listPreference(getString(R.string.key_color_effect), Main.parameters.getSupportedColorEffects());

        listPreference(getString(R.string.key_anti_banding), Main.parameters.getSupportedAntibanding());

        listPreference(getString(R.string.key_flash_mode), Main.parameters.getSupportedFlashModes());

        listPreference(getString(R.string.key_focus_mode), Main.parameters.getSupportedFocusModes());

        listPreference(getString(R.string.key_scene_mode), Main.parameters.getSupportedSceneModes());

        listPreference(getString(R.string.key_white_mode), Main.parameters.getSupportedWhiteBalance());

        ListPreference listPreferenceExposure = (ListPreference) findPreference(getString(R.string.key_exposure));
        listPreferenceExposure.setSummary(listPreferenceExposure.getValue());
        int max = Main.parameters.getMaxExposureCompensation();
        int min = Main.parameters.getMinExposureCompensation();
        int step = (int) Main.parameters.getExposureCompensationStep();
        List<String> exposure = new ArrayList<String>();
        for (; max >= min; max -= step) {
            exposure.add(String.valueOf(max));
        }
        if (exposure != null) {
            CharSequence[] entries = new CharSequence[0];
            entries = exposure.toArray(entries);
            listPreferenceExposure.setEntries(entries);
            listPreferenceExposure.setEntryValues(entries);
            listPreferenceExposure.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    preferences.write(getString(R.string.key_exposure), o.toString());
                    preference.setSummary(o.toString());
                    Main.parameters.setExposureCompensation(Integer.parseInt(o.toString()));
                    return true;
                }
            });
            listPreferenceExposure.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    return false;
                }

            });
        } else {
            findPreference(getString(R.string.key_exposure)).setEnabled(false);
            listPreferenceExposure.setSummary("Неподдерживает");
        }

        ListPreference listPreferenceSize = (ListPreference) findPreference(getString(R.string.key_pic_size));
        listPreferenceSize.setSummary(listPreferenceSize.getValue());
        List<Camera.Size> pictureSizes = Main.parameters.getSupportedPictureSizes();
        if (pictureSizes != null) {
            CharSequence[] entries = new CharSequence[0];
            List<String> sizeList = new ArrayList<String>();
            Iterator<Camera.Size> sizeIterator = pictureSizes.iterator();
            while (sizeIterator.hasNext()) {
                Camera.Size size = sizeIterator.next();
                int w = size.width;
                int h = size.height;
                sizeList.add(String.valueOf(w) + "x" + String.valueOf(h));
            }
            entries = sizeList.toArray(entries);
            listPreferenceSize.setEntries(entries);
            listPreferenceSize.setEntryValues(entries);
            listPreferenceSize.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    preferences.write(getString(R.string.key_pic_size), o.toString());
                    String[] str = o.toString().split("x");
                    preferences.write(getString(R.string.key_pic_size_width), str[0]);
                    preferences.write(getString(R.string.key_pic_size_height), str[1]);
                    findPreference(getString(R.string.key_pic_size_width)).setSummary(str[0]);
                    findPreference(getString(R.string.key_pic_size_height)).setSummary(str[1]);
                    preference.setSummary(o.toString());
                    Main.parameters.setPictureSize(Integer.parseInt(str[0]), Integer.parseInt(str[1]));
                    return true;
                }
            });
            listPreferenceSize.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    return false;
                }

            });
        } else {
            findPreference(getString(R.string.key_pic_size)).setEnabled(false);
            listPreferenceSize.setSummary("Неподдерживает");
        }

        findPreference(getString(R.string.key_pic_size_height)).setSummary(preferences.read(getString(R.string.key_pic_size_height), " "));
        findPreference(getString(R.string.key_pic_size_width)).setSummary(preferences.read(getString(R.string.key_pic_size_width), " "));

        ListPreference preferenceRotation = (ListPreference) findPreference(getString(R.string.key_rotation));
        preferenceRotation.setSummary(preferences.read(getString(R.string.key_rotation), "0"));
        preferenceRotation.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                int rotation = Integer.parseInt(o.toString());
                if (rotation >= 0 && rotation <= 270) {
                    preferences.write(getString(R.string.key_rotation), o.toString());
                    preference.setSummary(o.toString());
                    Main.parameters.setRotation(rotation);
                    return true;
                }
                return false;
            }
        });
        preferenceRotation.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                return false;
            }
        });

        name = findPreference(getString(R.string.key_period_take));
        name.setSummary("Время периода сьемки: " + preferences.read(getString(R.string.key_period_take), ""));
        name.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                int time = Integer.parseInt(o.toString());
                if (time > 0 && time < 600) {
                    preference.setSummary("Время периода сьемки: " + o.toString());
                    preferences.write(getString(R.string.key_period_take), o.toString());
                    return true;
                }
                return false;
            }
        });

        name = findPreference(getString(R.string.key_quality_pic));
        name.setSummary("Качество фото: " + preferences.read(getString(R.string.key_quality_pic), ""));
        name.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                int time = Integer.parseInt(o.toString());
                if (time > 0 && time <= 100) {
                    preference.setSummary("Качество фото: " + o.toString());
                    preferences.write(getString(R.string.key_quality_pic), o.toString());
                    return true;
                }
                return false;
            }
        });
    }

    private void listPreference(final String key, List<String> parameters) {
        ListPreference listPreference = (ListPreference) findPreference(key);
        listPreference.setSummary(listPreference.getValue());
        //List<String> colorEffects = WebCamService.parameters.getSupportedColorEffects();
        if (parameters != null) {
            CharSequence[] entries = new CharSequence[0];
            entries = parameters.toArray(entries);
            listPreference.setEntries(entries);
            listPreference.setEntryValues(entries);
            listPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    preferences.write(key, o.toString());
                    preference.setSummary(o.toString());
                    if (key.equals(getString(R.string.key_color_effect)))
                        Main.parameters.setColorEffect(o.toString());
                    else if (key.equals(getString(R.string.key_anti_banding)))
                        Main.parameters.setAntibanding(o.toString());
                    else if (key.equals(getString(R.string.key_flash_mode)))
                        Main.parameters.setFlashMode(o.toString());
                    else if (key.equals(getString(R.string.key_focus_mode)))
                        Main.parameters.setFocusMode(o.toString());
                    else if (key.equals(getString(R.string.key_scene_mode)))
                        Main.parameters.setSceneMode(o.toString());
                    else if (key.equals(getString(R.string.key_white_mode)))
                        Main.parameters.setWhiteBalance(o.toString());
                    return true;
                }
            });
            listPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    return false;
                }

            });
        } else {
            findPreference(key).setEnabled(false);
            listPreference.setSummary("Неподдерживает");
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        flag_change = true;
    }
}

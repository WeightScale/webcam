package com.kostya.webcam;

import android.content.Context;
import android.preference.ListPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

import java.util.List;

/**
 * Created by Kostya on 13.07.14.
 */
public class DynamicPreference extends ListPreference {
    public DynamicPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DynamicPreference(Context context) {
        super(context);
    }

    @Override
    protected View onCreateDialogView() {
        ListView view = new ListView(getContext());
        view.setAdapter(adapter());
        setEntries(entries());
        setEntryValues(entryValues());
        //setValueIndex(initializeIndex());
        return view;
    }

    private ListAdapter adapter() {
        return new ArrayAdapter(getContext(), android.R.layout.select_dialog_singlechoice);
    }

    private CharSequence[] entries() {
        CharSequence[] entries = new CharSequence[0];
        List<String> colorEffects = WebCamService.parameters.getSupportedColorEffects();
        entries = colorEffects.toArray(entries);
        return entries;
    }

    private CharSequence[] entryValues() {
        CharSequence[] entries = new CharSequence[0];
        List<String> colorEffects = WebCamService.parameters.getSupportedColorEffects();
        entries = colorEffects.toArray(entries);
        return entries;
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        setSummary(getEntry());
    }
}

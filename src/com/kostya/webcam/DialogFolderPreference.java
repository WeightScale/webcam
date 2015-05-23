package com.kostya.webcam;

import android.app.AlertDialog;
import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by Kostya on 17.12.2014.
 */
public class DialogFolderPreference extends DialogPreference {

    public DialogFolderPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        /*builder.setTitle(R.string.pin_changepin_title);
        builder.setPositiveButton(null, null);
        builder.setNegativeButton(null, null);*/
        super.onPrepareDialogBuilder(builder);
    }

    @Override
    protected View onCreateDialogView() {
        return super.onCreateDialogView();
    }
}

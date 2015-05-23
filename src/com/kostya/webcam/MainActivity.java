package com.kostya.webcam;

import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

/**
 * Created by Kostya on 12.06.14.
 */
public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.main);
        //startService(new Intent(this, WebCamService.class));
        //startService(new Intent(this, TakeService.class));
        startService(new Intent(this, SendDataService.class));
        startService(new Intent(this, SMSIncomingService.class));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case 1:
                if (data != null && data.getExtras() != null) {
                    String accountName = data.getExtras().getString(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        new Preferences(getSharedPreferences(getString(R.string.pref_settings), Context.MODE_PRIVATE)).write(getString(R.string.key_account_name), accountName);
                    }
                }
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.preferences:
                startActivity(new Intent(this, ActivityPreferences.class));
                break;
            case R.id.exit:
                closedService();
                finish();
                //System.exit(0);
                break;
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        return;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }

    void closedService() {

        stopService(new Intent(this, SMSIncomingService.class));
        stopService(new Intent(this, SendDataService.class));
        stopService(new Intent(this, TakeService.class));
        stopService(new Intent(this, BluetoothServer.class));

    }
}

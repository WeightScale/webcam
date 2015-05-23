package com.kostya.webcam;

import android.accounts.*;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.services.drive.Drive;

import java.io.IOException;

/**
 * Created by Kostya on 30.06.14.
 */
public abstract class ConnectDiskAsyncTask extends AsyncTask<Void, Long, Void> {
    private boolean closed = true;
    final Context context;
    final Internet internet;
    Drive drive;

    ConnectDiskAsyncTask(Context c, Internet i, Drive d) {
        context = c;
        internet = i;
        drive = d;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        closed = false;
    }

    @Override
    protected Void doInBackground(Void... params) {
        if (drive == null) {
            AccountManager am = (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);

            Account[] accounts = am.getAccountsByType("com.google");
            Account account = null;
            for (Account account1 : accounts) {
                if (account1.name.equalsIgnoreCase("kreogen.lg@gmail.com")) {
                    account = account1;
                    break;
                }
            }

            context.sendBroadcast(new Intent(Internet.INTERNET_CONNECT));
            int count = 0, time_wait = 0;
            while (!isCancelled()) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                }
                if (!internet.checkInternetConnection()) {
                    continue;
                }

                Bundle authTokenBundle = null;
                try {
                    //AccountManagerFuture<Bundle> accFut = AccountManager.get(getBaseContext()).getAuthToken(account,"oauth2:" + "https://www.googleapis.com/auth/drive",null,this,null,null);
                    AccountManager accountManager = AccountManager.get(context.getApplicationContext());
                    AccountManagerFuture<Bundle> accountManagerFuture = accountManager.getAuthToken(account, "oauth2:" + "https://www.googleapis.com/auth/drive", null, null, null, null);
                    authTokenBundle = accountManagerFuture.getResult();
                    final String token = authTokenBundle.get(AccountManager.KEY_AUTHTOKEN).toString();
                    am.invalidateAuthToken("com.google", token);
                    GoogleCredential credential = new GoogleCredential.Builder()
                            .setTransport(new NetHttpTransport())
                            .setJsonFactory(new JacksonFactory())
                            .setClientSecrets(WebCamService.CLIENT_ID, WebCamService.CLIENT_SECRET)
                            .build().setAccessToken(token);
                    drive = new Drive.Builder(new NetHttpTransport(), new JacksonFactory(), credential).build();
                    break;
                } catch (OperationCanceledException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (AuthenticatorException e) {
                    e.printStackTrace();
                } catch (NullPointerException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }
        if (!isCancelled()) {
            //threadSendToDisk.execute();todo
        }
        closed = true;
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        closed = true;
    }
}

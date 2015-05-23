package com.kostya.webcam;

import android.accounts.*;
import android.app.*;
import android.content.*;
import android.os.*;
import com.google.android.gms.auth.*;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.FileContent;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.services.drive.*;
import com.google.api.services.drive.model.*;

import java.io.*;
import java.io.File;
import java.util.*;

/**
 * Created by Kostya on 25.07.14.
 */
public class SendDataService extends Service {
    private final ThreadConnectDisk threadConnectDisk = new ThreadConnectDisk(this);
    private final ThreadSendToDisk threadSendToDisk = new ThreadSendToDisk();
    private Preferences preferences;
    private Internet internet;
    private Drive drive = null;
    private static BroadcastReceiver broadcastReceiver;
    UtilityDriver utilityDriver;

    static boolean flag_drive_build = false;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        preferences = new Preferences(getSharedPreferences(getString(R.string.pref_settings), Context.MODE_PRIVATE));
        internet = new Internet(this);

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) { //контроль состояний сетей
                String action = intent.getAction();
                if (action == null)
                    return;
                if (action.equals(Internet.INTERNET_CONNECT)) {
                    internet.connect();
                } else if (action.equals(Internet.INTERNET_DISCONNECT)) {
                    internet.disconnect();
                }
            }
        };
        IntentFilter filter = new IntentFilter(Internet.INTERNET_CONNECT);
        filter.addAction(Internet.INTERNET_DISCONNECT);
        this.registerReceiver(broadcastReceiver, filter);

        threadConnectDisk.execute();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        internet.disconnect();
        unregisterReceiver(broadcastReceiver);
        threadConnectDisk.cancel(true);
        threadSendToDisk.cancel(true);
        while (!threadConnectDisk.closed) ;
        while (!threadSendToDisk.closed) ;
    }

    private boolean saveFileToDrive(final File fileContent, String parentId) throws Exception {

        try {

            if (!fileContent.exists())
                return false;
            FileContent mediaContent = new FileContent("image/jpeg", fileContent);
            if (mediaContent.getLength() == 0) {  //не содержит контент
                if (!fileContent.delete()) {
                    new ErrorDBAdapter(this).insertNewEntry("512", "Невозможно удалить медиоконтент " + fileContent.getPath());
                }
                return false;
            }
            // File's metadata.
            /*com.google.api.services.drive.model.File body = new com.google.api.services.drive.model.File();
            body.setTitle(fileContent.getName());
            body.setMimeType("image/jpeg");
            body.setParents(Arrays.asList(new ParentReference().setId(preferences.read(getString(R.string.key_folder_id), "NULL"))));

            com.google.api.services.drive.model.File file = drive.files().insert(body, mediaContent).execute();*/
            /*com.google.api.services.drive.model.File file = utilityDriver.uploadFile(fileContent.getName()
                    ,preferences.read(getString(R.string.key_folder_id), null)
                    ,"image/jpeg",fileContent);*/
            com.google.api.services.drive.model.File file = utilityDriver.uploadFile(fileContent.getName(), parentId, "image/jpeg", fileContent);
            if (file != null) {
                if (!fileContent.delete()) {
                    new ErrorDBAdapter(this).insertNewEntry("513", "Невозможно удалить медиоконтент " + fileContent.getPath());
                }
                return true;
            }
        } catch (UserRecoverableAuthIOException e) {
            Intent intent = new Intent(getBaseContext(), ActivityGoogleDrivePreference.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setAction("UserRecoverableAuthIOException");
            intent.putExtra("request_authorization", e.getIntent());
            startActivity(intent);
            new ErrorDBAdapter(this).insertNewEntry("510", e.getMessage());
            throw new Exception("510");
        } catch (IOException e) {
            new ErrorDBAdapter(this).insertNewEntry("511", "Возможно нужно установить Сервисы Google Play" + e.getMessage());
            //startActivityParameter("511", "Введите идентификатор папки google driver");
            throw new Exception("511");
        }
        return false;
    }

    public class ThreadConnectDisk extends AsyncTask<Void, Long, Boolean> {
        private boolean closed = true;

        final Context context;

        ThreadConnectDisk(Context c) {
            context = c;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            closed = false;
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            if (drive == null) {
                AccountManager accountManager = (AccountManager) getSystemService(ACCOUNT_SERVICE);

                Account[] accounts = accountManager.getAccountsByType("com.google");
                if (accounts.length == 0) {
                    new ErrorDBAdapter(context).insertNewEntry("501", "Нет accounts of type com.google");
                    startActivityParameter("502", "Добавить account в google.com");
                    return false;
                    //todo проверить accounts на null
                }

                Account account = null;
                for (Account acc : accounts) {
                    if (acc.name.equalsIgnoreCase(preferences.read(getString(R.string.key_account_name), ""))) {
                        account = acc;
                        break;
                    }
                }

                if (account == null) {
                    new ErrorDBAdapter(context).insertNewEntry("502", "Невыбран account в пункте настроек <account name>");
                    startActivityParameter("502", "ВЫБЕРИТЕ ACCOUNT NAME!!!");
                    return false;
                }
                while (!isCancelled()) {

                    sendBroadcast(new Intent(Internet.INTERNET_CONNECT));

                    int count = 0, time_wait = 0;
                    while (!isCancelled()) {
                        try {
                            Thread.sleep(200);
                        } catch (InterruptedException e) {
                        }
                        if (!Internet.flagIsInternet) {
                            if (time_wait++ > 150) {
                                new ErrorDBAdapter(context).insertNewEntry("508", "Time out соединения с интернетоом" + String.valueOf(time_wait * 200) + " мсек");
                                sendBroadcast(new Intent(Internet.INTERNET_DISCONNECT));
                                break;
                            }
                            continue;
                        }
                        time_wait = 0;
                        if (count++ > 3) {                    //Колличество больше прекращяем попытки передачи
                            new ErrorDBAdapter(context).insertNewEntry("509", String.valueOf(count) + " Превышено количество попыток соединения с google drive");
                            return false;
                        }

                        try {
                            utilityDriver = new UtilityDriver(getApplicationContext(), account.name);

                            /*final String token = GoogleAuthUtil.getToken(getApplicationContext(), account.name, "oauth2:" + DriveScopes.DRIVE);

                            GoogleAuthUtil.invalidateToken(getApplicationContext(), token);
                            GoogleCredential credential = new GoogleCredential.Builder().build().setAccessToken(token);
                            drive = new Drive.Builder(AndroidHttp.newCompatibleTransport(), new JacksonFactory(), credential).build();*/

                            /*List<com.google.api.services.drive.model.File> result = new ArrayList<com.google.api.services.drive.model.File>();
                            com.google.api.services.drive.Drive.Files.List listRequest = drive.files().list();
                            //listRequest.setMaxResults(10);
                            listRequest.setQ( "mimeType = 'application/vnd.google-apps.folder' and title = 'WEB' and trashed = false");
                            do {
                                FileList fList = listRequest.execute();
                                result.addAll(fList.getItems());
                                listRequest.setPageToken(fList.getNextPageToken());

                            } while (listRequest.getPageToken() != null && listRequest.getPageToken().length() > 0);

                            com.google.api.services.drive.model.File body = new com.google.api.services.drive.model.File();
                            body.setTitle("WEB");
                            body.setMimeType("application/vnd.google-apps.folder");

                            *//*if (parentId != null && parentId.length() > 0) {

                                ParentReference parent = new ParentReference();
                                parent.setId(parentId);
                                body.setParents(Arrays.asList(parent));
                            }*//*

                            com.google.api.services.drive.model.File file = drive.files().insert(body).execute();*/

                            /*if(drive!=null)*/
                            return true;
                        } catch (NullPointerException e) {
                            new ErrorDBAdapter(context).insertNewEntry("506", "Возможно нужно установить Сервисы Google Play" + e.getMessage());
                        } /*catch (UserRecoverableAuthIOException e){
                            new ErrorDBAdapter(context).insertNewEntry("507", e.getMessage());
                        } catch (IOException e) {
                            new ErrorDBAdapter(context).insertNewEntry("507", e.getMessage());
                        }*/ /*catch (GooglePlayServicesAvailabilityException e) {
                            new ErrorDBAdapter(context).insertNewEntry("505", e.getMessage());
                            //Dialog alert = GooglePlayServicesUtil.getErrorDialog(playEx.getConnectionStatusCode(),getApplicationContext(),ACTIVITY_AUTH_REQUEST_CODE);
                        } catch (UserRecoverableAuthException e) {
                            Intent intent = new Intent(getBaseContext(),ActivityGoogleDrivePreference.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent.putExtra("request_authorization", e.getIntent());
                            startActivity(intent);
                            new ErrorDBAdapter(context).insertNewEntry("507", e.getMessage());
                            return false;
                        } catch (GoogleAuthException e) {
                            e.printStackTrace();
                        }*/
                    }

                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                    }
                }
            }
            if (isCancelled())
                return false;
            closed = true;
            return true;
        }

        @Override
        protected void onPostExecute(Boolean aVoid) {
            super.onPostExecute(aVoid);
            if (aVoid) {
                flag_drive_build = true;
                threadSendToDisk.execute();
                startService(new Intent(getApplicationContext(), TakeService.class).setAction("start"));
            } else {
                sendBroadcast(new Intent(Internet.INTERNET_DISCONNECT));
                flag_drive_build = false;
            }
            closed = true;
        }
    }

    public class ThreadSendToDisk extends AsyncTask<Void, Long, Void> {
        private boolean closed = true;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            closed = false;
        }

       /* @Override
        protected Void doInBackground(Void... params) {
            while(!isCancelled()){
                try {Thread.sleep(20);} catch(InterruptedException e) {}
                File[] files = Main.path.listFiles();
                if(files == null)
                    continue;
                if(files.length > 0){
                    long difference = System.currentTimeMillis();
                    sendBroadcast(new Intent(Internet.INTERNET_CONNECT));
                    while(!isCancelled()) {

                        try {Thread.sleep(20);} catch(InterruptedException e) {}
                        if(!Internet.flagIsInternet)
                            continue;
                        for (File file : files) {

                            try {
                                saveFileToDrive(file);//todo
                            } catch (Exception e) {
                                //this.cancel(false);
                                stopSelf();
                                break;
                            }
                            setErrorTimeTake(System.currentTimeMillis() - difference);
                            if(isCancelled())
                                break;
                        }
                        break;
                    }
                    //difference = System.currentTimeMillis() - difference;
                    //setErrorTimeTake(difference);
                }
            }
            closed = true;
            return null;
        }*/

        @Override
        protected Void doInBackground(Void... params) {
            while (!isCancelled()) {
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                }

                long difference = System.currentTimeMillis();
                sendBroadcast(new Intent(Internet.INTERNET_CONNECT));
                while (!isCancelled()) {

                    try {
                        Thread.sleep(20);
                    } catch (InterruptedException e) {
                    }
                    if (!Internet.flagIsInternet)
                        continue;
                    String parentId = null;
                    try {
                        com.google.api.services.drive.model.File folderRoot = utilityDriver.getFolder(Main.LOCATE_FOLDER_PATH, null);
                        parentId = folderRoot.getId();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    try {
                        scanDirectory(Main.path, parentId);//todo
                    } catch (Exception e) {
                        //this.cancel(false);
                        stopSelf();
                        break;
                    }
                    setErrorTimeTake(System.currentTimeMillis() - difference);
                    if (isCancelled())
                        break;

                    break;
                }
            }
            closed = true;
            return null;
        }

        void setErrorTimeTake(long errorTimeTake) {
            long time = errorTimeTake - (Long.parseLong(preferences.read(getString(R.string.key_period_take), "10")) * 1000);
            if (time > 0)
                Main.error_time_take = time;
            else
                Main.error_time_take = 0;
        }
    }

    void startActivityParameter(String code, String massage) {
        Intent intent = new Intent(getBaseContext(), ActivityGoogleDrivePreference.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        HashMap<String, String> hashMap = new HashMap<String, String>();
        hashMap.put(code, massage);
        intent.putExtra("map", hashMap);
        startActivity(intent);
    }

    private void scanDirectory(File rootDirectory, String parentId) {

        File[] filesInDirectory = rootDirectory.listFiles();
        if (filesInDirectory == null)
            return;
        try {
            for (File file : filesInDirectory) {

                if (file.isDirectory()) {
                    com.google.api.services.drive.model.File folderParent = utilityDriver.getFolder(file.getName(), parentId);
                    String id = folderParent.getId();
                    scanDirectory(file, id);
                } else {
                    saveFileToDrive(file, parentId);
                }
            }
            if (rootDirectory.listFiles().length == 0)
                rootDirectory.delete();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*void test(){
        Stack <File> stack = new Stack <File> ();
        String[] sa = new File(rootDir).list(
                new FilenameFilter(){
                    public boolean accept(File d, String n){
                        return new File(d, n).isDirectory();
                    }
                });
        for(String s : sa)
            stack.push(new File(rootDir, s));
        while(!stack.isEmpty()){
            for(String s : stack.pop().list()){
                File f = new File(dir, s);
                if(f.isDirectory())
                    stack.push(f);
                else if(s.endsWith(".xsl"))
                    System.out.println("Found " + f.getAbsolutePath());
            }
        }
    }*/

}
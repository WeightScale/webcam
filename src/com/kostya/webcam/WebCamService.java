package com.kostya.webcam;

import android.accounts.*;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.media.MediaScannerConnection;
//import android.media.audiofx.EnvironmentalReverb;
import android.net.Uri;
import android.os.*;
//import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.SurfaceView;
import android.widget.Toast;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.AbstractInputStreamContent;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonGenerator;
import com.google.api.client.json.JsonParser;
import com.google.api.client.json.gson.GsonFactory;
//import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.ParentReference;

import java.io.*;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.*;

public class WebCamService extends Service {
    private final ThreadConnectDisk threadConnectDisk = new ThreadConnectDisk();
    private final ThreadTakePicture threadTakePicture = new ThreadTakePicture();
    private final ThreadSendToDisk threadSendToDisk = new ThreadSendToDisk();

    //private PowerManager.WakeLock wakeLock;
    private Camera camera = null;
    public static Camera.Parameters parameters = null;
    Preferences preferences;
    //Camera.PictureCallback jpegCallback = null;
    private Drive drive = null;
    private static BroadcastReceiver broadcastReceiver;
    private Internet internet;
    //private static Uri fileUri;
    //private File path;
    private File file;

    private boolean flag_wait_take = false;

    public static final String CLIENT_ID = "293710503425-uqkfd2itv33ii13qlielicmcm5a3u910.apps.googleusercontent.com";

    public static final String CLIENT_SECRET = "LvBsIcIQdGxyIU0x8a5hmjmz";

    private static final String DEF_FOLDER_ID = "0B3X8oRNGzxTQcVVVMmo5UmNPcms";

    private static final String LOCATE_FOLDER_PATH = "/WebPhoto/";

    private GoogleAccountCredential credential;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        //sendBroadcast(new Intent(INTERNET_CONNECT));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        //startForeground(1, generateNotification(R.drawable.camera, "WebCam", "Отправка фото"));
        //return START_NOT_STICKY;
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //notificationManager.notify(0,generateNotification(R.drawable.accept_database,"Остановка сервиса","Сервис остановлен"));
        threadConnectDisk.cancel(true);
        threadTakePicture.cancel(true);
        threadSendToDisk.cancel(true);
        while (!threadConnectDisk.closed) ;
        while (!threadTakePicture.closed) ;
        while (!threadSendToDisk.closed) ;
        internet.disconnect();
        unregisterReceiver(broadcastReceiver);
    }

    /**
     * Called when the activity is first created.
     */
    public class ThreadConnectDisk extends AsyncTask<Void, Long, Void> {
        private boolean closed = true;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            closed = false;
        }

        @Override
        protected Void doInBackground(Void... params) {
            preferences = new Preferences(getSharedPreferences(getString(R.string.pref_settings), Context.MODE_PRIVATE));
            if (drive == null) {
                AccountManager am = (AccountManager) getSystemService(ACCOUNT_SERVICE);

                Account[] accounts = am.getAccountsByType("com.google");
                if (accounts.length == 0) {
                    //todo проверить accounts на null
                }

                Account account = null;
                for (Account account1 : accounts) {
                    if (account1.name.equalsIgnoreCase(preferences.read(getString(R.string.key_account_name), "weight.check.lg@gmail.com"))) {
                        account = account1;
                        break;
                    }

                }

                if (account == null) {
                    //todo проверить account на null
                }

                sendBroadcast(new Intent(Internet.INTERNET_CONNECT));
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
                        AccountManager accountManager = AccountManager.get(getBaseContext());
                        AccountManagerFuture<Bundle> accountManagerFuture = accountManager.getAuthToken(account, "oauth2:" + "https://www.googleapis.com/auth/drive", null, null, null, null);
                        authTokenBundle = accountManagerFuture.getResult();
                        final String token = authTokenBundle.get(AccountManager.KEY_AUTHTOKEN).toString();
                        am.invalidateAuthToken("com.google", token);
                        GoogleCredential credential = new GoogleCredential.Builder()
                                .setTransport(new NetHttpTransport())
                                .setJsonFactory(new JacksonFactory())
                                .setClientSecrets(CLIENT_ID, CLIENT_SECRET)
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
            if (!isCancelled())
                threadSendToDisk.execute();
            closed = true;
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            closed = true;
        }
    }

    public class ThreadTakePicture extends AsyncTask<Void, Long, Void> {
        private boolean closed = true;
        Preferences preferences = new Preferences(getSharedPreferences(getString(R.string.pref_settings), Context.MODE_PRIVATE));

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            closed = false;
        }

        @Override
        protected Void doInBackground(Void... params) {
            //preferences = new Preferences(getSharedPreferences(Preferences.PREF_SETTINGS,Context.MODE_PRIVATE));
            while (!isCancelled()) {
                takeImage();
                try {
                    Thread.sleep(Integer.parseInt(preferences.read(getString(R.string.key_period_take), "10")) * 1000);
                } catch (InterruptedException e) {
                }
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

    public class ThreadSendToDisk extends AsyncTask<Void, Long, Void> {
        private final boolean closed = true;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... params) {
            while (!isCancelled()) {

                File[] files = Main.path.listFiles();
                if (files == null)
                    continue;
                if (files.length > 0) {
                    sendBroadcast(new Intent(Internet.INTERNET_CONNECT));
                    while (!isCancelled()) {

                        try {
                            Thread.sleep(20);
                        } catch (InterruptedException e) {
                        }
                        if (!internet.checkInternetConnection())
                            continue;

                        for (File file1 : files) {
                            saveFileToDrive(file1);//todo
                        }
                        break;
                    }
                }
            }
            return null;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //setContentView(R.layout.main);
        //PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        //wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "My Tag");
        //wakeLock.acquire();

        internet = new Internet(this);
        preferences = new Preferences(getSharedPreferences(getString(R.string.pref_settings), Context.MODE_PRIVATE));

        int temp = Integer.parseInt(preferences.read(getString(R.string.key_period_take), "10"));
        if (temp <= 0 || temp > 600)
            preferences.write(getString(R.string.key_period_take), String.valueOf(10));

        temp = Integer.parseInt(preferences.read(getString(R.string.key_quality_pic), "50"));
        if (temp < 10 || temp > 100)
            preferences.write(getString(R.string.key_quality_pic), "50");

        /*path = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + LOCATE_FOLDER_PATH);
        if (!path.exists())//если нет папки создаем
            if(!path.mkdirs()){
                //todo что зделать если не создали папку
            }*/

        camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);

        parameters = camera.getParameters();

        camera.release();
        load_parameters();

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

        //takeImage();
        //threadConnectDisk.execute();
        threadTakePicture.execute();
        //startForeground(1, generateNotification(R.drawable.camera, "WebCam", "Отправка фото"));
    }

    private void takeImage() {

        while (flag_wait_take) ; //Ждем если обрабатывается фото
        if (camera != null)
            camera.release();
        camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
        /*SurfaceView view = new SurfaceView(this);
        try {
            camera.setPreviewDisplay(view.getHolder());
        } catch (IOException e) {
            e.printStackTrace();
        }*/
        camera.setParameters(parameters);
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        file = new File(Main.path, timeStamp + ".jpg");

        final Camera.PictureCallback jpegCallback = new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                byte[] compressImage = compressImage(data);
                try {
                    FileOutputStream outputStream = new FileOutputStream(file.getPath());
                    outputStream.write(compressImage);
                    outputStream.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                camera.stopPreview();
                camera.release();
                //wakeLock.release();
                flag_wait_take = false;
            }
        };

        camera.startPreview();
        //camera.unlock();

        try {
            Thread.sleep(2000);
        } catch (Exception e) {
            e.printStackTrace();
        }

        flag_wait_take = true;

        try {
            camera.takePicture(null, null, null, jpegCallback);
        } catch (Exception e) {
            try {
                camera.reconnect();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            camera.stopPreview();
            camera.release();
        }
    }

    byte[] compressImage(byte[] input) {
        preferences = new Preferences(getSharedPreferences(getString(R.string.pref_settings), Context.MODE_PRIVATE));
        Bitmap original = BitmapFactory.decodeByteArray(input, 0, input.length);
        //Bitmap resized = Bitmap.createScaledBitmap(original, PHOTO_WIDTH, PHOTO_HEIGHT, true);

        ByteArrayOutputStream blob = new ByteArrayOutputStream();
        original.compress(Bitmap.CompressFormat.JPEG, Integer.parseInt(preferences.read(getString(R.string.key_quality_pic), "50")), blob);

        return blob.toByteArray();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }

    synchronized private void saveFileToDrive(final File fileContent) {

        try {

            if (!fileContent.exists())
                return;
            FileContent mediaContent = new FileContent("image/jpeg", fileContent);
            if (mediaContent.getLength() == 0) {  //не содержит контент
                if (!fileContent.delete()) {
                    //todo что зделать если не удалили фаил
                }
                return;
            }

            // File's metadata.
            com.google.api.services.drive.model.File body = new com.google.api.services.drive.model.File();
            body.setTitle(fileContent.getName());
            body.setMimeType("image/jpeg");
            body.setParents(Arrays.asList(new ParentReference().setId(preferences.read(getString(R.string.key_folder_id), DEF_FOLDER_ID))));

            com.google.api.services.drive.model.File file = drive.files().insert(body, mediaContent).execute();
            if (file != null) {
                if (!fileContent.delete()) {
                    //todo что зделать если не удалили фаил
                }
            }
        } catch (UserRecoverableAuthIOException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Notification generateNotification(int icon, String title, String message) {

        //NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = new Notification(icon, title, System.currentTimeMillis());
        Intent notificationIntent = new Intent(getApplicationContext(), WebCamService.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent intent = PendingIntent.getActivity(getBaseContext(), 0, notificationIntent, 0);
        notification.setLatestEventInfo(this, "Web Cam", message, intent);
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        //notificationManager.notify(0, notification);
        return notification;
    }

    /*public void showToast(final String toast) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), toast, Toast.LENGTH_SHORT).show();
            }
        });
    }*/
    public void load_parameters() {

        preferences = new Preferences(getSharedPreferences(getString(R.string.pref_settings), Context.MODE_PRIVATE));

        List<String> colorEffects = parameters.getSupportedColorEffects();
        if (colorEffects != null) {
            String color = preferences.read(getString(R.string.key_color_effect), parameters.getColorEffect());
            if (colorEffects.contains(color))
                parameters.setColorEffect(color);
        }

        List<String> antiBanding = parameters.getSupportedAntibanding();
        if (antiBanding != null) {
            String banding = preferences.read(getString(R.string.key_anti_banding), parameters.getAntibanding());
            if (antiBanding.contains(banding))
                parameters.setAntibanding(banding);
        }

        List<String> flashModes = parameters.getSupportedFlashModes();
        if (flashModes != null) {
            String flash = preferences.read(getString(R.string.key_flash_mode), parameters.getFlashMode());
            if (flashModes.contains(flash))
                parameters.setFlashMode(flash);
        }

        List<String> focusModes = parameters.getSupportedFocusModes();
        if (focusModes != null) {
            String focus = preferences.read(getString(R.string.key_focus_mode), parameters.getFocusMode());
            if (focusModes.contains(focus))
                parameters.setFocusMode(focus);
        }

        List<String> sceneModes = parameters.getSupportedSceneModes();
        if (sceneModes != null) {
            String scene = preferences.read(getString(R.string.key_scene_mode), parameters.getSceneMode());
            if (sceneModes.contains(scene))
                parameters.setSceneMode(scene);
        }

        List<String> whiteBalance = parameters.getSupportedWhiteBalance();
        if (whiteBalance != null) {
            String white = preferences.read(getString(R.string.key_white_mode), parameters.getWhiteBalance());
            if (sceneModes.contains(white))
                parameters.setWhiteBalance(white);
        }

        int max_exp = parameters.getMaxExposureCompensation();
        int min_exp = parameters.getMinExposureCompensation();
        int exposure = Integer.parseInt(preferences.read(getString(R.string.key_exposure), String.valueOf(parameters.getExposureCompensation())));
        if (exposure >= min_exp && exposure <= max_exp)
            parameters.setExposureCompensation(exposure);

        List<Camera.Size> pictureSizes = parameters.getSupportedPictureSizes();
        int width = Integer.parseInt(preferences.read(getString(R.string.key_pic_size_width), String.valueOf(parameters.getPictureSize().width)));
        int height = Integer.parseInt(preferences.read(getString(R.string.key_pic_size_height), String.valueOf(parameters.getPictureSize().height)));
        parameters.setPictureSize(width, height);

        int rotation = Integer.parseInt(preferences.read(getString(R.string.key_rotation), "90"));
        if (rotation >= 0 && rotation <= 270)
            parameters.setRotation(rotation);

    }

}

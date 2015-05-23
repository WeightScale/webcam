package com.kostya.webcam;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.os.*;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Timer;

/**
 * Created by Kostya on 28.07.14.
 */
public class TakeService extends Service {
    private ThreadTakePicture threadTakePicture;    //private PowerManager.WakeLock wakeLock;
    private Camera camera = null;
    Preferences preferences;
    //private static Camera.Parameters parameters = null;
    private File file;

    private boolean f_wait_take = false;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        if (intent != null)
            if (intent.getAction() != null)
                if (intent.getAction().equals("take")) {
                    if (intent.getExtras() != null) {
                        //flag_take_single = intent.getBooleanExtra(Preferences.KEY_FLAG_TAKE_SINGLE, true);
                        boolean flag = intent.getBooleanExtra(getString(R.string.key_flag_take_single), true);
                        preferences.write(getString(R.string.key_flag_take_single), flag);
                        if (threadTakePicture.closed)
                            threadTakePicture.execute();
                    }
                } else if (intent.getAction().equals("start")) {
                    if (threadTakePicture.closed)
                        threadTakePicture.execute();
                }
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        threadTakePicture = new ThreadTakePicture();
        preferences = new Preferences(getSharedPreferences(getString(R.string.pref_settings), Context.MODE_PRIVATE));
        int temp = Integer.parseInt(preferences.read(getString(R.string.key_period_take), "10"));
        if (temp <= 0 || temp > 600)
            preferences.write(getString(R.string.key_period_take), String.valueOf(10));

        temp = Integer.parseInt(preferences.read(getString(R.string.key_quality_pic), "50"));
        if (temp < 10 || temp > 100)
            preferences.write(getString(R.string.key_quality_pic), "50");

        //flag_take_single = preferences.read(Preferences.KEY_FLAG_TAKE_SINGLE,true);

        camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
        Main.parameters = camera.getParameters();

        camera.release();
        load_parameters();
        //threadTakePicture.execute();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        threadTakePicture.cancel(true);
        while (!threadTakePicture.closed) ;
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }

    void load_parameters() {

        Preferences preferences = new Preferences(getSharedPreferences(getString(R.string.pref_settings), Context.MODE_PRIVATE));

        List<String> colorEffects = Main.parameters.getSupportedColorEffects();
        if (colorEffects != null) {
            String color = preferences.read(getString(R.string.key_color_effect), Main.parameters.getColorEffect());
            if (colorEffects.contains(color))
                Main.parameters.setColorEffect(color);
        }

        List<String> antiBanding = Main.parameters.getSupportedAntibanding();
        if (antiBanding != null) {
            String banding = preferences.read(getString(R.string.key_anti_banding), Main.parameters.getAntibanding());
            if (antiBanding.contains(banding))
                Main.parameters.setAntibanding(banding);
        }

        List<String> flashModes = Main.parameters.getSupportedFlashModes();
        if (flashModes != null) {
            String flash = preferences.read(getString(R.string.key_flash_mode), Main.parameters.getFlashMode());
            if (flashModes.contains(flash))
                Main.parameters.setFlashMode(flash);
        }

        List<String> focusModes = Main.parameters.getSupportedFocusModes();
        if (focusModes != null) {
            String focus = preferences.read(getString(R.string.key_focus_mode), Main.parameters.getFocusMode());
            if (focusModes.contains(focus))
                Main.parameters.setFocusMode(focus);
        }

        List<String> sceneModes = Main.parameters.getSupportedSceneModes();
        if (sceneModes != null) {
            String scene = preferences.read(getString(R.string.key_scene_mode), Main.parameters.getSceneMode());
            if (sceneModes.contains(scene))
                Main.parameters.setSceneMode(scene);
        }

        List<String> whiteBalance = Main.parameters.getSupportedWhiteBalance();
        if (whiteBalance != null) {
            String white = preferences.read(getString(R.string.key_white_mode), Main.parameters.getWhiteBalance());
            if (whiteBalance.contains(white))
                Main.parameters.setWhiteBalance(white);
        }

        int max_exp = Main.parameters.getMaxExposureCompensation();
        int min_exp = Main.parameters.getMinExposureCompensation();
        int exposure = Integer.parseInt(preferences.read(getString(R.string.key_exposure), String.valueOf(Main.parameters.getExposureCompensation())));
        if (exposure >= min_exp && exposure <= max_exp)
            Main.parameters.setExposureCompensation(exposure);

        //List<Camera.Size> pictureSizes = parameters.getSupportedPictureSizes();
        int width = Integer.parseInt(preferences.read(getString(R.string.key_pic_size_width), String.valueOf(Main.parameters.getPictureSize().width)));
        int height = Integer.parseInt(preferences.read(getString(R.string.key_pic_size_height), String.valueOf(Main.parameters.getPictureSize().height)));
        Main.parameters.setPictureSize(width, height);

        int rotation = Integer.parseInt(preferences.read(getString(R.string.key_rotation), "90"));
        if (rotation >= 0 && rotation <= 270)
            Main.parameters.setRotation(rotation);

    }

    byte[] compressImage(byte[] input) {
        //Preferences preferences = new Preferences(getSharedPreferences(Preferences.PREF_SETTINGS,Context.MODE_PRIVATE));
        Bitmap original = BitmapFactory.decodeByteArray(input, 0, input.length);
        //Bitmap resized = Bitmap.createScaledBitmap(original, PHOTO_WIDTH, PHOTO_HEIGHT, true);
        // create a matrix object
        Matrix matrix = new Matrix();
        matrix.postRotate(Integer.parseInt(preferences.read(getString(R.string.key_rotation), "90"))); // anti-clockwise by 90 degrees
        Bitmap bitmapRotate = Bitmap.createBitmap(original, 0, 0, original.getWidth(), original.getHeight(), matrix, true);
        ByteArrayOutputStream blob = new ByteArrayOutputStream();
        bitmapRotate.compress(Bitmap.CompressFormat.JPEG, Integer.parseInt(preferences.read(getString(R.string.key_quality_pic), "50")), blob);

        return blob.toByteArray();
    }

    private void takeImage() {

        while (f_wait_take) ; //Ждем если обрабатывается фото
        if (camera != null)
            camera.release();
        camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);

        camera.setParameters(Main.parameters);
        String timeStamp = new SimpleDateFormat("HH_mm_ss").format(new Date());
        String folderStamp = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        //file = new File(Main.path.getPath(),timeStamp + ".jpg");
        File folderPath = new File(Main.path.getAbsolutePath() + File.separator + folderStamp);
        folderPath.mkdirs();
        file = new File(folderPath.getPath(), timeStamp + ".jpg");
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
                f_wait_take = false;
            }
        };

        camera.startPreview();
        //camera.unlock();

        try {
            Thread.sleep(2000);
        } catch (Exception e) {
            e.printStackTrace();
        }

        f_wait_take = true;

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

    public class ThreadTakePicture extends AsyncTask<Void, Long, Void> {
        private boolean closed = true;
        Preferences preferences = new Preferences(getSharedPreferences(getString(R.string.pref_settings), Context.MODE_PRIVATE));
        private boolean f_single_take = preferences.read(getString(R.string.key_flag_take_single), true);

        TimerTake timerTake;
        boolean stop_timer = false;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            closed = false;
            //preferences = new Preferences(getSharedPreferences(Preferences.PREF_SETTINGS,Context.MODE_PRIVATE));
            timerTake = new TimerTake(Long.parseLong(preferences.read(getString(R.string.key_period_take), "10")) * 1000, 1000);
        }

        @Override
        protected Void doInBackground(Void... params) {

            do {
                takeImage();
                publishProgress((Long.parseLong(preferences.read(getString(R.string.key_period_take), "10")) * 1000) + Main.error_time_take, (long) 1000);
                timerTake.onStart();
                while (!isCancelled() && timerTake.isStart()) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                f_single_take = preferences.read(getString(R.string.key_flag_take_single), true);
            } while (!isCancelled() && !f_single_take);

            while (!isCancelled() && f_wait_take) ;
            stopSelf();
            closed = true;
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            closed = true;
        }

        @Override
        protected void onProgressUpdate(Long... values) {
            super.onProgressUpdate(values);
            timerTake = new TimerTake(values[0], values[1]);
        }
    }

    public class TimerTake extends CountDownTimer {
        private boolean start = false;

        public TimerTake(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        public void onStart() {
            start = true;
            start();
        }

        @Override
        public void onFinish() {
            start = false;
        }

        @Override
        protected void finalize() throws Throwable {
            super.finalize();
        }

        public void onTick(long millisUntilFinished) {
            long l = (Long.parseLong(preferences.read(getString(R.string.key_period_take), "10")) * 1000) + Main.error_time_take;

            if (millisUntilFinished > l)
                this.onFinish();
        }

        boolean isStart() {
            return start;
        }

    }

    public class TakeTimer extends Timer {

    }
}

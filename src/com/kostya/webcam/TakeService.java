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
import java.util.TimerTask;

/** Сервис сьемки фото.
 * @author Kostya
 */
public class TakeService extends Service {
    Timer timer;
    /** Процесс сьемки */
    //private ThreadTakePicture threadTakePicture;    //private PowerManager.WakeLock wakeLock;
    /** Камера */
    private Camera camera = null;
    /** Настройки */
    Preferences preferences;
    /** Фаил изображения */
    private File file;
    /** Флаг делается изображение */
    private boolean fWaitTake = false;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        /** Есть цель*/
        if (intent != null)
            /** Есть действие цели */
            if (intent.getAction() != null)
                /** Действие цели сделать одно фото */
                if (intent.getAction().equals("take")) {
                    /** Цель имеет параметры */
                    if (intent.getExtras() != null) {
                        //flag_take_single = intent.getBooleanExtra(Preferences.KEY_FLAG_TAKE_SINGLE, true);

                        /** Флаг одиночная сьемка */
                        boolean flag = intent.getBooleanExtra(getString(R.string.key_flag_take_single), true);
                        /** Запомнить флаг в настройках*/
                        preferences.write(getString(R.string.key_flag_take_single), flag);
                        if (timer != null) {
                            timer.cancel();
                        }
                        timer = new Timer();
                        timer.schedule(new TimerTakeTask(), 10);
                        /** Если процесс сьемки закрыт *//*
                        if (!threadTakePicture.isStart())
                            *//** Запустить процесс сьемки *//*
                            threadTakePicture.start();*/
                    }
                    /** Действие цели запустить непрерывный процесс сьемки*/
                } else if (intent.getAction().equals("start")) {
                    if (timer != null) {
                        timer.cancel();
                    }
                    timer = new Timer();
                    timer.schedule(new TimerTakeTask(), 0,(Long.parseLong(preferences.read(getString(R.string.key_period_take), "10")) * 1000) + Main.error_time_take);
                    /** Если процесс сьемки закрыт *//*
                    if (!threadTakePicture.isStart())
                    *//** Запустить процесс сьемки *//*
                        threadTakePicture.start();*/
                }
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        /** Экземпляр процесса сьемки фото */
        //threadTakePicture = new ThreadTakePicture();
        /** Экземпляр настроек камеры */
        preferences = new Preferences(getSharedPreferences(getString(R.string.pref_settings), Context.MODE_PRIVATE));
        /** Время задержки между кадрами */
        int temp = Integer.parseInt(preferences.read(getString(R.string.key_period_take), "10"));
        /** Должно быть в диапазоне в секундах */
        if (temp <= 0 || temp > 600)
            preferences.write(getString(R.string.key_period_take), String.valueOf(10));
        /** Качество фото в процентах*/
        temp = Integer.parseInt(preferences.read(getString(R.string.key_quality_pic), "50"));
        /** Должно быть в диапазоне в процентах */
        if (temp < 10 || temp > 100)
            preferences.write(getString(R.string.key_quality_pic), "50");

        //flag_take_single = preferences.read(Preferences.KEY_FLAG_TAKE_SINGLE,true);

        /** Открываем главную камеру*/
        camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
        Main.parameters = camera.getParameters();
        /** Сбрасываем настройки */
        camera.release();
        /** Загружаем новые настройки */
        loadParameters();
        //threadTakePicture.execute();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //threadTakePicture.cancel();
        //while (threadTakePicture.isStart()) ;
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }

    /**
     * Загрузка параметров камеры из настроек программы.
     */
    void loadParameters() {

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

    /** Сжатие изибражения
     * @param input Входящии данные.
     * @return Сжатые данные.
     */
    byte[] compressImage(byte[] input) {
        //Preferences preferences = new Preferences(getSharedPreferences(Preferences.PREF_SETTINGS,Context.MODE_PRIVATE));
        Bitmap original = BitmapFactory.decodeByteArray(input, 0, input.length);
        //Bitmap resized = Bitmap.createScaledBitmap(original, PHOTO_WIDTH, PHOTO_HEIGHT, true);
        // create a matrix object
        Matrix matrix = new Matrix();
        /** Поворот изображения в градусах против часовой стрелки*/
        matrix.postRotate(Integer.parseInt(preferences.read(getString(R.string.key_rotation), "90"))); // anti-clockwise by 90 degrees
        Bitmap bitmapRotate = Bitmap.createBitmap(original, 0, 0, original.getWidth(), original.getHeight(), matrix, true);
        ByteArrayOutputStream blob = new ByteArrayOutputStream();
        bitmapRotate.compress(Bitmap.CompressFormat.JPEG, Integer.parseInt(preferences.read(getString(R.string.key_quality_pic), "50")), blob);

        return blob.toByteArray();
    }

    /**
     * Сделать фотографию.
     */
    private void takeImage() {
        /** Пока обрабатывается фото */
        while (fWaitTake);
        /** Экземпляр камеры существует */
        if (camera != null)
            /** Сбрасываем настройки */
            camera.release();
        /** Открываем главную камеру */
        camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
        /** Загружаем настройки из программы */
        camera.setParameters(Main.parameters);
        /** Создаем штамп времени */
        String timeStamp = new SimpleDateFormat("HH_mm_ss").format(new Date());
        /** Создаем имя папки по дате */
        String folderStamp = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        //file = new File(Main.path.getPath(),timeStamp + ".jpg");
        /** Создаем папку с именем штампа даты */
        File folderPath = new File(Main.path.getAbsolutePath() + File.separator + folderStamp);
        /** Делаем папку */
        folderPath.mkdirs();
        /** Создаем фаил с именем штампа времени */
        file = new File(folderPath.getPath(), timeStamp + ".jpg");
        /** Начать сьемку изображения */
        camera.startPreview();
        //camera.unlock();
        /** Задержка  2 секунды для стабилизации камеры */
        try { Thread.sleep(2000);} catch (Exception e) {}
        /** Установливаем флаг делаем фото */
        fWaitTake = true;

        try {
            /** Сделать сьемку изображения */
            camera.takePicture(null, null, null, jpegCallback);
        } catch (Exception e) {
            try {
                /** При ошибке сделать пересоединение */
                camera.reconnect();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            /** Остановить сьемку */
            camera.stopPreview();
            /** Сбросить настройки */
            camera.release();
        }
    }

    /** Обратный вызов камеры */
    final Camera.PictureCallback jpegCallback = new Camera.PictureCallback() {
        /** Фото сделано.
         * @param data Данные изображения.
         * @param camera Камера которая сделала изображение.
         */
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            /** Сжимаем данные изображения */
            byte[] compressImage = compressImage(data);
            try {
                /** Создаем поток для записи фаила в папку временного хранения */
                FileOutputStream outputStream = new FileOutputStream(file.getPath());
                /** Записываем фаил в папку */
                outputStream.write(compressImage);
                /** Закрываем поток */
                outputStream.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            /** Закрываем камеру */
            camera.stopPreview();
            /** Сбрасываем настройки */
            camera.release();
            //wakeLock.release();
            /** Сбрасываем флаг фото сделано */
            fWaitTake = false;
        }

    };

    /**
     * Процесс сьемки.
     */
    /*public class ThreadTakePicture extends Thread{
        private boolean start;
        private boolean cancelled;
        *//** Таймер между сьемкой кадров *//*
        TimerTake timerTake;
        private boolean fSingleTake;

        @Override
        public synchronized void start() {
            fSingleTake = preferences.read(getString(R.string.key_flag_take_single), true);
            super.start();
            start = true;
        }

        @Override
        public void run() {
            do {
                *//** Сделать фото*//*
                takeImage();
                *//** Выйти из процесса и передать данные*//*
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        *//** Экземпляр таймера с новыми параметрами *//*
                        timerTake = new TimerTake((Long.parseLong(preferences.read(getString(R.string.key_period_take), "10")) * 1000) + Main.error_time_take, (long) 1000);
                    }
                });
                //_handler.updateTimer((Long.parseLong(preferences.read(getString(R.string.key_period_take), "10")) * 1000) + Main.error_time_take, (long) 1000);
                //publishProgress((Long.parseLong(preferences.read(getString(R.string.key_period_take), "10")) * 1000) + Main.error_time_take, (long) 1000);
                *//** Запустить таймер сьемки *//*
                timerTake.onStart();
                *//** Пока не сбросили процесс и таймер запущен*//*
                while (!cancelled && timerTake.isStart()) {
                    try { Thread.sleep(10); } catch (InterruptedException e) { e.printStackTrace(); }
                }
                *//** Устанавливаем флаг одиночной сьемки *//*
                fSingleTake = preferences.read(getString(R.string.key_flag_take_single), true);
                *//** Пока не сбросили процесс и флаг не одиночная сьемка *//*
            } while (!cancelled && !fSingleTake);
            *//** Пока не сбросили процесс и флаг делаем фото *//*
            while (!cancelled && fWaitTake) ;
            *//** Останавливаем сервис*//*
            stopSelf();
            start = false;
        }

        private void cancel() {
            cancelled = true;
        }

        public boolean isStart() {
            return start;
        }

    }*/

    /**
     * Процесс сьемки.
     */
    /*public class ThreadTakePicture extends AsyncTask<Void, Long, Void> {
        private boolean closed = true;
        *//** Экземпляр настроек камеры сохраненых *//*
        Preferences preferences = new Preferences(getSharedPreferences(getString(R.string.pref_settings), Context.MODE_PRIVATE));
        *//** Флаг одиночной сьемки *//*
        private boolean f_single_take = preferences.read(getString(R.string.key_flag_take_single), true);

        *//** Таймер между сьемкой кадров *//*
        TimerTake timerTake;
        *//** Флаг стоп таймера *//*
        boolean stop_timer = false;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            closed = false;
            //preferences = new Preferences(getSharedPreferences(Preferences.PREF_SETTINGS,Context.MODE_PRIVATE));
            *//** Создаем экземпляр таимера сьемки *//*
            timerTake = new TimerTake(Long.parseLong(preferences.read(getString(R.string.key_period_take), "10")) * 1000, 1000);
        }

        *//** Процесс
         * @param params
         * @return
         *//*
        @Override
        protected Void doInBackground(Void... params) {

            do {
                *//** Сделать фото*//*
                takeImage();
                *//** Выйти из процесса и передать данные*//*
                publishProgress((Long.parseLong(preferences.read(getString(R.string.key_period_take), "10")) * 1000) + Main.error_time_take, (long) 1000);
                *//** Запустить таймер сьемки *//*
                timerTake.onStart();
                *//** Пока не сбросили процесс и таймер запущен*//*
                while (!isCancelled() && timerTake.isStart()) {
                    try { Thread.sleep(10); } catch (InterruptedException e) { e.printStackTrace(); }
                }
                *//** Устанавливаем флаг одиночной сьемки *//*
                f_single_take = preferences.read(getString(R.string.key_flag_take_single), true);
                *//** Пока не сбросили процесс и флаг не одиночная сьемка *//*
            } while (!isCancelled() && !f_single_take);
            *//** Пока не сбросили процесс и флаг делаем фото *//*
            while (!isCancelled() && f_wait_take) ;
            *//** Останавливаем сервис*//*
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
            *//** Экземпляр таймера с новыми параметрами *//*
            timerTake = new TimerTake(values[0], values[1]);
        }
    }*/

    /**
     * Таймер для периода сьемки.
     */
    /*public class TimerTake extends CountDownTimer {
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

    }*/

    public class TakeTimer extends Timer{

    }

    public class TimerTakeTask extends TimerTask{

        @Override
        public void run() {
            /** Сделать фото*/
            takeImage();
        }
    }

}

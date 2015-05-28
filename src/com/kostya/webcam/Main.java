package com.kostya.webcam;

import android.app.Application;
import android.content.Context;
import android.hardware.Camera;
import android.os.Environment;
import android.os.PowerManager;
import com.kostya.webcam.provider.ErrorDBAdapter;

import java.io.File;
import java.util.List;

/** Класс Main
 * @author Kostya
 */
public class Main extends Application {
    private PowerManager.WakeLock wakeLock;
    private Camera camera = null;
    public static Camera.Parameters parameters = null;
    public static File path;
    /** Локальная папка для временного хранения файлов */
    public static final String LOCATE_FOLDER_PATH = "WebPhoto";
    /** Время поправки для таймера */
    public static long error_time_take = 0;

    @Override
    public void onCreate() {
        super.onCreate();

        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "My Tag");
        wakeLock.acquire();
        /** проверяем доступность SD карты*/
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            new ErrorDBAdapter(this).insertNewEntry("500", "SD-карта не доступна: " + Environment.getExternalStorageState());
            //todo что зделать если не доступна SD карта
            //return;
        }
        /** Создаем папку для файлов */
        path = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + File.separator + LOCATE_FOLDER_PATH);
        /** Если нет папки тогда создаем */
        if (!path.exists()) {
            if (!path.mkdirs()) {
                new ErrorDBAdapter(this).insertNewEntry("500", "Path no create: " + path.getPath());
                //todo что зделать если не создали папку
            }
        }
        /** Окрываем экземпляр основной камеры */
        camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
        /** Получаем параметры камеры */
        parameters = camera.getParameters();

        camera.release();
        load_parameters();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
    }

    /**
     * Загружаем параметры камеры в настройки программы
     */
    public void load_parameters() {

        Preferences preferences = new Preferences(getSharedPreferences(getString(R.string.pref_settings), Context.MODE_PRIVATE));

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

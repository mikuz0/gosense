package com.draekko.gosense.ui.settings;

import static android.content.Context.POWER_SERVICE;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.draekko.gosense.BuildConfig;
import com.draekko.gosense.R;
import com.draekko.gosense.activities.SplashScreenActivity;
import com.draekko.gosense.data.AirPressureData;
import com.draekko.gosense.data.RelativeHumidityData;
import com.draekko.gosense.data.SensorDevice;
import com.draekko.gosense.data.TemperatureData;
import com.draekko.gosense.database.GoSenseContract;
import com.draekko.gosense.database.GoSenseDb;
import com.draekko.gosense.misc.TraySettings;
import com.draekko.traypreferences.TrayPreference;
import com.draekko.traypreferences.TrayPreferenceFragment;
import com.draekko.traypreferences.TrayPreferenceScreen;
import com.draekko.traypreferences.TrayPreferenceManager;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class SettingsFragment extends TrayPreferenceFragment {

    private static final String TAG = "SettingsFragment";

    public interface OnTemperatureUnitChanged {
        public void onChanged(boolean isFahrenheit);
    }

    public interface OnAirPressureUnitChanged {
        public void onChanged(int unit);
    }

    public interface OnDataCleared {
        public void onCleared();
    }

    public interface OnImportExportData {
        public void onExportData(boolean status);
        public void onImportData(boolean status);
    }

    private boolean importing = false;
    private boolean exporting = false;

    private String tHeader =
            GoSenseContract.TEMPERATURE_RECORD.COLUMN.ID + "," +
                    GoSenseContract.TEMPERATURE_RECORD.COLUMN.TIME + "," +
                    GoSenseContract.TEMPERATURE_RECORD.COLUMN.TEMPERATURE + "," +
                    GoSenseContract.TEMPERATURE_RECORD.COLUMN.NAME + "," +
                    GoSenseContract.TEMPERATURE_RECORD.COLUMN.ADDRESS + "," +
                    GoSenseContract.TEMPERATURE_RECORD.COLUMN.LATITUDE + "," +
                    GoSenseContract.TEMPERATURE_RECORD.COLUMN.LONGITUDE + "\n";

    private String rhHeader =
            GoSenseContract.RELATIVEHUMIDITY_RECORD.COLUMN.ID + "," +
                    GoSenseContract.RELATIVEHUMIDITY_RECORD.COLUMN.TIME + "," +
                    GoSenseContract.RELATIVEHUMIDITY_RECORD.COLUMN.RELATIVEHUMIDITY + "," +
                    GoSenseContract.RELATIVEHUMIDITY_RECORD.COLUMN.NAME + "," +
                    GoSenseContract.RELATIVEHUMIDITY_RECORD.COLUMN.ADDRESS + "," +
                    GoSenseContract.RELATIVEHUMIDITY_RECORD.COLUMN.LATITUDE + "," +
                    GoSenseContract.RELATIVEHUMIDITY_RECORD.COLUMN.LONGITUDE + "\n";

    private String apHeader =
            GoSenseContract.AIRPRESSURE_RECORD.COLUMN.ID + "," +
                    GoSenseContract.AIRPRESSURE_RECORD.COLUMN.TIME + "," +
                    GoSenseContract.AIRPRESSURE_RECORD.COLUMN.AIRPRESSURE + "," +
                    GoSenseContract.AIRPRESSURE_RECORD.COLUMN.NAME + "," +
                    GoSenseContract.AIRPRESSURE_RECORD.COLUMN.ADDRESS + "," +
                    GoSenseContract.AIRPRESSURE_RECORD.COLUMN.LATITUDE + "," +
                    GoSenseContract.AIRPRESSURE_RECORD.COLUMN.LONGITUDE + "\n";

    private String pairedHeader =
            GoSenseContract.DEVICE_RECORD.COLUMN.ID + "," +
                    GoSenseContract.DEVICE_RECORD.COLUMN.ADDRESS + "," +
                    GoSenseContract.DEVICE_RECORD.COLUMN.NAME + "," +
                    GoSenseContract.DEVICE_RECORD.COLUMN.ALIASNAME + "," +
                    GoSenseContract.DEVICE_RECORD.COLUMN.COLOR + "\n";

    private String fileMsgTop = "AUTO GENERATED FILE DO NOT EDIT";

    private ArrayList<OnAirPressureUnitChanged> mOnAirPressureUnitChanged = new ArrayList<>();
    private ArrayList<OnTemperatureUnitChanged> mOnTemperatureUnitChanged = new ArrayList<>();
    private ArrayList<OnDataCleared> mOnDataCleared = new ArrayList<>();
    private ArrayList<OnImportExportData> mOnImportExportData = new ArrayList<>();

    private AlertDialog mFileIoDialog;
    private Context mContext;
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private static SettingsFragment mFragment;

    public static SettingsFragment getInstance() {
        if (mFragment == null) {
            mFragment = new SettingsFragment();
        }
        return mFragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(false);
        addPreferencesFromResource(R.xml.gosense_prefs);
        handleSharedPreferenceChanged();
    }

    @Override
    public void onResume() {
        super.onResume();
        handleSharedPreferenceChanged();
    }

    @Override
    public void onCreatePreferences(@Nullable Bundle bundle, String s) {
    }

    // --- SAF Лаунчеры ---

    private final ActivityResultLauncher<Intent> exportFileLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            performExport(uri);
                        }
                    }
                }
            });

    private final ActivityResultLauncher<Intent> importFileLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            performImport(uri);
                        }
                    }
                }
            });

    // --- Обработчики настроек ---

    public void handleSharedPreferenceChanged() {
        TrayPreference pref_battery_optimization = findPreference("PREF_KEY_BATTERY_OPTIMIZATION");
        if (pref_battery_optimization != null) {
            pref_battery_optimization.setOnPreferenceClickListener(new TrayPreference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(@NonNull TrayPreference preference) {
                    String packageName = mContext.getPackageName();
                    PowerManager pm = (PowerManager) mContext.getSystemService(POWER_SERVICE);
                    if (pm.isIgnoringBatteryOptimizations(packageName)) {
                        Toast.makeText(mContext, getString(R.string.toast_battery_optimization_disabled), Toast.LENGTH_LONG).show();
                        return true;
                    } else {
                        Intent intent = new Intent();
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            try {
                                intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                                intent.setData(Uri.parse("package:" + packageName));
                                startActivity(intent);
                                return true;
                            } catch (Exception e) {
                                Toast.makeText(mContext, getString(R.string.toast_battery_optimization_error), Toast.LENGTH_LONG).show();
                                e.printStackTrace();
                            }
                        }
                        try {
                            intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                            intent.setData(Uri.parse("package:" + packageName));
                            startActivity(intent);
                        } catch (Exception e) {
                            Toast.makeText(mContext, getString(R.string.toast_battery_optimization_error), Toast.LENGTH_LONG).show();
                            e.printStackTrace();
                        }
                        return true;
                    }
                }
            });
        }

        if (BuildConfig.DEBUG) {
            TrayPreference pref_developer_reset = findPreference("PREF_KEY_RESET_EVERYTHING");
            if (pref_developer_reset != null) {
                pref_developer_reset.setOnPreferenceClickListener(new TrayPreference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(@NonNull TrayPreference preference) {
                        GoSenseDb goSenseDb = new GoSenseDb(mContext);
                        goSenseDb.deleteAll();
                        goSenseDb.createTables();
                        TraySettings.deleteAll();
                        getActivity().finish();
                        getActivity().finishAffinity();
                        Intent intent = new Intent(mContext, SplashScreenActivity.class);
                        startActivity(intent);
                        return true;
                    }
                });
            }
        } else {
            TrayPreference pref_developer = findPreference(TraySettings.PREF_KEY_DEVELOPER);
            if (pref_developer != null) {
                TrayPreferenceScreen preference_screen = (TrayPreferenceScreen)findPreference(TraySettings.PREF_KEY_PREFERENCE_SCREEN);
                if (preference_screen != null) {
                    preference_screen.removeTrayPreference(pref_developer);
                }
            }
        }

        TrayPreference preferenceCleared = findPreference(TraySettings.PREF_KEY_CLEAR_DATABASE);
        if (preferenceCleared != null) {
            preferenceCleared.setOnPreferenceClickListener(new TrayPreference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(@NonNull TrayPreference preference) {
                    if (importing) {
                        showToast(getString(R.string.import_in_progress_clearing), Toast.LENGTH_LONG);
                        return true;
                    }
                    if (exporting) {
                        showToast(getString(R.string.export_in_progress_clearing), Toast.LENGTH_LONG);
                        return true;
                    }
                    final Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            GoSenseDb goSenseDb = new GoSenseDb(mContext);
                            goSenseDb.deleteAll();
                            goSenseDb.close();
                            if (mOnDataCleared.size() > 0) {
                                for (OnDataCleared onDataCleared : mOnDataCleared) {
                                    onDataCleared.onCleared();
                                }
                            }
                            Toast.makeText(mContext, getString(R.string.all_db_data_cleared), Toast.LENGTH_LONG).show();
                        }
                    });
                    thread.start();
                    return true;
                }
            });
        }

        TrayPreference preferenceExport = findPreference(TraySettings.PREF_KEY_BT_EXPORT_DATABASE);
        if (preferenceExport != null) {
            preferenceExport.setOnPreferenceClickListener(new TrayPreference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(@NonNull TrayPreference preference) {
                    if (importing) {
                        showToast(getString(R.string.import_in_progress), Toast.LENGTH_LONG);
                        return true;
                    }
                    if (exporting) {
                        showToast(getString(R.string.export_in_progress), Toast.LENGTH_LONG);
                        return true;
                    }
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
                    String filename = "gosense-" + sdf.format(new Date()) + ".csv.gz";
                    Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("application/gzip");
                    intent.putExtra(Intent.EXTRA_TITLE, filename);
                    exportFileLauncher.launch(intent);
                    return true;
                }
            });
        }

        TrayPreference preferenceImport = findPreference(TraySettings.PREF_KEY_BT_IMPORT_DATABASE);
        if (preferenceImport != null) {
            preferenceImport.setOnPreferenceClickListener(new TrayPreference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(@NonNull TrayPreference preference) {
                    if (importing) {
                        showToast(getString(R.string.import_in_progress), Toast.LENGTH_LONG);
                        return true;
                    }
                    if (exporting) {
                        showToast(getString(R.string.export_in_progress), Toast.LENGTH_LONG);
                        return true;
                    }
                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("*/*");
                    importFileLauncher.launch(intent);
                    return true;
                }
            });
        }
    }

    // --- Реализация экспорта через SAF ---

    private void performExport(Uri uri) {
        exporting = true;
        Log.i(TAG, "Started exporting data");
        startDialogProgress(getString(R.string.exporting_backup_file));
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (mOnImportExportData.size() > 0) {
                    for (OnImportExportData onImportExportData : mOnImportExportData) {
                        onImportExportData.onExportData(true);
                    }
                }
                boolean success = exportDataToUri(uri);
                if (mOnImportExportData.size() > 0) {
                    for (OnImportExportData onImportExportData : mOnImportExportData) {
                        onImportExportData.onExportData(false);
                    }
                }
                stopProgress();
                exporting = false;
                if (success) {
                    showToast(getString(R.string.finished_exporting_data), Toast.LENGTH_LONG);
                }
            }
        }).start();
    }

    private boolean exportDataToUri(Uri uri) {
        GoSenseDb goSenseDb = new GoSenseDb(mContext);
        GZIPOutputStream fos = null;
        boolean done = false;
        try {
            OutputStream outputStream = mContext.getContentResolver().openOutputStream(uri);
            if (outputStream == null) return false;
            fos = new GZIPOutputStream(outputStream);

            ArrayList<SensorDevice> pairedList = goSenseDb.getPairedSensorDevices();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:sss");
            String currentDateTime = sdf.format(new Date());
            String topMsg = fileMsgTop + String.format(" (%s)", currentDateTime) + "\n";
            fos.write(topMsg.getBytes());

            fos.write(pairedHeader.getBytes());
            if (pairedList.size() > 0) {
                for (SensorDevice sensorDevice : pairedList) {
                    String device = sensorDevice.getId() + "," +
                            sensorDevice.getAddress() + "," +
                            sensorDevice.getName() + "," +
                            sensorDevice.getAliasName() + "," +
                            sensorDevice.getColor() + "\n";
                    fos.write(device.getBytes());
                }
            } else {
                fos.write("empty\n".getBytes());
            }

            ArrayList<TemperatureData> tList = goSenseDb.getTemperatureData();
            fos.write(tHeader.getBytes());
            if (tList.size() > 0) {
                for (TemperatureData tData : tList) {
                    String temperature = tData.getId() + "," + tData.getTimestamp() + "," +
                            tData.getTemperature() + "," + tData.getName() + "," +
                            tData.getAddress() + "," + tData.getLatitude() + "," +
                            tData.getLatitude() + "\n";
                    fos.write(temperature.getBytes());
                }
            } else {
                fos.write("empty\n".getBytes());
            }

            ArrayList<RelativeHumidityData> rhList = goSenseDb.getRelativeHumidityData();
            fos.write(rhHeader.getBytes());
            if (rhList.size() > 0) {
                for (RelativeHumidityData rhData : rhList) {
                    String relativeHumidity = rhData.getId() + "," + rhData.getTimestamp() + "," +
                            rhData.getRelativeHumidity() + "," + rhData.getName() + "," +
                            rhData.getAddress() + "," + rhData.getLatitude() + "," +
                            rhData.getLatitude() + "\n";
                    fos.write(relativeHumidity.getBytes());
                }
            } else {
                fos.write("empty\n".getBytes());
            }

            ArrayList<AirPressureData> apList = goSenseDb.getAirPressureData();
            fos.write(apHeader.getBytes());
            if (apList.size() > 0) {
                for (AirPressureData apData : apList) {
                    String airPressure = apData.getId() + "," + apData.getTimestamp() + "," +
                            apData.getAirPressure() + "," + apData.getName() + "," +
                            apData.getAddress() + "," + apData.getLatitude() + "," +
                            apData.getLatitude() + "\n";
                    fos.write(airPressure.getBytes());
                }
            } else {
                fos.write("empty\n".getBytes());
            }
            done = true;
        } catch (FileNotFoundException e) {
            showToast(getString(R.string.export_data_error) + " [FileNotFoundException]", Toast.LENGTH_LONG);
            e.printStackTrace();
        } catch (IOException e) {
            showToast(getString(R.string.export_data_error) + " [IOException]", Toast.LENGTH_LONG);
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try { fos.flush(); fos.finish(); fos.close(); } catch (IOException e) { e.printStackTrace(); }
            }
            goSenseDb.close();
        }
        return done;
    }

    // --- Реализация импорта через SAF ---

    private void performImport(Uri uri) {
        importing = true;
        Log.i(TAG, "Started importing data");
        startDialogProgress(getString(R.string.importing_backup_file));
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (mOnImportExportData.size() > 0) {
                    for (OnImportExportData onImportExportData : mOnImportExportData) {
                        onImportExportData.onImportData(true);
                    }
                }
                importDataFromUri(uri);
                if (mOnImportExportData.size() > 0) {
                    for (OnImportExportData onImportExportData : mOnImportExportData) {
                        onImportExportData.onImportData(false);
                    }
                }
                stopProgress();
                importing = false;
            }
        }).start();
    }

    private boolean importDataFromUri(Uri uri) {
        GoSenseDb goSenseDb = new GoSenseDb(mContext);
        try {
            InputStream inputStream = mContext.getContentResolver().openInputStream(uri);
            if (inputStream == null) return false;
            GZIPInputStream gzipis = new GZIPInputStream(inputStream);
            BufferedReader br = new BufferedReader(new InputStreamReader(gzipis));
            String data;
            int mode = 0;
            while ((data = br.readLine()) != null) {
                if (data.contains("GENERATED")) { mode = 0; continue; }
                else if (data.contains("TEMPERATURE")) { mode = 1; continue; }
                else if (data.contains("RELATIVEHUMIDITY")) { mode = 2; continue; }
                else if (data.contains("AIRPRESSURE")) { mode = 3; continue; }
                else if (data.contains("ALIASNAME") && data.contains("COLOR")) { mode = 4; continue; }

                String[] array = data.split(",");
                if (mode == 1) {
                    TemperatureData temperatureData = new TemperatureData();
                    temperatureData.setId(Integer.valueOf(array[0]));
                    temperatureData.setTimestamp(Long.valueOf(array[1]));
                    temperatureData.setTemperature(Float.valueOf(array[2]));
                    temperatureData.setName(array[3]);
                    temperatureData.setAddress(array[4]);
                    temperatureData.setLatitude(Double.valueOf(array[5]));
                    temperatureData.setLongitude(Double.valueOf(array[6]));
                    goSenseDb.saveTemperatureIfNotExist(temperatureData);
                } else if (mode == 2) {
                    RelativeHumidityData relativeHumidityData = new RelativeHumidityData();
                    relativeHumidityData.setId(Integer.valueOf(array[0]));
                    relativeHumidityData.setTimestamp(Long.valueOf(array[1]));
                    relativeHumidityData.setRelativeHumidity(Float.valueOf(array[2]));
                    relativeHumidityData.setName(array[3]);
                    relativeHumidityData.setAddress(array[4]);
                    relativeHumidityData.setLatitude(Double.valueOf(array[5]));
                    relativeHumidityData.setLongitude(Double.valueOf(array[6]));
                    goSenseDb.saveRelativeHumidityIfNotExist(relativeHumidityData);
                } else if (mode == 3) {
                    AirPressureData airPressureData = new AirPressureData();
                    airPressureData.setId(Integer.valueOf(array[0]));
                    airPressureData.setTimestamp(Long.valueOf(array[1]));
                    airPressureData.setAirPressure(Float.valueOf(array[2]));
                    airPressureData.setName(array[3]);
                    airPressureData.setAddress(array[4]);
                    airPressureData.setLatitude(Double.valueOf(array[5]));
                    airPressureData.setLongitude(Double.valueOf(array[6]));
                    goSenseDb.saveAirPressureIfNotExist(airPressureData);
                } else if (mode == 4) {
                    SensorDevice sensorDevice = new SensorDevice();
                    sensorDevice.setId(Integer.valueOf(array[0]));
                    sensorDevice.setAddress(array[1]);
                    sensorDevice.setName(array[2]);
                    sensorDevice.setAliasName(array[3]);
                    sensorDevice.setColor(Integer.valueOf(array[4]));
                    goSenseDb.saveSensorDeviceIfNotExist(sensorDevice);
                }
            }
            gzipis.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            goSenseDb.close();
        }
    }

    // --- Вспомогательные методы ---

    private void showToast(String msg, int duration) {
        if (getActivity() == null) return;
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getActivity(), msg, duration).show();
            }
        });
    }

    private void startDialogProgress(String message) {
        stopProgress();
        if (getActivity() == null) return;
        requireActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mFileIoDialog = new AlertDialog.Builder(mContext).setMessage(message).setCancelable(true).show();
                TextView messageText = mFileIoDialog.findViewById(android.R.id.message);
                if (messageText != null) {
                    messageText.setGravity(Gravity.CENTER);
                    messageText.setAllCaps(true);
                }
                mFileIoDialog.show();
            }
        });
    }

    private void stopProgress() {
        if (getActivity() == null) return;
        requireActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mFileIoDialog != null) {
                    mFileIoDialog.dismiss();
                    mFileIoDialog = null;
                }
            }
        });
    }

    public void registerOnImportExportData(OnImportExportData onImportExportData) {
        if (onImportExportData != null && !mOnImportExportData.contains(onImportExportData)) {
            mOnImportExportData.add(onImportExportData);
        }
    }
    public void unregisterOnImportExportData(OnImportExportData onImportExportData) {
        if (onImportExportData != null && mOnImportExportData.contains(onImportExportData)) {
            mOnImportExportData.remove(onImportExportData);
        }
    }

    public void registerOnDataCleared(OnDataCleared onDataCleared) {
        if (onDataCleared != null && !mOnDataCleared.contains(onDataCleared)) {
            mOnDataCleared.add(onDataCleared);
        }
    }
    public void unregisterOnDataCleared(OnDataCleared onDataCleared) {
        if (onDataCleared != null && mOnDataCleared.contains(onDataCleared)) {
            mOnDataCleared.remove(onDataCleared);
        }
    }

    public void registerOnTemperatureUnitChanged(OnTemperatureUnitChanged onTemperatureUnitChanged) {
        if (onTemperatureUnitChanged != null && !mOnTemperatureUnitChanged.contains(onTemperatureUnitChanged)) {
            mOnTemperatureUnitChanged.add(onTemperatureUnitChanged);
        }
    }
    public void unregisterOnTemperatureUnitChanged(OnTemperatureUnitChanged onTemperatureUnitChanged) {
        if (onTemperatureUnitChanged != null && mOnTemperatureUnitChanged.contains(onTemperatureUnitChanged)) {
            mOnTemperatureUnitChanged.remove(onTemperatureUnitChanged);
        }
    }

    public void registerOnAirPressureUnitChanged(OnAirPressureUnitChanged onAirPressureUnitChanged) {
        if (onAirPressureUnitChanged != null && !mOnAirPressureUnitChanged.contains(onAirPressureUnitChanged)) {
            mOnAirPressureUnitChanged.add(onAirPressureUnitChanged);
        }
    }
    public void unregisterOnAirPressureUnitChanged(OnAirPressureUnitChanged onAirPressureUnitChanged) {
        if (onAirPressureUnitChanged != null && mOnAirPressureUnitChanged.contains(onAirPressureUnitChanged)) {
            mOnAirPressureUnitChanged.remove(onAirPressureUnitChanged);
        }
    }
}
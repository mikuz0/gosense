package com.draekko.gosense.ui.settings;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.content.Context.POWER_SERVICE;

import static com.draekko.gosense.activities.SplashScreenActivity.REQUEST_MANAGE_FILES;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
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
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.draekko.gosense.BuildConfig;
import com.draekko.gosense.R;
import com.draekko.gosense.activities.SplashScreenActivity;
import com.draekko.gosense.app.GoSenseApplication;
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
import com.nbsp.materialfilepicker.MaterialFilePicker;
import com.nbsp.materialfilepicker.ui.FilePickerActivity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.attribute.UserPrincipal;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class SettingsFragment extends TrayPreferenceFragment {

    private static final String TAG = "SettingsFragment";

    private static final  int REQUEST_WRITE_MEDIA_PERMISSION = 0x4444;

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
    private TrayPreference trayPreference;

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

    TrayPreferenceManager trayPreferenceManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(false);
        addPreferencesFromResource(R.xml.gosense_prefs);
        handleSharedPreferenceChanged();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        handleSharedPreferenceChanged();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onCreatePreferences(@Nullable Bundle bundle, String s) {
        //addPreferencesFromResource(R.xml.app_prefs);
    }

    public void handleSharedPreferenceChanged() {
        TrayPreference pref_battery_optimization = findPreference("PREF_KEY_BATTERY_OPTIMIZATION");
        pref_battery_optimization.setOnPreferenceClickListener(new TrayPreference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(@NonNull TrayPreference preference) {
                String packageName = mContext.getPackageName();
                PowerManager pm = (PowerManager) mContext.getSystemService(POWER_SERVICE);
                if (pm.isIgnoringBatteryOptimizations(packageName)) {
                    Toast.makeText(mContext, "Battery Optimization Already Disabled", Toast.LENGTH_LONG).show();
                    return true;
                } else {
                    Intent intent = new Intent();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        try {
                            intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                            intent.setData(Uri.parse("package:" + packageName));
                            intent.addCategory(Intent.CATEGORY_DEFAULT);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                            intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                            startActivity(intent);
                            return true;
                        } catch (Exception e) {
                            Toast.makeText(mContext, "Error: Could not disable battery optimization", Toast.LENGTH_LONG).show();
                            e.printStackTrace();
                        }
                    }
                    try {
                        intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                        intent.setData(Uri.parse("package:" + packageName));
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    } catch (Exception e) {
                        Toast.makeText(mContext, "Error: Could not disable battery optimization", Toast.LENGTH_LONG).show();
                        e.printStackTrace();
                    }
                    return true;
                }
            }
        });

        if (BuildConfig.DEBUG) {
            TrayPreference pref_developer_reset = findPreference("PREF_KEY_RESET_EVERYTHING");
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

        TrayPreference preferenceExport = findPreference(TraySettings.PREF_KEY_BT_EXPORT_DATABASE);
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

                if (!checkRequirements()) {
                    showToast(getString(R.string.missing_permissions), Toast.LENGTH_LONG);
                    return true;
                }

                final Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (mOnImportExportData.size() > 0) {
                            for (OnImportExportData onImportExportData : mOnImportExportData) {
                                onImportExportData.onExportData(true);
                            }
                        }

                        exportData();

                        if (mOnImportExportData.size() > 0) {
                            for (OnImportExportData onImportExportData : mOnImportExportData) {
                                onImportExportData.onExportData(false);
                            }
                        }
                    }
                });
                thread.start();

                return true;
            }
        });

        TrayPreference preferenceImport = findPreference(TraySettings.PREF_KEY_BT_IMPORT_DATABASE);
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

                if (!checkRequirements()) {
                    showToast(getString(R.string.missing_permissions), Toast.LENGTH_LONG);
                    return true;
                }

                String root = Environment.getExternalStorageDirectory().getAbsolutePath();
                String path = root + "/GoSense";
                MaterialFilePicker materialFilePicker = new MaterialFilePicker();
                materialFilePicker.withCloseMenu(true);
                materialFilePicker.withCallbackActivityResults(mStartForResult);
                materialFilePicker.withPath(root);
                materialFilePicker.withPath(path);
                materialFilePicker.withFilter(Pattern.compile(".*\\.(csv.gz)$"));
                materialFilePicker.withFilterDirectories(false);
                materialFilePicker.withTitle("Import Data File");
                materialFilePicker.withSupportFragment(SettingsFragment.getInstance());
                materialFilePicker.start();

                /*
                 */

                return true;
            }
        });
    }

    ActivityResultLauncher<Intent> mStartForResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Log.i(TAG, "result:" + result.toString());
                        Intent intent = result.getData();
                        String dataFile = intent.getStringExtra(FilePickerActivity.RESULT_FILE_PATH);;

                        final Thread thread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                if (mOnImportExportData.size() > 0) {
                                    for (OnImportExportData onImportExportData : mOnImportExportData) {
                                        onImportExportData.onImportData(true);
                                    }
                                }

                                importData(dataFile);

                                if (mOnImportExportData.size() > 0) {
                                    for (OnImportExportData onImportExportData : mOnImportExportData) {
                                        onImportExportData.onImportData(false);
                                    }
                                }
                            }
                        });
                        thread.start();
                    }
                }
            });

    public boolean importData(String dataFile) {
        if (exporting) {
            showToast(getString(R.string.export_in_progress), Toast.LENGTH_LONG);
            return false;
        }
        if (importing) {
            showToast(getString(R.string.import_in_progress), Toast.LENGTH_LONG);
            return false;
        }

        importing = true;

        Log.i(TAG, "Started importing data");
        startDialogProgress(getString(R.string.importing_backup_file));
        GoSenseDb goSenseDb = new GoSenseDb(mContext);

        File importFile = new File(dataFile);
        if (importFile.exists()) {
            try {
                GZIPInputStream gzipis =
                        new GZIPInputStream(new FileInputStream(importFile));

                BufferedReader br = new BufferedReader(new InputStreamReader(gzipis));

                String data;
                int mode = 0;
                ArrayList<TemperatureData> tData = new ArrayList<>();
                ArrayList<RelativeHumidityData> rhData = new ArrayList<>();
                ArrayList<AirPressureData> apData = new ArrayList<>();
                ArrayList<SensorDevice> pairedData = new ArrayList<>();

                while ((data = br.readLine()) != null) {
                    if (data.contains("GENERATED")) {
                        mode = 0;
                        continue;
                    } else if (data.contains("TEMPERATURE")) {
                        Log.i(TAG, "importing temperature data");
                        mode = 1;
                        continue;
                    } else if (data.contains("RELATIVEHUMIDITY")) {
                        Log.i(TAG, "importing humidity data");
                        mode = 2;
                        continue;
                    } else if (data.contains("AIRPRESSURE")) {
                        Log.i(TAG, "importing airpressure data");
                        mode = 3;
                        continue;
                    } else if (data.contains("ALIASNAME") && data.contains("COLOR")) {
                        Log.i(TAG, "importing device data");
                        mode = 4;
                        continue;
                    }

                    String [] array = data.split(",");
                    if (mode == 1) {
                        // _ID,TIME,TEMPERATURE,NAME,ADDRESS,LATITUDE,LONGITUDE
                        TemperatureData temperatureData = new TemperatureData();
                        temperatureData.setId(Integer.valueOf(array[0]));
                        temperatureData.setTimestamp(Long.valueOf(array[1]));
                        temperatureData.setTemperature(Float.valueOf(array[2]));
                        temperatureData.setName(array[3]);
                        temperatureData.setAddress(array[4]);
                        temperatureData.setLatitude(Double.valueOf(array[5]));
                        temperatureData.setLongitude(Double.valueOf(array[6]));
                        goSenseDb.saveTemperatureIfNotExist(temperatureData);
                    }
                    if (mode == 2) {
                        // _ID,TIME,RELATIVEHUMIDITY,NAME,ADDRESS,LATITUDE,LONGITUDE
                        RelativeHumidityData relativeHumidityData = new RelativeHumidityData();
                        relativeHumidityData.setId(Integer.valueOf(array[0]));
                        relativeHumidityData.setTimestamp(Long.valueOf(array[1]));
                        relativeHumidityData.setRelativeHumidity(Float.valueOf(array[2]));
                        relativeHumidityData.setName(array[3]);
                        relativeHumidityData.setAddress(array[4]);
                        relativeHumidityData.setLatitude(Double.valueOf(array[5]));
                        relativeHumidityData.setLongitude(Double.valueOf(array[6]));
                        goSenseDb.saveRelativeHumidityIfNotExist(relativeHumidityData);
                    }
                    if (mode == 3) {
                        // _ID,TIME,AIRPRESSURE,NAME,ADDRESS,LATITUDE,LONGITUDE
                        AirPressureData airPressureData = new AirPressureData();
                        airPressureData.setId(Integer.valueOf(array[0]));
                        airPressureData.setTimestamp(Long.valueOf(array[1]));
                        airPressureData.setAirPressure(Float.valueOf(array[2]));
                        airPressureData.setName(array[3]);
                        airPressureData.setAddress(array[4]);
                        airPressureData.setLatitude(Double.valueOf(array[5]));
                        airPressureData.setLongitude(Double.valueOf(array[6]));
                        goSenseDb.saveAirPressureIfNotExist(airPressureData);
                    }
                    if (mode == 4) {
                        //_ID,ADDRESS,NAME,ALIASNAME,COLOR
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
            } catch (SecurityException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Log.i(TAG, "File doesn't exist : "  + importFile.getName());
        }

        goSenseDb.close();
        Log.i(TAG, "Finished importing data");

        stopProgress();

        importing = false;

        return true;
    }

    public boolean exportData() {
        if (exporting) {
            showToast(getString(R.string.export_in_progress), Toast.LENGTH_LONG);
            return false;
        }
        if (importing) {
            showToast(getString(R.string.import_in_progress), Toast.LENGTH_LONG);
            return false;
        }

        exporting = true;

        Log.i(TAG, "Started exporting data");
        startDialogProgress(getString(R.string.exporting_backup_file));

        GoSenseDb goSenseDb = new GoSenseDb(mContext);

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");
        File storage = Environment.getExternalStorageDirectory();
        File exportDirectory = new File(storage + "/GoSense");
        if (!exportDirectory.exists()) {
            exportDirectory.mkdirs();
        }
        if (!exportDirectory.exists()) {
            Log.e(TAG, "failed to access " + exportDirectory.getAbsolutePath() + " doesn't exist.");
            return false;
        }

        long time = System.currentTimeMillis();
        Date date = new Date();
        date.setTime(time);
        String dateStr = simpleDateFormat.format(date);
        String filename = "gosense-" + dateStr + "-" + System.currentTimeMillis() + ".csv.gz";

        File exportDataFile = new File(exportDirectory, filename);

        // disabled since emulated storage doesn't let you change
        // file permissions on some devices.

        if (false) {
            try {
                if (!exportDataFile.exists()) {
                    exportDataFile.createNewFile();
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
            try {
                if (exportDataFile.exists()) {
                    Path filePath = exportDataFile.toPath();
                    String perm = "rwxrwxrwx";
                    UserPrincipalLookupService lookupService = FileSystems.getDefault().getUserPrincipalLookupService();
                    Set<PosixFilePermission> permissions = PosixFilePermissions.fromString(perm);
                    UserPrincipal everybody = lookupService.lookupPrincipalByName("everybody");
                    Files.setPosixFilePermissions(filePath, permissions);
                    Files.setOwner(filePath, everybody);
                }
            } catch (UnsupportedOperationException uoe) {
                uoe.printStackTrace();
            } catch (SecurityException se) {
                se.printStackTrace();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }

        GZIPOutputStream fos = null;
        boolean done = false;
        boolean failed = false;
        try {
            FileOutputStream lfos = new FileOutputStream(exportDataFile);
            fos = new GZIPOutputStream(lfos);

            ArrayList<SensorDevice> pairedList = goSenseDb.getPairedSensorDevices();

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:sss");
            String currentDateTime = sdf.format(date);
            String topMsg = fileMsgTop + String.format(" (%s)", currentDateTime) + "\n";
            fos.write(topMsg.getBytes());

            // Write paired list header
            fos.write(pairedHeader.getBytes());

            // Output paired devices list
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

            pairedList = null;

            ArrayList<TemperatureData> tList = goSenseDb.getTemperatureData();

            // Write temperature list header
            fos.write(tHeader.getBytes());

            // Output temperature list
            if (tList.size() > 0) {
                for (TemperatureData tData : tList) {
                    String temperature =
                            tData.getId() + "," +
                            tData.getTimestamp() + "," +
                            tData.getTemperature() + "," +
                            tData.getName() + "," +
                            tData.getAddress() + "," +
                            tData.getLatitude() + "," +
                            tData.getLatitude() + "\n";
                    fos.write(temperature.getBytes());
                }
            } else {
                fos.write("empty\n".getBytes());
            }

            tList = null;

            ArrayList<RelativeHumidityData> rhList = goSenseDb.getRelativeHumidityData();

            // Write relative humidity list header
            fos.write(rhHeader.getBytes());

            // Output relative humidity list
            if (rhList.size() > 0) {
                for (RelativeHumidityData rhData : rhList) {
                    String relativeHumidity =
                            rhData.getId() + "," +
                            rhData.getTimestamp() + "," +
                            rhData.getRelativeHumidity() + "," +
                            rhData.getName() + "," +
                            rhData.getAddress() + "," +
                            rhData.getLatitude() + "," +
                            rhData.getLatitude() + "\n";
                    fos.write(relativeHumidity.getBytes());
                }
            } else {
                fos.write("empty\n".getBytes());
            }

            rhList = null;

            ArrayList<AirPressureData> apList = goSenseDb.getAirPressureData();

            // Write air pressure list header
            fos.write(apHeader.getBytes());

            // Output air pressure list
            if (apList.size() > 0) {
                for (AirPressureData apData : apList) {
                    String airPressure =
                            apData.getId() + "," +
                            apData.getTimestamp() + "," +
                            apData.getAirPressure() + "," +
                            apData.getName() + "," +
                            apData.getAddress() + "," +
                            apData.getLatitude() + "," +
                            apData.getLatitude() + "\n";
                    fos.write(airPressure.getBytes());
                }
            } else {
                fos.write("empty\n".getBytes());
            }

            apList = null;

            done = true;
        } catch (FileNotFoundException e) {
            failed = true;
            stopProgress();
            showToast(getString(R.string.export_data_error) + " [FileNotFoundException]", Toast.LENGTH_LONG);
            e.printStackTrace();
        } catch (SecurityException e) {
            failed = true;
            stopProgress();
            showToast(getString(R.string.export_data_error) + " [SecurityException]", Toast.LENGTH_LONG);
            e.printStackTrace();
        } catch (IOException e) {
            failed = true;
            stopProgress();
            showToast(getString(R.string.export_data_error) + " [IOException]", Toast.LENGTH_LONG);
            e.printStackTrace();
        }

        if (fos != null) {
            try {
                fos.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                fos.finish();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        stopProgress();

        if (failed) {
            Log.i(TAG, "Failed at exporting data");
            exportDataFile.delete();
        } else {
            showToast(getString(R.string.finished_exporting_data), Toast.LENGTH_LONG);
            Log.i(TAG, "Finished exporting data");
        }

        goSenseDb.close();

        exporting = false;

        return done;
    }

    private void showToast(String msg, int duration) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getActivity(), msg, duration).show();
            }
        });
    }

    private void startDialogProgress(String message) {
        stopProgress();
        requireActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mFileIoDialog = new AlertDialog.Builder(mContext).setMessage(message).setCancelable(true).show();
                TextView messageText = mFileIoDialog.findViewById(android.R.id.message);
                messageText.setGravity(Gravity.CENTER);
                messageText.setAllCaps(true);
                mFileIoDialog.show();
            }
        });
    }

    private void stopProgress() {
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

    ActivityResultLauncher<Intent> fileaccessActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    Intent goSense = new Intent(requireActivity(), SplashScreenActivity.class);
                    startActivity(goSense);
                    requireActivity().finish();
                    requireActivity().finishAffinity();
                }
            });

    public boolean checkRequirements() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                new AlertDialog.Builder(mContext).
                        setTitle(R.string.title_file_permission).
                        setMessage(R.string.text_file_permission).
                        setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //request for the permission
                                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                                Uri uri = Uri.fromParts("package", mContext.getPackageName(), null);
                                intent.setData(uri);
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    fileaccessActivityResultLauncher.launch(intent);
                                } else {
                                    startActivityForResult(intent, REQUEST_MANAGE_FILES);
                                }
                            }
                        }).
                        setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                requireActivity().finish();
                                requireActivity().finishAffinity();
                            }
                        }).
                        create().
                        show();
                return false;
            }
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            boolean canWriteMedia = ContextCompat.checkSelfPermission(mContext, WRITE_EXTERNAL_STORAGE) ==
                    PackageManager.PERMISSION_GRANTED;
            if (!canWriteMedia) {
                requestMediaWritePermission(new String[]{ WRITE_EXTERNAL_STORAGE });
                return false;
            }
        }

        return true;
    }

    private void requestMediaWritePermission(String[] permissions) {
        if (false) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), permissions[0])) {
                new androidx.appcompat.app.AlertDialog.Builder(requireActivity()).
                        setTitle(R.string.title_notify_permission).
                        setMessage(R.string.text_notify_permission).
                        setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ActivityCompat.requestPermissions(requireActivity(), permissions, REQUEST_WRITE_MEDIA_PERMISSION);
                                dialog.dismiss();
                            }
                        }).
                        create().
                        show();
            } else {
                ActivityCompat.requestPermissions(requireActivity(), permissions, REQUEST_WRITE_MEDIA_PERMISSION);
            }
        }
        ActivityCompat.requestPermissions(requireActivity(), permissions, REQUEST_WRITE_MEDIA_PERMISSION);
    }
}

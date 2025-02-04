package com.draekko.gosense.misc;

import android.content.Context;
import android.util.Log;

import com.draekko.gosense.BuildConfig;
import com.draekko.gosense.app.GoSenseAppContext;
import com.draekko.gosense.data.SensorDevice;
import com.draekko.gosense.data.SensorPairListData;
import com.draekko.tray.TrayPreferences;

public class TraySettings {

    public static final String TAG = "TraySettings";

    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_MODULE_NAME = BuildConfig.APPLICATION_ID + "_preferences";

    public static final int DEVICE_SCAN_SORT_ORDER_RSSI = 1;
    public static final int DEVICE_SCAN_SORT_ORDER_TIME = 2;

    public static class DataStoragePreference extends TrayPreferences {
        public DataStoragePreference(final Context context) {
            //super(context, context.getPackageName() + "_preferences", DATABASE_VERSION);
            super(context, DATABASE_MODULE_NAME, DATABASE_VERSION);
        }

        @Override
        protected void onCreate(final int initialVersion) {
            super.onCreate(initialVersion);
        }

        @Override
        protected void onUpgrade(final int oldVersion, final int newVersion) {
            super.onUpgrade(oldVersion, newVersion);
        }

        @Override
        protected void onDowngrade(final int oldVersion, final int newVersion) {
            super.onDowngrade(oldVersion, newVersion);
        }

        public TrayPreferences getPreferences() {
            return this;
        }
    }

    public static final int AIR_PRESSURE_MBAR = 1;
    public static final int AIR_PRESSURE_HPA = 2;
    public static final int AIR_PRESSURE_KPA = 3;
    public static final int AIR_PRESSURE_PSI = 4;

    public static final String PREF_KEY_IS_IMPERIAL_TEMP = "PREF_KEY_IS_IMPERIAL_TEMP";
    public static final String PREF_KEY_IS_WINTER_SEASON = "PREF_KEY_IS_WINTER_SEASON";
    public static final String PREF_KEY_SELECTED_ADDRESS = "PREF_KEY_SELECTED_ADDRESS";
    public static final String PREF_KEY_ACTIVE_SENSOR_DEVICE = "PREF_KEY_ACTIVE_SENSOR_DEVICE";
    public static final String PREF_KEY_AIR_PRESSURE_CHECK = "PREF_KEY_AIR_PRESSURE_CHECK";
    public static final String PREF_KEY_HAS_AIR_PRESSURE = "PREF_KEY_HAS_AIR_PRESSURE";
    public static final String PREF_KEY_FIRST_CHECK_PRESSURE_SENSOR = "PREF_KEY_FIRST_CHECK_PRESSURE_SENSOR";
    public static final String PREF_KEY_AIR_PRESSURE_UNIT = "PREF_KEY_AIR_PRESSURE_UNIT";
    public static final String PREF_KEY_AUTOCONNECT_BT_ON_DISCONNECT = "PREF_KEY_AUTOCONNECT_BT_ON_DISCONNECT";
    public static final String PREF_KEY_AUTOCONNECT_BT_ON_START = "PREF_KEY_AUTOCONNECT_BT_ON_START";
    public static final String PREF_KEY_KEEP_SCREEN_ON = "PREF_KEY_KEEP_SCREEN_ON";
    public static final String PREF_KEY_SCAN_SORT_ORDER = "PREF_KEY_SCAN_SORT_ORDER";
    public static final String PREF_KEY_USE_NEW_CONNECTION_AS_PRIMARY = "PREF_KEY_USE_NEW_CONNECTION_AS_PRIMARY";
    public static final String PREF_KEY_USE_LOCATON = "PREF_KEY_USE_LOCATON";
    public static final String PREF_KEY_CLEAR_DATABASE = "PREF_KEY_CLEAR_DATABASE";
    public static final String PREF_KEY_DEVELOPER = "PREF_KEY_DEVELOPER";
    public static final String PREF_KEY_PREFERENCE_SCREEN = "PREF_KEY_PREFERENCE_SCREEN";
    public static final String PREF_KEY_BT_EXPORT_DATABASE = "PREF_KEY_BT_EXPORT_DATABASE";
    public static final String PREF_KEY_BT_IMPORT_DATABASE = "PREF_KEY_BT_IMPORT_DATABASE";
    public static final String PREF_KEY_GRAPH_UPDATE_INTERVAL = "PREF_KEY_GRAPH_UPDATE_INTERVAL";
    public static final String PREF_KEY_AVERAGE_GRAPH_DATA = "PREF_KEY_AVERAGE_GRAPH_DATA";
    public static final String PREF_KEY_DATA_RETENTION_INTERVAL = "PREF_KEY_DATA_RETENTION_INTERVAL";
    public static final String PREF_KEY_DO_AUTO_SCALE = "PREF_KEY_DO_AUTO_SCALE";

    public static boolean putData(Object data, String key) {
        Context applicationContext = GoSenseAppContext.getInstance().getApplicationContext();
        if (applicationContext == null) {
            return false;
        }
        DataStoragePreference dataStoragePreference = new DataStoragePreference(applicationContext);

        if (key.equals(PREF_KEY_IS_IMPERIAL_TEMP)) {
            dataStoragePreference.put(key, (Boolean)data);
        } else if (key.equals(PREF_KEY_GRAPH_UPDATE_INTERVAL)) {
            dataStoragePreference.put(key, (Integer)data);
        } else if (key.equals(PREF_KEY_IS_WINTER_SEASON)) {
            dataStoragePreference.put(key, (Boolean)data);
        } else if (key.equals(PREF_KEY_FIRST_CHECK_PRESSURE_SENSOR)) {
            dataStoragePreference.put(key, (Boolean)data);
        } else if (key.equals(PREF_KEY_AVERAGE_GRAPH_DATA)) {
            dataStoragePreference.put(key, (Boolean)data);
        } else if (key.equals(PREF_KEY_KEEP_SCREEN_ON)) {
            dataStoragePreference.put(key, (Boolean)data);
        } else if (key.equals(PREF_KEY_DO_AUTO_SCALE)) {
            dataStoragePreference.put(key, (Boolean)data);
        } else if (key.equals(PREF_KEY_HAS_AIR_PRESSURE)) {
            dataStoragePreference.put(key, (Boolean)data);
        } else if (key.equals(PREF_KEY_USE_NEW_CONNECTION_AS_PRIMARY)) {
            dataStoragePreference.put(key, (Boolean)data);
        } else if (key.equals(PREF_KEY_AUTOCONNECT_BT_ON_DISCONNECT)) {
            dataStoragePreference.put(key, (Boolean)data);
        } else if (key.equals(PREF_KEY_AUTOCONNECT_BT_ON_START)) {
            dataStoragePreference.put(key, (Boolean)data);
        } else if (key.equals(PREF_KEY_USE_LOCATON)) {
            dataStoragePreference.put(key, (Boolean)data);
        } else if (key.equals(PREF_KEY_AIR_PRESSURE_UNIT)) {
            dataStoragePreference.put(key, (String)data);
        } else if (key.equals(PREF_KEY_DATA_RETENTION_INTERVAL)) {
            dataStoragePreference.put(key, (String)data);
        } else if (key.equals(PREF_KEY_SCAN_SORT_ORDER)) {
            dataStoragePreference.put(key, String.valueOf(data));
        } else if (key.equals(PREF_KEY_SELECTED_ADDRESS)) {
            dataStoragePreference.put(key, (String) data);
        } else if (key.equals(PREF_KEY_ACTIVE_SENSOR_DEVICE)) {
            if (data  instanceof  SensorDevice) {
                dataStoragePreference.put(key,
                        Utils.convertSensorDeviceToString((SensorDevice) data));
            } else if (data  instanceof  SensorDevice) {
                dataStoragePreference.put(key,
                        Utils.convertSensorPairedDeviceToString((SensorPairListData) data));
            } else {
                Log.e(TAG, "wrong device instance, cannot save data");
            }
        } else {
            return false;
        }
        return true;
    }

    public static Object getData(String key) {
        Context applicationContext = GoSenseAppContext.getInstance().getApplicationContext();
        //if (applicationContext == null) {
        //    return 0;
        //}

        DataStoragePreference dataStoragePreference = new DataStoragePreference(applicationContext);

        if (key.equals(PREF_KEY_IS_IMPERIAL_TEMP)) {
            return dataStoragePreference.getBoolean(key, false);
        } else if (key.equals(PREF_KEY_GRAPH_UPDATE_INTERVAL)) {
            return dataStoragePreference.getInt(key, 1);
        } else if (key.equals(PREF_KEY_IS_WINTER_SEASON)) {
            return dataStoragePreference.getBoolean(key, false);
        } else if (key.equals(PREF_KEY_FIRST_CHECK_PRESSURE_SENSOR)) {
            return dataStoragePreference.getBoolean(key, true);
        } else if (key.equals(PREF_KEY_KEEP_SCREEN_ON)) {
            return dataStoragePreference.getBoolean(key, true);
        } else if (key.equals(PREF_KEY_DO_AUTO_SCALE)) {
            return dataStoragePreference.getBoolean(key, true);
        } else if (key.equals(PREF_KEY_AUTOCONNECT_BT_ON_DISCONNECT)) {
            return dataStoragePreference.getBoolean(key, true);
        } else if (key.equals(PREF_KEY_AUTOCONNECT_BT_ON_START)) {
            return dataStoragePreference.getBoolean(key, false);
        } else if (key.equals(PREF_KEY_AVERAGE_GRAPH_DATA)) {
            return dataStoragePreference.getBoolean(key, false);
        } else if (key.equals(PREF_KEY_USE_LOCATON)) {
            return dataStoragePreference.getBoolean(key, false);
        } else if (key.equals(PREF_KEY_USE_NEW_CONNECTION_AS_PRIMARY)) {
            return dataStoragePreference.getBoolean(key, true);
        } else if (key.equals(PREF_KEY_AIR_PRESSURE_UNIT)) {
            return dataStoragePreference.getString(key, "1");
        } else if (key.equals(PREF_KEY_DATA_RETENTION_INTERVAL)) {
            return dataStoragePreference.getString(key, "1");
        } else if (key.equals(PREF_KEY_SCAN_SORT_ORDER)) {
            String data =  dataStoragePreference.getString(key, "1");
            return Integer.valueOf(data);
        } else if (key.equals(PREF_KEY_HAS_AIR_PRESSURE)) {
            return dataStoragePreference.getBoolean(key, false);
        } else if (key.equals(PREF_KEY_SELECTED_ADDRESS)) {
            return dataStoragePreference.getString(key, null);
        } else if (key.equals(PREF_KEY_ACTIVE_SENSOR_DEVICE)) {
            String device = dataStoragePreference.getString(key, "");
            if (device == null || device.isEmpty()) {
                return null;
            }
            try {
                return Utils.convertStringToSensorDevice(device);
            } catch (Exception e) {
                return null;
            }
        }

        Log.d(TAG, "Unhandle key : " + key);

        return null;
    }

    // GET DATA

    public static boolean isTemperatureUnitFahrenheit() {
        return (Boolean)getData(PREF_KEY_IS_IMPERIAL_TEMP);
    }

    public static boolean isSeasonWinter() {
        return (Boolean)getData(PREF_KEY_IS_WINTER_SEASON);
    }

    public static String getSelectedAddress() {
        return (String)getData(PREF_KEY_SELECTED_ADDRESS);
    }

    public static int getGraphUpdateInterval() {
        return (int)getData(PREF_KEY_GRAPH_UPDATE_INTERVAL);
    }

    public static SensorDevice getActiveSensorDevice() {
        return (SensorDevice)getData(PREF_KEY_ACTIVE_SENSOR_DEVICE);
    }

    public static boolean getHasBarometer() {
        return (Boolean)getData(PREF_KEY_HAS_AIR_PRESSURE);
    }

    public static boolean getPressureSensorFirstCheck() {
        return (Boolean)getData(PREF_KEY_FIRST_CHECK_PRESSURE_SENSOR);
    }

    public static int getAirPressureUnit() {
        String data = (String) getData(PREF_KEY_AIR_PRESSURE_UNIT);
        if (data == null || data.isEmpty()) {
            return 1;
        }
        Integer value = Integer.valueOf(data);
        if (value == null) {
            return 1;
        }
        return value;
    }

    public static boolean getUseAverageGraph() {
        return (Boolean)getData(PREF_KEY_AVERAGE_GRAPH_DATA);
    }

    public static boolean getKeepScreenOn() {
        return (Boolean)getData(PREF_KEY_KEEP_SCREEN_ON);
    }

    public static boolean getAutoConnectDevicesOnDisconnect() {
        return (Boolean)getData(PREF_KEY_AUTOCONNECT_BT_ON_DISCONNECT);
    }

    public static boolean getAutoConnectDevicesOnStart() {
        return (Boolean)getData(PREF_KEY_AUTOCONNECT_BT_ON_START);
    }

    public static int getScanSortPriority() {
        return (Integer)getData(PREF_KEY_SCAN_SORT_ORDER);
    }

    public static boolean getUseNewConnectionAsPrimary() {
        return (Boolean)getData(PREF_KEY_USE_NEW_CONNECTION_AS_PRIMARY);
    }

    public static boolean getUseLocation() {
        return (Boolean)getData(PREF_KEY_USE_LOCATON);
    }

    public static boolean getDoAutoScale() {
        return (Boolean)getData(PREF_KEY_DO_AUTO_SCALE);
    }

    public static int getDataRetentionIntervalCode() {
        String data = (String) getData(PREF_KEY_DATA_RETENTION_INTERVAL);
        if (data == null || data.isEmpty()) {
            return 1;
        }
        Integer value = Integer.valueOf(data);
        if (value == null) {
            return 1;
        }
        return value;
    }

    // PUT DATA

    public static void setHasBarometer(boolean value) {
        putData(value, PREF_KEY_HAS_AIR_PRESSURE);
    }

    public static void setPressureSensorFirstCheck(boolean value) {
        putData(value, PREF_KEY_FIRST_CHECK_PRESSURE_SENSOR);
    }

    public static void setActiveSensorDevice(SensorDevice sensorDevice) {
        putData(sensorDevice, PREF_KEY_ACTIVE_SENSOR_DEVICE);
    }

    public static void setActiveSensorPairedDevice(SensorPairListData sensorDevice) {
        putData(sensorDevice, PREF_KEY_ACTIVE_SENSOR_DEVICE);
    }

    public static void setAutoConnectDevicesOnDisconnect(boolean value) {
        putData(value, PREF_KEY_AUTOCONNECT_BT_ON_DISCONNECT);
    }

    public static void setAutoConnectDevicesOnStart(boolean value) {
        putData(value, PREF_KEY_AUTOCONNECT_BT_ON_START);
    }

    public static void setIsTemperatureUnitFahrenheit(boolean value) {
        putData(value, PREF_KEY_IS_IMPERIAL_TEMP);
    }

    public static void setAirPressureUnit(int value) {
        putData(String.valueOf(value), PREF_KEY_AIR_PRESSURE_UNIT);
    }

    public static boolean deleteAll() {
        Context applicationContext = GoSenseAppContext.getInstance().getApplicationContext();
        if (applicationContext == null) {
            return false;
        }
        DataStoragePreference dataStoragePreference = new DataStoragePreference(applicationContext);
        return dataStoragePreference.clear();
    }

    public static TrayPreferences getPreferences() {
        Context applicationContext = GoSenseAppContext.getInstance().getApplicationContext();
        if (applicationContext == null) {
            return null;
        }
        DataStoragePreference dataStoragePreference = new DataStoragePreference(applicationContext);
        return dataStoragePreference.getPreferences();
    }
}

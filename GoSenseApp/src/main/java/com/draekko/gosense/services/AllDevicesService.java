/*
 * GoSense Android App
 * Copyright (C) 2025 Draekko
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.draekko.gosense.services;

import static android.app.PendingIntent.FLAG_MUTABLE;
import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import static androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC;
import static com.draekko.gosense.misc.Utils.PSI_VALUE_ONE_MILLIBAR;
import static com.draekko.gosense.misc.Utils.getBtState;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.draekko.gosense.R;
import com.draekko.gosense.activities.GoSensePagerActivity;
import com.draekko.gosense.data.AirPressureData;
import com.draekko.gosense.data.RelativeHumidityData;
import com.draekko.gosense.data.RemoteRelativeHumidityMeasurement;
import com.draekko.gosense.data.RemoteTemperatureMeasurement;
import com.draekko.gosense.data.SensorDevice;
import com.draekko.gosense.data.TemperatureData;
import com.draekko.gosense.database.GoSenseDb;
import com.draekko.gosense.misc.Converter;
import com.draekko.gosense.misc.TraySettings;
import com.draekko.gosense.services.AllServicesDeviceDataHandler.OnBluetoothDeviceDiscovery;
import com.draekko.gosense.misc.Utils;

import com.halfhp.fig.BuildConfig;
import com.welie.blessed.BluetoothCentralManager;
import com.welie.blessed.BluetoothCentralManagerCallback;
import com.welie.blessed.BluetoothPeripheral;
import com.welie.blessed.ConnectionState;
import com.welie.blessed.HciStatus;
import com.welie.blessed.ScanFailure;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.os.BuildCompat;

import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class AllDevicesService extends Service
        implements SensorEventListener {

    private static final String TAG = "AllDevicesService";

    private static final String CHANNEL_ID_DEBUG = "gosense_service_notify_debug";
    private static final String CHANNEL_ID_RELEASE = "gosense_service_notify_release";

    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 1;
    private static final long MIN_TIME_BW_UPDATES = 1000 * 5;
    private static final int ID_PROCESS_RELEASE = 222777;
    private static final int ID_PROCESS_DEBUG = 333888;

    private DecimalFormat decimalFormat = new DecimalFormat("###0.0");
    private DecimalFormat decimalFormatStorage = new DecimalFormat("###0.0#");
    private DecimalFormat decimalPsiFormat = new DecimalFormat("###0.0##");

    /*
    public class DummyNotification extends ActivityCompat {
    }
    */

    public interface OnPressureSensorChange {
        public void airPressure(float airpressure);
    }

    public interface OnAirPressureSensorChange {
        public void airPressure(String address, long time, float airpressure);
    }

    public interface OnTemperatureSensorChange {
        public void temperature(String address, long time, float temperature);
    }

    public interface OnRelativeHumiditySensorChange {
        public void relativeHumidity(String address, long time, float relativehumidity);
    }

    public interface OnHandlerPressureSensorChange {
        public void airPressure(float airpressure, long timestamp);
    }

    private static ArrayList<OnAirPressureSensorChange> mOnAirPressureSensorChange = new ArrayList<>();
    private static ArrayList<OnTemperatureSensorChange> mOnTemperatureSensorChange = new ArrayList<>();
    private static ArrayList<OnRelativeHumiditySensorChange> mOnRelativeHumiditySensorChange = new ArrayList<>();
    private NotificationManager notificationManager;
    public IBinder mBinder = new LocalBinder();
    private OnPressureSensorChange mOnPressureSensorChange;
    private OnHandlerPressureSensorChange mOnHandlerPressureSensorChange;
    private boolean debug = false;
    private Context mContext;
    private GoSenseDb goSenseDb;
    private String connectedName;
    private String connectedAddress;
    private long timestamp;
    private long lastTimestamp;
    private PowerManager powerManager;
    private PowerManager.WakeLock wakeLock;
    private SensorManager sensorManager;
    private AllServicesDeviceDataHandler btDataHandler;
    private Notification notification1;
    private Notification notification2;
    private Handler toastHandler;
    private LocationListener locationListener = null;
    private boolean useLocation = true;
    private LocationManager locationManager;
    private boolean checkGPS;
    private boolean checkNetwork;
    private boolean checkFused;
    private boolean checkPassive;
    private Location currentLocation;
    private float mTemperature;
    private float mHumidity;
    private float mAirpressure;
    private int mBattery;
    private boolean connected = false;
    private boolean exiting = false;
    private BluetoothCentralManagerCallback mBluetoothCentralManagerCallback;

    private BluetoothCentralManagerCallback mDataHandlerBluetoothCentralManagerCallback = new BluetoothCentralManagerCallback() {
        @Override
        public void onBluetoothAdapterStateChanged(int state) {
            super.onBluetoothAdapterStateChanged(state);
            if (mBluetoothCentralManagerCallback != null) {
                mBluetoothCentralManagerCallback.onBluetoothAdapterStateChanged(state);
            }
            Log.i(TAG, String.format("Bluetooth Adapter State Changed :  %s", getBtState(state)));
        }

        @Override
        public void onScanFailed(@NotNull ScanFailure scanFailure) {
            super.onScanFailed(scanFailure);
            if (mBluetoothCentralManagerCallback != null) {
                mBluetoothCentralManagerCallback.onScanFailed(scanFailure);
            }
            Log.e(TAG, "Scan Failed : " + scanFailure);
        }

        @Override
        public void onDiscoveredPeripheral(@NotNull BluetoothPeripheral peripheral, @NotNull ScanResult scanResult) {
            super.onDiscoveredPeripheral(peripheral, scanResult);
            if (mBluetoothCentralManagerCallback != null) {
                mBluetoothCentralManagerCallback.onDiscoveredPeripheral(peripheral, scanResult);
            }
        }

        @Override
        public void onDisconnectedPeripheral(@NotNull BluetoothPeripheral peripheral, @NotNull HciStatus status) {
            updateNotification(peripheral, 0, -9999, -9999, -9999, false);
            cancelNotifications();

            connected = false;
            String deviceName = peripheral.getName();
            if (deviceName == null || deviceName.isEmpty()) {
                deviceName = Utils.getNameFromPairedDb(mContext, peripheral.getAddress());
            }
            if (deviceName == null || deviceName.isEmpty()) {
                deviceName = getString(R.string.empty_bluetooth_device);
            }
            Log.w(TAG, "DISCONNECTED : " + deviceName + " " + peripheral.getAddress() + " STATE : " + status);
            updateEndStartDataConnection(deviceName, peripheral.getAddress());
            connectedName = null;
            connectedAddress = null;

            super.onDisconnectedPeripheral(peripheral, status);

            if (mBluetoothCentralManagerCallback != null) {
                mBluetoothCentralManagerCallback.onDisconnectedPeripheral(peripheral, status);
            }
        }

        @Override
        public void onDisconnectingPeripheral(@NotNull BluetoothPeripheral peripheral) {
            super.onDisconnectingPeripheral(peripheral);
            String deviceName = peripheral.getName();
            if (deviceName == null || deviceName.isEmpty()) {
                deviceName = Utils.getNameFromPairedDb(mContext, peripheral.getAddress());
            }
            if (deviceName == null || deviceName.isEmpty()) {
                deviceName = getString(R.string.empty_bluetooth_device);
            }
            Log.i(TAG, "DISCONNECTING : " + deviceName + " " + peripheral.getAddress());
            if (mBluetoothCentralManagerCallback != null) {
                mBluetoothCentralManagerCallback.onDisconnectingPeripheral(peripheral);
            }
        }

        @Override
        public void onConnectionFailed(@NotNull BluetoothPeripheral peripheral, @NotNull HciStatus status) {
            connected = false;
            String deviceName = peripheral.getName();
            if (deviceName == null || deviceName.isEmpty()) {
                deviceName = Utils.getNameFromPairedDb(mContext, peripheral.getAddress());
            }
            if (deviceName == null || deviceName.isEmpty()) {
                deviceName = getString(R.string.empty_bluetooth_device);
            }
            Log.e(TAG, "CONNECTION FAILED : " + deviceName + " " + peripheral.getAddress() + " STATE : " + status);
            updateEndStartDataConnection(deviceName, peripheral.getAddress());
            connectedName = null;
            connectedAddress = null;
            super.onConnectionFailed(peripheral, status);
            if (mBluetoothCentralManagerCallback != null) {
                mBluetoothCentralManagerCallback.onConnectionFailed(peripheral, status);
            }
        }

        @Override
        public void onConnectedPeripheral(@NotNull BluetoothPeripheral peripheral) {
            connected = true;
            connectedName = peripheral.getName();
            if (connectedName == null || connectedName.isEmpty()) {
                connectedName = Utils.getNameFromPairedDb(mContext, peripheral.getAddress());
            }
            if (connectedName == null || connectedName.isEmpty()) {
                connectedName = getString(R.string.empty_bluetooth_device);
            }
            Log.i(TAG, "CONNECTED : " + connectedName + " " + peripheral.getAddress());
            connectedAddress = peripheral.getAddress();;
            updateEndStartDataConnection(connectedName, connectedAddress);
            super.onConnectedPeripheral(peripheral);
            if (mBluetoothCentralManagerCallback != null) {
                mBluetoothCentralManagerCallback.onConnectedPeripheral(peripheral);
            }
            readRemoteBattery();
        }

        @Override
        public void onConnectingPeripheral(@NotNull BluetoothPeripheral peripheral) {
            super.onConnectingPeripheral(peripheral);
            String deviceName = peripheral.getName();
            if (deviceName == null || deviceName.isEmpty()) {
                deviceName = Utils.getNameFromPairedDb(mContext, peripheral.getAddress());
            }
            if (deviceName == null || deviceName.isEmpty()) {
                deviceName = mContext.getString(R.string.empty_bluetooth_device);
            }
            Log.i(TAG, "CONNECTING : " + deviceName + " " + peripheral.getAddress());
            if (mBluetoothCentralManagerCallback != null) {
                mBluetoothCentralManagerCallback.onConnectingPeripheral(peripheral);
            }
        }
    };

    private final BroadcastReceiver locationServiceStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null && action.equals(LocationManager.MODE_CHANGED_ACTION)) {
                useLocation = locationManager.isLocationEnabled();
                Log.i(TAG, String.format("Location service state changed to: %s", useLocation ? "on" : "off"));
            }
        }
    };

    AllServicesDeviceDataHandler.OnReadRemoteBatteryLevel mOnReadRemoteBatteryLevel = new AllServicesDeviceDataHandler.OnReadRemoteBatteryLevel() {
        @Override
        public void remoteBatteryLevel(@NotNull BluetoothPeripheral peripheral, int batterylevel) {
            mBattery = batterylevel;
        }
    };

    private final BroadcastReceiver remoteTemperatureDataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            BluetoothPeripheral peripheral = getPeripheral(intent.getStringExtra(AllServicesDeviceDataHandler.MEASUREMENT_EXTRA_PERIPHERAL));
            RemoteTemperatureMeasurement measurement = (RemoteTemperatureMeasurement) intent.getSerializableExtra(AllServicesDeviceDataHandler.MEASUREMENT_REMOTE_TEMPERATURE_EXTRA);
            if (measurement == null) {
                return;
            }

            mTemperature = measurement.temperatureValue;
            if (false)  Log.i(TAG, "Temperature = " + mTemperature);

            long logTime = System.currentTimeMillis();
            TemperatureData temperatureData = new TemperatureData();
            temperatureData.setTimestamp(logTime);

            if (TraySettings.getUseLocation() && locationManager.isLocationEnabled() && currentLocation != null) {
                temperatureData.setLatitude(currentLocation.getLatitude());
                temperatureData.setLongitude(currentLocation.getLongitude());
            } else {
                temperatureData.setLatitude(-9999);
                temperatureData.setLongitude(-9999);
            }
            temperatureData.setTemperature(mTemperature);
            temperatureData.setAddress(peripheral.getAddress());

            if (addressToNameCache.containsKey(peripheral.getAddress())) {
                temperatureData.setAddress(peripheral.getAddress());
                temperatureData.setName(addressToNameCache.get(peripheral.getAddress()));
            } else {
                SensorDevice sensorDevice = goSenseDb.getPairedSensorDevice(peripheral.getAddress());
                if (sensorDevice != null) {
                    temperatureData.setName(sensorDevice.getName());
                    addressToNameCache.put(peripheral.getAddress(), sensorDevice.getName());
                } else {
                    temperatureData.setName("BT-DEVICE");
                    addressToNameCache.put(peripheral.getAddress(), "BT-DEVICE");
                }
            }

            goSenseDb.saveTemperature(temperatureData);

            if (mOnTemperatureSensorChange.size() > 0) {
                for (OnTemperatureSensorChange temperatureSensorChange : mOnTemperatureSensorChange) {
                    temperatureSensorChange.temperature(peripheral.getAddress(), logTime, mTemperature);
                }
            }

            updateNotification(peripheral, mBattery, mTemperature, mHumidity, mAirpressure, TraySettings.getHasBarometer());
        }
    };

    HashMap<String, String > addressToNameCache = new HashMap();
    private final BroadcastReceiver remoteRelativeHumidityDataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            BluetoothPeripheral peripheral = getPeripheral(intent.getStringExtra(AllServicesDeviceDataHandler.MEASUREMENT_EXTRA_PERIPHERAL));
            RemoteRelativeHumidityMeasurement measurement = (RemoteRelativeHumidityMeasurement) intent.getSerializableExtra(AllServicesDeviceDataHandler.MEASUREMENT_REMOTE_RELATIVE_HUMIDITY_EXTRA);
            if (measurement == null) {
                return;
            }

            mHumidity = measurement.humidityValue;
            if (false)  Log.i(TAG, "Temperature = " + mTemperature);

            long logTime = System.currentTimeMillis();
            RelativeHumidityData humidityData = new RelativeHumidityData();
            humidityData.setTimestamp(logTime);
            if (TraySettings.getUseLocation() && locationManager.isLocationEnabled() && currentLocation != null) {
                humidityData.setLatitude(currentLocation.getLatitude());
                humidityData.setLongitude(currentLocation.getLongitude());
            } else {
                humidityData.setLatitude(-9999);
                humidityData.setLongitude(-9999);
            }
            humidityData.setRelativeHumidity(mHumidity);
            humidityData.setAddress(peripheral.getAddress());

            if (addressToNameCache.containsKey(peripheral.getAddress())) {
                humidityData.setAddress(peripheral.getAddress());
                humidityData.setName(addressToNameCache.get(peripheral.getAddress()));
            } else {
                SensorDevice sensorDevice = goSenseDb.getPairedSensorDevice(peripheral.getAddress());
                if (sensorDevice != null) {
                    humidityData.setName(sensorDevice.getName());
                    addressToNameCache.put(peripheral.getAddress(), sensorDevice.getName());
                } else {
                    humidityData.setName("BT-DEVICE");
                    addressToNameCache.put(peripheral.getAddress(), "BT-DEVICE");
                }
            }

            goSenseDb.saveRelativeHumidity(humidityData);

            if (mOnRelativeHumiditySensorChange.size() > 0) {
                for (OnRelativeHumiditySensorChange relativeHumiditySensorChange : mOnRelativeHumiditySensorChange) {
                    relativeHumiditySensorChange.relativeHumidity(peripheral.getAddress(), logTime, mHumidity);
                }
            }

            updateNotification(peripheral, mBattery, mTemperature, mHumidity, mAirpressure, TraySettings.getHasBarometer());
        }
    };

    private Runnable queryBattery = new Runnable() {
        @Override
        public void run() {
            readRemoteBattery();
        }
    };

    public class LocalBinder extends Binder {
        public LocalBinder() {
        }

        public AllDevicesService getService() {
            return AllDevicesService.this;
        }
    }

    @SuppressLint("WrongConstant")
    private void handleBind() {

        ContextCompat.registerReceiver(mContext, remoteTemperatureDataReceiver,
                new IntentFilter(AllServicesDeviceDataHandler.MEASUREMENT_REMOTE_TEMPERATURE),
                ContextCompat.RECEIVER_NOT_EXPORTED);
        ContextCompat.registerReceiver(mContext, remoteRelativeHumidityDataReceiver,
                new IntentFilter(AllServicesDeviceDataHandler.MEASUREMENT_REMOTE_RELATIVE_HUMIDITY),
                ContextCompat.RECEIVER_NOT_EXPORTED);

        if (goSenseDb == null) {
            goSenseDb = new GoSenseDb(mContext);
        }
        if (wakeLock == null || !wakeLock.isHeld()) {
            if (wakeLock == null) {
                wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "GOSENSE::AirPressureWakelockTag");
                wakeLock.acquire();
            } else {
                if (!wakeLock.isHeld()) {
                    wakeLock.acquire();
                }
            }
        }
        if (hasBarometer()) {
            if (mContext != null) {
                sensorManager = (SensorManager) getSystemService(Service.SENSOR_SERVICE);
                sensorManager.registerListener(this,
                        sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE),
                        SensorManager.SENSOR_DELAY_NORMAL);
            }
            updateEndStartApDataConnection(
                    Utils.DEFAULT_HW_AP_DEVICE_NAME,
                    Utils.DEFAULT_EMPTY_ADDRESS);
        }
    }

    private void handleUnbind() {
        if (hasBarometer()) {
            if (sensorManager != null) {
                sensorManager.unregisterListener(this);
            }
            updateEndStartApDataConnection(
                    Utils.DEFAULT_HW_AP_DEVICE_NAME,
                    Utils.DEFAULT_EMPTY_ADDRESS);
        }
        unregisterReceiver(remoteTemperatureDataReceiver);
        unregisterReceiver(remoteRelativeHumidityDataReceiver);
        if (goSenseDb != null) {
            goSenseDb.close();
            goSenseDb = null;
        }
        if (wakeLock != null) {
            if (wakeLock.isHeld()) {
                wakeLock.release();
                wakeLock = null;
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        handleBind();
        return this.mBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        handleBind();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        handleUnbind();
        return super.onUnbind(intent);
    }

    @Override
    @SuppressLint({"WrongConstant", "MissingPermission"})
    public void onCreate() {
        mContext = this;

        toastHandler = new Handler(Looper.getMainLooper());

        powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        btDataHandler = AllServicesDeviceDataHandler.getInstance(mContext);
        btDataHandler.registerOnReadRemoteBatteryLevel(mOnReadRemoteBatteryLevel);
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);

        goSenseDb = new GoSenseDb(this);

        lastTimestamp = System.currentTimeMillis();
        btDataHandler.setBluetoothCentralManagerCallback(mDataHandlerBluetoothCentralManagerCallback);

        if (useLocation) {
            checkFused = locationManager.isProviderEnabled(LocationManager.FUSED_PROVIDER);
            checkGPS = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            checkNetwork = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            checkPassive = locationManager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER);
            if (!checkGPS && !checkNetwork) {
                Log.w(TAG, "No active location providers found");
                try {
                    removeLocationUpdates();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    if (checkGPS) {
                        handleGpsLocationListenerChange();
                    } else if (checkNetwork) {
                        handleNetLocationListenerChange();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (location != null) {
                    currentLocation = location;
                    Log.i(TAG, "[GPS] Last known location [" + location.getLatitude() + "," + location.getLongitude() + "]");
                } else {
                    Log.i(TAG, "[GPS] no previous location found");
                }
                if (location == null) {
                    location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    if (location != null) {
                        currentLocation = location;
                        Log.i(TAG, "[NET] Last known location [" + location.getLatitude() + "," + location.getLongitude() + "]");
                    } else {
                        Log.i(TAG, "[NET] no previous location found");
                    }
                }
                if (location == null && checkFused) {
                    location = locationManager.getLastKnownLocation(LocationManager.FUSED_PROVIDER);
                    if (location != null) {
                        currentLocation = location;
                        Log.i(TAG, "[Fused] Last known location [" + location.getLatitude() + "," + location.getLongitude() + "]");
                    } else {
                        Log.i(TAG, "[Fused] no previous location found");
                    }
                }
            }
        }

        ContextCompat.registerReceiver(mContext, remoteTemperatureDataReceiver,
                new IntentFilter(AllServicesDeviceDataHandler.MEASUREMENT_REMOTE_TEMPERATURE),
                ContextCompat.RECEIVER_NOT_EXPORTED);
        ContextCompat.registerReceiver(mContext, remoteRelativeHumidityDataReceiver,
                new IntentFilter(AllServicesDeviceDataHandler.MEASUREMENT_REMOTE_RELATIVE_HUMIDITY),
                ContextCompat.RECEIVER_NOT_EXPORTED);

        if (hasBarometer()) {
            if (mContext != null) {
                sensorManager = (SensorManager) getSystemService(Service.SENSOR_SERVICE);
                sensorManager.registerListener(this,
                        sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE),
                        SensorManager.SENSOR_DELAY_NORMAL);
                updateEndStartApDataConnection(
                        Utils.DEFAULT_HW_AP_DEVICE_NAME,
                        Utils.DEFAULT_EMPTY_ADDRESS);
            }
        }

        createNotificationChannel();
        startInBackground();
        Log.i(TAG, "onCreate: AllServicesDevice started");
    }

    private void initLocationListener() {
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                locationChanged(location);
            }
            @Override
            public void onProviderDisabled(String provider) {
                providerDisabled(provider);
            }
            @Override
            public void onProviderEnabled(String provider) {
                providerEnabled(provider);
            }
        };
    }

    @SuppressLint("MissingPermission")
    private void handleGpsLocationListenerChange() {
        initLocationListener();
        locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                MIN_TIME_BW_UPDATES,
                MIN_DISTANCE_CHANGE_FOR_UPDATES,
                locationListener);
    }

    @SuppressLint("MissingPermission")
    private void handleFusedLocationListenerChange() {
        initLocationListener();
        locationManager.requestLocationUpdates(
                LocationManager.FUSED_PROVIDER,
                MIN_TIME_BW_UPDATES,
                MIN_DISTANCE_CHANGE_FOR_UPDATES,
                locationListener);
    }

    @SuppressLint("MissingPermission")
    private void handleNetLocationListenerChange() {
        initLocationListener();
        locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                MIN_TIME_BW_UPDATES,
                MIN_DISTANCE_CHANGE_FOR_UPDATES,
                locationListener);
    }

    private void removeLocationUpdates() {
        if (locationListener != null) {
            locationManager.removeUpdates(locationListener);
            locationListener = null;
        }
    }

    @Override
    public void onDestroy() {
        if (connected) {
            connected = false;
            if (connectedName != null && connectedAddress != null) {
                updateEndStartDataConnection(connectedName, connectedAddress);
            }
        }

        if (hasBarometer()) {
            updateEndStartApDataConnection(
                    Utils.DEFAULT_HW_AP_DEVICE_NAME,
                    Utils.DEFAULT_EMPTY_ADDRESS);
        }

        try {
            unregisterReceiver(remoteTemperatureDataReceiver);
        } catch (Exception e) {
        }
        try {
            unregisterReceiver(remoteRelativeHumidityDataReceiver);
        } catch (Exception e) {
        }
        try {
            unregisterReceiver(locationServiceStateReceiver);
        } catch (Exception e) {
        }

        if (goSenseDb != null) {
            goSenseDb.close();
            goSenseDb = null;
        }
        stopForeground(true);
        if (wakeLock != null)
            wakeLock.release();
        super.onDestroy();
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        String action = intent.getAction();
        if (action != null && !action.isEmpty()) {
            if (action.contains(Utils.ACTION_AUTO_RECONNECT)) {
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        SensorDevice sensorDevice = TraySettings.getActiveSensorDevice();
                        if (sensorDevice != null &&
                                sensorDevice.getAddress() != null &&
                                !sensorDevice.getAddress().isEmpty()) {
                            Log.i(TAG, "Attempting auto reconnect to " +
                                    sensorDevice.getAliasName() + " " +
                                    sensorDevice.getAddress());
                            connect(sensorDevice.getAddress());
                        } else {
                            Log.e(TAG, "ERROR: auto reconnect not done, invalid address");
                        }
                    }
                }, 10000);
            }
        }
        return START_STICKY;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_PRESSURE) {
            timestamp = System.currentTimeMillis();
            if (timestamp >= lastTimestamp + 10000) {
                mAirpressure = event.values[0];

                long logTime = System.currentTimeMillis();
                AirPressureData airPressureData = new AirPressureData();
                airPressureData.setTimestamp(logTime);
                if (TraySettings.getUseLocation() && locationManager.isLocationEnabled() && currentLocation != null) {
                    airPressureData.setLatitude(currentLocation.getLatitude());
                    airPressureData.setLongitude(currentLocation.getLongitude());
                } else {
                    airPressureData.setLatitude(-9999);
                    airPressureData.setLongitude(-9999);
                }
                airPressureData.setAirPressure(mAirpressure);
                airPressureData.setAddress(Utils.DEFAULT_EMPTY_ADDRESS);
                airPressureData.setName(Build.BRAND.toUpperCase(Locale.US) + " " +
                        Build.MODEL.toUpperCase(Locale.US));

                goSenseDb.saveAirPressure(airPressureData);

                if (mOnAirPressureSensorChange.size() > 0) {
                    for (OnAirPressureSensorChange airPressureSensorChange : mOnAirPressureSensorChange) {
                        airPressureSensorChange.airPressure(getString(R.string.empty_bluetooth_mac_address), logTime, mAirpressure);
                    }
                }

                if (mOnPressureSensorChange != null) {
                    mOnPressureSensorChange.airPressure(mAirpressure);
                }
                if (mOnHandlerPressureSensorChange != null) {
                    mOnHandlerPressureSensorChange.airPressure(mAirpressure, timestamp);
                }
                lastTimestamp = timestamp;

                if (debug) {
                    Log.i("Baro", "Air Pressure " + mAirpressure);
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String chanid = CHANNEL_ID_RELEASE;
            if (BuildConfig.DEBUG) {
                chanid = CHANNEL_ID_DEBUG;
            }
            notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel notificationChannel =
                    new NotificationChannel(
                            chanid,
                            getString(R.string.device_channel_name) + "_1",
                            NotificationManager.IMPORTANCE_HIGH);
            notificationChannel.setDescription(getString(R.string.device_channel_description));
            notificationManager.createNotificationChannel(notificationChannel);
        }
    }

    public void startInBackground() {
        String chanid = CHANNEL_ID_RELEASE;
        if (BuildConfig.DEBUG) {
            chanid = CHANNEL_ID_DEBUG;
        }
        NotificationCompat.Builder autoCancel = new NotificationCompat.Builder(this, chanid).
                setSmallIcon(R.drawable.ic_notify_logo).
                setContentTitle(getString(R.string.data_handler_title)).
                setContentText(getString(R.string.data_handler_msg)).
                setPriority(NotificationCompat.PRIORITY_HIGH).
                setCategory(NotificationCompat.CATEGORY_WORKOUT).
                setVisibility(NotificationCompat.VISIBILITY_PUBLIC).
                setSilent(true).
                setVibrate(new long[] { 0 }).
                setSound(Settings.System.DEFAULT_NOTIFICATION_URI).
                setAutoCancel(false);
        Intent intent = new Intent(this, GoSensePagerActivity.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
        autoCancel.setContentIntent(PendingIntent.getActivity(mContext, 0x1111, intent, FLAG_MUTABLE));
        notification1 = autoCancel.build();
        notification1.flags = Notification.FLAG_NO_CLEAR;
    }

    @SuppressLint("RemoteViewLayout")
    public void updateNotification(BluetoothPeripheral peripheral, int battery, float temp, float humidity, float airpressure, boolean show_ap) {
        String chanid = CHANNEL_ID_RELEASE;
        int procid = ID_PROCESS_RELEASE;
        if (BuildConfig.DEBUG) {
            chanid = CHANNEL_ID_DEBUG;
            procid = ID_PROCESS_DEBUG;
        }

        int deviceStatus = R.string.bt_device_disconnected;
        if (peripheral.getState() == ConnectionState.CONNECTED) {
            deviceStatus = R.string.bt_device_connected;
        }

        String batteryText = "\u221e%";
        if (battery > 0) {
            batteryText = battery + "%";
        }

        String deviceName = peripheral.getName();
        if (deviceName == null || deviceName.isEmpty()) {
            deviceName = Utils.getNameFromPairedDb(mContext, peripheral.getAddress());
        }
        RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.notification_data);
        remoteViews.setTextViewText(R.id.notifyTitleTextView, deviceName);
        remoteViews.setTextViewText(R.id.notifyBatteryTextView, batteryText);

        if (show_ap) {
            remoteViews.setViewVisibility(R.id.notifyAirPressure, View.VISIBLE);
            String format = "mbar";
            String value = decimalFormat.format(airpressure);
            if (TraySettings.getAirPressureUnit() == 2) {
                format = "hPa";
            }
            if (TraySettings.getAirPressureUnit() == 3) {
                format = "kPa";
                value = decimalPsiFormat.format(airpressure / 10.0f);
            }
            if (TraySettings.getAirPressureUnit() == 4) {
                format = "Psi";
                value = decimalPsiFormat.format(airpressure * PSI_VALUE_ONE_MILLIBAR);
            }
            remoteViews.setTextViewText(R.id.notifyAirPressure, value + " " + format);
        } else {
            remoteViews.setViewVisibility(R.id.notifyAirPressure, View.GONE);
        }

        String tempUnit = getString(R.string.temperature_celsius);
        if(TraySettings.isTemperatureUnitFahrenheit()) {
            tempUnit = getString(R.string.temperature_fahrenheit);
            temp = Converter.convertToF(temp);
        }
        String temperature = decimalFormat.format(temp) + tempUnit;
        remoteViews.setTextViewText(R.id.notifyTemperature, temperature);

        remoteViews.setTextViewText(R.id.notifyRelativeHumaidity, humidity + "%");

        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) {
            return;
        }

        NotificationCompat.Builder visibility = new NotificationCompat.Builder(this, chanid).
                setContent(remoteViews).
                setStyle(new NotificationCompat.DecoratedCustomViewStyle()).
                setCustomContentView(remoteViews).
                setSmallIcon(R.drawable.ic_notify_logo).
                setAutoCancel(false).
                setCategory(NotificationCompat.CATEGORY_WORKOUT).
                setPriority(NotificationCompat.PRIORITY_MAX).
                setVisibility(VISIBILITY_PUBLIC).
                setSilent(true).
                setVibrate(new long[] { 0 }).
                setSound(Settings.System.DEFAULT_NOTIFICATION_URI).
                setColor(mContext.getColor(R.color.black)).
                setColorized(true);
        Intent intent = new Intent(this, GoSensePagerActivity.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
        visibility.setContentIntent(PendingIntent.getActivity(this, 0, intent, FLAG_MUTABLE | FLAG_UPDATE_CURRENT));
        notification2 = visibility.build();
        notificationManager.notify(procid, notification2);
        startForeground(procid, notification2);
    }

    private boolean hasBarometer() {
        /*
        SensorManager sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        List<Sensor> sensors = sensorManager.getSensorList(Sensor.TYPE_PRESSURE);
        if (sensors != null && sensors.size() > 0) {
            for (Sensor sensor: sensors) {
                if (sensor.getType() == Sensor.TYPE_PRESSURE) {
                    Log.d(TAG, "Found Device: " + sensor.getName() + " from " + sensor.getVendor());
                    return true;
                }
            }
        }
        return false;
         */
        return TraySettings.getHasBarometer();
    }

    public void setOnPressureSensorChange(OnPressureSensorChange onPressureSensorChange) {
        mOnPressureSensorChange = onPressureSensorChange;
    }

    public void setOnHandlerPressureSensorChange(OnHandlerPressureSensorChange onHandlerPressureSensorChange) {
        mOnHandlerPressureSensorChange = onHandlerPressureSensorChange;
    }

    public void setOnBluetoothDeviceDiscovery(OnBluetoothDeviceDiscovery onnBluetoothDeviceDiscovery) {
        btDataHandler.setOnBluetoothDeviceDiscovery(onnBluetoothDeviceDiscovery);
    }

    public void connect(String address) {
        btDataHandler.connect(address);
    }

    public void connect(BluetoothPeripheral peripheral) {
        btDataHandler.connect(peripheral);
    }

    public void startBtScan() {
        boolean scan_low_latency = false;
        if (TraySettings.getBleScanMode() != 1) {
            scan_low_latency = true;
        }
        btDataHandler.startScan(true, scan_low_latency);
    }

    public void stopBtScan() {
        btDataHandler.stopScan();
    }

    public BluetoothCentralManager getCentral() {
        return btDataHandler.getCentral();
    }

    public void close() {
        getCentral().close();
    }

    public void readRemoteBattery() {
        btDataHandler.readRemoteBattery();
    }

    public int getBatteryLevel() {
        return btDataHandler.getBatteryLevel();
    }

    public int getRssi() {
        return btDataHandler.getRssi();
    }

    public void registerOnReadRemoteRssi(AllServicesDeviceDataHandler.OnReadRemoteRssi onReadRemoteRssi) {
        btDataHandler.registerOnReadRemoteRssi(onReadRemoteRssi);
    }

    public void unregisterOnReadRemoteRssi(AllServicesDeviceDataHandler.OnReadRemoteRssi onReadRemoteRssi) {
        btDataHandler.unregisterOnReadRemoteRssi(onReadRemoteRssi);
    }

    public void registerOnReadRemoteBatteryLevel(AllServicesDeviceDataHandler.OnReadRemoteBatteryLevel onReadRemoteBatteryLevel) {
        btDataHandler.registerOnReadRemoteBatteryLevel(onReadRemoteBatteryLevel);
    }

    public void unregisterOnReadRemoteBatteryLevel(AllServicesDeviceDataHandler.OnReadRemoteBatteryLevel onReadRemoteBatteryLevel) {
        btDataHandler.unregisterOnReadRemoteBatteryLevel(onReadRemoteBatteryLevel);
    }

    public void registerOnServicesDiscovery(AllServicesDeviceDataHandler.OnServicesDiscovery onServicesDiscovery) {
        btDataHandler.registerOnServicesDiscovery(onServicesDiscovery);
    }

    public void unregisterOnServicesDiscovery(AllServicesDeviceDataHandler.OnServicesDiscovery onServicesDiscovery) {
        btDataHandler.unregisterOnServicesDiscovery(onServicesDiscovery);
    }

    public void setBluetoothCentralManagerCallback(BluetoothCentralManagerCallback bluetoothCentralManagerCallback) {
        mBluetoothCentralManagerCallback = bluetoothCentralManagerCallback;
    }

    public void disconnectAll() {
        btDataHandler.disconnectAll();
    }

    public void disconnect(String address) {
        btDataHandler.disconnect(address);
    }

    public List<BluetoothPeripheral> getConnectedPeripherals() {
        return btDataHandler.getConnectedPeripherals();
    }

    private void locationChanged(@NonNull Location location) {
        if (location == null) {
            Log.w(TAG, "location == null");
        } else {
            Log.i(TAG, "location [" + location.getLatitude() + "," + location.getLongitude() + "]");
            currentLocation = location;
        }
    }

    private void providerDisabled(String provider) {

    }

    private void providerEnabled(String provider) {

    }

    private BluetoothPeripheral getPeripheral(String peripheralAddress) {
        BluetoothCentralManager central = getCentral();
        return central.getPeripheral(peripheralAddress);
    }

    private void updateEndStartApDataConnection(String connectName, String connectedAddress) {
        if (goSenseDb != null)  {
            long time = System.currentTimeMillis();
            AirPressureData airPressureData = new AirPressureData(
                    time,
                    -9999,
                    -9999,
                    -9999,
                    connectName ,
                    connectedAddress);
            goSenseDb.saveAirPressure(airPressureData);
        }
    }

    private void updateEndStartDataConnection(String connectName, String connectedAddress) {
        if (goSenseDb != null)  {
            long time = System.currentTimeMillis();
            TemperatureData temperatureData = new TemperatureData(
                    time,
                    -9999,
                    -9999,
                    -9999,
                    connectName,
                    connectedAddress);

            RelativeHumidityData relativeHumidityData = new RelativeHumidityData(
                    time,
                    -9999,
                    -9999,
                    -9999,
                    connectName,
                    connectedAddress);

            goSenseDb.saveTemperature(temperatureData);
            goSenseDb.saveRelativeHumidity(relativeHumidityData);
        }
    }

    public void registerOnAirPressureSensorChange(OnAirPressureSensorChange onAirPressureSensorChange) {
        if (onAirPressureSensorChange != null && !mOnAirPressureSensorChange.contains(onAirPressureSensorChange)) {
            mOnAirPressureSensorChange.add(onAirPressureSensorChange);
        }
    }

    public void registerOnTemperatureSensorChange(OnTemperatureSensorChange onTemperatureSensorChange) {
        if (onTemperatureSensorChange != null && !mOnTemperatureSensorChange.contains(onTemperatureSensorChange)) {
            mOnTemperatureSensorChange.add(onTemperatureSensorChange);
        }
    }

    public void registerOnRelativeHumiditySensorChange(OnRelativeHumiditySensorChange onRelativeHumiditySensorChange) {
        if (onRelativeHumiditySensorChange != null && !mOnRelativeHumiditySensorChange.contains(onRelativeHumiditySensorChange)) {
            mOnRelativeHumiditySensorChange.add(onRelativeHumiditySensorChange);
        }
    }

    public void registerOnDeviceReconnection(AllServicesDeviceDataHandler.OnDeviceReconnection onDeviceReconnection) {
        btDataHandler.registerOnDeviceReconnection(onDeviceReconnection);
    }

    public void unregisterOnAirPressureSensorChange(OnAirPressureSensorChange onAirPressureSensorChange) {
        if (onAirPressureSensorChange != null && mOnAirPressureSensorChange.contains(onAirPressureSensorChange)) {
            mOnAirPressureSensorChange.remove(onAirPressureSensorChange);
        }
    }

    public void unregisterOnTemperatureSensorChange(OnTemperatureSensorChange onTemperatureSensorChange) {
        if (onTemperatureSensorChange != null && mOnTemperatureSensorChange.contains(onTemperatureSensorChange)) {
            mOnTemperatureSensorChange.remove(onTemperatureSensorChange);
        }
    }

    public void unregisterOnRelativeHumiditySensorChange(OnRelativeHumiditySensorChange onRelativeHumiditySensorChange) {
        if (onRelativeHumiditySensorChange != null && mOnRelativeHumiditySensorChange.contains(onRelativeHumiditySensorChange)) {
            mOnRelativeHumiditySensorChange.remove(onRelativeHumiditySensorChange);
        }
    }

    public void unregisterOnDeviceReconnection(AllServicesDeviceDataHandler.OnDeviceReconnection onDeviceReconnection) {
        btDataHandler.registerOnDeviceReconnection(onDeviceReconnection);
    }


    public void registerOnBluetoothStatusChange(AllServicesDeviceDataHandler.OnBluetoothStatusChange onBluetoothStatusChange) {
        btDataHandler.registerOnBluetoothStatusChange(onBluetoothStatusChange);
    }

    public void unregisterOnBluetoothStatusChange(AllServicesDeviceDataHandler.OnBluetoothStatusChange onBluetoothStatusChange) {
        btDataHandler.unregisterOnBluetoothStatusChange(onBluetoothStatusChange);
    }

    public boolean isBluetoothAdapterEnabled() {
        return btDataHandler.getCentral().isBluetoothEnabled();
    }

    private void cancelNotifications() {
        if (notificationManager == null) {
            notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        }
        if (notificationManager == null) {
            Log.e(TAG, "can't cancel all notifications, notification manager returns null");
            return;
        }

        String chanid = CHANNEL_ID_RELEASE;
        int procid = ID_PROCESS_RELEASE;
        if (BuildConfig.DEBUG) {
            chanid = CHANNEL_ID_DEBUG;
            procid = ID_PROCESS_DEBUG;
        }
        notificationManager.cancel(chanid, procid);
        notificationManager.cancelAll();

        if (exiting) {
            return;
        }

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                Date date = new Date(System.currentTimeMillis());
                String pattern = "'" + getString(R.string.at) +"' hh:mm:ss '" + getString(R.string.on) + "' yyyy/MM/dd";
                SimpleDateFormat sdf = new SimpleDateFormat(pattern);
                String msg = getString(R.string.bt_dev_or_gatt_disconnected) + " " + sdf.format(date);
                NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(mContext, chanid)
                        .setSmallIcon(R.drawable.ic_notify_logo)
                        .setContentTitle(getString(R.string.warning))
                        .setCategory(NotificationCompat.CATEGORY_ALARM)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setVibrate(new long[] { 50, 50, 100, 50, 50 })
                        .setChannelId(chanid)
                        .setContentText(msg)
                        .setAutoCancel(false);

                Intent intent = new Intent(mContext, GoSensePagerActivity.class);
                intent.setAction(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_LAUNCHER);
                intent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
                mBuilder.setContentIntent(PendingIntent.getActivity(mContext, 0, intent, FLAG_MUTABLE | FLAG_UPDATE_CURRENT));
                Notification notification2 = mBuilder.build();

                NotificationChannel channel = new NotificationChannel(
                        chanid,
                        "Disconnection Warning",
                        NotificationManager.IMPORTANCE_HIGH);
                notificationManager.createNotificationChannel(channel);

                notificationManager.notify(procid, notification2);
            }
        }, 250);
    }

    private void showToast(String msg, int length) {
        toastHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), msg, length).show();
            }
        });
    }

    public void setExit(boolean value) {
        exiting = value;
    }
}

package com.draekko.gosense.services;

import static android.bluetooth.le.ScanSettings.SCAN_MODE_LOW_LATENCY;
import static android.bluetooth.le.ScanSettings.SCAN_MODE_LOW_POWER;
import static com.draekko.gosense.misc.Utils.getBtState;
import static com.welie.blessed.BluetoothBytesParser.FORMAT_UINT8;
import static com.welie.blessed.BluetoothBytesParser.asHexString;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.draekko.gosense.data.RemoteBatteryLevelMeasurement;
import com.draekko.gosense.data.RemoteRelativeHumidityMeasurement;
import com.draekko.gosense.data.RemoteTemperatureMeasurement;
import com.draekko.gosense.misc.TraySettings;
import com.draekko.gosense.misc.Utils;
import com.welie.blessed.BluetoothBytesParser;
import com.welie.blessed.BluetoothCentralManager;
import com.welie.blessed.BluetoothCentralManagerCallback;
import com.welie.blessed.BluetoothPeripheral;
import com.welie.blessed.BluetoothPeripheralCallback;
import com.welie.blessed.ConnectionPriority;
import com.welie.blessed.ConnectionState;
import com.welie.blessed.GattStatus;
import com.welie.blessed.HciStatus;
import com.welie.blessed.PhyOptions;
import com.welie.blessed.PhyType;
import com.welie.blessed.ScanFailure;
import com.welie.blessed.ScanMode;
import com.welie.blessed.Transport;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class AllServicesDeviceDataHandler {

    public static final String TAG = "AllServicesDeviceDataHandler";

    private BluetoothCentralManagerCallback mBluetoothCentralManagerCallback;

    private ArrayList<OnReadRemoteRssi> mOnReadRemoteRssiList = new ArrayList<>();
    private ArrayList<OnReadRemoteBatteryLevel> mOnReadRemoteBatteryLevel = new ArrayList<>();
    private ArrayList<OnServicesDiscovery> mOnServicesDiscovery = new ArrayList<>();
    private ArrayList<OnDeviceReconnection> mOnDeviceReconnection = new ArrayList<>();

    /* ======================================================================================= */
    // Interfaces

    public interface OnBluetoothDeviceDiscovery {
        public void onDiscoveredPeripheral(@NotNull BluetoothCentralManager central,
                                           @NotNull BluetoothPeripheral peripheral,
                                           @NotNull ScanResult scanResult);
    }

    public interface OnReadRemoteRssi {
        public void remoteRssi(@NotNull BluetoothPeripheral peripheral, int rssi);
    }
    public interface OnReadRemoteBatteryLevel {
        public void remoteBatteryLevel(@NotNull BluetoothPeripheral peripheral, int batterylevel);
    }

    public interface OnServicesDiscovery {
        public void onServicesDiscovered(@NotNull BluetoothPeripheral peripheral);
    }

    public interface OnDeviceReconnection {
        public void onReconnecting(@NotNull BluetoothPeripheral peripheral);
    }

    /* ======================================================================================= */
    // Static variables
    public static final String MEASUREMENT_REMOTE_TEMPERATURE = "blessed.measurement.remote.temperature";
    public static final String MEASUREMENT_REMOTE_TEMPERATURE_EXTRA = "blessed.measurement.remote.temperature.extra";
    public static final String MEASUREMENT_REMOTE_RELATIVE_HUMIDITY = "blessed.measurement.remote.relativehumidity";
    public static final String MEASUREMENT_REMOTE_RELATIVE_HUMIDITY_EXTRA = "blessed.measurement.remote.relativehumidity.extra";
    public static final String MEASUREMENT_REMOTE_BATTERY_LEVEL = "blessed.measurement.remote.batterylevel";
    public static final String MEASUREMENT_REMOTE_BATTERY_LEVEL_EXTRA = "blessed.measurement.remote.batterylevel.extra";
    public static final String MEASUREMENT_EXTRA_PERIPHERAL = "blessed.measurement.peripheral";

    public static final String TEMP_RH_SERVICE = "0000181a-0000-1000-8000-00805f9b34fb";
    public static final UUID UUID_TEMP_RH_SERVICE = UUID.fromString(TEMP_RH_SERVICE);
    public static final String TEMP_NOTIFICATIONS = "00002a6e-0000-1000-8000-00805f9b34fb";
    public static final UUID UUID_TEMP_NOTIFICATIONS = UUID.fromString(TEMP_NOTIFICATIONS);
    public static final String RH_NOTIFICATIONS = "00002a6f-0000-1000-8000-00805f9b34fb";
    public static final UUID UUID_RH_NOTIFICATIONS = UUID.fromString(RH_NOTIFICATIONS);

    public static final UUID DIS_SERVICE_UUID = UUID.fromString("0000180A-0000-1000-8000-00805f9b34fb");
    public static final UUID MANUFACTURER_NAME_CHARACTERISTIC_UUID = UUID.fromString("00002A29-0000-1000-8000-00805f9b34fb");
    public static final UUID MODEL_NUMBER_CHARACTERISTIC_UUID = UUID.fromString("00002A24-0000-1000-8000-00805f9b34fb");

    public static final UUID BATTERY_SERVICE_UUID = UUID.fromString("0000180F-0000-1000-8000-00805f9b34fb");
    public static final UUID BATTERY_LEVEL_CHARACTERISTIC_UUID = UUID.fromString("00002A19-0000-1000-8000-00805f9b34fb");


    /* ======================================================================================= */
    // Local variables
    public BluetoothCentralManager central;
    private static AllServicesDeviceDataHandler instance = null;
    private final Context mContext;
    private final Handler handler = new Handler();
    private OnBluetoothDeviceDiscovery mOnBluetoothDeviceDiscovery;
    private int mBatteryLevel;
    private int mRssi;
    private double mLatitude;
    private double mLongitude;
    private boolean disconnectDevice = false;
    private int reconnectCount = 0;
    private boolean scanConnect = false;
    private String scanConnectAddress = null;

    // Callback for central
    private BluetoothPeripheral connectedPeripheral;
    private final BluetoothCentralManagerCallback bluetoothCentralManagerCallback = new BluetoothCentralManagerCallback() {

        @Override
        public void onConnectedPeripheral(@NotNull BluetoothPeripheral peripheral) {
            getRssi(peripheral);
            if (scanConnect) {
                if (peripheral.getAddress().toLowerCase(Locale.US).contains(scanConnectAddress.toLowerCase(Locale.US))) {
                    central.stopScan();
                    central.connectPeripheral(peripheral, peripheralCallback);
                    scanConnect = false;
                    scanConnectAddress = null;
                }
            }
            if (mBluetoothCentralManagerCallback != null)  {
                mBluetoothCentralManagerCallback.onConnectedPeripheral(peripheral);
            }
            connectedPeripheral = peripheral;
            disconnectDevice = false;
            reconnectCount = 0;
        }

        @Override
        public void onConnectionFailed(@NotNull BluetoothPeripheral peripheral, final @NotNull HciStatus status) {
            if (scanConnect) {
                central.stopScan();
                scanConnect = false;
                scanConnectAddress = null;
            }
            if (mBluetoothCentralManagerCallback != null)  {
                mBluetoothCentralManagerCallback.onConnectionFailed(peripheral, status);
            }
            // Reconnect to this device when it becomes available again
            if (TraySettings.getAutoConnectDevicesOnDisconnect() && !disconnectDevice) {
                reconnectDevice(peripheral);
            }
        }

        @Override
        public void onDisconnectedPeripheral(@NotNull final BluetoothPeripheral peripheral, final @NotNull HciStatus status) {
            if (mBluetoothCentralManagerCallback != null)  {
                mBluetoothCentralManagerCallback.onDisconnectedPeripheral(peripheral, status);
            }
            // Reconnect to this device when it becomes available again
            if (TraySettings.getAutoConnectDevicesOnDisconnect() && !disconnectDevice) {
                reconnectDevice(peripheral);
            }
        }

        @Override
        public void onDiscoveredPeripheral(@NotNull BluetoothPeripheral peripheral, @NotNull ScanResult scanResult) {
            if (scanConnect) {
                if (peripheral.getAddress().toLowerCase(Locale.US).contains(scanConnectAddress.toLowerCase(Locale.US))) {
                    central.stopScan();
                    central.connectPeripheral(peripheral, peripheralCallback);
                    scanConnect = false;
                    scanConnectAddress = null;
                    return;
                }
                return;
            }

            String name = peripheral.getName();
            if (name == null || name.isEmpty()) {
                name = scanResult.getScanRecord().getDeviceName();
            }
            if (name == null || name.isEmpty()) {
                return;
            }

            Log.d(TAG, "Discovered : " + peripheral.getName() + peripheral.getAddress());

            if (mBluetoothCentralManagerCallback != null)  {
                mBluetoothCentralManagerCallback.onDiscoveredPeripheral(peripheral, scanResult);
            }

            if (mOnBluetoothDeviceDiscovery != null) {
                mOnBluetoothDeviceDiscovery.onDiscoveredPeripheral(central, peripheral, scanResult);
            }
        }

        @Override
        public void onBluetoothAdapterStateChanged(int state) {
            if (false) Log.i(TAG, String.format("Bluetooth adapter changed state to %s", getBtState(state)));
            if (mBluetoothCentralManagerCallback != null)  {
                mBluetoothCentralManagerCallback.onBluetoothAdapterStateChanged(state);
            }
            if (!disconnectDevice) {
                if (state == 12) {
                    Log.d(TAG, "state == 12");
                } else  if (state == 13) {
                    Log.d(TAG, "state == 13");
                } else if ((state & 12) == 12) {
                    Log.d(TAG, "(state & 0xC) == 0xC : " + (state & 0x3));
                }
                if (state == BluetoothAdapter.STATE_OFF ||
                        state == BluetoothAdapter.STATE_DISCONNECTED) {
                    reinitCentral();
                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (getConnectedPeripherals().size() == 0) {
                                reconnectDevice(connectedPeripheral);
                            }
                        }
                    }, 5000);
                }
            }
        }

        @Override
        public void onScanFailed(@NotNull ScanFailure scanFailure) {
            if (false) Log.i(TAG, String.format("Scanning failed with error %s", scanFailure));
            if (mBluetoothCentralManagerCallback != null)  {
                mBluetoothCentralManagerCallback.onScanFailed(scanFailure);
            }
        }
    };

    /* ======================================================================================= */
    // Callback for peripherals

    private final BluetoothPeripheralCallback peripheralCallback = new BluetoothPeripheralCallback() {

        @Override
        public void onReadRemoteRssi(@NotNull BluetoothPeripheral peripheral, int rssi,
                                     @NotNull GattStatus status) {
            String name = peripheral.getName();
            if (name == null || name.isEmpty()) {
                name = Utils.getNameFromPairedDb(mContext, peripheral.getAddress());
            }
            String address = peripheral.getAddress();
            if (false) {
                if (status == GattStatus.SUCCESS) {
                    Log.i(TAG, String.format("%s %s : %d dBm", name, address, rssi));
                } else {
                    Log.i(TAG, String.format("%s %s : \u221e dBm", name, address));
                }
            }
            mRssi = rssi;
            if (mOnReadRemoteRssiList.size() > 0) {
                for (OnReadRemoteRssi onReadRemoteRssiList: mOnReadRemoteRssiList) {
                    onReadRemoteRssiList.remoteRssi(peripheral, mRssi);
                }
            }
        }

        @Override
        public void onServicesDiscovered(@NotNull BluetoothPeripheral peripheral) {
            // Request a higher MTU, iOS always asks for 185
            peripheral.requestMtu(185);

            // Request a new connection priority
            peripheral.requestConnectionPriority(ConnectionPriority.HIGH);

            if (false) {
                peripheral.setPreferredPhy(PhyType.LE_1M, PhyType.LE_CODED, PhyOptions.S8);
            } else {
                peripheral.setPreferredPhy(PhyType.LE_2M, PhyType.LE_2M, PhyOptions.S2);
            }

            // Read manufacturer and model number from the Device Information Service
            peripheral.readCharacteristic(DIS_SERVICE_UUID, MANUFACTURER_NAME_CHARACTERISTIC_UUID);
            peripheral.readCharacteristic(DIS_SERVICE_UUID, MODEL_NUMBER_CHARACTERISTIC_UUID);

            peripheral.readPhy();

            // Try to turn on notifications for other characteristics
            peripheral.readCharacteristic(BATTERY_SERVICE_UUID, BATTERY_LEVEL_CHARACTERISTIC_UUID);
            peripheral.setNotify(BATTERY_SERVICE_UUID, BATTERY_LEVEL_CHARACTERISTIC_UUID, true);
            peripheral.setNotify(UUID_TEMP_RH_SERVICE, UUID_TEMP_NOTIFICATIONS, true);
            peripheral.setNotify(UUID_TEMP_RH_SERVICE, UUID_RH_NOTIFICATIONS, true);

            if (mOnServicesDiscovery.size() > 0) {
                for (OnServicesDiscovery onServicesDiscovery : mOnServicesDiscovery) {
                    onServicesDiscovery.onServicesDiscovered(peripheral);
                }
            }
        }

        @Override
        public void onNotificationStateUpdate(@NotNull BluetoothPeripheral peripheral,
                                              @NotNull BluetoothGattCharacteristic characteristic,
                                              @NotNull GattStatus status) {
            if (status == GattStatus.SUCCESS) {
                final boolean isNotifying = peripheral.isNotifying(characteristic);
                Log.i(TAG, String.format("SUCCESS: Notify set to '%s' for %s", isNotifying, characteristic.getUuid()));
            } else {
                Log.e(TAG, String.format("ERROR: Changing notification state failed for %s (%s)", characteristic.getUuid(), status));
            }
        }

        @Override
        public void onCharacteristicWrite(@NotNull BluetoothPeripheral peripheral, @NotNull byte[] value,
                                          @NotNull BluetoothGattCharacteristic characteristic,
                                          @NotNull GattStatus status) {
            if (status == GattStatus.SUCCESS) {
                Log.i(TAG, String.format("SUCCESS: Writing <%s> to <%s>", asHexString(value), characteristic.getUuid()));
            } else {
                Log.i(TAG, String.format("ERROR: Failed writing <%s> to <%s> (%s)", asHexString(value), characteristic.getUuid(), status));
            }
        }

        @Override
        public void onCharacteristicUpdate(@NotNull BluetoothPeripheral peripheral, @NotNull byte[] value,
                                           @NotNull BluetoothGattCharacteristic characteristic,
                                           @NotNull GattStatus status) {
            if (status != GattStatus.SUCCESS) {
                return;
            }

            UUID characteristicUUID = characteristic.getUuid();
            BluetoothBytesParser parser = new BluetoothBytesParser(value);

            if (characteristicUUID.equals(UUID_TEMP_NOTIFICATIONS)) {
                if (false) Log.i("Handler", UUID_TEMP_NOTIFICATIONS.toString());
                RemoteTemperatureMeasurement measurement = new RemoteTemperatureMeasurement(value);
                Intent intent = new Intent(MEASUREMENT_REMOTE_TEMPERATURE);
                intent.putExtra(MEASUREMENT_REMOTE_TEMPERATURE_EXTRA, measurement);
                sendMeasurement(intent, peripheral);
            } else if (characteristicUUID.equals(UUID_RH_NOTIFICATIONS)) {
                if (false) Log.i("Handler", UUID_RH_NOTIFICATIONS.toString());
                RemoteRelativeHumidityMeasurement measurement = new RemoteRelativeHumidityMeasurement(value);
                Intent intent = new Intent(MEASUREMENT_REMOTE_RELATIVE_HUMIDITY);
                intent.putExtra(MEASUREMENT_REMOTE_RELATIVE_HUMIDITY_EXTRA, measurement);
                sendMeasurement(intent, peripheral);
            } else if (characteristicUUID.equals(BATTERY_LEVEL_CHARACTERISTIC_UUID)) {
                mBatteryLevel = parser.getIntValue(FORMAT_UINT8);
                RemoteBatteryLevelMeasurement measurement = new RemoteBatteryLevelMeasurement(value);
                Intent intent = new Intent(MEASUREMENT_REMOTE_BATTERY_LEVEL);
                intent.putExtra(MEASUREMENT_REMOTE_BATTERY_LEVEL_EXTRA, measurement);
                sendMeasurement(intent, peripheral);
                if (mOnReadRemoteBatteryLevel.size() > 0) {
                    for (OnReadRemoteBatteryLevel onReadRemoteBatteryLevel: mOnReadRemoteBatteryLevel) {
                        onReadRemoteBatteryLevel.remoteBatteryLevel(peripheral, mBatteryLevel);
                    }
                }
                if (false) Log.i(TAG, String.format("Received battery level %d%%", mBatteryLevel));
            } else if (characteristicUUID.equals(MANUFACTURER_NAME_CHARACTERISTIC_UUID)) {
                String manufacturer = parser.getStringValue(0);
                Log.i(TAG, String.format("Received manufacturer: %s", manufacturer));
            } else if (characteristicUUID.equals(MODEL_NUMBER_CHARACTERISTIC_UUID)) {
                String modelNumber = parser.getStringValue(0);
                Log.i(TAG, String.format("Received model number: %s", modelNumber));
            }
        }

        @Override
        public void onMtuChanged(@NotNull BluetoothPeripheral peripheral, int mtu, @NotNull GattStatus status) {
            Log.i(TAG, String.format("new MTU set: %d", mtu));
        }

        private void sendMeasurement(@NotNull Intent intent, @NotNull BluetoothPeripheral peripheral ) {
            intent.putExtra(MEASUREMENT_EXTRA_PERIPHERAL, peripheral.getAddress());
            intent.setPackage(mContext.getPackageName());
            mContext.sendBroadcast(intent);
        }
    };

    public void getRssi(BluetoothPeripheral peripheral) {
        if (peripheral.getState() == ConnectionState.CONNECTED) {
            peripheral.readRemoteRssi();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    getRssi(peripheral);
                }
            }, 2000);
        }
    }

    public static synchronized AllServicesDeviceDataHandler getInstance(Context context) {
        if (instance == null) {
            instance = new AllServicesDeviceDataHandler(context.getApplicationContext());
        }
        return instance;
    }

    public void initCentral() {
        // Create BluetoothCentral
        central = new BluetoothCentralManager(mContext, bluetoothCentralManagerCallback, new Handler());

        // Scan for peripherals with a certain service UUIDs
        central.startPairingPopupHack();
    }

    public void reinitCentral() {
        if (central != null) {
            central.close();
        }
        initCentral();
    }

    private AllServicesDeviceDataHandler(Context context) {
        this.mContext = context;
        initCentral();
    }

    private void reconnectDevice(BluetoothPeripheral peripheral) {
        if (reconnectCount < 6) {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(mContext, "Reconnection attempt #" + reconnectCount + 1, Toast.LENGTH_LONG);
                    Log.i(TAG, "Reconnection attempt #" + reconnectCount + 1);
                    if (mOnDeviceReconnection.size() > 0) {
                        for (OnDeviceReconnection onDeviceReconnection : mOnDeviceReconnection) {
                            onDeviceReconnection.onReconnecting(peripheral);
                        }
                    }
                    reconnectCount++;
                    central.connectPeripheral(peripheral, peripheralCallback);
                }
            }, 10000);
        } else {
            Log.i(TAG, "Too many reconnection attempts, quitting");
        }
    }

    public void setOnBluetoothDeviceDiscovery(OnBluetoothDeviceDiscovery onBluetoothDeviceDiscovery) {
        mOnBluetoothDeviceDiscovery = onBluetoothDeviceDiscovery;
    }
    public void stopScan() {
        central.stopScan();
    }

    public void startScan(boolean useCustom, boolean useLowLatency) {
        ScanMode scanmode_latency = ScanMode.LOW_POWER;
        if (useLowLatency) {
            scanmode_latency = ScanMode.LOW_LATENCY;
        }
        central.setScanMode(scanmode_latency);
        central.setTransport(Transport.LE);
        if (useCustom) {
            int scan_latency = SCAN_MODE_LOW_POWER;
            if (useLowLatency) {
                scan_latency = SCAN_MODE_LOW_LATENCY;
            }
            ScanSettings scanSettings = new ScanSettings.Builder().setScanMode(scan_latency).build();
            ArrayList scanFilterList = new ArrayList();
            scanFilterList.add(new ScanFilter.Builder().build());
            central.scanForPeripheralsWithCustom(scanFilterList,scanSettings);
        } else {
            central.scanForPeripherals();
        }
    }

    public BluetoothCentralManager getCentral() {
        return central;
    }

    public void connect(String address) {
        scanConnect = true;
        scanConnectAddress = address;
        if (central.isScanning()) {
            central.stopScan();
        }
        central.setTransport(Transport.LE);
        central.scanForPeripheralsWithAddresses(new String[] { address });
    }

    public void connect(BluetoothPeripheral peripheral) {
        scanConnect = false;
        scanConnectAddress = null;
        if (central.isScanning()) {
            central.stopScan();
        }
        central.setTransport(Transport.LE);
        central.connectPeripheral(peripheral, peripheralCallback);
    }

    public int getBatteryLevel() {
        return mBatteryLevel;
    }

    public int getRssi() {
        return mRssi;
    }

    public void registerOnReadRemoteRssi(OnReadRemoteRssi onReadRemoteRssi) {
        if (onReadRemoteRssi != null && !mOnReadRemoteRssiList.contains(onReadRemoteRssi)) {
            mOnReadRemoteRssiList.add(onReadRemoteRssi);
        }
    }

    public void unregisterOnReadRemoteRssi(OnReadRemoteRssi onReadRemoteRssi) {
        if (onReadRemoteRssi != null && mOnReadRemoteRssiList.contains(onReadRemoteRssi)) {
            mOnReadRemoteRssiList.remove(onReadRemoteRssi);
        }
    }

    public void registerOnReadRemoteBatteryLevel(OnReadRemoteBatteryLevel onReadRemoteBatteryLevel) {
        if (onReadRemoteBatteryLevel != null &&  !mOnReadRemoteBatteryLevel.contains(onReadRemoteBatteryLevel)) {
            mOnReadRemoteBatteryLevel.add(onReadRemoteBatteryLevel);
        }
    }

    public void unregisterOnReadRemoteBatteryLevel(OnReadRemoteBatteryLevel onReadRemoteBatteryLevel) {
        if (onReadRemoteBatteryLevel != null && mOnReadRemoteBatteryLevel.contains(onReadRemoteBatteryLevel)) {
            mOnReadRemoteBatteryLevel.remove(onReadRemoteBatteryLevel);
        }
    }

    public void registerOnServicesDiscovery(OnServicesDiscovery onServicesDiscovery) {
        if (onServicesDiscovery != null && !mOnServicesDiscovery.contains(onServicesDiscovery)) {
            mOnServicesDiscovery.add(onServicesDiscovery);
        }
    }

    public void unregisterOnServicesDiscovery(OnServicesDiscovery onServicesDiscovery) {
        if (onServicesDiscovery != null && mOnServicesDiscovery.contains(onServicesDiscovery)) {
            mOnServicesDiscovery.remove(onServicesDiscovery);
        }
    }

    public void registerOnDeviceReconnection(OnDeviceReconnection onDeviceReconnection) {
        if (onDeviceReconnection != null && !mOnDeviceReconnection.contains(onDeviceReconnection)) {
            mOnDeviceReconnection.add(onDeviceReconnection);
        }
    }

    public void unregisterOnDeviceReconnection(OnDeviceReconnection onDeviceReconnection) {
        if (onDeviceReconnection != null && mOnDeviceReconnection.contains(onDeviceReconnection)) {
            mOnDeviceReconnection.remove(onDeviceReconnection);
        }
    }

    public void readRemoteBattery() {
        List<BluetoothPeripheral> list = central.getConnectedPeripherals();
        if (list != null && list.size() > 0){
            for (BluetoothPeripheral peripheral: list) {
                peripheral.readCharacteristic(BATTERY_SERVICE_UUID, BATTERY_LEVEL_CHARACTERISTIC_UUID);
            }
        }
    }

    public void setBluetoothCentralManagerCallback(BluetoothCentralManagerCallback bluetoothCentralManagerCallback) {
        mBluetoothCentralManagerCallback = bluetoothCentralManagerCallback;
    }

    public void disconnect(String address) {
        disconnectDevice = true;
        BluetoothPeripheral peripheral = central.getPeripheral(address);
        peripheral.cancelConnection();
    }

    public void disconnectAll() {
        disconnectDevice = true;
        List<BluetoothPeripheral> peripherals = central.getConnectedPeripherals();
        if (peripherals != null && peripherals.size() > 0) {
            for (BluetoothPeripheral peripheral: peripherals) {
                peripheral.cancelConnection();
            }

            List<BluetoothPeripheral> peripheralsCheck = central.getConnectedPeripherals();
            boolean isConnected = true;
            int size = peripheralsCheck.size();
            int count = size;
            while (size > 0 && count != 0) {
                count = size;
                for (BluetoothPeripheral peripheral: peripherals) {
                    if (peripheral.getState() != ConnectionState.CONNECTED) {
                        count--;
                    }
                }
                if (count == 0) {
                    Log.i(TAG, "All devices were disconnected");
                    return;
                }
            }
            Log.i(TAG, "All devices were disconnected");
        } else {
            Log.i(TAG, "No devices available for disconnection");
        }
    }

    public List<BluetoothPeripheral> getConnectedPeripherals() {
        return central.getConnectedPeripherals();
    }

    public void setLatitude(double latitude) {
        mLatitude = latitude;
    }

    public void setLongitude(double longitude) {
        mLongitude = longitude;
    }
}

package com.draekko.gosense.activities;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.BLUETOOTH_CONNECT;
import static android.Manifest.permission.BLUETOOTH_SCAN;
import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
import static android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
import static com.draekko.gosense.activities.SplashScreenActivity.REQUEST_BT_PERMISSION;
import static com.draekko.gosense.activities.SplashScreenActivity.REQUEST_MANAGE_FILES;
import static com.draekko.gosense.activities.SplashScreenActivity.REQUEST_PAIRING;
import static com.draekko.gosense.misc.Utils.getBtState;
import static com.draekko.gosense.misc.Utils.hasBarometer;
import static com.draekko.gosense.misc.Utils.pixelsFromDp;
import static com.draekko.gosense.views.InfiniteViewPager2.SCROLL_STATE_DRAGGING;
import static com.draekko.gosense.views.InfiniteViewPager2.SCROLL_STATE_IDLE;
import static com.draekko.gosense.views.InfiniteViewPager2.SCROLL_STATE_SETTLING;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.StrictMode;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;

import com.draekko.gosense.R;
import com.draekko.gosense.data.SensorDevice;
import com.draekko.gosense.misc.SetupAlarm;
import com.draekko.gosense.misc.TraySettings;
import com.draekko.gosense.misc.Utils;
import com.draekko.gosense.services.AllDevicesService;
import com.draekko.gosense.services.AllServicesDeviceDataHandler;
import com.draekko.gosense.ui.comfort_zone.ComfortZoneFragment;
import com.draekko.gosense.ui.dashboard.DashboardFragment;
import com.draekko.gosense.ui.devices.DevicesFragment;
import com.draekko.gosense.ui.history.HistoryFragment;
import com.draekko.gosense.ui.settings.SettingsFragment;
import com.draekko.gosense.services.AllServicesDeviceDataHandler.OnReadRemoteRssi;
import com.draekko.gosense.services.AllServicesDeviceDataHandler.OnReadRemoteBatteryLevel;
import com.draekko.gosense.views.InfiniteFragmentStateAdapter;
import com.draekko.gosense.views.InfiniteViewPager2;
import com.draekko.gosense.views.InfiniteRecyclerTabLayout;
import com.welie.blessed.BluetoothCentralManagerCallback;
import com.welie.blessed.BluetoothPeripheral;
import com.welie.blessed.HciStatus;
import com.welie.blessed.ScanFailure;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GoSensePagerActivity extends FragmentActivity {

    private static final String TAG = "GoSensePagerActivity";

    private static final int TAB_SETTINGS_PRE_ID = 0;
    private static final int TAB_DASHBOARD_ID = 1;
    private static final int TAB_COMFORT_ZONE_ID = 2;
    private static final int TAB_HISTORY_ID = 3;
    private static final int TAB_DEVICES_ID = 4;
    private static final int TAB_SETTINGS_ID = 5;
    private static final int TAB_DASHBOARD_POST_ID = 6;

    private static final long RECONNECT_TIME = 20000;

    private static final int REQUEST_ENABLE_BT                = 0x1001;

    private static OnRssiUpdates mDashboardRssiUpdate;
    private static OnRssiUpdates mDevicesRssiUpdate;
    private static OnRssiUpdates mComfortZoneRssiUpdate;

    private static ArrayList<OnConnnectionStates> mOnConnnectionStates = new ArrayList<>();

    private ActivityResultLauncher<Intent> launchBtEnabledActivityResult = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        //Intent data = result.getData();
                    }
                }
            });

    public static void registerOnColorChange(DevicesFragment.OnColorChange onColorChange) {
        if (onColorChange != null) {
            DevicesFragment.getInstance().registerOnColorChange(onColorChange);
        }
    }

    public static void unregisterOnColorChange(DevicesFragment.OnColorChange onColorChange) {
        if (onColorChange != null) {
            DevicesFragment.getInstance().registerOnColorChange(onColorChange);
        }
    }

    public static void registerOnBluetoothStatusChange(AllServicesDeviceDataHandler.OnBluetoothStatusChange onBluetoothStatusChange) {
        if (onBluetoothStatusChange != null && mAllDevicesService != null) {
            mAllDevicesService.registerOnBluetoothStatusChange(onBluetoothStatusChange);
        }
    }

    public static void unregisterOnBluetoothStatusChange(AllServicesDeviceDataHandler.OnBluetoothStatusChange onBluetoothStatusChange) {
        if (onBluetoothStatusChange != null && mAllDevicesService != null) {
            mAllDevicesService.unregisterOnBluetoothStatusChange(onBluetoothStatusChange);
        }
    }

    public static void registerOnAirPressureSensorChange(AllDevicesService.OnAirPressureSensorChange onAirPressureSensorChange) {
        if (onAirPressureSensorChange != null) {
            mAllDevicesService.registerOnAirPressureSensorChange(onAirPressureSensorChange);
        }
    }

    public static void unregisterOnAirPressureSensorChange(AllDevicesService.OnAirPressureSensorChange onAirPressureSensorChange) {
        if (onAirPressureSensorChange != null) {
            mAllDevicesService.unregisterOnAirPressureSensorChange(onAirPressureSensorChange);
        }
    }

    public static void registerOnConnnectionStates(OnConnnectionStates onConnnectionStates) {
        if (onConnnectionStates != null && !mOnConnnectionStates.contains(onConnnectionStates)) {
            mOnConnnectionStates.add(onConnnectionStates);
        }
    }

    public static void unregisterOnConnnectionStates(OnConnnectionStates onConnnectionStates) {
        if (onConnnectionStates != null && mOnConnnectionStates.contains(onConnnectionStates)) {
            mOnConnnectionStates.remove(onConnnectionStates);
        }
    }

    public static void registerOnReadRemoteRssi(AllServicesDeviceDataHandler.OnReadRemoteRssi onReadRemoteRssi) {
        if (onReadRemoteRssi != null) {
            mAllDevicesService.registerOnReadRemoteRssi(onReadRemoteRssi);
        }
    }

    public static void unregisterOnReadRemoteRssi(OnReadRemoteRssi onReadRemoteRssi) {
        if (onReadRemoteRssi != null) {
            mAllDevicesService.unregisterOnReadRemoteRssi(onReadRemoteRssi);
        }
    }

    public static void registerOnReadRemoteBatteryLevel(OnReadRemoteBatteryLevel onReadRemoteBatteryLevel) {
        if (onReadRemoteBatteryLevel != null) {
            mAllDevicesService.registerOnReadRemoteBatteryLevel(onReadRemoteBatteryLevel);
        }
    }

    public static void unregisterOnReadRemoteBatteryLevel(OnReadRemoteBatteryLevel onReadRemoteBatteryLevel) {
        if (onReadRemoteBatteryLevel != null) {
            mAllDevicesService.unregisterOnReadRemoteBatteryLevel(onReadRemoteBatteryLevel);
        }
    }

    private final BroadcastReceiver locationServiceStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null && action.equals(LocationManager.MODE_CHANGED_ACTION)) {
                boolean isEnabled = areLocationServicesEnabled();
                if (!isEnabled || TraySettings.getUseLocation()) {
                    checkPermissions();
                }
            }
        }
    };

    private final ActivityResultLauncher<String[]> blePermissionRequest = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            new ActivityResultCallback<Map<String, Boolean>>() {
                @Override
                public void onActivityResult(Map<String, Boolean> result) {
                    for(String key : result.keySet()) {
                        Log.d(TAG, String.format("%s, %s", key, result));
                    }
                }
            });

    private InfiniteViewPager2.OnPageChangeCallback onPageChangeCallback = new InfiniteViewPager2.OnPageChangeCallback() {

        private boolean toggle = false;

        @Override
        public void onPageScrolled(int position, float positionOffset,
                                   int positionOffsetPixels) {
            if (debug) Log.i(TAG, "onPageScrolled: pos: " + position + ", posOff: " + position + ", posOffPix: " + positionOffsetPixels);
            if (positionOffsetPixels == 0) {
                if (position == TAB_DASHBOARD_POST_ID) {
                    infiniteRecyclerTabLayout.setCurrentItem(TAB_DASHBOARD_ID, false);
                }
                if (position == TAB_SETTINGS_PRE_ID) {
                    infiniteRecyclerTabLayout.setCurrentItem(TAB_SETTINGS_ID, false);
                }
                if (position != TAB_COMFORT_ZONE_ID) {
                    ((ComfortZoneFragment)PAGES[2]).setOnHidden(true);
                }
                if (position == TAB_COMFORT_ZONE_ID) {
                    ((ComfortZoneFragment)PAGES[2]).setOnHidden(false);
                }
                if (position != TAB_HISTORY_ID) {
                    ((HistoryFragment)PAGES[3]).setOnHidden(true);
                }
                if (position == TAB_HISTORY_ID) {
                    ((HistoryFragment)PAGES[3]).setOnHidden(false);
                }
            }
        }

        @Override
        public void onPageSelected(int position) {
            if (debug) {
                Log.i(TAG, "onPageSelected: pos: " + position +  " | state: " + pagerState );
                if ((position == TAB_SETTINGS_PRE_ID || position == TAB_DASHBOARD_POST_ID)) {
                    Log.i(TAG, "NEED TO HANDLE bad position");
                }
            }
            if (position != TAB_COMFORT_ZONE_ID) {
                ((ComfortZoneFragment)PAGES[2]).setOnHidden(true);
            }
            if (position != TAB_HISTORY_ID) {
                ((HistoryFragment)PAGES[3]).setOnHidden(true);
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            super.onPageScrollStateChanged(state);
            pagerState = state;
            if (debug) Log.i(TAG, "onPageScrollStateChanged: state: " + pagerState);
            switch (state) {
                case SCROLL_STATE_IDLE:
                    if (debug) Log.i(TAG, "onPageScrollStateChanged: state: IDLE");
                    break;
                case SCROLL_STATE_DRAGGING:
                    if (debug) Log.i(TAG, "onPageScrollStateChanged: state: DRAGGING");
                    break;
                case SCROLL_STATE_SETTLING:
                    if (debug) Log.i(TAG, "onPageScrollStateChanged: state: SETTLING");
                    break;
            }
        }
    };

    public interface OnConnnectionStates {
        public void onConnected(@NotNull BluetoothPeripheral peripheral);
        public void onDisconnected(@NotNull BluetoothPeripheral peripheral, @NotNull HciStatus status);
    }

    public interface OnRssiUpdates {
        public void onRssiUpdate(String address, int rssi);
    }

    public static void setDashboardRssiUpdate(OnRssiUpdates onRssiUpdates) {
        mDashboardRssiUpdate = onRssiUpdates;
    }
    public static void setDevicesRssiUpdate(OnRssiUpdates onRssiUpdates) {
        mDevicesRssiUpdate = onRssiUpdates;
    }
    public static void setComfortZoneRssiUpdate(OnRssiUpdates onRssiUpdates) {
        mComfortZoneRssiUpdate = onRssiUpdates;
    }

    private static final long TIMEOUT_CONNECT = 25000;

    private int totalItems = 7;

    private int[] PAGE_ICONS = new int[]{
            R.drawable.ic_settings,
            R.drawable.ic_dashboard,
            R.drawable.ic_comfort_zone,
            R.drawable.ic_history,
            R.drawable.ic_device,
            R.drawable.ic_settings,
            R.drawable.ic_dashboard,
    };

    private SensorDevice activePeripheral;
    private AlertDialog mConnectionDialog;
    private Context mContext;
    private Activity mActivity;
    private String [] PAGE_TITLES;
    private Fragment[] PAGES = new Fragment[totalItems];
    private InfiniteViewPager2 mInfiniteViewPager2;
    private InfiniteRecyclerTabLayout infiniteRecyclerTabLayout;
    private boolean debug = false;
    private int pagerState = -1;
    private boolean importing = false;
    private boolean exporting = false;

    private static boolean inSettingsFragment = false;
    private boolean inNewBtDeviceScan = false;
    private boolean isExternalStorageManager = false;

    private static AllDevicesService mAllDevicesService;
    private KeyguardManager keyguardManager;
    private HandlerThread mHandlerThread;
    private Handler mHandler;
    private boolean mSettingsMode;

    SettingsFragment.OnImportExportData mOnImportExportData = new SettingsFragment.OnImportExportData() {
        @Override
        public void onExportData(boolean status) {
            exporting = status;
        }

        @Override
        public void onImportData(boolean status) {
            importing = status;
        }
    };

    private BluetoothCentralManagerCallback mBluetoothCentralManagerCallback = new BluetoothCentralManagerCallback() {
        @Override
        public void onBluetoothAdapterStateChanged(int state) {
            super.onBluetoothAdapterStateChanged(state);
            if (false) Log.i(TAG, String.format("Bluetooth Adapter State Changed :  %s", getBtState(state)));
        }

        @Override
        public void onScanFailed(@NotNull ScanFailure scanFailure) {
            super.onScanFailed(scanFailure);
            if (false) Log.e(TAG, "Scan Failed : " + scanFailure);
        }

        @Override
        public void onDiscoveredPeripheral(@NotNull BluetoothPeripheral peripheral, @NotNull ScanResult scanResult) {
            super.onDiscoveredPeripheral(peripheral, scanResult);
        }

        @Override
        public void onDisconnectedPeripheral(@NotNull BluetoothPeripheral peripheral, @NotNull HciStatus status) {
            super.onDisconnectedPeripheral(peripheral, status);
            if (mOnConnnectionStates != null && mOnConnnectionStates.size() > 0) {
                for (OnConnnectionStates connnectionStates: mOnConnnectionStates) {
                    connnectionStates.onDisconnected(peripheral, status);
                }
            }
        }

        @Override
        public void onDisconnectingPeripheral(@NotNull BluetoothPeripheral peripheral) {
            super.onDisconnectingPeripheral(peripheral);
        }

        @Override
        public void onConnectionFailed(@NotNull BluetoothPeripheral peripheral, @NotNull HciStatus status) {
            super.onConnectionFailed(peripheral, status);
            if (mOnConnnectionStates != null && mOnConnnectionStates.size() > 0) {
                for (OnConnnectionStates connnectionStates: mOnConnnectionStates) {
                    connnectionStates.onDisconnected(peripheral, status);
                }
            }
        }

        @Override
        public void onConnectedPeripheral(@NotNull BluetoothPeripheral peripheral) {
            super.onConnectedPeripheral(peripheral);
            String deviceName = peripheral.getName();
            if (deviceName == null || deviceName.isEmpty()) {
                deviceName = Utils.getAliasNameFromPairedDb(mContext, peripheral.getAddress());
            }
            SensorDevice sensorDevice = new SensorDevice();
            sensorDevice.setIsConnected(false);
            sensorDevice.setIsConnected(false);
            sensorDevice.setColor(0xFF00FF00);
            sensorDevice.setAliasName(deviceName);
            sensorDevice.setName(deviceName);
            sensorDevice.setAddress(peripheral.getAddress());
            TraySettings.setActiveSensorDevice(sensorDevice);
            if (mOnConnnectionStates != null && mOnConnnectionStates.size() > 0) {
                for (OnConnnectionStates connnectionStates: mOnConnnectionStates) {
                    connnectionStates.onConnected(peripheral);
                }
            }
        }

        @Override
        public void onConnectingPeripheral(@NotNull BluetoothPeripheral peripheral) {
            super.onConnectingPeripheral(peripheral);
        }
    };

    public static boolean isConnected() {
        if (mAllDevicesService == null) {
            return false;
        }
        return mAllDevicesService.getCentral().getConnectedPeripherals() != null &&
                mAllDevicesService.getCentral().getConnectedPeripherals().size() > 0;
    }

    private BroadcastReceiver mScrnUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String strAction = intent.getAction();
            if(strAction.equals(Intent.ACTION_USER_PRESENT) ||
                    strAction.equals(Intent.ACTION_SCREEN_OFF) ||
                    strAction.equals(Intent.ACTION_SCREEN_ON)) {
                if (isScreenLocked()) {
                    Log.i(TAG, "Activity : Screen LOCKED");
                } else {
                    Log.i(TAG, "Activity : Screen UNLOCKED");
                }
                if (isConnected()) {
                    Log.i(TAG, "BT is connected");
                } else {
                    Log.i(TAG, "BT is disconnected");
                }
            }
        }
    };

    private AllServicesDeviceDataHandler.OnDeviceReconnection mOnDeviceReconnection =
            new AllServicesDeviceDataHandler.OnDeviceReconnection() {

                @Override
                public void onReconnecting(@NotNull BluetoothPeripheral peripheral) {
                    Toast.makeText(mContext, getString(R.string.attempting_reconnection), Toast.LENGTH_LONG).show();
                }
            };

    private ServiceConnection mAllDevicesServiceConnection = new ServiceConnection() {
        @SuppressLint("MissingPermission")
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            if (service == null) {
                return;
            }
            mAllDevicesService = (AllDevicesService)((AllDevicesService.LocalBinder) service).getService();
            mAllDevicesService.setBluetoothCentralManagerCallback(mBluetoothCentralManagerCallback);
            if (mAllDevicesService.getConnectedPeripherals().size() == 0) {
                Toast.makeText(mContext, "No devices are connected", Toast.LENGTH_LONG).show();
                mInfiniteViewPager2.setCurrentItem(4, false);
            }
            mAllDevicesService.registerOnDeviceReconnection(mOnDeviceReconnection);

            if (!mAllDevicesService.isBluetoothAdapterEnabled()) {
                Intent intentBtEnabled = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    launchBtEnabledActivityResult.launch(intentBtEnabled);
                } else {
                    startActivityForResult(intentBtEnabled, REQUEST_ENABLE_BT);
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mAllDevicesService = null;
            Log.e(TAG, "BluetoothLeService has been killed!");
        }
    };

    private boolean isScreenLocked() {
        return keyguardManager.isDeviceLocked();
    }

    public static AllDevicesService getAllDevicesService() {
        return mAllDevicesService;
    }

    private void startAllDevicesServiceService() {
        if (mAllDevicesService != null) {
            return;
        }
        Intent dataServiceIntent = new Intent(this, AllDevicesService.class);
        bindService(dataServiceIntent, mAllDevicesServiceConnection, BIND_AUTO_CREATE);
    }

    /**
     * Проверяет, выданы ли все разрешения, необходимые для работы Bluetooth-сканирования.
     * Библиотека blessed требует ACCESS_FINE_LOCATION даже на Android 12+.
     */
    private boolean hasRequiredPermissions() {
        boolean fineLocation = ContextCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            boolean scan = ContextCompat.checkSelfPermission(this, BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
            boolean connect = ContextCompat.checkSelfPermission(this, BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
            return fineLocation && scan && connect;
        }
        return fineLocation;
    }

    /**
     * Запрашивает недостающие разрешения или запускает службу, если всё уже выдано.
     */
    private void requestNeededPermissionsOrStartService() {
        List<String> perms = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                perms.add(BLUETOOTH_SCAN);
            }
            if (ContextCompat.checkSelfPermission(this, BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                perms.add(BLUETOOTH_CONNECT);
            }
        }
        if (ContextCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            perms.add(ACCESS_FINE_LOCATION);
        }
        if (perms.isEmpty()) {
            startAllDevicesServiceService();
        } else {
            ActivityCompat.requestPermissions(this, perms.toArray(new String[0]), REQUEST_BT_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BT_PERMISSION) {
            boolean allGranted = grantResults.length > 0;
            for (int r : grantResults) {
                if (r != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                startAllDevicesServiceService();
            } else {
                Toast.makeText(this,
                        getString(R.string.text_bt_permission),
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        mActivity = this;

        setRequestedOrientation(SCREEN_ORIENTATION_PORTRAIT);

        if (false) {
            StrictMode.allowThreadDiskReads();
            StrictMode.allowThreadDiskWrites();
            if (true) {
                StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.LAX);
                StrictMode.setVmPolicy(StrictMode.VmPolicy.LAX);
            } else {
                StrictMode.setThreadPolicy(
                        new StrictMode.ThreadPolicy.Builder()
                                .detectAll()
                                .build()
                );
                StrictMode.setVmPolicy(
                        new StrictMode.VmPolicy.Builder()
                                .detectAll()
                                .build()
                );
            }
        }

        setContentView(R.layout.gosensepageractivity);

        keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);

        mHandlerThread = new HandlerThread("GoSensePagerActivityThread");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());

        boolean barometer = false;
        String barometerStr = "No pressure sensor was found";
        if (hasBarometer(mContext)) {
            barometerStr = "Found pressure sensor";
            barometer = true;
        }
        Log.d(TAG, barometerStr);
        TraySettings.setHasBarometer(barometer);

        if (activePeripheral == null) {
            activePeripheral = TraySettings.getActiveSensorDevice();
        }

        // Запускаем службу только после проверки разрешений.
        // Если разрешений нет — запросим их, а службу запустим в onRequestPermissionsResult.
        requestNeededPermissionsOrStartService();

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.add(SettingsFragment.getInstance(), "GoSenseSettingsFragment");
        fragmentTransaction.commit();

        fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.add(DashboardFragment.getInstance(), "GoSenseDashboardFragment");
        fragmentTransaction.commit();

        fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.add(ComfortZoneFragment.getInstance(), "GoSenseComfortZoneFragment");
        fragmentTransaction.commit();

        fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.add(HistoryFragment.getInstance(), "GoSenseHistoryFragment");
        fragmentTransaction.commit();

        fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.add(DevicesFragment.getInstance(), "GoSenseDevicesFragment");
        fragmentTransaction.commit();

        PAGE_TITLES = getResources().getStringArray(R.array.tablayout_names);

        PAGES[0] = SettingsFragment.getInstance();
        PAGES[1] = DashboardFragment.getInstance();
        PAGES[2] = ComfortZoneFragment.getInstance();
        PAGES[3] = HistoryFragment.getInstance();
        PAGES[4] = DevicesFragment.getInstance();
        PAGES[5] = SettingsFragment.getInstance();
        PAGES[6] = DashboardFragment.getInstance();

        SettingsFragment.getInstance().registerOnImportExportData(mOnImportExportData);

        mInfiniteViewPager2 = findViewById(R.id.viewpager);
        mInfiniteViewPager2.setAdapter(new GoSenseUIPagerAdapter(this));

        CustomViewAdapter customViewAdapter = new CustomViewAdapter(mInfiniteViewPager2);

        infiniteRecyclerTabLayout = findViewById(R.id.recTablayout);
        infiniteRecyclerTabLayout.hideIndicator();
        infiniteRecyclerTabLayout.setUpWithAdapter(customViewAdapter);

        mInfiniteViewPager2.registerOnPageChangeCallback(onPageChangeCallback);
        infiniteRecyclerTabLayout.setCurrentItem(TAB_DASHBOARD_ID, false);
        infiniteRecyclerTabLayout.setAutoSelectionMode(true);

        new SetupAlarm(this).setup();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_MANAGE_FILES) {
            Intent intent = new Intent(mContext, SplashScreenActivity.class);
            startActivity(intent);
            finish();
        } else if (requestCode == REQUEST_PAIRING) {
            mSettingsMode = false;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (TraySettings.getKeepScreenOn()) {
            getWindow().addFlags(FLAG_KEEP_SCREEN_ON);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if ((TraySettings.getKeepScreenOn())) {
            getWindow().clearFlags(FLAG_KEEP_SCREEN_ON);
        }
    }

    @Override
    public void onBackPressed() {
        if (exporting || importing) {
            Toast.makeText(this, getString(R.string.import_export_in_progress), Toast.LENGTH_LONG).show();
        } else {
            new AlertDialog.Builder(this).
                    setTitle(R.string.exit_app_title).
                    setMessage(R.string.exit_app_message).
                    setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            try {
                                mAllDevicesService.setExit(true);
                                mAllDevicesService.disconnectAll();
                            } catch (Exception e) {
                            }
                            try {
                                mAllDevicesService.close();
                            } catch (Exception e) {
                            }

                            finish();
                            finishAffinity();
                            dialog.dismiss();
                        }
                    }).
                    setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    }).show();
        }
    }

    private IntentFilter makeScrnIntentFilter() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_SCREEN_ON);
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        intentFilter.addAction(Intent.ACTION_USER_PRESENT);
        return intentFilter;
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                isExternalStorageManager = true;
            } else {
                isExternalStorageManager = false;
            }
        }

        // Ugly hack to hide invisible items from user display and space
        // usable items to fit window width
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int width = displayMetrics.widthPixels;
        int height = (int)pixelsFromDp(this, 60.0f);
        int iw = width / (totalItems - 2) + 1;
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(iw * totalItems, height);
        lp.setMarginStart(-iw);
        infiniteRecyclerTabLayout.setLayoutParams(lp);

        ContextCompat.registerReceiver(mContext, mScrnUpdateReceiver,
                makeScrnIntentFilter(), ContextCompat.RECEIVER_EXPORTED);
        registerReceiver(locationServiceStateReceiver, new IntentFilter((LocationManager.MODE_CHANGED_ACTION)));

        // Если разрешения выданы пользователем вручную в настройках, но служба ещё не запущена — запустим.
        if (hasRequiredPermissions() && mAllDevicesService == null) {
            startAllDevicesServiceService();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                isExternalStorageManager = true;
            } else {
                isExternalStorageManager = false;
            }
        }
    }

    public class GoSenseUIPagerAdapter extends InfiniteFragmentStateAdapter {

        public GoSenseUIPagerAdapter(FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return PAGES[position];
        }

        @Override
        public int getItemCount() {
            return totalItems;
        }
    }

    public class CustomViewAdapter extends InfiniteRecyclerTabLayout.Adapter<CustomViewAdapter.ViewHolder> {

        private InfiniteViewPager2 mViewPager;
        public CustomViewAdapter(InfiniteViewPager2 viewPager) {
            super(viewPager);
            mViewPager = viewPager;
        }

        @NonNull
        @Override
        public CustomViewAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.viewpager_adapter_layout, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull CustomViewAdapter.ViewHolder holder, int position) {
            ColorStateList colorStateList;
            int color;
            String text = "";
            int icon;

            boolean selected = position == getCurrentIndicatorPosition();

            if (position == TAB_SETTINGS_PRE_ID || position == TAB_DASHBOARD_POST_ID) {
                color = R.color.ap_transparent;
            } else {
                if (selected) {
                    color = R.color.red;
                    text = PAGE_TITLES[position];
                } else {
                    color = R.color.white;
                }
            }

            icon = PAGE_ICONS[position];
            colorStateList = ColorStateList.valueOf(getColor(color));

            holder.imageView.setImageDrawable(getDrawable(icon));
            holder.imageView.setImageTintList(colorStateList);
            holder.textView.setTextColor(getColor(color));
            holder.textView.setText(text);
            if (selected) {
                holder.textView.setVisibility(View.VISIBLE);
            } else {
                holder.textView.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() {
            return totalItems;
        }

        public class ViewHolder extends RecyclerView.ViewHolder {

            public ImageView imageView;
            public TextView textView;
            public LinearLayout frameView;

            public ViewHolder(View itemView) {
                super(itemView);

                int width = infiniteRecyclerTabLayout.getMeasuredWidth();
                int height = infiniteRecyclerTabLayout.getMeasuredHeight();

                imageView = itemView.findViewById(R.id.image);
                textView = itemView.findViewById(R.id.text);
                frameView = itemView.findViewById(R.id.frame);

                int pos = getAbsoluteAdapterPosition();

                int iw = width / totalItems;
                frameView.setLayoutParams(new LinearLayout.LayoutParams(iw, height));

                if (pos == TAB_SETTINGS_PRE_ID || pos == TAB_DASHBOARD_POST_ID) {
                    frameView.setVisibility(View.INVISIBLE);
                    return;
                } else {
                    frameView.setVisibility(View.VISIBLE);
                }

                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int position = getAbsoluteAdapterPosition();
                        if (position == TAB_SETTINGS_PRE_ID || position == TAB_DASHBOARD_POST_ID){
                            return;
                        }
                        infiniteRecyclerTabLayout.setCurrentItem(position, false);
                    }
                });
            }
        }
    }

    private void requestBtPermission(String[] permissions) {
        ActivityCompat.requestPermissions(mActivity, permissions, REQUEST_BT_PERMISSION);
        if (true) return;
        if (ActivityCompat.shouldShowRequestPermissionRationale(mActivity, permissions[0])) {
            new AlertDialog.Builder(this).
                    setTitle(R.string.title_bt_permission).
                    setMessage(R.string.text_bt_permission).
                    setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(mActivity, permissions, REQUEST_BT_PERMISSION);
                        }
                    }).
                    setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    }).
                    create().
                    show();
        } else {
            ActivityCompat.requestPermissions(mActivity, permissions, REQUEST_BT_PERMISSION);
        }
    }

    private boolean isBtScanConnectPermissionGranted() {
        boolean canConnectBt = true;
        boolean canScanBt = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            canScanBt = ContextCompat.checkSelfPermission(mContext, BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
            canConnectBt = ContextCompat.checkSelfPermission(mContext, BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
            if (!canConnectBt || !canScanBt) {
                requestBtPermission(new String[]{ BLUETOOTH_SCAN, BLUETOOTH_CONNECT });
            }
        }
        return canConnectBt;
    }

    private boolean isBtConnectScanPermissionGranted() {
        boolean canConnectBt = true;
        boolean canScanBt = true;
        boolean canLocation = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            canConnectBt = ContextCompat.checkSelfPermission(mContext, BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
            canScanBt = ContextCompat.checkSelfPermission(mContext, BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
            canLocation = ContextCompat.checkSelfPermission(mContext, ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
            if (!canConnectBt || !canScanBt || !canLocation) {
                requestBtPermission(new String[]{ BLUETOOTH_CONNECT, BLUETOOTH_SCAN, ACCESS_FINE_LOCATION });
            }
            return canConnectBt && canScanBt && canLocation;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            canLocation = ContextCompat.checkSelfPermission(mContext, ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
            if (!canLocation) {
                requestBtPermission(new String[]{ ACCESS_FINE_LOCATION });
            }
            return canLocation;
        }
        return true;
    }

    private void startDialogProgress(String message, boolean centered, DialogInterface.OnCancelListener cancelListener) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isFinishing()) {
                    return;
                }
                stopProgress();
                if (centered) {
                    AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(mActivity);
                    dialogBuilder.setMessage(message);
                    dialogBuilder.setOnCancelListener(cancelListener);
                    mConnectionDialog = dialogBuilder.show();
                    TextView messageText = mConnectionDialog.findViewById(android.R.id.message);
                    messageText.setGravity(Gravity.CENTER);
                    messageText.setAllCaps(true);
                    mConnectionDialog.show();
                } else {
                    mConnectionDialog = new AlertDialog.Builder(mActivity).
                            setMessage(message).
                            setOnCancelListener(cancelListener).
                            show();
                }
            }
        });
    }

    private void showData() {
        Log.i(TAG, "show data");
    }

    @Override
    public void onDestroy() {
        prepareToDestroy();
        super.onDestroy();
    }

    private void stopProgress() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog alertDialog = mConnectionDialog;
                if (alertDialog != null) {
                    alertDialog.dismiss();
                    mConnectionDialog = null;
                }
            }
        });
    }

    private void prepareToDestroy() {
        try {
            unregisterReceiver(mScrnUpdateReceiver);
        } catch (Exception e) {
        }
        try {
            unregisterReceiver(locationServiceStateReceiver);
        } catch (Exception e) {
        }

        stopProgress();
        mHandler.removeCallbacksAndMessages(null);
        if (mAllDevicesServiceConnection != null) {
            try {
                unbindService(mAllDevicesServiceConnection);
            } catch (Exception e) {
            }
        }
    }

    private boolean areLocationServicesEnabled() {
        LocationManager locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) {
            Log.e(TAG, "could not get location manager");
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return locationManager.isLocationEnabled();
        } else {
            boolean isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            return isGpsEnabled || isNetworkEnabled;
        }
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String[] missingPermissions = getMissingPermissions(getRequiredPermissions());
            if (missingPermissions.length > 0) {
                blePermissionRequest.launch(missingPermissions);
            } else {
                permissionsGranted();
            }
        }
    }

    private String[] getMissingPermissions(String[] requiredPermissions) {
        List<String> missingPermissions = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (String requiredPermission : requiredPermissions) {
                if (mContext.checkSelfPermission(requiredPermission) != PackageManager.PERMISSION_GRANTED) {
                    missingPermissions.add(requiredPermission);
                }
            }
        }
        return missingPermissions.toArray(new String[0]);
    }

    private String[] getRequiredPermissions() {
        int targetSdkVersion = mContext.getApplicationInfo().targetSdkVersion;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && targetSdkVersion >= Build.VERSION_CODES.S) {
            return new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT};
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && targetSdkVersion >= Build.VERSION_CODES.Q) {
            return new String[]{Manifest.permission.ACCESS_FINE_LOCATION};
        } else return new String[]{Manifest.permission.ACCESS_COARSE_LOCATION};
    }

    private void permissionsGranted() {
        int targetSdkVersion = mContext.getApplicationInfo().targetSdkVersion;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S && targetSdkVersion < Build.VERSION_CODES.S) {
            if (checkLocationServices()) {
                mAllDevicesService = GoSensePagerActivity.getAllDevicesService();
            }
        } else {
            mAllDevicesService = GoSensePagerActivity.getAllDevicesService();
        }
    }

    private boolean checkLocationServices() {
        if (!areLocationServicesEnabled()) {
            new AlertDialog.Builder(mContext)
                    .setTitle("Location services are not enabled")
                    .setMessage("Scanning for Bluetooth peripherals requires locations services to be enabled.")
                    .setPositiveButton("Enable", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.cancel();
                            startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    })
                    .create()
                    .show();
            return false;
        } else {
            return true;
        }
    }
}
package com.draekko.gosense.ui.devices;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.BLUETOOTH_CONNECT;
import static android.Manifest.permission.BLUETOOTH_SCAN;
import static android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS;

import static com.draekko.contextmenu.IconContextMenu.SANS;
import static com.draekko.gosense.services.AllServicesDeviceDataHandler.BATTERY_LEVEL_CHARACTERISTIC_UUID;
import static com.draekko.gosense.services.AllServicesDeviceDataHandler.BATTERY_SERVICE_UUID;
import static com.draekko.gosense.services.AllServicesDeviceDataHandler.UUID_RH_NOTIFICATIONS;
import static com.draekko.gosense.services.AllServicesDeviceDataHandler.UUID_TEMP_NOTIFICATIONS;
import static com.draekko.gosense.services.AllServicesDeviceDataHandler.UUID_TEMP_RH_SERVICE;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.le.ScanResult;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.graphics.Rect;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.draekko.contextmenu.IconContextItemSelectedListener;
import com.draekko.contextmenu.IconContextMenu;
import com.draekko.gosense.R;
import com.draekko.gosense.activities.GoSensePagerActivity;
import com.draekko.gosense.data.ScannedDevicesData;
import com.draekko.gosense.data.SensorDevice;
import com.draekko.gosense.data.SensorPairListData;
import com.draekko.gosense.database.GoSenseDb;
import com.draekko.gosense.databinding.FragmentDevicesBinding;
import com.draekko.gosense.misc.TraySettings;
import com.draekko.gosense.misc.Utils;
import com.draekko.gosense.services.AllDevicesService;
import com.draekko.gosense.services.AllServicesDeviceDataHandler;
import com.google.android.material.button.MaterialButton;
import com.skydoves.colorpickerview.ColorPickerDialog;
import com.skydoves.colorpickerview.ColorPickerView;
import com.skydoves.colorpickerview.listeners.ColorListener;
import com.welie.blessed.BluetoothCentralManager;
import com.welie.blessed.BluetoothPeripheral;
import com.welie.blessed.ConnectionState;
import com.welie.blessed.HciStatus;

import org.jetbrains.annotations.NotNull;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DevicesFragment extends Fragment {

    private final String TAG = "DevicesFragment";
    private static final int REQUEST_BT_PERMISSION = 3;
    private static final int REQUEST_ENABLE_BT = 0;
    private static final int REQUEST_LOCATION = 1;
    private static final int REQUEST_NOTIFICATION = 2;

    private static final long SCAN_TIMEOUT = 20000;
    private static final long SCAN_UPDATE_TIME = 500;
    private static final long TIMEOUT_CONNECT = 30000;

    private static final long MAX_CONNECTED_DEVICES = 6;


    public interface OnFinishFragment {
        public void finished();
    }

    public interface OnColorChange {
        public void colorChanged(int color);
    }

    private ArrayList<OnColorChange> mOnColorChange = new ArrayList<>();

    private ColorListener mColorListener = new ColorListener() {
        @Override
        public void onColorSelected(int color, boolean fromUser) {
            mColorSelection = color;
            if (false) Log.i(TAG, String.format("device color selected 0x%06X", color & Utils.COLOR_MASK));
        }
    };

    AllServicesDeviceDataHandler.OnReadRemoteBatteryLevel mOnReadRemoteBatteryLevel = new AllServicesDeviceDataHandler.OnReadRemoteBatteryLevel() {
        @Override
        public void remoteBatteryLevel(@NotNull BluetoothPeripheral peripheral, int batterylevel) {
            pairedDevicesListAdapter.updateBattery(peripheral.getAddress(), batterylevel);
        }
    };

    AllServicesDeviceDataHandler.OnReadRemoteRssi mOnReadRemoteRssi = new AllServicesDeviceDataHandler.OnReadRemoteRssi() {
        @Override
        public void remoteRssi(@NotNull BluetoothPeripheral peripheral, int rssi) {
            pairedDevicesListAdapter.updateRssi(peripheral.getAddress(), rssi);
        }
    };

    private GoSensePagerActivity.OnConnnectionStates mDeviceOnConnnectionStates = new GoSensePagerActivity.OnConnnectionStates() {
        @Override
        public void onConnected(@NotNull BluetoothPeripheral peripheral) {
            onDeviceConnected();
            GoSensePagerActivity.registerOnReadRemoteBatteryLevel(mOnReadRemoteBatteryLevel);
            GoSensePagerActivity.registerOnReadRemoteRssi(mOnReadRemoteRssi);
            pairedDevicesListAdapter.updateConnected(peripheral.getAddress(), true);

            if (pairedDevicesListAdapter.getItemCount() == 1) {
                pairedDevicesListAdapter.updateSelected(peripheral.getAddress(), true);
            } else {
                ArrayList<SensorPairListData> list = pairedDevicesListAdapter.getAdapterList();
                if (TraySettings.getUseNewConnectionAsPrimary()) {
                    for (SensorPairListData sensorPairListData : list) {
                        pairedDevicesListAdapter.updateSelected(sensorPairListData.getAddress(), false);
                    }
                    pairedDevicesListAdapter.updateSelected(peripheral.getAddress(), true);
                } else {
                    boolean selected = false;
                    for (SensorPairListData sensorPairListData : list) {
                        if (sensorPairListData.getSelected()) {
                            selected = true;
                            break;
                        }
                    }
                    if (!selected) {
                        pairedDevicesListAdapter.updateSelected(peripheral.getAddress(), true);
                    }
                }
            }

            actionScanButton.setText(getString(R.string.start_scan));
            progressBar.setVisibility(View.GONE);
            try {
                mHandler.removeCallbacks(timeoutConnect);
            } catch (Exception e) {

            }
            empty_device_list_msg.setVisibility(View.VISIBLE);
            listView.setVisibility(View.INVISIBLE);

            if (pairedDevicesListAdapter.getItemCount() > 0) {
                no_device_notifications.setVisibility(View.GONE);
            }
        }

        @Override
        public void onDisconnected(@NotNull BluetoothPeripheral peripheral, @NotNull HciStatus status) {
            pairedDevicesListAdapter.updateBattery(peripheral.getAddress(), -1);
            pairedDevicesListAdapter.updateRssi(peripheral.getAddress(), -9999);
            pairedDevicesListAdapter.updateConnected(peripheral.getAddress(), false);
            pairedDevicesListAdapter.updateSelected(peripheral.getAddress(), false);
            GoSensePagerActivity.unregisterOnReadRemoteBatteryLevel(mOnReadRemoteBatteryLevel);
            GoSensePagerActivity.unregisterOnReadRemoteRssi(mOnReadRemoteRssi);
        }
    };

    private final DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.ENGLISH);
    AdapterView.OnItemClickListener onItemClickListener = new AdapterView.OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            ScannedDevicesData item = scanAdapter.getDevice(position);

            connectToName = item.getName();
            connectToAddress = item.getAddress();

            ScannedDevicesData device = scanAdapter.getDevice(position);
            if (device == null) {
                Toast.makeText(getActivity(), getString(R.string.device_failed), Toast.LENGTH_SHORT).show();
                return;
            } else {
                SensorDevice storedDevice = TraySettings.getActiveSensorDevice();;
                Log.i(TAG, "Name    : " + connectToName);
                Log.i(TAG, "Address : " + connectToAddress);
                currentDevice = false;
                handleDeviceSelection(connectToAddress, connectToName);
            }
        }
    };

    private ColorPickerDialog.Builder mColorPickerDialogBuilder;
    private int mColorSelection;
    private ColorPickerView mColorPickerView;
    private boolean pairing = false;
    private static boolean hideUnknowns = true;
    private SensorDevice activePeripheral;
    private SensorDevice oldbtdevice;
    private boolean currentDevice;
    private View mRootView;
    private AlertDialog mConnectionDialog;
    private Handler mHandler;
    private ServiceConnection mServiceConnection;
    private ScannedDevicesListAdapter scanAdapter;
    private boolean mUpdateData = true;
    private Context mContext;
    private boolean debug = false;
    private int currentRequestCode = 0;
    private ListView listView;
    private Map<String, ScannedDevicesData> mScannedDevices = new HashMap();
    private RecyclerView connectedDevices;
    private PairedDevicesListAdapter pairedDevicesListAdapter;
    private int itemPosition;
    private TextView empty_device_list_msg;
    private TextView no_device_notifications;
    private FragmentDevicesBinding binding;
    private ProgressBar progressBar;
    private ArrayList<SensorDevice> sensorDevicesList;
    private MaterialButton actionScanButton;
    private AllDevicesService mAllDevicesService;
    private String attemptAddress;

    public class VerticalSpaceItemDecoration extends RecyclerView.ItemDecoration {

        private final int verticalSpaceHeight;

        public VerticalSpaceItemDecoration(int verticalSpaceHeight) {
            this.verticalSpaceHeight = verticalSpaceHeight;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
                                   RecyclerView.State state) {
            outRect.bottom = verticalSpaceHeight;
        }
    }

    private String connectToName;
    private String connectToAddress;
    private int connectToColor =  Utils.DEFAULT_COLOR;
    private SensorPairListData connectToDeviceData;


    private final Runnable timeoutConnect = new Runnable() {
        @Override
        public void run() {
            cancelConnect();
            String toastTimeoutMsg = getString(R.string.toast_pairing_timeout);
            if (currentDevice) {
                toastTimeoutMsg = getString(R.string.toast_connection_timeout);
            }
            Toast.makeText(mContext, toastTimeoutMsg, Toast.LENGTH_LONG).show();
            currentDevice = false;

            if (attemptAddress != null) {
                pairedDevicesListAdapter.updateSelected(attemptAddress, false);
            }
        }
    };

    private static DevicesFragment mFragment;

    public static DevicesFragment getInstance() {
        if (mFragment == null) {
            mFragment = new DevicesFragment();
        }
        return mFragment;
    }

    private AllServicesDeviceDataHandler.OnServicesDiscovery mOnServicesDiscovery = new AllServicesDeviceDataHandler.OnServicesDiscovery() {
        @Override
        public void onServicesDiscovered(@NotNull BluetoothPeripheral peripheral) {
            BluetoothGattCharacteristic batterTimeCharacteristic = peripheral.getCharacteristic(BATTERY_SERVICE_UUID, BATTERY_LEVEL_CHARACTERISTIC_UUID);
            BluetoothGattCharacteristic tempTimeCharacteristic = peripheral.getCharacteristic(UUID_TEMP_RH_SERVICE, UUID_TEMP_NOTIFICATIONS);
            BluetoothGattCharacteristic rhTimeCharacteristic = peripheral.getCharacteristic(UUID_TEMP_RH_SERVICE, UUID_RH_NOTIFICATIONS);
            if (batterTimeCharacteristic != null) {
                Log.i(TAG, "Has Battery Level Characteristic");
            }
            if (tempTimeCharacteristic != null) {
                Log.i(TAG, "Has Temperature Characteristic");
            }
            if (rhTimeCharacteristic != null) {
                Log.i(TAG, "Has Relative Humidity Characteristic");
            }
            if (rhTimeCharacteristic == null || tempTimeCharacteristic == null) {
                Log.i(TAG, "Device missing all required characteristics");
                String msg = peripheral.getName() + " " + getString(R.string.not_supported_device);
                Log.i(TAG, msg);
                Toast.makeText(mContext, msg, Toast.LENGTH_LONG).show();
                peripheral.cancelConnection();
                if (oldbtdevice != null) {
                    TraySettings.setActiveSensorDevice(oldbtdevice);
                    oldbtdevice = null;
                }
                return;
            }

            if (!isInDatabase(peripheral.getAddress())) {
                addToPairedDatabase(peripheral);
            }
        }
    };

    public boolean isInDatabase(String address) {
        ArrayList<SensorPairListData> list = pairedDevicesListAdapter.getAdapterList();
        for (SensorPairListData sensorPairListData : list) {
            if (Utils.addressIsSame(sensorPairListData.getAddress(), address)) {
                return true;
            }
        }
        return false;
    }

    public void addToPairedDatabase(BluetoothPeripheral peripheral) {
        Log.i(TAG, "SELECTED SCANNED DEVICE: " + peripheral.getName() + " " + peripheral.getAddress());

        SensorPairListData newSensorPairListData = new SensorPairListData();
        newSensorPairListData.setConnected(true);
        newSensorPairListData.setSelected(TraySettings.getUseNewConnectionAsPrimary());
        newSensorPairListData.setName(peripheral.getName());
        newSensorPairListData.setAliasName(peripheral.getName());
        newSensorPairListData.setAddress(peripheral.getAddress());
        newSensorPairListData.setColor(Utils.DEFAULT_COLOR);
        newSensorPairListData.setSelected(false);
        newSensorPairListData.setRssi(-9999);
        newSensorPairListData.setBattery(-1);

        Log.i(TAG, "Adding newly selected device to database");

        List<BluetoothPeripheral> peripherals = mAllDevicesService.getConnectedPeripherals();

        ArrayList<SensorPairListData> list = pairedDevicesListAdapter.getAdapterList();
        if (list.size() > 0) {
            pairedDevicesListAdapter.addSensorDevice(newSensorPairListData);
            list = pairedDevicesListAdapter.getAdapterList();

            if (TraySettings.getUseNewConnectionAsPrimary()) {
                for (SensorPairListData sensorPairListData : list) {
                    String address = sensorPairListData.getAddress();
                    pairedDevicesListAdapter.updateSelected(address, false);
                }
                pairedDevicesListAdapter.updateSelected(newSensorPairListData.getAddress(), true);
                TraySettings.setActiveSensorPairedDevice(newSensorPairListData);
            } else {
                if (oldbtdevice != null) {
                    pairedDevicesListAdapter.updateSelected(oldbtdevice.getAddress(), true);
                }
                pairedDevicesListAdapter.updateSelected(newSensorPairListData.getAddress(), false);
            }
        } else {
            pairedDevicesListAdapter.addSensorDevice(newSensorPairListData);
            pairedDevicesListAdapter.updateSelected(newSensorPairListData.getAddress(), true);
        }

        //pairedDevicesListAdapter.clear();

        int size = pairedDevicesListAdapter.getItemCount();
        for (int oo = 0; oo < size; oo++) {
            pairedDevicesListAdapter.notifyItemChanged(oo);
        }
        synchronized (pairedDevicesListAdapter) {
            pairedDevicesListAdapter.notifyAll();
        }

        GoSenseDb goSenseDb = new GoSenseDb(mContext);
        goSenseDb.saveSensorDevice(newSensorPairListData);
        goSenseDb.close();

        pairedDevicesListAdapter.updateItems();

        no_device_notifications.setVisibility(View.GONE);

        oldbtdevice = null;
    }

    @SuppressLint("WrongConstant")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startAllDevicesService();
        GoSenseDb goSenseDb = new GoSenseDb(mContext);
        sensorDevicesList = goSenseDb.getPairedSensorDevices();
        goSenseDb.close();
        GoSensePagerActivity.registerOnConnnectionStates(mDeviceOnConnnectionStates);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentDevicesBinding.inflate(inflater, container, false);
        mRootView = binding.getRoot();

        no_device_notifications = mRootView.findViewById(R.id.no_device_notifications);
        empty_device_list_msg = mRootView.findViewById(R.id.start_device_scan);

        int dividerHeight = (int)Utils.pixelsFromDp(mContext, 2);
        connectedDevices = mRootView.findViewById(R.id.connected_device_list);
        connectedDevices.setLayoutManager(new LinearLayoutManager(mContext));
        connectedDevices.addItemDecoration(new VerticalSpaceItemDecoration(dividerHeight));

        if (activePeripheral == null) {
            activePeripheral = TraySettings.getActiveSensorDevice();
        }

        pairedDevicesListAdapter = new PairedDevicesListAdapter(mContext);
        pairedDevicesListAdapter.setOnItemClickListener(new PairedDevicesListAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                Log.i(TAG, "Clicked device position " + position);
                int menu = R.menu.device_disconnection_menu;
                itemPosition = position;

                ArrayList<SensorPairListData> list = pairedDevicesListAdapter.getAdapterList();
                connectToDeviceData = list.get(position);
                BluetoothPeripheral peripheral =
                        mAllDevicesService.getCentral().getPeripheral(connectToDeviceData.getAddress());
                boolean isConnected = peripheral.getState() == ConnectionState.CONNECTED;
                boolean onlyOneDevice = pairedDevicesListAdapter.getAdapterList().size() == 1;

                connectToName = connectToDeviceData.getAliasName();
                connectToAddress = connectToDeviceData.getAddress();
                connectToColor = connectToDeviceData.getColor();

                boolean selected = false;
                if (list.size() == 1) {
                    selected = false;
                } else {
                    selected = connectToDeviceData.getSelected();
                }

                if (!isConnected) {
                    menu = R.menu.device_connect_menu;
                }
                Menu optionMenu = IconContextMenu.newMenu(mContext, menu);

                if (false) {
                    if (selected || !isConnected || onlyOneDevice) {
                        optionMenu.removeItem(R.id.action_preferred);
                    }
                } else {
                    optionMenu.removeItem(R.id.action_preferred);
                }

                IconContextMenu iconContextMenu = new IconContextMenu(mContext, optionMenu, IconContextMenu.THEME_DARK, SANS);
                iconContextMenu.setOnIconContextItemSelectedListener(new IconContextItemSelectedListener() {
                    @Override
                    public void onIconContextItemSelected(MenuItem item, Object info) {
                        if (item.getItemId() == R.id.action_delete) {
                            SensorPairListData device = pairedDevicesListAdapter.getSensorDevice(itemPosition);
                            if (device == null) {
                                Toast.makeText(getActivity(), getString(R.string.device_failed), Toast.LENGTH_SHORT).show();
                                return;
                            }

                            sensorDevicesList.remove(itemPosition);
                            pairedDevicesListAdapter.removeSensorDevice(itemPosition);

                            GoSenseDb goSenseDb = new GoSenseDb(mContext);
                            goSenseDb.deleteSensorDevice(device.getAddress());
                            goSenseDb.close();

                            ArrayList<SensorPairListData> list = pairedDevicesListAdapter.getAdapterList();
                            if (list.size() > 0) {
                                if (list.size() == 1) {
                                    pairedDevicesListAdapter.updateSelected(list.get(0).getAddress(), true);
                                } else {
                                    for (SensorPairListData sensorPairListData: list) {

                                    }
                                }
                                no_device_notifications.setVisibility(View.GONE);
                            } else {
                                no_device_notifications.setVisibility(View.VISIBLE);
                            }

                            pairedDevicesListAdapter.notifyDataSetChanged();

                            connectToName = null;
                            connectToAddress = null;

                            return;
                        }

                        if (item.getItemId() == R.id.action_disconnect) {
                            SensorPairListData device = pairedDevicesListAdapter.getSensorDevice(itemPosition);
                            if (device == null) {
                                Toast.makeText(getActivity(), getString(R.string.device_failed), Toast.LENGTH_SHORT).show();
                                iconContextMenu.dismiss();
                                return;
                            }

                            mAllDevicesService.disconnect(peripheral.getAddress());
                            pairedDevicesListAdapter.updateSelected(peripheral.getAddress(), false);

                            connectToName = null;
                            connectToAddress = null;

                            return;
                        }

                        if (item.getItemId() == R.id.action_set_color) {
                            setDeviceGraphColor();
                        }

                        if (item.getItemId() == R.id.action_preferred) {
                            ArrayList<SensorPairListData> list = pairedDevicesListAdapter.getAdapterList();
                            SensorPairListData sensorDeviceData = list.get(itemPosition);
                            if (list.size() == 1) {
                                pairedDevicesListAdapter.updateSelected(sensorDeviceData.getAddress(), true);
                            } else {
                                for (SensorPairListData sensorPairListData : list) {
                                    pairedDevicesListAdapter.updateSelected(sensorPairListData.getAddress(), false);
                                }
                                pairedDevicesListAdapter.updateSelected(sensorDeviceData.getAddress(), true);
                            }
                            list = null;

                            connectToName = null;
                            connectToAddress = null;
                            iconContextMenu.dismiss();
                            return;
                        }

                        if (item.getItemId() == R.id.action_connect) {
                            SensorPairListData device = pairedDevicesListAdapter.getSensorDevice(itemPosition);
                            if (device == null) {
                                Toast.makeText(getActivity(), getString(R.string.device_failed), Toast.LENGTH_SHORT).show();
                                iconContextMenu.dismiss();
                                return;
                            }
                            //BluetoothService bluetoothService = GoSensePagerActivity.getBluetoothService();
                            if (mAllDevicesService != null) {
                                //SensorDevice storedDevice = TraySettings.getActiveSensorDevice();;

                                List<BluetoothPeripheral> connectedList = mAllDevicesService.getConnectedPeripherals();
                                if (connectedList.size() > 1) {
                                    Toast.makeText(mContext, getString(R.string.disconnecting_current_devices),  Toast.LENGTH_LONG).show();
                                    for (BluetoothPeripheral connectedDevice: connectedList) {
                                        if (Utils.addressIsSame(connectToAddress, connectedDevice.getAddress())) {
                                            Toast.makeText(mContext, getString(R.string.device_is_connected),  Toast.LENGTH_LONG).show();
                                            return;
                                        } else {
                                            connectedDevice.cancelConnection();
                                        }
                                    }
                                } else if (connectedList.size() == 1) {
                                    Toast.makeText(mContext, getString(R.string.disconnecting_current_device),  Toast.LENGTH_LONG).show();
                                    connectedList.get(0).cancelConnection();
                                }


                                Log.i(TAG, "Name    : " + connectToName);
                                Log.i(TAG, "Address : " + connectToAddress);

                                currentDevice = false;
                                handleDeviceSelection(connectToAddress, connectToName);
                                pairedDevicesListAdapter.notifyDataSetChanged();
                                iconContextMenu.dismiss();

                                connectToName = null;
                                connectToAddress = null;
                                iconContextMenu.dismiss();
                                return;
                            }

                            Toast.makeText(getActivity(), getString(R.string.device_failed), Toast.LENGTH_SHORT).show();

                            connectToName = null;
                            connectToAddress = null;
                            iconContextMenu.dismiss();
                            return;
                        }

                        connectToName = null;
                        connectToAddress = null;
                        iconContextMenu.dismiss();
                    }
                });
                iconContextMenu.show();
            }
        });

        if (sensorDevicesList.size() > 0) {
            for (int pp = 0; pp < sensorDevicesList.size(); pp++) {
                boolean selected = false;
                if (activePeripheral != null) {
                    String address1 = activePeripheral.getAddress().toLowerCase(Locale.US);
                    String address2 = sensorDevicesList.get(pp).getAddress().toLowerCase(Locale.US);
                    if (address1.contains(address2)) {
                        selected = true;
                    }
                }
                SensorDevice sensorDevice = sensorDevicesList.get(pp);
                SensorPairListData sensorPairListData = new SensorPairListData();
                sensorPairListData.setAddress(sensorDevice.getAddress());
                sensorPairListData.setName(sensorDevice.getName());
                sensorPairListData.setAliasName(sensorDevice.getAliasName());
                sensorPairListData.setColor(sensorDevice.getColor());
                sensorPairListData.setBattery(-1);
                sensorPairListData.setRssi(-9999);
                sensorPairListData.setConnected(false);
                sensorPairListData.setSelected(false);
                pairedDevicesListAdapter.addSensorDevice(sensorPairListData);
            }
            no_device_notifications.setVisibility(View.GONE);
        }
        connectedDevices.setAdapter(pairedDevicesListAdapter);

        return mRootView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mHandler = new Handler(Looper.getMainLooper());

        initScanWindow();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        if (hidden) {
            stopBtScan();
            if (mServiceConnection != null) {
                getActivity().unbindService(mServiceConnection);
            }
        } else {
            startAllDevicesService();
            initScanWindow();
        }
        Log.i(TAG, "onHiddenChanged: " + hidden);
    }

    private void startAllDevicesService() {
        if (mAllDevicesService == null) {
            mAllDevicesService = GoSensePagerActivity.getAllDevicesService();
            if (mAllDevicesService != null) {
                mAllDevicesService.setOnBluetoothDeviceDiscovery(mOnBluetoothDeviceDiscovery);
            }
            if (mAllDevicesService == null) {
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        startAllDevicesService();
                    }
                }, 100);
            } else {
                mAllDevicesService.registerOnServicesDiscovery(mOnServicesDiscovery);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION && grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startBtScan();
            }
        } else if (requestCode == REQUEST_NOTIFICATION && grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startBtScan();
            }
        } else if (requestCode != REQUEST_BT_PERMISSION || grantResults.length <= 0) {
            Log.e(TAG, "ERROR: No BT permissions!");
        } else {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startBtScan();
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    AllServicesDeviceDataHandler.OnBluetoothDeviceDiscovery mOnBluetoothDeviceDiscovery = new AllServicesDeviceDataHandler.OnBluetoothDeviceDiscovery() {
        @Override
        public void onDiscoveredPeripheral(@NotNull BluetoothCentralManager central, @NotNull BluetoothPeripheral peripheral, @NotNull ScanResult scanResult) {
            if (peripheral.getState() == ConnectionState.CONNECTED) {
                return;
            }
            if (scanResult != null) {
                updateScannedDevice(peripheral.getAddress(), peripheral, scanResult.getRssi());
                if (mUpdateData) {
                    mUpdateData = false;
                    scanAdapter.updateDevice(mScannedDevices);
                    scanAdapter.notifyDataSetChanged();
                    mHandler.postDelayed(new Runnable() {
                                             @Override
                                             public void run() {
                                                 mUpdateData = true;
                                                 scanAdapter.updateDevice(mScannedDevices);
                                             }
                                         },
                            SCAN_UPDATE_TIME);
                }
                if (debug) {
                    Log.d(TAG, scanResult.toString());
                }
                return;
            }
            Log.e(TAG, "Scanned failed device!");
        }
    };

    public void initScanWindow() {
        if (mAllDevicesService != null) {
            mAllDevicesService.setOnBluetoothDeviceDiscovery(mOnBluetoothDeviceDiscovery);
        }

        scanAdapter = new ScannedDevicesListAdapter(requireActivity());
        listView = mRootView.findViewById(R.id.scanning_device_list);
        listView.setFooterDividersEnabled(true);
        listView.setAdapter((ListAdapter) scanAdapter);
        listView.setOnItemClickListener(onItemClickListener);

        progressBar = mRootView.findViewById(R.id.scanning_spinner);
        progressBar.setVisibility(View.INVISIBLE);
        actionScanButton = mRootView.findViewById(R.id.scan_button);
        actionScanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (progressBar.getVisibility() == View.VISIBLE) {
                    stopBtScan();
                } else {
                    startBtScan();
                }
            }
        });
    }

    private void stopBtScan() {
        progressBar.setVisibility(View.GONE);
        actionScanButton.setText(getString(R.string.start_scan));
        if (mAllDevicesService != null) {
            mAllDevicesService.stopBtScan();
        }
        if (scanAdapter.getCount() == 0) {
            empty_device_list_msg.setVisibility(View.VISIBLE);
        } else {
            empty_device_list_msg.setVisibility(View.GONE);
        }
    }

    private void startBtScan() {
        empty_device_list_msg.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
        listView.setVisibility(View.VISIBLE);
        actionScanButton.setText(getString(R.string.stop_scan));
        if (mAllDevicesService != null) {
            mAllDevicesService.startBtScan();
            scanAdapter.clear();
            scanAdapter.notifyDataSetChanged();
            mHandler.postDelayed(timeoutBtScan, SCAN_TIMEOUT);
        }
    }

    public Runnable timeoutBtScan = new Runnable() {
        @Override
        public void run() {
            stopBtScan();
        }
    };

    public void initServiceConnection() {
        Log.i(TAG, "Start init BluetoothLeService");
        if (mServiceConnection != null) {
            if (mServiceConnection != null) {
                try {
                    getActivity().unbindService(mServiceConnection);
                } catch (Exception e) {
                }
            }
        }
        mServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder service) {
                if (service == null) {
                    return;
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                Log.e(TAG, "BluetoothLeService has been killed!");
            }
        };
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                startBtScan();
            }
        } else if (requestCode == REQUEST_LOCATION) {
            startBtScan();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mAllDevicesService != null) {
            mAllDevicesService.registerOnServicesDiscovery(mOnServicesDiscovery);
        }
        GoSensePagerActivity.registerOnConnnectionStates(mDeviceOnConnnectionStates);
        if (TraySettings.getAutoConnectDevicesOnStart()) {
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    SensorDevice sensorDevice = TraySettings.getActiveSensorDevice();
                    if (sensorDevice == null) {
                        Log.i(TAG, "No previously selected devices found");
                    } else {
                        connectToColor = sensorDevice.getColor();
                        connectToAddress = sensorDevice.getAddress();
                        connectToName = sensorDevice.getAliasName();
                        if (connectToName == null || connectToName.isEmpty()) {
                            connectToName = getString(R.string.empty_bluetooth_device);
                        }
                        Log.i(TAG, "Attempting auto reconnect on start");
                        Log.i(TAG, "Name    : " + connectToName);
                        Log.i(TAG, "Address : " + connectToAddress);
                        currentDevice = false;
                        handleDeviceSelection(connectToAddress, connectToName);
                    }
                }
            }, 5000);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        stopBtScan();
        GoSensePagerActivity.unregisterOnConnnectionStates(mDeviceOnConnnectionStates);
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
    public void onDestroy() {
        stopBtScan();
        stopProgress();
        mHandler.removeCallbacksAndMessages(null);
        if (mAllDevicesService != null) {
            mAllDevicesService.unregisterOnServicesDiscovery(mOnServicesDiscovery);
        }
        if (mServiceConnection != null) {
            try {
                getActivity().unbindService(mServiceConnection);
            } catch (Exception e) {
            }
        }
        super.onDestroy();
    }

    private void cancelConnect() {
        if (oldbtdevice != null) {
            TraySettings.setActiveSensorDevice(oldbtdevice);
            oldbtdevice = null;
        }

        try {
            mHandler.removeCallbacks(timeoutConnect);
        } catch (Exception e) {
        }
        stopProgress();
        scanAdapter.notifyDataSetChanged();
    }

    private void startDialogProgress(String message, DialogInterface.OnCancelListener cancelListener) {
        stopProgress();
        mConnectionDialog = new AlertDialog.Builder(mContext).setMessage(message).setOnCancelListener(cancelListener).show();
        TextView messageText = mConnectionDialog.findViewById(android.R.id.message);
        messageText.setGravity(Gravity.CENTER);
        messageText.setAllCaps(true);
        mConnectionDialog.show();
    }

    private void stopProgress() {
        if (mConnectionDialog != null) {
            mConnectionDialog.dismiss();
            mConnectionDialog = null;
        }
    }

    private void updateScannedDevice(String address, BluetoothPeripheral device, int rssi) {
        if (mScannedDevices.containsKey(address)) {
            ScannedDevicesData scannedDevicesData = mScannedDevices.get(address);
            if (scannedDevicesData != null) {
                scannedDevicesData.setRssi(rssi);
                return;
            }
            return;
        }
        ScannedDevicesData scannedDevicesData = new ScannedDevicesData(device, rssi);
        if (device.getName() == null || device.getName().isEmpty()) {
            String name = Utils.getNameFromPairedDb(mContext, address);
            scannedDevicesData.setName(name);
        }
        mScannedDevices.put(address, scannedDevicesData);
    }

    private void showNotSupportedDeviceAlert() {
        new AlertDialog.Builder(mContext).
                setTitle(R.string.device_not_supported).
                setMessage(R.string.device_not_supported_message).
                setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        }).
                show();
    }

    private boolean isPermissionGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(mContext, BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(mContext, BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                requestBtPermission(new String[]{ BLUETOOTH_SCAN, BLUETOOTH_CONNECT}, 3);
                return false;
            }
        } else {
            if (ContextCompat.checkSelfPermission(mContext, ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestLocationPermission(REQUEST_LOCATION);
                return false;
            }
        }
        return true;
    }

    private boolean isLocationEnabledIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S && isLocationServiceDisabled()) {
            requestEnableLocation(REQUEST_LOCATION);
            return false;
        }
        return true;
    }

    private boolean isLocationServiceDisabled() {
        LocationManager locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return !locationManager.isLocationEnabled();
        }
        if (locationManager != null) {
            return (locationManager.isProviderEnabled("network") || locationManager.isProviderEnabled("gps")) ? false : true;
        }
        return true;
    }

    private void requestBtPermission(String[] permissions, int requestCode) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), permissions[0])) {
            new AlertDialog.Builder(mContext).
                    setTitle(R.string.title_bt_permission).
                    setMessage(R.string.text_bt_permission).
                    setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(getActivity(), permissions, requestCode);
                        }
                    }).
                    create().
                    show();
        } else {
            ActivityCompat.requestPermissions(getActivity(), permissions, requestCode);
        }
    }

    private void requestLocationPermission(int requestCode) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), ACCESS_FINE_LOCATION)) {
            new AlertDialog.Builder(mContext).
                    setTitle(R.string.title_location_permission).
                    setMessage(R.string.text_location_permission).
                    setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(getActivity(), new String[]{ ACCESS_FINE_LOCATION }, requestCode);
                        }
                    }).
                    create().
                    show();
        } else {
            ActivityCompat.requestPermissions(getActivity(), new String[]{ ACCESS_FINE_LOCATION }, requestCode);
        }
    }

    private void requestEnableLocation(int requestCode) {
        currentRequestCode = requestCode;
        new AlertDialog.Builder(mContext).
                setTitle(R.string.title_location_enable).
                setMessage(R.string.text_location_enable).
                setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            ActivityResultLauncher<Intent> someActivityResultLauncher = registerForActivityResult(
                                    new ActivityResultContracts.StartActivityForResult(),
                                    new ActivityResultCallback<ActivityResult>() {
                                        @Override
                                        public void onActivityResult(ActivityResult result) {
                                            if (currentRequestCode == REQUEST_ENABLE_BT) {
                                                if (result.getResultCode() == Activity.RESULT_OK) {
                                                    startBtScan();
                                                } else {
                                                    dialog.dismiss();
                                                }
                                            } else if (currentRequestCode == REQUEST_LOCATION) {
                                                startBtScan();
                                            }
                                        }
                                    });
                            someActivityResultLauncher.launch(new Intent(ACTION_LOCATION_SOURCE_SETTINGS));
                        } else {
                            startActivityForResult(new Intent(ACTION_LOCATION_SOURCE_SETTINGS), requestCode);
                        }
                    }
                }).
                setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).
                setCancelable(false).
                show();
    }

    private void sendAppBroadcast(String action) {
        Intent intent = new Intent(action);
        intent.setPackage(mContext.getPackageName());
        getActivity().sendBroadcast(intent);
    }

    private void handleDeviceSelection(String address, String name) {
        oldbtdevice = TraySettings.getActiveSensorDevice();
        attemptAddress = address;

        SensorDevice newActiveDevice = new SensorDevice();
        newActiveDevice.setAddress(attemptAddress);
        newActiveDevice.setName(name);
        newActiveDevice.setAliasName(name);
        TraySettings.setActiveSensorDevice(newActiveDevice);

        /* HACK to work around concurrency for storing values */
        while (true) {
            SensorDevice btdevice = TraySettings.getActiveSensorDevice();;
            if (btdevice != null) {
                if (btdevice.getAddress().contains(attemptAddress)) {
                    break;
                }
            }
            SensorDevice newDevice = new SensorDevice();
            newDevice.setAddress(attemptAddress);
            newDevice.setName(name);
            TraySettings.setActiveSensorDevice(newDevice);
        }

        stopBtScan();

        List<BluetoothPeripheral> list = mAllDevicesService.getCentral().getConnectedPeripherals();
        if (list != null && list.size() > 0) {
            for (BluetoothPeripheral peripheral: list) {
                if (peripheral.getAddress().toLowerCase(Locale.US).contains(attemptAddress.toLowerCase(Locale.US))) {
                    mAllDevicesService.getCentral().cancelConnection(peripheral);
                    break;
                }
            }
        }
        BluetoothPeripheral peripheral =  mAllDevicesService.getCentral().getPeripheral(attemptAddress);
        if (peripheral == null) {
            mAllDevicesService.connect(attemptAddress);
        } else {
            mAllDevicesService.connect(peripheral);
        }

        boolean isInList = false;
        ArrayList<SensorPairListData> adapterList = pairedDevicesListAdapter.getAdapterList();
        for (SensorPairListData sensorPairListData: adapterList) {
            if (Utils.addressIsSame(sensorPairListData.getAddress(), attemptAddress)) {
                isInList = true;
                break;
            }
        }

        mHandler.postDelayed(timeoutConnect, TIMEOUT_CONNECT);
        String msg;
        if (isInList) {
            msg = getString(R.string.attempting_reconnection);
        } else {
            msg = getString(R.string.attempting_connection);
        }
        pairing = false;
        GoSenseDb goSenseDb = new GoSenseDb(mContext);
        boolean currentlyPaired = false;
        ArrayList<SensorDevice> pairedDeviceList = goSenseDb.getPairedSensorDevices();
        if (pairedDeviceList != null && pairedDeviceList.size() > 0) {
            for (SensorDevice sensorDevice: pairedDeviceList) {
                String address1 = sensorDevice.getAddress().toLowerCase(Locale.US);
                String address2 = newActiveDevice.getAddress().toLowerCase(Locale.US);
                if (address2.contains(address1)) {
                    currentlyPaired = true;
                    break;
                }
            }
        }
        if (!currentlyPaired) {
            pairing = true;
        }
        startDialogProgress(msg, new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                cancelConnect();
                if (oldbtdevice != null) {
                    if (oldbtdevice.getName() != null && oldbtdevice.getAddress() != null) {
                        TraySettings.setActiveSensorDevice(oldbtdevice);
                    }
                }
            }
        });
    }

    public void onDeviceConnected() {
        stopProgress();
        mHandler.removeCallbacks(timeoutConnect);
        scanAdapter.notifyDataSetChanged();
    }

    public void onPaired() {
        SensorDevice sensorDevice = TraySettings.getActiveSensorDevice();
        String connection = "Device is paired";
        if (sensorDevice != null &&
                sensorDevice.getAliasName() != null &&
                !sensorDevice.getAliasName().isEmpty()) {
            connection = sensorDevice.getAliasName() + " is paired";
        }

        GoSenseDb goSenseDb = new GoSenseDb(mContext);
        goSenseDb.saveSensorDevice(sensorDevice);
        goSenseDb.close();

        pairing  = false;

        Toast.makeText(mContext, connection, Toast.LENGTH_LONG).show();
        stopProgress();
        mHandler.removeCallbacks(timeoutConnect);
        scanAdapter.notifyDataSetChanged();
    }

    private Point colorPoint;

    public void setDeviceGraphColor() {
        mColorPickerDialogBuilder = new ColorPickerDialog.Builder(getContext());
        mColorPickerDialogBuilder.setNegativeButton(getString(R.string.dismiss),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mColorSelection = connectToColor;
                        connectToDeviceData.setColor(connectToColor);
                        mColorPickerView.unregisterColorListener(mColorListener);
                        dialog.dismiss();
                    }
                });
        mColorPickerDialogBuilder.setPositiveButton(getString(R.string.apply),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        connectToColor = mColorSelection;
                        connectToDeviceData.setColor(mColorSelection);
                        pairedDevicesListAdapter.updateColor(connectToDeviceData.getAddress(), mColorSelection);
                        GoSenseDb goSenseDb = new GoSenseDb(mContext);
                        goSenseDb.updateSensorDevice(connectToDeviceData);
                        goSenseDb.close();

                        if (mOnColorChange.size() > 0) {
                            for (OnColorChange onColorChange : mOnColorChange) {
                                onColorChange.colorChanged(mColorSelection);
                            }
                        }

                        mColorPickerView.unregisterColorListener(mColorListener);
                        dialog.dismiss();
                    }
                });
        mColorPickerDialogBuilder.setNeutralButton(getString(R.string.reset), null);
        mColorPickerDialogBuilder.setTitle(getString(R.string.set_graph_color));
        mColorPickerDialogBuilder.attachAlphaSlideBar(false);
        mColorPickerDialogBuilder.attachBrightnessSlideBar(true);
        mColorPickerView = mColorPickerDialogBuilder.getColorPickerView();
        mColorPickerView.selectByHsv(connectToColor);
        colorPoint = mColorPickerView.getSelectedPoint();
        mColorPickerView.registerColorListener(mColorListener);
        mColorPickerDialogBuilder.setDimAmount(0.9f);
        mColorPickerDialogBuilder.setGravity(Gravity.CENTER);
        mColorPickerDialogBuilder.useCustomGravity(true);
        mColorPickerDialogBuilder.setCancelable(false);
        mColorPickerDialogBuilder.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                mColorPickerView.selectByHsv(connectToColor);
                colorPoint = mColorPickerView.getSelectedPoint();
                mColorPickerView.setSelectorPoint(colorPoint.x, colorPoint.y);
                Button button = ((androidx.appcompat.app.AlertDialog) dialog).getButton(AlertDialog.BUTTON_NEUTRAL);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mColorPickerView.selectByHsv(connectToColor);
                        Log.i(TAG, "neutral clicked");
                    }
                });
            }
        });
        mColorPickerDialogBuilder.show();
    }

    public void registerOnColorChange(OnColorChange onColorChange) {
        if (onColorChange != null && !mOnColorChange.contains(onColorChange)) {
            mOnColorChange.add(onColorChange);
        }
    }

    public void unregisterOnColorChange(OnColorChange onColorChange) {
        if (onColorChange != null && mOnColorChange.contains(onColorChange)) {
            mOnColorChange.remove(onColorChange);
        }
    }
}
package com.draekko.gosense.ui.dashboard;

import static com.draekko.gosense.misc.Utils.PSI_VALUE_ONE_MILLIBAR;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.draekko.gosense.R;
import com.draekko.gosense.activities.GoSensePagerActivity;
import com.draekko.gosense.data.RemoteBatteryLevelMeasurement;
import com.draekko.gosense.data.RemoteRelativeHumidityMeasurement;
import com.draekko.gosense.data.RemoteTemperatureMeasurement;
import com.draekko.gosense.data.SensorDevice;
import com.draekko.gosense.databinding.FragmentDashboardBinding;
import com.draekko.gosense.misc.Converter;
import com.draekko.gosense.misc.TraySettings;
import com.draekko.gosense.services.AllDevicesService;
import com.draekko.gosense.services.AllServicesDeviceDataHandler;
import com.draekko.gosense.ui.settings.SettingsFragment;
import com.draekko.traypreferences.TrayDropDownPreference;
import com.draekko.traypreferences.TraySwitchPreference;
import com.welie.blessed.BluetoothCentralManager;
import com.welie.blessed.BluetoothPeripheral;
import com.welie.blessed.HciStatus;

import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;

public class DashboardFragment extends Fragment {

    private final static String TAG = "DashboardFragment";

    private boolean isConnected = false;

    private AllServicesDeviceDataHandler.OnReadRemoteRssi mOnReadRemoteRssi =
            new AllServicesDeviceDataHandler.OnReadRemoteRssi() {
        @Override
        public void remoteRssi(@NotNull BluetoothPeripheral peripheral, int rssi) {
            deviceListAdapter.updateRssi(peripheral.getAddress(), rssi);
        }
    };

    AllServicesDeviceDataHandler.OnReadRemoteBatteryLevel mOnReadRemoteBatteryLevel =
            new AllServicesDeviceDataHandler.OnReadRemoteBatteryLevel() {
        @Override
        public void remoteBatteryLevel(@NotNull BluetoothPeripheral peripheral, int batterylevel) {
            deviceListAdapter.updateBattery(peripheral.getAddress(), batterylevel);
        }
    };

    private GoSensePagerActivity.OnConnnectionStates mDashOnConnnectionStates =
            new GoSensePagerActivity.OnConnnectionStates() {
        @Override
        public void onConnected(@NotNull BluetoothPeripheral peripheral) {
            isConnected = true;
            toggleBtMessage(isConnected);
        }

        @Override
        public void onDisconnected(@NotNull BluetoothPeripheral peripheral, @NotNull HciStatus status) {
            isConnected = false;
            toggleBtMessage(isConnected);
            clearDisplay();
        }
    };

    private BluetoothPeripheral getPeripheral(String peripheralAddress) {
        BluetoothCentralManager central = mAllDevicesService.getCentral();
        return central.getPeripheral(peripheralAddress);
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

    private boolean checkLocationServices() {
        if (!areLocationServicesEnabled()) {
            new AlertDialog.Builder(mContext)
                    .setTitle(getString(R.string.title_location_enable))
                    .setMessage(getString(R.string.text_location_enable))
                    .setPositiveButton(getString(R.string.enable), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.cancel();
                            startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                        }
                    })
                    .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // if this button is clicked, just close
                            // the dialog box and do nothing
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

    private final BroadcastReceiver remoteBatteryLevelDataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            BluetoothPeripheral peripheral = getPeripheral(intent.getStringExtra(AllServicesDeviceDataHandler.MEASUREMENT_EXTRA_PERIPHERAL));
            RemoteBatteryLevelMeasurement measurement = (RemoteBatteryLevelMeasurement) intent.getSerializableExtra(AllServicesDeviceDataHandler.MEASUREMENT_REMOTE_BATTERY_LEVEL_EXTRA);
            if (measurement == null) {
                return;
            }
            mBattery = measurement.mBtBattery;
            if (false) Log.i(TAG, "Battery Level = " + mBattery);
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

            float temp = mTemperature = measurement.temperatureValue;
            if (false)  Log.i(TAG, "Temperature = " + mTemperature);

            int tempUnit = R.string.unit_celsius;
            if (TraySettings.isTemperatureUnitFahrenheit()) {
                tempUnit = R.string.unit_fahrenheit;
            }

            float temperature = temp;
            if (TraySettings.isTemperatureUnitFahrenheit()) {
                temperature = Converter.convertToF(temp);
            }
            dash_temp_value.setText(decimalFormat.format(temperature) + getString(tempUnit));

            float dewPoint = Converter.calcDewPoint(mHumidity, temp);
            if (TraySettings.isTemperatureUnitFahrenheit()) {
                dewPoint = Converter.convertToF(dewPoint);
            }
            dash_dp_value.setText(decimalFormat.format(dewPoint) + getString(tempUnit));

            float heatIndex = Converter.convertToC(Converter.calculateHeatIndexFahrenheitOld(mHumidity, Converter.convertToF(temp)));
            if (TraySettings.isTemperatureUnitFahrenheit()) {
                heatIndex = Converter.convertToF(heatIndex);
            }
            dash_hi_value.setText(decimalFormat.format(heatIndex) + getString(tempUnit));

            float humidex = Converter.calculateHumidexCelsius(mHumidity, temp);
            if (TraySettings.isTemperatureUnitFahrenheit()) {
                humidex = Converter.convertToF(humidex);
            }
            dash_hdex_value.setText(decimalFormat.format(humidex) + getString(tempUnit));
        }
    };

    private final BroadcastReceiver remotehumidityDataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            BluetoothPeripheral peripheral = getPeripheral(intent.getStringExtra(AllServicesDeviceDataHandler.MEASUREMENT_EXTRA_PERIPHERAL));
            RemoteRelativeHumidityMeasurement measurement = (RemoteRelativeHumidityMeasurement) intent.getSerializableExtra(AllServicesDeviceDataHandler.MEASUREMENT_REMOTE_RELATIVE_HUMIDITY_EXTRA);
            if (measurement == null) {
                return;
            }

            mHumidity = measurement.humidityValue;
            if (false) Log.i(TAG, "Humidity = " + mHumidity);
            dash_humidity_value.setText(decimalFormat.format(mHumidity) + "%");
        }
    };

    private GoSensePagerActivity.OnRssiUpdates rssiUpdates = new GoSensePagerActivity.OnRssiUpdates() {
        @Override
        public void onRssiUpdate(String address, int rssi) {
            if (false) Log.i(TAG, "DashboardFragment " + address + " " + rssi + " dBm");
            deviceListAdapter.updateRssi(address, rssi);
        }
    };

    private FragmentDashboardBinding binding;
    private Context mContext;
    private AllDevicesService mAllDevicesService;

    private ImageButton dash_ap_button;
    private ImageButton dash_temp_button;
    private ImageButton dash_rh_button;
    private ImageButton dash_dp_button;
    private ImageButton dash_hi_button;
    private ImageButton dash_hd_button;
    private ImageView dash_ap_icon;
    private TextView dash_ap_text;
    private TextView dash_ap_value;
    private TextView dash_temp_value;
    private TextView dash_humidity_value;
    private TextView dash_dp_value;
    private TextView dash_hi_value;
    private TextView dash_hdex_value;

    private TextView no_devices_connected_text;
    private DeviceListAdapter deviceListAdapter;
    private RecyclerView deviceRecyclerView;
    private int rssi = -1000;

    DecimalFormat decimalFormat = new DecimalFormat("###0.0");
    DecimalFormat decimalPsiFormat = new DecimalFormat("###0.0##");

    private int mBattery;
    private float mHumidity;
    private float mTemperature;
    private float mAirpressure;

    private SensorDevice activeSensorDevice;

    AllDevicesService.OnPressureSensorChange mOnPressureSensorChange =
            new AllDevicesService.OnPressureSensorChange() {
                @Override
                public void airPressure(float airpressure) {
                    mAirpressure = airpressure;
                    if (dash_ap_value != null) {
                        String value = decimalFormat.format(airpressure);
                        String apUnit = "mbar";
                        if (TraySettings.getAirPressureUnit() == 2) {
                            apUnit = "hPa";
                        }
                        if (TraySettings.getAirPressureUnit() == 3) {
                            apUnit = "kPa";
                            value = decimalPsiFormat.format(airpressure / 10.0f);
                        }
                        if (TraySettings.getAirPressureUnit() == 4) {
                            apUnit = "Psi";
                            value = decimalPsiFormat.format(airpressure * PSI_VALUE_ONE_MILLIBAR);
                        }

                        dash_ap_value.setText(value + " " + apUnit);
                    }
                }
            };

    AllDevicesService.OnAirPressureSensorChange mOnAirPressureSensorChange =
            new AllDevicesService.OnAirPressureSensorChange() {
                @Override
                public void airPressure(String address, long time, float airpressure) {
                    mAirpressure = airpressure;
                    if (dash_ap_value != null) {
                        String value = decimalFormat.format(airpressure);
                        String apUnit = "mbar";
                        if (TraySettings.getAirPressureUnit() == 2) {
                            apUnit = "hPa";
                        }
                        if (TraySettings.getAirPressureUnit() == 3) {
                            apUnit = "kPa";
                            value = decimalPsiFormat.format(airpressure / 10.0f);
                        }
                        if (TraySettings.getAirPressureUnit() == 4) {
                            apUnit = "Psi";
                            value = decimalPsiFormat.format(airpressure * PSI_VALUE_ONE_MILLIBAR);
                        }

                        dash_ap_value.setText(value + " " + apUnit);
                    }
                }
            };

    private static DashboardFragment mFragment;

    public static DashboardFragment getInstance() {
        if (mFragment == null) {
            mFragment = new DashboardFragment();
        }
        return mFragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
    }

    private void startAllDevicesServiceWatcher() {
        if (mAllDevicesService == null) {
            mAllDevicesService = GoSensePagerActivity.getAllDevicesService();
            if (mAllDevicesService == null) {
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        startAllDevicesServiceWatcher();
                    }
                }, 100);
            } else {
                GoSensePagerActivity.registerOnAirPressureSensorChange(mOnAirPressureSensorChange);
                GoSensePagerActivity.registerOnConnnectionStates(mDashOnConnnectionStates);
                GoSensePagerActivity.registerOnReadRemoteRssi(mOnReadRemoteRssi);
                GoSensePagerActivity.registerOnReadRemoteBatteryLevel(mOnReadRemoteBatteryLevel);
            }
        }
    }

    @SuppressLint("WrongConstant")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAllDevicesService = GoSensePagerActivity.getAllDevicesService();
        if (mAllDevicesService == null) {
            startAllDevicesServiceWatcher();
        }

        if (mAllDevicesService != null) {
            GoSensePagerActivity.registerOnAirPressureSensorChange(mOnAirPressureSensorChange);
            GoSensePagerActivity.registerOnConnnectionStates(mDashOnConnnectionStates);
            GoSensePagerActivity.registerOnReadRemoteRssi(mOnReadRemoteRssi);
            GoSensePagerActivity.registerOnReadRemoteBatteryLevel(mOnReadRemoteBatteryLevel);
        }

        int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ? Context.RECEIVER_NOT_EXPORTED : 0 ;
        ContextCompat.registerReceiver(mContext, remoteBatteryLevelDataReceiver, new IntentFilter( AllServicesDeviceDataHandler.MEASUREMENT_REMOTE_BATTERY_LEVEL ), flags);
        ContextCompat.registerReceiver(mContext, remoteTemperatureDataReceiver, new IntentFilter( AllServicesDeviceDataHandler.MEASUREMENT_REMOTE_TEMPERATURE ), flags);
        ContextCompat.registerReceiver(mContext, remotehumidityDataReceiver, new IntentFilter( AllServicesDeviceDataHandler.MEASUREMENT_REMOTE_RELATIVE_HUMIDITY ), flags);

        if (false && TraySettings.getHasBarometer()) {
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                }
            }, 100);
        }

        GoSensePagerActivity.setDashboardRssiUpdate(rssiUpdates);
        activeSensorDevice = TraySettings.getActiveSensorDevice();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        dash_temp_value = root.findViewById(R.id.dashboard_temperature_value);
        dash_humidity_value = root.findViewById(R.id.dashboard_humidity_value);
        dash_dp_value = root.findViewById(R.id.dashboard_dew_point_value);
        dash_hi_value = root.findViewById(R.id.dashboard_heat_index_value);
        dash_hdex_value = root.findViewById(R.id.dashboard_humidex_value);

        dash_ap_button = root.findViewById(R.id.dashboard_airpres_button);
        dash_temp_button = root.findViewById(R.id.dashboard_temperature_button);
        dash_rh_button = root.findViewById(R.id.dashboard_humidity_button);
        dash_dp_button = root.findViewById(R.id.dashboard_dew_point_button);
        dash_hi_button = root.findViewById(R.id.dashboard_heat_index_button);
        dash_hd_button = root.findViewById(R.id.dashboard_humidex_button);

        dash_ap_icon = root.findViewById(R.id.dashboard_airpres_icon);
        dash_ap_text = root.findViewById(R.id.dashboard_airpres_text);
        dash_ap_value = root.findViewById(R.id.dashboard_airpres_value);

        no_devices_connected_text = root.findViewById(R.id.no_devices_connected_text);
        deviceListAdapter = new DeviceListAdapter(requireActivity());
        deviceRecyclerView = root.findViewById(R.id.device_list);
        deviceRecyclerView.setLayoutManager(new LinearLayoutManager(mContext));
        deviceRecyclerView.setAdapter(deviceListAdapter);

        if (!TraySettings.getHasBarometer()) {
            dash_ap_button.setVisibility(View.GONE);
            dash_ap_icon.setVisibility(View.GONE);
            dash_ap_text.setVisibility(View.GONE);
            dash_ap_value.setVisibility(View.GONE);
        }

        dash_ap_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleChangePressure();
            }
        });
        dash_dp_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleChangeTemp();
            }
        });
        dash_temp_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleChangeTemp();
            }
        });
        dash_hi_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleChangeTemp();
            }
        });
        dash_hd_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleChangeTemp();
            }
        });

        if (GoSensePagerActivity.isConnected()) {
            no_devices_connected_text.setVisibility(View.GONE);
            deviceRecyclerView.setVisibility(View.VISIBLE);
        } else {
            no_devices_connected_text.setVisibility(View.VISIBLE);
            deviceRecyclerView.setVisibility(View.INVISIBLE);
        }

        return root;
    }

    public void toggleBtMessage(boolean isConnected) {
        if (isConnected) {
            activeSensorDevice = TraySettings.getActiveSensorDevice();
            no_devices_connected_text.setVisibility(View.GONE);
            deviceRecyclerView.setVisibility(View.VISIBLE);
            if (activeSensorDevice != null) {
                deviceListAdapter.addSensorDevice(activeSensorDevice, rssi);
            }
        } else {
            no_devices_connected_text.setVisibility(View.VISIBLE);
            deviceRecyclerView.setVisibility(View.INVISIBLE);
            deviceListAdapter.clear();
        }
        deviceListAdapter.notifyDataSetChanged();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mAllDevicesService != null) {
            GoSensePagerActivity.registerOnAirPressureSensorChange(mOnAirPressureSensorChange);
            GoSensePagerActivity.registerOnConnnectionStates(mDashOnConnnectionStates);
            GoSensePagerActivity.registerOnReadRemoteRssi(mOnReadRemoteRssi);
            GoSensePagerActivity.registerOnReadRemoteBatteryLevel(mOnReadRemoteBatteryLevel);
        }
        if (GoSensePagerActivity.isConnected()) {
            no_devices_connected_text.setVisibility(View.GONE);
            deviceRecyclerView.setVisibility(View.VISIBLE);
        } else {
            no_devices_connected_text.setVisibility(View.VISIBLE);
            deviceRecyclerView.setVisibility(View.INVISIBLE);
        }
        deviceListAdapter.notifyDataSetChanged();
    }

    @Override
    public void onDestroyView() {
        if (mAllDevicesService != null) {
            mAllDevicesService.setOnPressureSensorChange(null);
            GoSensePagerActivity.unregisterOnAirPressureSensorChange(mOnAirPressureSensorChange);
            GoSensePagerActivity.unregisterOnConnnectionStates(mDashOnConnnectionStates);
            GoSensePagerActivity.unregisterOnReadRemoteRssi(mOnReadRemoteRssi);
            GoSensePagerActivity.unregisterOnReadRemoteBatteryLevel(mOnReadRemoteBatteryLevel);
        }
        super.onDestroyView();
        try {
            mContext.unregisterReceiver(remoteBatteryLevelDataReceiver);
        } catch (Exception e) {
        }
        try {
            mContext.unregisterReceiver(remoteTemperatureDataReceiver);
        } catch (Exception e) {
        }
        try {
            mContext.unregisterReceiver(remotehumidityDataReceiver);
        } catch (Exception e) {
        }
        binding = null;
    }

    private void handleChangePressure() {
        int change = TraySettings.getAirPressureUnit();
        switch (change) {
            case 1:
                change = 2;
                break;
            case 2:
                change = 3;
                break;
            case 3:
                change = 4;
                break;
            case 4:
                change = 1;
                break;
        }

        TraySettings.setAirPressureUnit(change);

        String value = decimalFormat.format(mAirpressure);
        String apUnit = "mbar";
        if (change == 2) {
            apUnit = "hPa";
        }
        if (change == 3) {
            apUnit = "kPa";
            value = decimalPsiFormat.format(mAirpressure / 10.0f);
        }
        if (change == 4) {
            apUnit = "Psi";
            value = decimalPsiFormat.format(mAirpressure * PSI_VALUE_ONE_MILLIBAR);
        }

        dash_ap_value.setText(value + " " + apUnit);

        SettingsFragment settingsFragment = SettingsFragment.getInstance();
        if (settingsFragment != null) {
            TrayDropDownPreference trayDropDownPreference =
                    settingsFragment.findPreference(TraySettings.PREF_KEY_AIR_PRESSURE_UNIT);
            if (trayDropDownPreference != null)  {
                trayDropDownPreference.setValueIndex(change-1);
            }
        }
    }

    private void handleChangeTemp() {
        boolean change = !TraySettings.isTemperatureUnitFahrenheit();
        TraySettings.setIsTemperatureUnitFahrenheit(change);

        int tempUnit = R.string.unit_celsius;
        if (change) {
            tempUnit = R.string.unit_fahrenheit;
        }

        float temperature = mTemperature;
        if (TraySettings.isTemperatureUnitFahrenheit()) {
            temperature = Converter.convertToF(temperature);
        }
        dash_temp_value.setText(decimalFormat.format(temperature) + getString(tempUnit));

        dash_humidity_value.setText(decimalFormat.format(mHumidity) + "%");

        float dewPoint = Converter.calcDewPoint(mHumidity, mTemperature);
        if (change) {
            dewPoint = Converter.convertToF(dewPoint);
        }
        dash_dp_value.setText(decimalFormat.format(dewPoint) + getString(tempUnit));

        float heatIndex = Converter.convertToC(Converter.calculateHeatIndexFahrenheitOld(mHumidity, Converter.convertToF(mTemperature)));
        if (change) {
            heatIndex = Converter.convertToF(heatIndex);
        }
        dash_hi_value.setText(decimalFormat.format(heatIndex) + getString(tempUnit));

        float humidex = Converter.calculateHumidexCelsius(mHumidity, mTemperature);
        if (change) {
            humidex = Converter.convertToF(humidex);
        }
        dash_hdex_value.setText(decimalFormat.format(humidex) + getString(tempUnit));

        SettingsFragment settingsFragment = SettingsFragment.getInstance();
        if (settingsFragment != null) {
            TraySwitchPreference traySwitchPreference = settingsFragment.findPreference(TraySettings.PREF_KEY_IS_IMPERIAL_TEMP);
            if (traySwitchPreference != null)  {
                traySwitchPreference.setChecked(change);
            }
        }
    }

    private void clearDisplay() {
        dash_temp_value.setText(getString(R.string.label_empty_t));
        dash_humidity_value.setText(getString(R.string.label_empty_rh));
        dash_dp_value.setText(getString(R.string.label_empty_t));
        dash_hi_value.setText(getString(R.string.label_empty_t));
        dash_hdex_value.setText(getString(R.string.label_empty_t));
        dash_ap_value.setText(getString(R.string.label_empty_ap));
    }
}
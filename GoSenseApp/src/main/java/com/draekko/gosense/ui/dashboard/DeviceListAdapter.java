package com.draekko.gosense.ui.dashboard;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.draekko.gosense.R;
import com.draekko.gosense.data.SensorDevice;
import com.draekko.gosense.data.SensorDeviceData;
import com.draekko.gosense.misc.TraySettings;

import java.util.ArrayList;
import java.util.Locale;

public  class DeviceListAdapter extends  RecyclerView.Adapter<DeviceListAdapter.DeviceViewHolder> {

    private final static String TAG = "DeviceListAdapter";

    private Activity mActivity;
    private LayoutInflater mInflater;
    private ArrayList<SensorDeviceData> sensorDevices = new ArrayList<>();

    private DeviceListAdapter mDeviceListAdapter;
    public DeviceListAdapter(Activity activity) {
        mActivity = activity;
        mInflater = LayoutInflater.from(mActivity);
        mDeviceListAdapter = this;
    }

    public void addSensorDevice(SensorDevice sensorDevice, int rssi) {
        addSensorDevice(new SensorDeviceData(sensorDevice, rssi));
    }

    public void removeSensorDevice(SensorDevice sensorDevice, int rssi) {
        removeSensorDevice(new SensorDeviceData(sensorDevice, rssi));
    }
    public void updateData(SensorDevice sensorDevice, int rssi) {
        updateData(new SensorDeviceData(sensorDevice, rssi));
    }

    public void addSensorDevice(SensorDeviceData sensorDataDevice) {
        if (sensorDevices.size() > 0) {
            for (int scanIndex = 0; scanIndex < sensorDevices.size(); scanIndex++) {
                String address1 = sensorDevices.get(scanIndex).getSensorDevice().getAddress().toLowerCase(Locale.US);
                String address2 = sensorDataDevice.getSensorDevice().getAddress().toLowerCase(Locale.US);
                if (address1.contains(address2)) {
                    Log.i(TAG, "DeviceListAdapter: Device was already added, will not add");
                    return;
                }
            }
        }
        sensorDevices.add(sensorDataDevice);
        doNotifyDataSetChanged();
    }

    public void removeSensorDevice(SensorDeviceData sensorDataDevice) {
        if (sensorDevices.size() == 0) {
            Log.i(TAG, "Cannot remove adapter entry, size = 0");
        } else if (sensorDevices.size() == 1) {
            sensorDevices.clear();
            doNotifyDataSetChanged();
        } else {
            for (int scanIndex = 0; scanIndex < sensorDevices.size(); scanIndex++) {
                String address1 = sensorDevices.get(scanIndex).getSensorDevice().getAddress().toLowerCase(Locale.US);
                String address2 = sensorDataDevice.getSensorDevice().getAddress().toLowerCase(Locale.US);
                if (address1.contains(address2)) {
                    sensorDevices.remove(scanIndex);
                    doNotifyDataSetChanged();
                    return;
                }
            }
        }
    }

    public void updateBattery(String address, int battery) {
        if (sensorDevices.size() == 0) {
            Log.i(TAG, "Cannot update adapter entry, size = 0");
            return;
        }
        if (sensorDevices.size() > 0) {
            for (int scanIndex = 0; scanIndex < sensorDevices.size(); scanIndex++) {
                String address1 = sensorDevices.get(scanIndex).getSensorDevice().getAddress().toLowerCase(Locale.US);
                if (address1.contains(address.toLowerCase(Locale.US))) {
                    SensorDeviceData deviceData = sensorDevices.get(scanIndex);
                    deviceData.setBattery(battery);
                    sensorDevices.set(scanIndex, deviceData);
                    doNotifyDataSetChanged();
                    return;
                }
            }
        }
    }

    public void updateRssi(String address, int rssi) {
        if (sensorDevices.size() == 0) {
            Log.i(TAG, "Cannot update adapter entry, size = 0");
            return;
        }
        if (sensorDevices.size() > 0) {
            for (int scanIndex = 0; scanIndex < sensorDevices.size(); scanIndex++) {
                String address1 = sensorDevices.get(scanIndex).getSensorDevice().getAddress().toLowerCase(Locale.US);
                if (address1.contains(address.toLowerCase(Locale.US))) {
                    SensorDeviceData deviceData = sensorDevices.get(scanIndex);
                    deviceData.setRssi(rssi);
                    sensorDevices.set(scanIndex, deviceData);
                    doNotifyDataSetChanged();
                    return;
                }
            }
        }
    }

    public void updateData(SensorDeviceData sensorDataDevice) {
        if (sensorDevices.size() == 0) {
            Log.i(TAG, "Cannot update adapter entry, size = 0");
            return;
        }
        if (sensorDevices.size() > 0) {
            for (int scanIndex = 0; scanIndex < sensorDevices.size(); scanIndex++) {
                String address1 = sensorDevices.get(scanIndex).getSensorDevice().getAddress().toLowerCase(Locale.US);
                String address2 = sensorDataDevice.getSensorDevice().getAddress().toLowerCase(Locale.US);
                if (address1.contains(address2)) {
                    sensorDevices.set(scanIndex, sensorDataDevice);
                    doNotifyDataSetChanged();
                    return;
                }
            }
        }
    }
    private void doNotifyDataSetChanged() {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                notifyDataSetChanged();
            }
        });
    }

    public void clear() {
        sensorDevices.clear();
        doNotifyDataSetChanged();
    }

    @NonNull
    @Override
    public DeviceListAdapter.DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = R.layout.dash_recyclerview_device_list_single;
        if (sensorDevices.size() > 1) {
            layout = R.layout.dash_recyclerview_device_list;
        }
        View view = mInflater.inflate(layout, parent, false);
        return new DeviceListAdapter.DeviceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(DeviceListAdapter.DeviceViewHolder holder, int position) {
        SensorDevice lSensorDevice = sensorDevices.get(position).getSensorDevice();
        int rssi = sensorDevices.get(position).getRssi();
        int battery = sensorDevices.get(position).getBattery();
        if (sensorDevices.size() > 1) {
            String address1 = sensorDevices.get(position).getSensorDevice().getAddress().toLowerCase(Locale.US);
            String address2 = TraySettings.getActiveSensorDevice().getAddress().toLowerCase(Locale.US);;
            if (holder.selected != null) {
                holder.selected.setEnabled(false);
                if (address1.contains(address2)) {
                    holder.selected.setChecked(true);
                } else {
                    holder.selected.setChecked(false);
                }
            }
        }
        String rssiStr = rssi + " dBm";
        if (rssi <= -1000) {
            rssiStr = "\u221E dBm";
        }
        String batteryStr = battery + "%";
        if (battery <= 0) {
            batteryStr = "\u221E %";
        }
        holder.rssi.setText(rssiStr);
        holder.address.setText(lSensorDevice.getAddress());
        holder.name.setText(lSensorDevice.getAliasName());
        holder.battery.setText(batteryStr);

        int batteryicon = R.drawable.ic_baseline_battery_unknown_24;
        if (battery > 98) {
            batteryicon = R.drawable.ic_baseline_battery_full_24;
        } else if (battery > 90) {
            batteryicon = R.drawable.ic_baseline_battery_6_bar_24;
        } else if (battery > 73) {
            batteryicon = R.drawable.ic_baseline_battery_5_bar_24;
        } else if (battery > 57) {
            batteryicon = R.drawable.ic_baseline_battery_4_bar_24;
        } else if (battery > 40) {
            batteryicon = R.drawable.ic_baseline_battery_3_bar_24;
        } else if (battery > 23) {
            batteryicon = R.drawable.ic_baseline_battery_2_bar_24;
        } else if (battery > 5) {
            batteryicon = R.drawable.ic_baseline_battery_1_bar_24;
        } else if (battery > 0) {
            batteryicon = R.drawable.ic_battery_alert_24;
        }

        holder.batteryicon.setImageDrawable(mActivity.getDrawable(batteryicon));
    }

    @Override
    public int getItemCount() {
        return sensorDevices.size();
    }

    public class DeviceViewHolder extends RecyclerView.ViewHolder {
        public RelativeLayout frame;
        public CheckBox selected;
        public TextView name;
        public TextView address;
        public TextView rssi;
        public TextView battery;
        public ImageView batteryicon;

        public DeviceViewHolder(@NonNull View itemView) {
            super(itemView);
            frame = itemView.findViewById(R.id.deviceframe);
            selected = itemView.findViewById(R.id.deviceselected);
            name = itemView.findViewById(R.id.devicename);
            address = itemView.findViewById(R.id.deviceaddress);
            rssi = itemView.findViewById(R.id.devicerssi);
            battery = itemView.findViewById(R.id.devicebattery);
            batteryicon = itemView.findViewById(R.id.devicebatteryicon);
        }
    }
}

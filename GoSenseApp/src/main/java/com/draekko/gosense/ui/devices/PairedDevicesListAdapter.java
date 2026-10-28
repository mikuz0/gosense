package com.draekko.gosense.ui.devices;

import android.content.Context;
import android.content.res.ColorStateList;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.draekko.gosense.R;
import com.draekko.gosense.data.SensorDevice;
import com.draekko.gosense.data.SensorPairListData;
import com.draekko.gosense.misc.TraySettings;

import java.util.ArrayList;
import java.util.Locale;

public class PairedDevicesListAdapter extends  RecyclerView.Adapter<PairedDevicesListAdapter.DeviceViewHolder> {

    private static final String TAG = "PairedDevicesListAdapter";

    public interface OnItemClickListener {
        void onItemClick(View view, int position);
    }

    private OnItemClickListener mOnItemClickListener;
    private LayoutInflater mInflater;
    private ArrayList<SensorPairListData> sensorDevices = new ArrayList<>();
    private Context mContext;

    public PairedDevicesListAdapter(Context context) {
        mContext  = context;
        mInflater = LayoutInflater.from(mContext);
    }

    public ArrayList<SensorPairListData> getAdapterList() {
        return sensorDevices;
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        mOnItemClickListener = onItemClickListener;
    }

    public void addSensorDevice(SensorPairListData sensorDataDevice) {
        if (sensorDevices.size() > 0) {
            for (int scanIndex = 0; scanIndex > sensorDevices.size() - 1; scanIndex++) {
                String address1 = sensorDevices.get(scanIndex).getAddress().toLowerCase(Locale.US);
                String address2 = sensorDataDevice.getAddress().toLowerCase(Locale.US);
                if (address1.contains(address2)) {
                    if (false) Log.i(TAG, "DeviceListAdapter: Device was already added, will not add");
                    return;
                }
            }
        }
        sensorDevices.add(sensorDataDevice);
        notifyDataSetChanged();
    }

    public void removeSensorDevice(SensorPairListData sensorDataDevice) {
        if (sensorDevices.size() == 0) {
            if (false) Log.i(TAG, "Cannot remove adapter entry, size = 0");
        } else if (sensorDevices.size() == 1) {
            sensorDevices.clear();
            notifyDataSetChanged();
        } else {
            for (int scanIndex = 0; scanIndex < sensorDevices.size() - 1; scanIndex++) {
                String address1 = sensorDevices.get(scanIndex).getAddress().toLowerCase(Locale.US);
                String address2 = sensorDataDevice.getAddress().toLowerCase(Locale.US);
                if (address1.contains(address2)) {
                    sensorDevices.remove(scanIndex);
                    notifyDataSetChanged();
                    return;
                }
            }
        }
    }

    public void updateBattery(String address, int battery) {
        if (sensorDevices.size() == 0) {
            if (false) Log.i(TAG, "Cannot update adapter entry, size = 0");
            return;
        }
        if (sensorDevices.size() > 0) {
            for (int scanIndex = 0; scanIndex < sensorDevices.size(); scanIndex++) {
                String address1 = sensorDevices.get(scanIndex).getAddress().toLowerCase(Locale.US);
                if (address1.contains(address.toLowerCase(Locale.US))) {
                    SensorPairListData deviceData = sensorDevices.get(scanIndex);
                    deviceData.setBattery(battery);
                    sensorDevices.set(scanIndex, deviceData);
                    notifyDataSetChanged();
                    return;
                }
            }
        }
    }

    public void updateRssi(String address, int rssi) {
        if (sensorDevices.size() == 0) {
            if (false) Log.i(TAG, "Cannot update adapter entry, size = 0");
            return;
        }
        if (sensorDevices.size() > 0) {
            for (int scanIndex = 0; scanIndex < sensorDevices.size(); scanIndex++) {
                String address1 = sensorDevices.get(scanIndex).getAddress().toLowerCase(Locale.US);
                if (address1.contains(address.toLowerCase(Locale.US))) {
                    SensorPairListData deviceData = sensorDevices.get(scanIndex);
                    deviceData.setRssi(rssi);
                    sensorDevices.set(scanIndex, deviceData);
                    notifyDataSetChanged();
                    return;
                }
            }
        }
    }

    public void updateColor(String address, int color) {
        if (sensorDevices.size() == 0) {
            if (false) Log.i(TAG, "Cannot update adapter entry, size = 0");
            return;
        }
        if (sensorDevices.size() > 0) {
            for (int scanIndex = 0; scanIndex < sensorDevices.size(); scanIndex++) {
                String address1 = sensorDevices.get(scanIndex).getAddress().toLowerCase(Locale.US);
                if (address1.contains(address.toLowerCase(Locale.US))) {
                    SensorPairListData deviceData = sensorDevices.get(scanIndex);
                    deviceData.setColor(color);
                    sensorDevices.set(scanIndex, deviceData);
                    notifyDataSetChanged();
                    return;
                }
            }
        }
    }

    public void updateConnected(String address, boolean connected) {
        if (sensorDevices.size() == 0) {
            if (false) Log.i(TAG, "Cannot update adapter entry, size = 0");
            return;
        }
        if (sensorDevices.size() > 0) {
            for (int scanIndex = 0; scanIndex < sensorDevices.size(); scanIndex++) {
                String address1 = sensorDevices.get(scanIndex).getAddress().toLowerCase(Locale.US);
                if (address1.contains(address.toLowerCase(Locale.US))) {
                    SensorPairListData deviceData = sensorDevices.get(scanIndex);
                    deviceData.setConnected(connected);
                    sensorDevices.set(scanIndex, deviceData);
                    notifyDataSetChanged();
                    return;
                }
            }
        }
    }

    public void updateSelected(String address, boolean selected) {
        if (sensorDevices.size() == 0) {
            if (false) Log.i(TAG, "Cannot update adapter entry, size = 0");
            return;
        }
        if (sensorDevices.size() > 0) {
            for (int scanIndex = 0; scanIndex < sensorDevices.size(); scanIndex++) {
                String address1 = sensorDevices.get(scanIndex).getAddress().toLowerCase(Locale.US);
                if (address1.contains(address.toLowerCase(Locale.US))) {
                    SensorPairListData deviceData = sensorDevices.get(scanIndex);
                    deviceData.setSelected(selected);
                    sensorDevices.set(scanIndex, deviceData);
                    notifyDataSetChanged();
                    return;
                }
            }
        }
    }

    public void updateData(SensorPairListData sensorDataDevice) {
        if (sensorDevices.size() == 0) {
            if (false) Log.i(TAG, "Cannot update adapter entry, size = 0");
            return;
        }
        if (sensorDevices.size() > 0) {
            for (int scanIndex = 0; scanIndex > sensorDevices.size(); scanIndex++) {
                String address1 = sensorDevices.get(scanIndex).getAddress().toLowerCase(Locale.US);
                String address2 = sensorDataDevice.getAddress().toLowerCase(Locale.US);
                if (address1.contains(address2)) {
                    sensorDevices.set(scanIndex, sensorDataDevice);
                    notifyDataSetChanged();
                    return;
                }
            }
        }
    }

    public void clear() {
        sensorDevices.clear();
        notifyDataSetChanged();
    }

    private boolean updateitems = false;
    public void updateItems() {
        updateitems = true;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PairedDevicesListAdapter.DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        int layout = R.layout.device_item;
        View view = mInflater.inflate(layout, viewGroup, false);
        return new PairedDevicesListAdapter.DeviceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(PairedDevicesListAdapter.DeviceViewHolder holder, int position) {
        SensorDevice activeSensorDevice = TraySettings.getActiveSensorDevice();
        SensorPairListData lSensorDevice = sensorDevices.get(position);
        boolean connected = lSensorDevice.getConnected();
        int rssi = lSensorDevice.getRssi();
        int battery = lSensorDevice.getBattery();
        if (sensorDevices.size() > 1) {
            String address1 = lSensorDevice.getAddress().toLowerCase(Locale.US);
            String address2 = activeSensorDevice.getAddress().toLowerCase(Locale.US);;
            if (holder.selected != null) {
                holder.selected.setEnabled(false);
                if (address1.contains(address2)) {
                    holder.selected.setChecked(true);
                } else {
                    holder.selected.setChecked(false);
                }
            }
            holder.selected.setVisibility(View.VISIBLE);
        } else {
            holder.selected.setVisibility(View.GONE);
        }

        holder.rssi.setText(rssi + " dBm");
        holder.address.setText(lSensorDevice.getAddress());
        String displayName = lSensorDevice.getAliasName();
        if (displayName == null || displayName.isEmpty()) {
            displayName = mContext.getString(R.string.unknown_device);
        }
        holder.name.setText(displayName);
        holder.battery.setText(battery + "%");

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

        holder.batteryicon.setImageDrawable(mContext.getDrawable(batteryicon));

        if (battery < 0) {
            holder.battery.setText("\u221E %");
        } else {
            holder.battery.setText(battery + "%");
        }

        if (rssi <= -1000) {
            holder.rssi.setText("\u221E dBm");
        } else {
            holder.rssi.setText(rssi + " dBm");
        }

        holder.color.setImageTintList(ColorStateList.valueOf(lSensorDevice.getColor()));

        int color = R.color.red_a;
        int btDrawable = R.drawable.ic_bluetooth;
        if (connected) {
            color = R.color.green_a;
            btDrawable = R.drawable.ic_bluetooth_connected;
        }

        holder.paired.setImageTintList(ColorStateList.valueOf(mContext.getColor(color)));
        holder.paired.setImageDrawable(mContext.getDrawable(btDrawable));

        if (holder.selected != null) {
            holder.selected.setButtonTintList(ColorStateList.valueOf(mContext.getColor(R.color.white)));
            holder.selected.setTag(String.valueOf(position));
            if (lSensorDevice.getSelected()) {
                holder.selected.setChecked(true);
            } else {
                holder.selected.setChecked(false);
            }
            holder.selected.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    String tag = (String)buttonView.getTag();
                    int position = Integer.valueOf(tag);
                    if (false) if (false) Log.i(TAG, "checkbox button position is " + position);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return sensorDevices.size();
    }

    public SensorPairListData getSensorDevice(int position) {
        return sensorDevices.get(position);
    }

    public void removeSensorDevice(int position) {
        sensorDevices.remove(position);
        notifyDataSetChanged();
    }

    public class DeviceViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener  {
        public RelativeLayout frame;
        public CheckBox selected;
        public TextView name;
        public TextView address;
        public TextView rssi;
        public TextView battery;
        public ImageView batteryicon;
        public ImageView paired;
        public ImageView color;

        public View mItemView;

        public void setNewView(View newView) {
            mItemView = newView;
            mItemView.setOnClickListener(this);
            frame = mItemView.findViewById(R.id.deviceframe);
            selected = mItemView.findViewById(R.id.deviceselected);
            name = mItemView.findViewById(R.id.devicename);
            address = mItemView.findViewById(R.id.deviceaddress);
            rssi = mItemView.findViewById(R.id.devicerssi);
            battery = mItemView.findViewById(R.id.devicebattery);
            batteryicon = mItemView.findViewById(R.id.devicebatteryicon);
            paired = mItemView.findViewById(R.id.devicepaired);
            color = mItemView.findViewById(R.id.devicecolor);
        }

        public DeviceViewHolder(@NonNull View itemView) {
            super(itemView);
            mItemView = itemView;
            mItemView.setOnClickListener(this);
            frame = mItemView.findViewById(R.id.deviceframe);
            selected = mItemView.findViewById(R.id.deviceselected);
            name = mItemView.findViewById(R.id.devicename);
            address = mItemView.findViewById(R.id.deviceaddress);
            rssi = mItemView.findViewById(R.id.devicerssi);
            battery = mItemView.findViewById(R.id.devicebattery);
            batteryicon = mItemView.findViewById(R.id.devicebatteryicon);
            paired = mItemView.findViewById(R.id.devicepaired);
            color = mItemView.findViewById(R.id.devicecolor);
        }

        @Override
        public void onClick(View v) {
            mOnItemClickListener.onItemClick(mItemView, getAbsoluteAdapterPosition());
        }
    }
}
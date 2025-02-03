package com.draekko.gosense.ui.devices;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import com.draekko.gosense.R;
import com.draekko.gosense.data.ScannedDevicesData;
import com.draekko.gosense.data.SensorDevice;
import com.draekko.gosense.misc.TraySettings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ScannedDevicesListAdapter extends BaseAdapter {
    private List<ScannedDevicesData> mCopyDevices = new ArrayList();
    private LayoutInflater mInflater;
    private Activity mActivity;

    @Override
    public long getItemId(int i) {
        return i;
    }

    ScannedDevicesListAdapter(Activity aActivity) {
        mActivity = aActivity;
        mInflater = LayoutInflater.from(aActivity);
    }

    public void clear() {
        mCopyDevices.clear();
        doNotifyDataSetChanged();
    }

    private void doNotifyDataSetChanged() {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                notifyDataSetChanged();
            }
        });
    }

    void updateDevice(Map<String, ScannedDevicesData> map) {
        ArrayList arrayList = new ArrayList(map.values());
        mCopyDevices = arrayList;
        if (TraySettings.getScanSortPriority() == 1) {
            Collections.sort(mCopyDevices, new ScannedDevicesListAdapter.RssiAddedComparator(this));
        } else {
            Collections.sort(mCopyDevices, new ScannedDevicesListAdapter.TimeAddedComparator(this));
        }
        doNotifyDataSetChanged();
    }

    ScannedDevicesData getDevice(int position) {
        return mCopyDevices.get(position);
    }

    @Override
    public int getCount() {
        return mCopyDevices.size();
    }

    @Override
    public Object getItem(int i) {
        return mCopyDevices.get(i);
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        ScannedDevicesListAdapter.ViewHolder viewHolder;
        if (view == null) {
            int layout = R.layout.scan_device_item_single;
            view = mInflater.inflate(layout, viewGroup, false);
            viewHolder = new ScannedDevicesListAdapter.ViewHolder(this);
            viewHolder.deviceName = (TextView) view.findViewById(R.id.devicename);
            viewHolder.deviceAddress = (TextView) view.findViewById(R.id.deviceaddress);
            viewHolder.rssi = (TextView) view.findViewById(R.id.devicerssi);
            view.setTag(viewHolder);
        } else {
            viewHolder = (ScannedDevicesListAdapter.ViewHolder) view.getTag();
        }
        ScannedDevicesData scannedDevices = mCopyDevices.get(i);
        String name = scannedDevices.getName();
        SensorDevice activePeripheral = TraySettings.getActiveSensorDevice();
        if (name != null && name.length() > 0) {
            viewHolder.deviceName.setText(name);
        } else {
            viewHolder.deviceName.setText(mActivity.getString(R.string.unknown_device));
        }
        viewHolder.deviceAddress.setText(scannedDevices.getAddress());
        viewHolder.rssi.setText(String.format(Locale.US, "%d dBm", Byte.valueOf((byte) scannedDevices.getRssi())));
        return view;
    }

    private class ViewHolder {
        TextView deviceAddress;
        TextView deviceName;
        TextView rssi;
        //TextView battery;
        CheckBox selected;

        private ViewHolder() {
        }

        ViewHolder(ScannedDevicesListAdapter deviceListAdapter) {
            this();
        }
    }

    private class TimeAddedComparator implements Comparator<ScannedDevicesData> {
        private TimeAddedComparator() {
        }

        TimeAddedComparator(ScannedDevicesListAdapter deviceListAdapter) {
            this();
        }

        @Override
        public int compare(ScannedDevicesData o1, ScannedDevicesData o2) {
            return compare2(o1, o2);
        }

        public int compare2(ScannedDevicesData o1, ScannedDevicesData o2) {
            try {
                return Long.valueOf(o1.getTimeAdded()).compareTo(Long.valueOf(o2.getTimeAdded()));
            } catch (Exception unused) {
                return -1;
            }
        }
    }

    private class RssiAddedComparator implements Comparator<ScannedDevicesData> {
        private RssiAddedComparator() {
        }

        RssiAddedComparator(ScannedDevicesListAdapter deviceListAdapter) {
            this();
        }

        public int compareToLowest(int v1, int v2) {
            if(v1 == v2) {
                return 0;
            }
            if(v1 < v2) {
                return -1;
            }
            return 1;
        }

        public int compareToHighest(int v1, int v2) {
            if(v1 == v2) {
                return 0;
            }
            if(v1 > v2) {
                return -1;
            }
            return 1;
        }

        @Override
        public int compare(ScannedDevicesData o1, ScannedDevicesData o2) {
            return compare2(o2, o1);
        }

        public int compare2(ScannedDevicesData o1, ScannedDevicesData o2) {
            try {
                return compareToLowest(o1.getRssi(), o2.getRssi());
            } catch (Exception unused) {
                return -1;
            }
        }
    }
}

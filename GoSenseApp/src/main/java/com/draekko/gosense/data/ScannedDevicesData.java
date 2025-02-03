package com.draekko.gosense.data;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;

import com.welie.blessed.BluetoothPeripheral;

import java.util.Objects;

public class ScannedDevicesData {

    private BluetoothPeripheral mBluetoothPeripheral;
    private String mAddress;
    private String mName;
    private long mTimeAdded;
    private int mRssi;

    @SuppressLint("MissingPermission")
    public ScannedDevicesData(BluetoothPeripheral device, int rssi) {
        if (device.getName() != null) {
            mName = device.getName();
        } else {
            mName = "Unknown";
        }
        mAddress = device.getAddress();
        mRssi = rssi;
        mTimeAdded = System.currentTimeMillis();
        mBluetoothPeripheral = device;
    }

    public String getName() {
        return mName;
    }

    public String getAddress() {
        return mAddress;
    }

    public int getRssi() {
        return mRssi;
    }

    public long getTimeAdded() {
        return mTimeAdded;
    }

    public BluetoothPeripheral getBluetoothPeripheral() {
        return mBluetoothPeripheral;
    }

    public void setAddress(String address) {
        this.mAddress = address;
    }

    public void setName(String name) {
        this.mName = name;
    }

    public void setRssi(int rssi) {
        mRssi = rssi;
    }

    public void setTimeAdded(long timeAdded) {
        this.mTimeAdded = timeAdded;
    }

    public void setBluetoothPeripheral(BluetoothPeripheral bluetoothPeripheral) {
        this.mBluetoothPeripheral = bluetoothPeripheral;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        return Objects.equals(mAddress, ((ScannedDevicesData) o).mAddress);
    }

    public int hashCode() {
        return Objects.hash(mAddress);
    }
}

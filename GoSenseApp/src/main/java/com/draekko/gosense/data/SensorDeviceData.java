package com.draekko.gosense.data;

public class SensorDeviceData {
    private SensorDevice sensorDevice;
    private int rssi;
    private int battery;

    public SensorDeviceData() {}

    public SensorDeviceData(SensorDevice sensorDevice, int rssi) {
        this.rssi = rssi;
        this.sensorDevice = sensorDevice;
    }

    public void setRssi(int rssi) {
        this.rssi = rssi;
    }

    public void setBattery(int battery) {
        this.battery = battery;
    }

    public void setSensorDevice(SensorDevice sensorDevice) {
        this.sensorDevice = sensorDevice;
    }

    public int getRssi() {
        return rssi;
    }

    public int getBattery() {
        return battery;
    }

    public SensorDevice getSensorDevice() {
        return sensorDevice;
    }
}



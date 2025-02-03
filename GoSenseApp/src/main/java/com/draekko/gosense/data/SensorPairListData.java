package com.draekko.gosense.data;

import com.draekko.gosense.misc.Utils;

public class SensorPairListData {
    private String name;
    private String aliasName;
    private String address;
    private int color;
    private int rssi;
    private int battery;
    private boolean isConnected;
    private boolean isSelected;

    public SensorPairListData() {
        this.name = "Unknown";
        this.aliasName = "Unknown";
        this.address = Utils.DEFAULT_EMPTY_ADDRESS;
        this.color = Utils.DEFAULT_COLOR;
        this.rssi = -9999;
        this.battery = -1;
        this.isConnected = false;
        this.isSelected = false;
    }

    public SensorPairListData(String name, String aliasName, String address, int color, int rssi,
                              int battery, boolean isConnected, boolean isSelected) {
        this.name = name;
        this.aliasName = aliasName;
        this.address = address;
        this.color = color | Utils.COLOR_MASK_ALPHA;
        this.rssi = rssi;
        this.battery = battery;
        this.isConnected = false;
        this.isSelected = false;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAliasName(String aliasName) {
        this.aliasName = aliasName;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setColor(int color) {
        this.color = color | Utils.COLOR_MASK_ALPHA;
    }

    public void setConnected(boolean connected) {
        isConnected = connected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    public void setRssi(int rssi) {
        this.rssi = rssi;
    }

    public void setBattery(int battery) {
        this.battery = battery;
    }

    public String getName() {
        return name;
    }

    public String getAliasName() {
        return aliasName;
    }

    public String getAddress() {
        return address;
    }

    public int getColor() {
        return color;
    }

    public int getRssi() {
        return rssi;
    }

    public int getBattery() {
        return battery;
    }

    public boolean getConnected() {
        return isConnected;
    }

    public boolean getSelected() {
        return isSelected;
    }

}

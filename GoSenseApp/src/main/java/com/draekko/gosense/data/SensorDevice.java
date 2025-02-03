/*
 * GoSense Android App
 * Copyright (C) 2024, 2025 Draekko
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.draekko.gosense.data;

import com.draekko.gosense.misc.Utils;

public class SensorDevice {
    private String mAliasName;
    private String mName;
    private String mAddress;
    private int mColor;
    private int mId;
    private boolean mIsConnected;
    private boolean mIsSelected;

    public SensorDevice(String deviceAddress, String deviceName, String deviceAliasName,
                        int color, boolean isConnected, boolean isSelected) {
        mId = -1;
        mAddress = deviceAddress;
        mName = deviceName;
        mAliasName = deviceAliasName;
        mIsConnected = isConnected;
        mIsSelected = isSelected;
        mColor = color | Utils.COLOR_MASK_ALPHA;
    }

    public SensorDevice(int id, String deviceAddress, String deviceName, String deviceAliasName,
                        int color, boolean isConnected, boolean isSelected) {
        mId = id;
        mAddress = deviceAddress;
        mName = deviceName;
        mAliasName = deviceAliasName;
        mIsConnected = isConnected;
        mIsSelected = isSelected;
        mColor = color | Utils.COLOR_MASK_ALPHA;
    }

    public SensorDevice() {
        mId = -1;
        mAddress = null;
        mName = null;
        mAliasName = null;
        mIsConnected = false;
        mIsSelected = false;
        mColor = Utils.DEFAULT_COLOR;
    }

    public int getId() {
        return mId;
    }

    public String getAddress() {
        return mAddress;
    }

    public int getColor() {
        return mColor;
    }

    public String getName() {
        return mName;
    }

    public String getAliasName() {
        return mAliasName;
    }

    public boolean isConnected() {
        return mIsConnected;
    }

    public void setIsConnected(boolean isConnected) {
        mIsConnected = isConnected;
    }

    public boolean isSelected() {
        return mIsSelected;
    }

    public void setIsSelected(boolean isSelected) {
        mIsSelected = isSelected;
    }

    public void setAddress(String  deviceAddress) {
        mAddress = deviceAddress;
    }

    public void setName(String deviceName) {
        mName = deviceName.replace(",", "_");
    }

    public void setAliasName(String deviceAliasName) {
        mAliasName = deviceAliasName.replace(",", "_");
    }

    public void setColor(int color) {
        mColor = color | Utils.COLOR_MASK_ALPHA;
    }

    public void setId(int id) {
        mId = id;
    }
}

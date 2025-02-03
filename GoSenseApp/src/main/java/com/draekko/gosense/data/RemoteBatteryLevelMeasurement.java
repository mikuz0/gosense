package com.draekko.gosense.data;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.util.Locale;

public class RemoteBatteryLevelMeasurement  implements Serializable {
    public int mBtBattery;

    public RemoteBatteryLevelMeasurement(byte[] byteArray) {
        mBtBattery = byteArray[0] & 0xff;
    }

    @NonNull
    @Override
    public String toString() {
        return String.format(Locale.ENGLISH,"Battery Level %.1f %s", mBtBattery, "%");
    }
}


package com.draekko.gosense.data;

import androidx.annotation.NonNull;

import com.welie.blessed.BluetoothBytesParser;

import java.io.Serializable;
import java.util.Locale;

public class RemoteTemperatureMeasurement  implements Serializable {
    public float temperatureValue;

    public RemoteTemperatureMeasurement(byte[] byteArray) {
        temperatureValue = (float)(((byteArray[1] & 0xff) * 256) + (byteArray[0] & 0xff)) / 100f;
    }

    @NonNull
    @Override
    public String toString() {
        return String.format(Locale.ENGLISH,"%.1f %s", temperatureValue, "\u2103");
    }
}

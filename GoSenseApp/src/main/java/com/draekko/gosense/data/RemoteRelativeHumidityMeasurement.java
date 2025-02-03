package com.draekko.gosense.data;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Date;
import java.util.Locale;

public class RemoteRelativeHumidityMeasurement implements Serializable {
    public float humidityValue;

    public RemoteRelativeHumidityMeasurement(byte[] byteArray) {
        humidityValue = (float)(((byteArray[1] & 0xff) * 256) + (byteArray[0] & 0xff)) / 100f;
    }

    @NonNull
    @Override
    public String toString() {
        return String.format(Locale.ENGLISH,"%.1f %s", humidityValue, "%");
    }
}

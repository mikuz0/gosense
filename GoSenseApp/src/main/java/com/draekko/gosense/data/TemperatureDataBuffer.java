package com.draekko.gosense.data;

import com.draekko.gosense.misc.DataBuffer;
import com.draekko.gosense.ui.dashboard.DashboardFragment;

import java.util.ArrayList;

public class TemperatureDataBuffer extends DataBuffer<TemperatureData> {

    public TemperatureDataBuffer(long timelimit) {
        super(timelimit);
    }

    public TemperatureDataBuffer(long timelimit, ArrayList<TemperatureData> newBuffer) {
        super(timelimit, newBuffer);
    }

    @Override
    public synchronized void popSingle() {
        if (!isEmpty()) {
            if (getDataBuffer().get(0).getTimestamp() < getTimeLimit()) {
                getDataBuffer().remove(0);
            }
        }
    }

    @Override
    public synchronized void popAll() {
        if(!isEmpty()) {
            ArrayList<TemperatureData> list = getDataBuffer();
            for (TemperatureData temperatureData : list) {
                if (temperatureData.getTimestamp() <= getTimeLimit()) {
                    list.remove(temperatureData);
                } else {
                    break;
                }
            }
        }
    }
}

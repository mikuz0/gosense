package com.draekko.gosense.data;

import com.draekko.gosense.misc.DataBuffer;

import java.util.ArrayList;

public class RelativeHumidityDataBuffer extends DataBuffer<RelativeHumidityData> {

    public RelativeHumidityDataBuffer(long timelimit) {
        super(timelimit);
    }

    public RelativeHumidityDataBuffer(long timelimit, ArrayList<RelativeHumidityData> newBuffer) {
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
            ArrayList<RelativeHumidityData> list = getDataBuffer();
            for (RelativeHumidityData relativeHumidityData : list) {
                if (relativeHumidityData.getTimestamp() <= getTimeLimit()) {
                    list.remove(relativeHumidityData);
                } else {
                    break;
                }
            }
        }
    }
}

package com.draekko.gosense.data;

import android.util.Log;

import com.draekko.gosense.misc.DataBuffer;

import java.util.ArrayList;

public class AirPressureDataBuffer  extends DataBuffer<AirPressureData> {

    public AirPressureDataBuffer(long timelimit) {
        super(timelimit);
    }

    public AirPressureDataBuffer(long timelimit, ArrayList<AirPressureData> newBuffer) {
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
        try {
            if(!isEmpty()) {
                ArrayList<AirPressureData> list = getDataBuffer();
                Log.d("AirPressureDataBuffer", "list size " + list.size());
                int count = 0;
                for (AirPressureData airPressureData : list) {
                    if (airPressureData.getTimestamp() <= getTimeLimit()) {
                        list.remove(airPressureData);
                        count++;
                    } else {
                        break;
                    }
                }
                Log.d("AirPressureDataBuffer", "list items removed " + count);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

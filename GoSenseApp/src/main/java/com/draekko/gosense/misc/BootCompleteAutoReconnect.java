package com.draekko.gosense.misc;

import static com.draekko.gosense.misc.Utils.ACTION_AUTO_RECONNECT;

import android.content.Context;
import android.content.Intent;

import com.draekko.gosense.services.AllDevicesService;

public class BootCompleteAutoReconnect {

    private Context mContext;

    public BootCompleteAutoReconnect(Context context) {
        mContext = context;
    }

    public void autoReconnect() {
        Intent serviceIntent = new Intent(mContext, AllDevicesService.class);
        serviceIntent.setAction(ACTION_AUTO_RECONNECT);
        mContext.startService(serviceIntent);
    }
}

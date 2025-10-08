package com.draekko.gosense.services;

import static android.app.AlarmManager.INTERVAL_DAY;

import static com.draekko.gosense.misc.Utils.ACTION_AUTO_RECONNECT;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.draekko.gosense.misc.SetupAlarm;
import com.draekko.gosense.misc.TraySettings;

public class BootCompletedReceiver extends BroadcastReceiver {

    private static final String TAG = "BootCompletedReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED") ||
                intent.getAction().equals("android.intent.action.LOCKED_BOOT_COMPLETED") ||
                intent.getAction().equals("android.intent.action.QUICKBOOT_POWERON") ||
                intent.getAction().equals("com.htc.intent.action.QUICKBOOT_POWERON")) {
            Log.i(TAG, "setting up data cleanup alarm");
            new SetupAlarm(context).setup();
            if (TraySettings.getAutoConnectDevicesOnStart()) {
                Intent serviceIntent = new Intent(context, AllDevicesService.class);
                serviceIntent.setAction(ACTION_AUTO_RECONNECT);
                context.startService(serviceIntent);
            }
        }
    }
}

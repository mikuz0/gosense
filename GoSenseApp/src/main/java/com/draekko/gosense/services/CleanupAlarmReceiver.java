package com.draekko.gosense.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.draekko.gosense.app.GoSenseApplication;
import com.draekko.gosense.database.GoSenseDb;
import com.draekko.gosense.misc.TraySettings;
import com.draekko.gosense.misc.Utils;

public class CleanupAlarmReceiver extends BroadcastReceiver {

    private static final String TAG = "CleanupAlarmReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "cleanup alarm triggered");

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                long culltime = System.currentTimeMillis();
                int index = TraySettings.getDataRetentionIntervalCode();
                switch(index) {
                    case 5: // 12 months
                        culltime -= Utils.TWELVE_MONTHS;
                        break;
                    case 4: // 6 months
                        culltime -= Utils.SIX_MONTHS;
                        break;
                    case 3: // 3 months
                        culltime -= Utils.THREE_MONTHS;
                        break;
                    case 2: // 1 month
                        culltime -= Utils.ONE_MONTH;
                        break;
                    case 1: // 2 weeks
                        culltime -= Utils.TWO_WEEKS;
                        break;
                }

                Log.i(TAG, "DB cleanup started");
                GoSenseDb goSenseDb = new GoSenseDb(context);
                goSenseDb.cleanup(culltime);
                goSenseDb.close();
                Log.i(TAG, "DB cleanup finished");
            }
        });
        thread.start();
    }
}

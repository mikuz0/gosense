package com.draekko.gosense.misc;

import static android.app.AlarmManager.INTERVAL_DAY;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import com.draekko.gosense.services.CleanupAlarmReceiver;

import java.util.Calendar;

public class SetupAlarm {

    private Context mContext;

    public SetupAlarm(Context context) {
        mContext = context;
    }

    public void setup() {
        Calendar updateTime = Calendar.getInstance();
        updateTime.set(Calendar.HOUR_OF_DAY, 1);
        updateTime.set(Calendar.MINUTE, 1);
        updateTime.set(Calendar.SECOND, 1);

        AlarmManager alarms = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);

        int intentFlags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        Intent alarmIntent = new Intent(mContext, CleanupAlarmReceiver.class);
        PendingIntent recurringCleanup =
                PendingIntent.getBroadcast(mContext, 0x1111, alarmIntent, intentFlags);
        alarms.cancel(recurringCleanup);
        alarms.setInexactRepeating(AlarmManager.RTC, updateTime.getTimeInMillis(), INTERVAL_DAY, recurringCleanup);
    }
}

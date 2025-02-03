/*
 * HRM Logger Android App
 * Copyright (C) 2024 Draekko
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.draekko.gosense.misc;

import static android.content.Context.SENSOR_SERVICE;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.util.Log;
import android.util.TypedValue;

import com.draekko.gosense.R;
import com.draekko.gosense.activities.GoSensePagerActivity;
import com.draekko.gosense.data.SensorDevice;
import com.draekko.gosense.data.SensorDeviceData;
import com.draekko.gosense.data.SensorPairListData;
import com.draekko.gosense.database.GoSenseDb;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;


public class Utils {

    public final static double PSI_VALUE_ONE_MILLIBAR = 0.0145037738d;
    public final static String HARDWARE_ADDRESS = "--:--:--:--";
    public final static String HARDWARE_NAME = Build.BRAND + " " + Build.DEVICE;
    public final static int COLOR_MASK_ALPHA = 0xFF000000;
    public final static int COLOR_MASK = 0xFFFFFF;
    public final static int DEFAULT_COLOR = COLOR_MASK_ALPHA | COLOR_MASK;
    public final static String DEFAULT_EMPTY_NAME = "BT DEVICE";
    public final static String DEFAULT_EMPTY_ADDRESS = "00:00:00:00";
    public final static String DEFAULT_HW_AP_DEVICE_NAME =
            Build.MANUFACTURER.toUpperCase(Locale.US) + " " +
            Build.MODEL.toUpperCase(Locale.US);

    public final static String ACTION_AUTO_RECONNECT = "action.com.draekko.gosense.autoreconnect";

    public final static long ONE_DAY = 60 * 60 * 24 * 1000;
    public final static long ONE_WEEK = 7 * ONE_DAY;
    public final static long TWO_WEEKS =  14 * ONE_DAY;
    public final static long ONE_MONTH = 30 * ONE_DAY;
    public final static long THREE_MONTHS = 92 * ONE_DAY;
    public final static long SIX_MONTHS = 183 * ONE_DAY;
    public final static long TWELVE_MONTHS = 366 * ONE_DAY;

    public static String getBtState(int state) {
        String stateString = "";
        switch (state) {
            case BluetoothAdapter.STATE_OFF:
                stateString = "STATE_OFF";
                break;
            case BluetoothAdapter.STATE_TURNING_OFF:
                stateString = "STATE_TURNING_OFF";
                break;
            case BluetoothAdapter.STATE_ON:
                stateString = "STATE_ON";
                break;
            case BluetoothAdapter.STATE_TURNING_ON:
                stateString = "STATE_TURNING_ON";
                break;
            case BluetoothAdapter.STATE_CONNECTED:
                stateString = "STATE_CONNECTED";
                break;
            case BluetoothAdapter.STATE_CONNECTING:
                stateString = "STATE_CONNECTING";
                break;
            case BluetoothAdapter.STATE_DISCONNECTED:
                stateString = "STATE_DISCONNECTED";
                break;
            case BluetoothAdapter.STATE_DISCONNECTING:
                stateString = "STATE_DISCONNECTING";
                break;
            default:
                stateString = "UNKNOWN STATE [" + state + "]";
                break;
        }
        return stateString;
    }

    public static String getNameFromPairedDb(Context context, String address) {
        String name = "DEVICE";
        GoSenseDb goSenseDb = new GoSenseDb(context);
        ArrayList<SensorDevice> list =  goSenseDb.getPairedSensorDevices();
        if (list.size() > 0) {
            for (SensorDevice sensorDevice : list) {
                String address1 = sensorDevice.getAddress();
                if (addressIsSame(address1, address)) {
                    name = sensorDevice.getName();
                    break;
                }
            }
        }
        goSenseDb.close();
        return name;
    }

    public static String getAliasNameFromPairedDb(Context context, String address) {
        String name = "DEVICE";
        GoSenseDb goSenseDb = new GoSenseDb(context);
        ArrayList<SensorDevice> list =  goSenseDb.getPairedSensorDevices();
        if (list.size() > 0) {
            for (SensorDevice sensorDevice : list) {
                String address1 = sensorDevice.getAddress();
                if (addressIsSame(address1, address)) {
                    name = sensorDevice.getAliasName();
                    break;
                }
            }
        }
        goSenseDb.close();
        return name;
    }

    public static int getGraphColorFromPairedDb(Context context, String address) {
        int color = DEFAULT_COLOR;
        GoSenseDb goSenseDb = new GoSenseDb(context);
        ArrayList<SensorDevice> list =  goSenseDb.getPairedSensorDevices();
        if (list.size() > 0) {
            for (SensorDevice sensorDevice : list) {
                String address1 = sensorDevice.getAddress();
                if (addressIsSame(address1, address)) {
                    color = sensorDevice.getColor() | COLOR_MASK_ALPHA;
                    break;
                }
            }
        }
        goSenseDb.close();
        return color;
    }

    public static boolean hasBarometer(Context context) {
        SensorManager sensorManager = (SensorManager)context.getSystemService(SENSOR_SERVICE);
        List<Sensor> sensors = sensorManager.getSensorList(Sensor.TYPE_PRESSURE);
        if (sensors != null && sensors.size() > 0) {
            for (Sensor sensor: sensors) {
                if (sensor.getType() == Sensor.TYPE_PRESSURE) {
                    return true;
                }
            }
        }
        return false;
    }


    public static boolean addressIsSame(SensorDevice sensorDevice1, SensorDeviceData sensorDevice2) {
        return addressIsSame(sensorDevice1.getAddress(),
                sensorDevice2.getSensorDevice().getAddress());
    }

    public static boolean addressIsSame(SensorDeviceData sensorDevice1, SensorDeviceData sensorDevice2) {
        return addressIsSame(sensorDevice1.getSensorDevice().getAddress(),
                sensorDevice2.getSensorDevice().getAddress());
    }

    public static boolean addressIsSame(SensorDevice sensorDevice1, SensorDevice sensorDevice2) {
        return addressIsSame(sensorDevice1.getAddress(), sensorDevice2.getAddress());
    }

    public static boolean addressIsSame(String address1, String address2) {
        if (address1.toLowerCase(Locale.US).contains(address2.toLowerCase(Locale.US))) {
            return true;
        }
        return false;
    }

    public static String convertSensorDeviceToString(SensorDevice sensorDevice) {
        StringBuilder sb = new StringBuilder();
        sb.append(sensorDevice.getName() + ",");
        sb.append(sensorDevice.getAliasName() + ",");
        sb.append(sensorDevice.getAddress() + ",");
        sb.append(sensorDevice.getColor() + ",");
        sb.append(sensorDevice.isSelected() + ",");
        sb.append(sensorDevice.isConnected());
        return sb.toString();
    }

    public static String convertSensorPairedDeviceToString(SensorPairListData sensorDevice) {
        StringBuilder sb = new StringBuilder();
        sb.append(sensorDevice.getName() + ",");
        sb.append(sensorDevice.getAliasName() + ",");
        sb.append(sensorDevice.getAddress() + ",");
        sb.append(sensorDevice.getColor() + ",");
        sb.append(sensorDevice.getSelected() + ",");
        sb.append(sensorDevice.getConnected());
        return sb.toString();
    }

    public static SensorDevice convertStringToSensorDevice(String sensorDeviceString) {
        if (sensorDeviceString == null || sensorDeviceString.isEmpty()) {
            return null;
        }

        String [] array = sensorDeviceString.split(",");

        final SensorDevice sensorDevice = new SensorDevice();
        try {
            sensorDevice.setName(array[0]);
        } catch (Exception e) {

        }
        try {
            sensorDevice.setAliasName(array[1]);
        } catch (Exception e) {

        }
        try {
            sensorDevice.setAddress(array[2]);
        } catch (Exception e) {

        }
        try {
            sensorDevice.setColor(Integer.valueOf(array[3]));
        } catch (Exception e) {

        }
        try {
            sensorDevice.setIsConnected(Boolean.valueOf(array[4]));
        } catch (Exception e) {

        }
        try {
            sensorDevice.setIsSelected(Boolean.valueOf(array[5]));
        } catch (Exception e) {

        }

        return sensorDevice;
    }

    public boolean hasAirPressureSensor(Context context) {
        try {
            SensorManager sensorManager = (SensorManager)context.getSystemService(SENSOR_SERVICE);
            List<Sensor> list = sensorManager.getSensorList(Sensor.TYPE_PRESSURE);
            if (list != null && list.size() > 0) {
                for (Sensor sensor: list) {
                    if (sensor.getType() == Sensor.TYPE_PRESSURE) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
        }

        return false;
    }

    public static int getAttributeColor(Context context, int colorAttr) {
        int[] attrs = { colorAttr };
        TypedArray ta = context.obtainStyledAttributes(attrs);
        final int color = ta.getResourceId(0, 0xFF777777);
        ta.recycle();
        return color;
    }

    public static float pixelsFromDp(Context context, float dp) {
        Resources r = context.getResources();
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics());
    }

    public static float pixelsFromSp(Context context, float sp) {
        Resources r = context.getResources();
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, r.getDisplayMetrics());
    }

    public static int dpToPx(int dp) {
        return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
    }

    public static String secondsToTimeFormat(long seconds) {
        long j = seconds * 1000;
        return String.format(Locale.US, "%02d:%02d:%02d", Long.valueOf(TimeUnit.MILLISECONDS.toHours(j)), Long.valueOf(TimeUnit.MILLISECONDS.toMinutes(j) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(j))), Long.valueOf(TimeUnit.MILLISECONDS.toSeconds(j) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(j))));
    }

    public static int commonMultiple(int number, int ref) {
        if (ref == 0) {
            return 0;
        }
        int i = number % ref;
        if (i == 0) {
            return number;
        }
        return ((double) i) < ((double) ref) / 2.0d ? number - i : (number - i) + ref;
    }

    private static String capitalize(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        char charAt = s.charAt(0);
        if (Character.isUpperCase(charAt)) {
            return s;
        }
        return Character.toUpperCase(charAt) + s.substring(1);
    }

    public static String getAppVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Logger.getLogger(context.getClass().getName()).log(Level.SEVERE, (String) null, (Throwable) e);
            return "";
        }
    }

    public static String getAppName(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return context.getResources().getString(R.string.app_name);
        } catch (PackageManager.NameNotFoundException e) {
            Logger.getLogger(context.getClass().getName()).log(Level.SEVERE, (String) null, (Throwable) e);
            return "";
        }
    }
}

/*
 * GoSense Android App
 * Copyright (C) 2025 Draekko
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

package com.draekko.gosense.database;

import static com.draekko.gosense.database.GoSenseDbHelper.DB_AIR_PRESSURE;
import static com.draekko.gosense.database.GoSenseDbHelper.DB_DEVICE;
import static com.draekko.gosense.database.GoSenseDbHelper.DB_RELATIVE_HUMIDITY;
import static com.draekko.gosense.database.GoSenseDbHelper.DB_TEMPERATURE;
import static com.draekko.gosense.database.GoSenseDbHelper.DELETE_DB_AIR_PRESSURE;
import static com.draekko.gosense.database.GoSenseDbHelper.DELETE_DB_DEVICE;
import static com.draekko.gosense.database.GoSenseDbHelper.DELETE_DB_RELATIVE_HUMIDITY;
import static com.draekko.gosense.database.GoSenseDbHelper.DELETE_DB_TEMPERATURE;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import androidx.annotation.NonNull;

import com.draekko.gosense.data.AirPressureData;
import com.draekko.gosense.data.RelativeHumidityData;
import com.draekko.gosense.data.SensorDevice;
import com.draekko.gosense.data.SensorPairListData;
import com.draekko.gosense.data.TemperatureData;
import com.draekko.gosense.misc.Utils;

import java.util.ArrayList;
import java.util.Locale;

public class GoSenseDb {

    private final static String TAG = "GoSenseDb";

    private final static String DESCEND = "DESC";
    private final static String ASCEND = "ASC";
    private final static int COLOR_MASK_ALPHA = Utils.COLOR_MASK_ALPHA;
    private final static int DEFAULT_COLOR = Utils.DEFAULT_COLOR;

    private Context mContext;
    private SQLiteDatabase db;
    private GoSenseDbHelper dbHelper;

    public  GoSenseDb(Context context) {
        mContext = context;
        dbHelper = new GoSenseDbHelper(mContext);
        db = dbHelper.getWritableDatabase();
    }

    public void close() {
        db.close();
    }

    private SensorDevice cursorSensorDevice(Cursor cursor) {
        if (cursor != null) {
            int id_idx = cursor.getColumnIndex(GoSenseContract.DEVICE_RECORD.COLUMN.ID);
            int address_idx = cursor.getColumnIndex(GoSenseContract.DEVICE_RECORD.COLUMN.ADDRESS);
            int name_idx = cursor.getColumnIndex(GoSenseContract.DEVICE_RECORD.COLUMN.NAME);
            int aliasname_idx = cursor.getColumnIndex(GoSenseContract.DEVICE_RECORD.COLUMN.ALIASNAME);
            int color_idx = cursor.getColumnIndex(GoSenseContract.DEVICE_RECORD.COLUMN.COLOR);

            int id = cursor.getInt(id_idx);
            String address = cursor.getString(address_idx);
            String name = cursor.getString(name_idx);
            String aliasname = cursor.getString(aliasname_idx);
            int color = cursor.getInt(color_idx) | COLOR_MASK_ALPHA;

            return new SensorDevice(id, address, name, aliasname, color, false, false);
        }

        return null;
    }

    private AirPressureData cursorAirPressure(Cursor cursor) {
        if (cursor != null) {
            int id_idx = cursor.getColumnIndex(GoSenseContract.AIRPRESSURE_RECORD.COLUMN.ID);
            int airpressure_idx = cursor.getColumnIndex(GoSenseContract.AIRPRESSURE_RECORD.COLUMN.AIRPRESSURE);
            int time_idx = cursor.getColumnIndex(GoSenseContract.AIRPRESSURE_RECORD.COLUMN.TIME);
            int lat_idx = cursor.getColumnIndex(GoSenseContract.AIRPRESSURE_RECORD.COLUMN.LATITUDE);
            int lng_idx = cursor.getColumnIndex(GoSenseContract.AIRPRESSURE_RECORD.COLUMN.LONGITUDE);
            int name_idx = cursor.getColumnIndex(GoSenseContract.AIRPRESSURE_RECORD.COLUMN.NAME);
            int addr_idx = cursor.getColumnIndex(GoSenseContract.AIRPRESSURE_RECORD.COLUMN.ADDRESS);

            long id = cursor.getLong(id_idx);
            long time = cursor.getLong(time_idx);
            double lat = cursor.getDouble(lat_idx);
            double lng = cursor.getDouble(lng_idx);
            float airpressure = cursor.getFloat(airpressure_idx);
            String address = cursor.getString(addr_idx);
            String name = cursor.getString(name_idx);
            return new AirPressureData(id, time, airpressure, lat, lng, name, address);
        }

        return null;
    }

    private TemperatureData cursorTemperatureNegative(Cursor cursor) {
        try {
            if (cursor != null) {
                int temp_idx = cursor.getColumnIndex(GoSenseContract.TEMPERATURE_RECORD.COLUMN.TEMPERATURE);
                int time_idx = cursor.getColumnIndex(GoSenseContract.TEMPERATURE_RECORD.COLUMN.TIME);

                long time = cursor.getLong(time_idx);
                float temp = cursor.getFloat(temp_idx);
                return new TemperatureData(-1, time, temp, -9999.0, -9999.0, null, null);
            }
        } catch (Exception e) {
        }

        return null;
    }

    private AirPressureData cursorAirPressureNegative(Cursor cursor) {
        try {
            if (cursor != null) {
                int ap_idx = cursor.getColumnIndex(GoSenseContract.AIRPRESSURE_RECORD.COLUMN.AIRPRESSURE);
                int time_idx = cursor.getColumnIndex(GoSenseContract.AIRPRESSURE_RECORD.COLUMN.TIME);

                long time = cursor.getLong(time_idx);
                float ap = cursor.getFloat(ap_idx);
                return new AirPressureData(-1, time, ap, -9999.0, -9999.0, null, null);
            }
        } catch (Exception e) {
        }

        return null;
    }

    private RelativeHumidityData cursorRelativeHumidityNegative(Cursor cursor) {
        try {
            if (cursor != null) {
                int rh_idx = cursor.getColumnIndex(GoSenseContract.RELATIVEHUMIDITY_RECORD.COLUMN.RELATIVEHUMIDITY);
                int time_idx = cursor.getColumnIndex(GoSenseContract.RELATIVEHUMIDITY_RECORD.COLUMN.TIME);

                long time = cursor.getLong(time_idx);
                float rh = cursor.getFloat(rh_idx);
                return new RelativeHumidityData(-1, time, rh, -9999.0, -9999.0, null, null);
            }
        } catch (Exception e) {
        }

        return null;
    }

    private TemperatureData cursorTemperature(Cursor cursor) {
        if (cursor != null) {
            int id_idx = cursor.getColumnIndex(GoSenseContract.TEMPERATURE_RECORD.COLUMN.ID);
            int temp_idx = cursor.getColumnIndex(GoSenseContract.TEMPERATURE_RECORD.COLUMN.TEMPERATURE);
            int time_idx = cursor.getColumnIndex(GoSenseContract.TEMPERATURE_RECORD.COLUMN.TIME);
            int lat_idx = cursor.getColumnIndex(GoSenseContract.TEMPERATURE_RECORD.COLUMN.LATITUDE);
            int lng_idx = cursor.getColumnIndex(GoSenseContract.TEMPERATURE_RECORD.COLUMN.LONGITUDE);
            int name_idx = cursor.getColumnIndex(GoSenseContract.TEMPERATURE_RECORD.COLUMN.NAME);
            int addr_idx = cursor.getColumnIndex(GoSenseContract.TEMPERATURE_RECORD.COLUMN.ADDRESS);

            long id = cursor.getLong(id_idx);
            long time = cursor.getLong(time_idx);
            double lat = cursor.getDouble(lat_idx);
            double lng = cursor.getDouble(lng_idx);
            float temp = cursor.getFloat(temp_idx);
            String address = cursor.getString(addr_idx);
            String name = cursor.getString(name_idx);
            return new TemperatureData(id, time, temp, lat, lng, name, address);
        }

        return null;
    }

    private RelativeHumidityData cursorRelativeHumidity(Cursor cursor) {
        if (cursor != null) {
            int id_idx = cursor.getColumnIndex(GoSenseContract.RELATIVEHUMIDITY_RECORD.COLUMN.ID);
            int rh_idx = cursor.getColumnIndex(GoSenseContract.RELATIVEHUMIDITY_RECORD.COLUMN.RELATIVEHUMIDITY);
            int time_idx = cursor.getColumnIndex(GoSenseContract.RELATIVEHUMIDITY_RECORD.COLUMN.TIME);
            int lat_idx = cursor.getColumnIndex(GoSenseContract.RELATIVEHUMIDITY_RECORD.COLUMN.LATITUDE);
            int lng_idx = cursor.getColumnIndex(GoSenseContract.RELATIVEHUMIDITY_RECORD.COLUMN.LONGITUDE);
            int name_idx = cursor.getColumnIndex(GoSenseContract.RELATIVEHUMIDITY_RECORD.COLUMN.NAME);
            int addr_idx = cursor.getColumnIndex(GoSenseContract.RELATIVEHUMIDITY_RECORD.COLUMN.ADDRESS);

            long id = cursor.getLong(id_idx);
            long time = cursor.getLong(time_idx);
            double lat = cursor.getDouble(lat_idx);
            double lng = cursor.getDouble(lng_idx);
            int rh = cursor.getInt(rh_idx);
            String address = cursor.getString(addr_idx);
            String name = cursor.getString(name_idx);
            return new RelativeHumidityData(id, time, rh, lat, lng, name, address);
        }

        return null;
    }

    public SensorDevice getPairedSensorDevice(String address) {
        Cursor cursor = null;
        String query = "SELECT " +
                GoSenseContract.DEVICE_RECORD.COLUMN.ID + "," +
                GoSenseContract.DEVICE_RECORD.COLUMN.ADDRESS + "," +
                GoSenseContract.DEVICE_RECORD.COLUMN.NAME + "," +
                GoSenseContract.DEVICE_RECORD.COLUMN.ALIASNAME + "," +
                GoSenseContract.DEVICE_RECORD.COLUMN.COLOR +
                " FROM " + GoSenseContract.DEVICE_RECORD.TABLE.NAME +
                " WHERE " + GoSenseContract.DEVICE_RECORD.COLUMN.ADDRESS + " = ?" +
                " ORDER BY " + GoSenseContract.DEVICE_RECORD.COLUMN.ADDRESS +
                " ASC";
        cursor = db.rawQuery(query, new String[]{ address });

        SensorDevice sensorDevice = null;
        if (cursor != null) {
            int count = cursor.getCount();
            if (count > 0) {
                cursor.moveToFirst();
                sensorDevice = cursorSensorDevice(cursor);
            }
        }

        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }

        return sensorDevice;
    }

    public ArrayList<SensorDevice> getPairedSensorDevices() {
        ArrayList<SensorDevice> sensorDeviceList = new ArrayList<>();
        Cursor cursor = null;
        String query = "SELECT " +
                GoSenseContract.DEVICE_RECORD.COLUMN.ID + "," +
                GoSenseContract.DEVICE_RECORD.COLUMN.ADDRESS + "," +
                GoSenseContract.DEVICE_RECORD.COLUMN.NAME + "," +
                GoSenseContract.DEVICE_RECORD.COLUMN.ALIASNAME + "," +
                GoSenseContract.DEVICE_RECORD.COLUMN.COLOR +
                " FROM " + GoSenseContract.DEVICE_RECORD.TABLE.NAME +
                " ORDER BY " + GoSenseContract.DEVICE_RECORD.COLUMN.ID +
                " ASC";

        try {
            cursor = db.rawQuery(query, null);

            if (cursor != null) {
                int count = cursor.getCount();
                if (count > 0) {
                    cursor.moveToFirst();
                    for (int oo = 0; oo < count; oo++) {
                        SensorDevice sensorDevice = cursorSensorDevice(cursor);
                        if (sensorDevice != null) {
                            sensorDeviceList.add(sensorDevice);
                        }
                        cursor.moveToNext();
                    }
                }
            }
        } catch (Exception e) {
        }

        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }

        return sensorDeviceList;
    }

    public ArrayList<AirPressureData> getAirPressureRange(long start_date, long end_date) {
        final ArrayList<AirPressureData> airPressureDataList = new ArrayList<>();
        Cursor cursor = null;

        try {
            String query = "SELECT " + GoSenseContract.AIRPRESSURE_RECORD.COLUMN.TIME +
                    " FROM " + GoSenseContract.AIRPRESSURE_RECORD.TABLE.NAME +
                    " WHERE " + GoSenseContract.AIRPRESSURE_RECORD.COLUMN.TIME +
                    " BETWEEN " + (start_date - 1) +
                    " AND " + (end_date + 1) +
                    " ORDER BY " + GoSenseContract.AIRPRESSURE_RECORD.COLUMN.TIME +
                    " ASC";
            cursor = db.rawQuery(query, null);

            if (cursor != null) {
                int count = cursor.getCount();
                if (count > 0) {
                    cursor.moveToFirst();
                    AirPressureData airPressureData = cursorAirPressure(cursor);
                    if (airPressureData != null) {
                        airPressureDataList.add(airPressureData);
                    }
                }
            }
        } catch (Exception e) {
        }

        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }

        return airPressureDataList;
    }

    public ArrayList<TemperatureData> getTemperatureRange(long start_date, long end_date) {
        final ArrayList<TemperatureData> temperatureDataList = new ArrayList<>();
        Cursor cursor = null;

        try {
            String query = "SELECT " + GoSenseContract.TEMPERATURE_RECORD.COLUMN.TIME +
                    " FROM " + GoSenseContract.TEMPERATURE_RECORD.TABLE.NAME +
                    " WHERE " + GoSenseContract.TEMPERATURE_RECORD.COLUMN.TIME +
                    " BETWEEN " + (start_date - 1) +
                    " AND " + (end_date + 1) +
                    " ORDER BY " + GoSenseContract.TEMPERATURE_RECORD.COLUMN.TIME +
                    " ASC";
            cursor = db.rawQuery(query, null);

            if (cursor != null) {
                int count = cursor.getCount();
                if (count > 0) {
                    cursor.moveToFirst();
                    TemperatureData temperatureData = cursorTemperature(cursor);
                    if (temperatureData != null) {
                        temperatureDataList.add(temperatureData);
                    }
                }
            }
        } catch (Exception e) {
        }

        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }

        return temperatureDataList;
    }

    public ArrayList<TemperatureData> getTemperatureRangeNegative(long start_date, long end_date) {
        final ArrayList<TemperatureData> temperatureDataList = new ArrayList<>();
        Cursor cursor = null;

        try {
            String query = "SELECT " + GoSenseContract.TEMPERATURE_RECORD.COLUMN.TIME + "," +
                    GoSenseContract.TEMPERATURE_RECORD.COLUMN.TEMPERATURE +
                    " FROM " + GoSenseContract.TEMPERATURE_RECORD.TABLE.NAME +
                    " WHERE " + GoSenseContract.TEMPERATURE_RECORD.COLUMN.TIME +
                    " BETWEEN " + (start_date - 1) +
                    " AND " + (end_date + 1) +
                    " AND " + GoSenseContract.TEMPERATURE_RECORD.COLUMN.TEMPERATURE + "=-9999.0" +
                    " ORDER BY " + GoSenseContract.TEMPERATURE_RECORD.COLUMN.TIME +
                    " ASC";
            cursor = db.rawQuery(query, null);

            if (cursor != null) {
                int count = cursor.getCount();
                if (count > 0) {
                    cursor.moveToFirst();
                    TemperatureData temperatureData = cursorTemperatureNegative(cursor);
                    if (temperatureData != null) {
                        temperatureDataList.add(temperatureData);
                    }
                }
            }
        } catch (Exception e) {
        }

        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }

        return temperatureDataList;
    }

    public ArrayList<AirPressureData> getAirPressureRangeNegative(long start_date, long end_date) {
        final ArrayList<AirPressureData> airpressureDataList = new ArrayList<>();
        Cursor cursor = null;

        try {
            String query = "SELECT " + GoSenseContract.AIRPRESSURE_RECORD.COLUMN.TIME + "," +
                    GoSenseContract.AIRPRESSURE_RECORD.COLUMN.AIRPRESSURE +
                    " FROM " + GoSenseContract.AIRPRESSURE_RECORD.TABLE.NAME +
                    " WHERE " + GoSenseContract.AIRPRESSURE_RECORD.COLUMN.TIME +
                    " BETWEEN " + (start_date - 1) +
                    " AND " + (end_date + 1) +
                    " AND " + GoSenseContract.AIRPRESSURE_RECORD.COLUMN.AIRPRESSURE + "=-9999.0" +
                    " ORDER BY " + GoSenseContract.AIRPRESSURE_RECORD.COLUMN.TIME +
                    " ASC";
            cursor = db.rawQuery(query, null);

            if (cursor != null) {
                int count = cursor.getCount();
                if (count > 0) {
                    cursor.moveToFirst();
                    AirPressureData airPressureData = cursorAirPressureNegative(cursor);
                    if (airPressureData != null) {
                        airpressureDataList.add(airPressureData);
                    }
                }
            }
        } catch (Exception e) {
        }

        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }

        return airpressureDataList;
    }

    public ArrayList<RelativeHumidityData> getRelativeHumidityRangeNegative(long start_date, long end_date) {
        final ArrayList<RelativeHumidityData> relativeHumidityDataList = new ArrayList<>();
        Cursor cursor = null;

        try {
            String query = "SELECT " + GoSenseContract.RELATIVEHUMIDITY_RECORD.COLUMN.TIME + "," +
                    GoSenseContract.RELATIVEHUMIDITY_RECORD.COLUMN.RELATIVEHUMIDITY +
                    " FROM " + GoSenseContract.RELATIVEHUMIDITY_RECORD.TABLE.NAME +
                    " WHERE " + GoSenseContract.RELATIVEHUMIDITY_RECORD.COLUMN.TIME +
                    " BETWEEN " + (start_date - 1) +
                    " AND " + (end_date + 1) +
                    " AND " + GoSenseContract.RELATIVEHUMIDITY_RECORD.COLUMN.RELATIVEHUMIDITY + "=-9999.0" +
                    " ORDER BY " + GoSenseContract.RELATIVEHUMIDITY_RECORD.COLUMN.TIME +
                    " ASC";
            cursor = db.rawQuery(query, null);

            if (cursor != null) {
                int count = cursor.getCount();
                if (count > 0) {
                    cursor.moveToFirst();
                    RelativeHumidityData relativeHumidityData = cursorRelativeHumidityNegative(cursor);
                    if (relativeHumidityData != null) {
                        relativeHumidityDataList.add(relativeHumidityData);
                    }
                }
            }
        } catch (Exception e) {
        }

        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }

        return relativeHumidityDataList;
    }

    public ArrayList<RelativeHumidityData> getRelativeHumidityRange(long start_date, long end_date) {
        final ArrayList<RelativeHumidityData> relativeHumidityDataList = new ArrayList<>();
        Cursor cursor = null;

        try {
            String query = "SELECT " +
                    GoSenseContract.RELATIVEHUMIDITY_RECORD.COLUMN.ID + "," +
                    GoSenseContract.RELATIVEHUMIDITY_RECORD.COLUMN.TIME + "," +
                    GoSenseContract.RELATIVEHUMIDITY_RECORD.COLUMN.ADDRESS + "," +
                    GoSenseContract.RELATIVEHUMIDITY_RECORD.COLUMN.LATITUDE + "," +
                    GoSenseContract.RELATIVEHUMIDITY_RECORD.COLUMN.LONGITUDE + "," +
                    GoSenseContract.RELATIVEHUMIDITY_RECORD.COLUMN.NAME + "," +
                    GoSenseContract.RELATIVEHUMIDITY_RECORD.COLUMN.RELATIVEHUMIDITY +
                    " FROM " + GoSenseContract.RELATIVEHUMIDITY_RECORD.TABLE.NAME +
                    " WHERE " + GoSenseContract.RELATIVEHUMIDITY_RECORD.COLUMN.TIME +
                    " BETWEEN " + (start_date - 1) +
                    " AND " + (end_date + 1) +
                    " ORDER BY " + GoSenseContract.TEMPERATURE_RECORD.COLUMN.TIME +
                    " ASC";
            cursor = db.rawQuery(query, null);

            if (cursor != null) {
                int count = cursor.getCount();
                if (count > 0) {
                    cursor.moveToFirst();
                    for (int oo = 0; oo < count; oo++) {
                        RelativeHumidityData relativeHumidityData = cursorRelativeHumidity(cursor);
                        if (relativeHumidityData != null) {
                            relativeHumidityDataList.add(relativeHumidityData);
                        }
                        cursor.moveToNext();
                    }
                }
            }
        } catch (Exception e) {
        }

        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }

        return relativeHumidityDataList;
    }
    public ArrayList<TemperatureData> getTemperatureData() {
        final  ArrayList<TemperatureData> temperatureDataList = new ArrayList<>();
        Cursor cursor = null;

        try {
            String query = "SELECT " +
                    GoSenseContract.TEMPERATURE_RECORD.COLUMN.ID + "," +
                    GoSenseContract.TEMPERATURE_RECORD.COLUMN.TIME + "," +
                    GoSenseContract.TEMPERATURE_RECORD.COLUMN.ADDRESS + "," +
                    GoSenseContract.TEMPERATURE_RECORD.COLUMN.LATITUDE + "," +
                    GoSenseContract.TEMPERATURE_RECORD.COLUMN.LONGITUDE + "," +
                    GoSenseContract.TEMPERATURE_RECORD.COLUMN.NAME + "," +
                    GoSenseContract.TEMPERATURE_RECORD.COLUMN.TEMPERATURE +
                    " FROM " + GoSenseContract.TEMPERATURE_RECORD.TABLE.NAME +
                    " ORDER BY " + GoSenseContract.TEMPERATURE_RECORD.COLUMN.ID +
                    " ASC";
            cursor = db.rawQuery(query, null);

            if (cursor != null) {
                int count = cursor.getCount();
                if (count > 0) {
                    cursor.moveToFirst();
                    for (int oo = 0; oo < count; oo++) {
                        TemperatureData temperatureData = cursorTemperature(cursor);
                        if (temperatureData != null) {
                            temperatureDataList.add(temperatureData);
                        }
                        cursor.moveToNext();
                    }
                }
            }
        } catch (Exception e) {
        }

        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }

        return temperatureDataList;
    }

    public ArrayList<RelativeHumidityData> getRelativeHumidityData() {
        final  ArrayList<RelativeHumidityData> relativeHumidityDataList = new ArrayList<>();
        Cursor cursor = null;

        try {
            String query = "SELECT " +
                    GoSenseContract.RELATIVEHUMIDITY_RECORD.COLUMN.ID + "," +
                    GoSenseContract.RELATIVEHUMIDITY_RECORD.COLUMN.TIME + "," +
                    GoSenseContract.RELATIVEHUMIDITY_RECORD.COLUMN.ADDRESS + "," +
                    GoSenseContract.RELATIVEHUMIDITY_RECORD.COLUMN.LATITUDE + "," +
                    GoSenseContract.RELATIVEHUMIDITY_RECORD.COLUMN.LONGITUDE + "," +
                    GoSenseContract.RELATIVEHUMIDITY_RECORD.COLUMN.NAME + "," +
                    GoSenseContract.RELATIVEHUMIDITY_RECORD.COLUMN.RELATIVEHUMIDITY +
                    " FROM " + GoSenseContract.RELATIVEHUMIDITY_RECORD.TABLE.NAME +
                    " ORDER BY " + GoSenseContract.TEMPERATURE_RECORD.COLUMN.ID +
                    " ASC";
            cursor = db.rawQuery(query, null);

            if (cursor != null) {
                int count = cursor.getCount();
                if (count > 0) {
                    cursor.moveToFirst();
                    for (int oo = 0; oo < count; oo++) {
                        RelativeHumidityData relativeHumidityData = cursorRelativeHumidity(cursor);
                        if (relativeHumidityData != null) {
                            relativeHumidityDataList.add(relativeHumidityData);
                        }
                        cursor.moveToNext();
                    }
                }
            }
        } catch (Exception e) {
        }

        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }

        return relativeHumidityDataList;
    }

    public ArrayList<AirPressureData> getAirPressureData() {
        final ArrayList<AirPressureData> airPressureDataList = new ArrayList<>();
        Cursor cursor = null;

        try {
            String query = "SELECT " +
                    GoSenseContract.AIRPRESSURE_RECORD.COLUMN.ID + "," +
                    GoSenseContract.AIRPRESSURE_RECORD.COLUMN.TIME + "," +
                    GoSenseContract.AIRPRESSURE_RECORD.COLUMN.ADDRESS + "," +
                    GoSenseContract.AIRPRESSURE_RECORD.COLUMN.LATITUDE + "," +
                    GoSenseContract.AIRPRESSURE_RECORD.COLUMN.LONGITUDE + "," +
                    GoSenseContract.AIRPRESSURE_RECORD.COLUMN.NAME + "," +
                    GoSenseContract.AIRPRESSURE_RECORD.COLUMN.AIRPRESSURE +
                    " FROM " + GoSenseContract.AIRPRESSURE_RECORD.TABLE.NAME +
                    " ORDER BY " + GoSenseContract.AIRPRESSURE_RECORD.COLUMN.ID +
                    " ASC";
            cursor = db.rawQuery(query, null);

            if (cursor != null) {
                int count = cursor.getCount();
                if (count > 0) {
                    cursor.moveToFirst();
                    for (int oo = 0; oo < count; oo++) {
                        AirPressureData airPressureData = cursorAirPressure(cursor);
                        if (airPressureData != null) {
                            airPressureDataList.add(airPressureData);
                        }
                        cursor.moveToNext();
                    }
                }
            }
        } catch (Exception e) {
        }

        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }

        return airPressureDataList;
    }

    public ArrayList<TemperatureData> getTemperatureAddressAndRange(String address, long start_date, long end_date) {
        final ArrayList<TemperatureData> temperatureDataList = new ArrayList<>();
        Cursor cursor = null;

        try {
            String query = "SELECT " +
                    GoSenseContract.TEMPERATURE_RECORD.COLUMN.ID + "," +
                    GoSenseContract.TEMPERATURE_RECORD.COLUMN.TIME + "," +
                    GoSenseContract.TEMPERATURE_RECORD.COLUMN.ADDRESS + "," +
                    GoSenseContract.TEMPERATURE_RECORD.COLUMN.LATITUDE + "," +
                    GoSenseContract.TEMPERATURE_RECORD.COLUMN.LONGITUDE + "," +
                    GoSenseContract.TEMPERATURE_RECORD.COLUMN.NAME + "," +
                    GoSenseContract.TEMPERATURE_RECORD.COLUMN.TEMPERATURE +
                    " FROM " + GoSenseContract.TEMPERATURE_RECORD.TABLE.NAME +
                    " WHERE " + GoSenseContract.TEMPERATURE_RECORD.COLUMN.ADDRESS + "='" + address + "' AND " +
                    GoSenseContract.TEMPERATURE_RECORD.COLUMN.TIME + " BETWEEN " +
                    (start_date - 1000) + " AND " + (end_date + 1000) +
                    " ORDER BY " + GoSenseContract.TEMPERATURE_RECORD.COLUMN.TIME +
                    " ASC";
            cursor = db.rawQuery(query, null);

            if (cursor != null) {
                int count = cursor.getCount();
                if (count > 0) {
                    cursor.moveToFirst();
                    for (int oo = 0; oo < count; oo++) {
                        TemperatureData temperatureData = cursorTemperature(cursor);
                        if (temperatureData != null) {
                            temperatureDataList.add(temperatureData);
                        }
                        cursor.moveToNext();
                    }
                }
            }
        } catch (Exception e) {
        }

        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }

        return temperatureDataList;
    }

    public ArrayList<RelativeHumidityData> getRelativeHumidityAddressAndRange(String address, long start_date, long end_date) {
        final ArrayList<RelativeHumidityData> relativeHumidityDataList = new ArrayList<>();
        Cursor cursor = null;

        try {
            String query = "SELECT " +
                    GoSenseContract.RELATIVEHUMIDITY_RECORD.COLUMN.ID + "," +
                    GoSenseContract.RELATIVEHUMIDITY_RECORD.COLUMN.TIME + "," +
                    GoSenseContract.RELATIVEHUMIDITY_RECORD.COLUMN.ADDRESS + "," +
                    GoSenseContract.RELATIVEHUMIDITY_RECORD.COLUMN.LATITUDE + "," +
                    GoSenseContract.RELATIVEHUMIDITY_RECORD.COLUMN.LONGITUDE + "," +
                    GoSenseContract.RELATIVEHUMIDITY_RECORD.COLUMN.NAME + "," +
                    GoSenseContract.RELATIVEHUMIDITY_RECORD.COLUMN.RELATIVEHUMIDITY +
                    " FROM " + GoSenseContract.RELATIVEHUMIDITY_RECORD.TABLE.NAME +
                    " WHERE " + GoSenseContract.RELATIVEHUMIDITY_RECORD.COLUMN.ADDRESS + "='" + address + "' AND " +
                    GoSenseContract.RELATIVEHUMIDITY_RECORD.COLUMN.TIME + " BETWEEN " +
                    (start_date - 1000) + " AND " + (end_date + 1000) +
                    " ORDER BY " + GoSenseContract.TEMPERATURE_RECORD.COLUMN.TIME +
                    " ASC";
            cursor = db.rawQuery(query, null);

            if (cursor != null) {
                int count = cursor.getCount();
                if (count > 0) {
                    cursor.moveToFirst();
                    for (int oo = 0; oo < count; oo++) {
                        RelativeHumidityData relativeHumidityData = cursorRelativeHumidity(cursor);
                        if (relativeHumidityData != null) {
                            relativeHumidityDataList.add(relativeHumidityData);
                        }
                        cursor.moveToNext();
                    }
                }
            }
        } catch (Exception e) {
        }

        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }

        return relativeHumidityDataList;
    }

    public ArrayList<AirPressureData> getAirPressureAddressAndRange(long start_date, long end_date) {
        final ArrayList<AirPressureData> airPressureDataList = new ArrayList<>();
        Cursor cursor = null;

        try {
            String query = "SELECT " +
                    GoSenseContract.AIRPRESSURE_RECORD.COLUMN.ID + "," +
                    GoSenseContract.AIRPRESSURE_RECORD.COLUMN.TIME + "," +
                    GoSenseContract.AIRPRESSURE_RECORD.COLUMN.ADDRESS + "," +
                    GoSenseContract.AIRPRESSURE_RECORD.COLUMN.LATITUDE + "," +
                    GoSenseContract.AIRPRESSURE_RECORD.COLUMN.LONGITUDE + "," +
                    GoSenseContract.AIRPRESSURE_RECORD.COLUMN.NAME + "," +
                    GoSenseContract.AIRPRESSURE_RECORD.COLUMN.AIRPRESSURE +
                    " FROM " + GoSenseContract.AIRPRESSURE_RECORD.TABLE.NAME +
                    " WHERE " + GoSenseContract.AIRPRESSURE_RECORD.COLUMN.TIME + " BETWEEN " +
                    (start_date - 1000) + " AND " + (end_date + 1000) +
                    " ORDER BY " + GoSenseContract.AIRPRESSURE_RECORD.COLUMN.TIME +
                    " ASC";
            cursor = db.rawQuery(query, null);

            if (cursor != null) {
                int count = cursor.getCount();
                if (count > 0) {
                    cursor.moveToFirst();
                    for (int oo = 0; oo < count; oo++) {
                        AirPressureData airPressureData = cursorAirPressure(cursor);
                        if (airPressureData != null) {
                            airPressureDataList.add(airPressureData);
                        }
                        cursor.moveToNext();
                    }
                }
            }
        } catch (Exception e) {
        }

        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }

        return airPressureDataList;
    }

    public void deleteSensorDevice(@NonNull SensorDevice sensorDevice) {
        String address = sensorDevice.getAddress();
        deleteSensorDevice(address);
    }

    public void deleteSensorDevice(@NonNull String address) {
        db.delete(GoSenseContract.DEVICE_RECORD.TABLE.NAME,
                "ADDRESS=?", new String[]{ address });
    }

    public void updateSensorDevice(@NonNull SensorDevice sensorDevice) {
        String name = sensorDevice.getName();
        String aliasname = sensorDevice.getAliasName();
        String address = sensorDevice.getAddress();
        int color = sensorDevice.getColor() | COLOR_MASK_ALPHA;

        ArrayList<SensorDevice> sensorDevices = getPairedSensorDevices();
        if (sensorDevices != null && sensorDevices.size() > 0) {
            for (SensorDevice sensorDeviceCheck: sensorDevices) {
                String address1 = sensorDeviceCheck.getAddress().toLowerCase(Locale.US);
                String address2 = address.toLowerCase(Locale.US);
                if (address1.contains(address2)) {
                    int id = sensorDeviceCheck.getId();
                    ContentValues values = new ContentValues();

                    //values.put(GoSenseContract.DEVICE_RECORD.COLUMN.ID, id);
                    values.put(GoSenseContract.DEVICE_RECORD.COLUMN.ADDRESS, address);
                    values.put(GoSenseContract.DEVICE_RECORD.COLUMN.NAME, name);
                    values.put(GoSenseContract.DEVICE_RECORD.COLUMN.ALIASNAME, aliasname);
                    values.put(GoSenseContract.DEVICE_RECORD.COLUMN.COLOR, color);

                    String whereClasue = GoSenseContract.DEVICE_RECORD.COLUMN.ID + "=?";
                    String [] whereArgs = new String[] { String.valueOf(id) };
                    long rowid = db.update(GoSenseContract.DEVICE_RECORD.TABLE.NAME, values, whereClasue, whereArgs);
                    if (rowid < 0) {
                        Log.e(TAG, "ERROR: unable to insert air pressure data into db");
                    }
                    return;
                }
            }
        }
    }

    public void updateSensorDevice(@NonNull SensorPairListData sensorDevice) {
        String name = sensorDevice.getName();
        String aliasname = sensorDevice.getAliasName();
        String address = sensorDevice.getAddress();
        int color = sensorDevice.getColor() | COLOR_MASK_ALPHA;

        ArrayList<SensorDevice> sensorDevices = getPairedSensorDevices();
        if (sensorDevices != null && sensorDevices.size() > 0) {
            for (SensorDevice sensorDeviceCheck: sensorDevices) {
                String address1 = sensorDeviceCheck.getAddress().toLowerCase(Locale.US);
                String address2 = address.toLowerCase(Locale.US);
                if (address1.contains(address2)) {
                    int id = sensorDeviceCheck.getId();
                    ContentValues values = new ContentValues();

                    //values.put(GoSenseContract.DEVICE_RECORD.COLUMN.ID, id);
                    values.put(GoSenseContract.DEVICE_RECORD.COLUMN.ADDRESS, address);
                    values.put(GoSenseContract.DEVICE_RECORD.COLUMN.NAME, name);
                    values.put(GoSenseContract.DEVICE_RECORD.COLUMN.ALIASNAME, aliasname);
                    values.put(GoSenseContract.DEVICE_RECORD.COLUMN.COLOR, color);

                    String whereClasue = GoSenseContract.DEVICE_RECORD.COLUMN.ID + "=?";
                    String [] whereArgs = new String[] { String.valueOf(id) };
                    long rowid = db.update(GoSenseContract.DEVICE_RECORD.TABLE.NAME, values, whereClasue, whereArgs);
                    if (rowid < 0) {
                        Log.e(TAG, "ERROR: unable to insert air pressure data into db");
                    }
                    return;
                }
            }
        }
    }

    public void saveSensorDevice(@NonNull SensorDevice sensorDevice) {
        boolean save = true;

        String name = sensorDevice.getName();
        String aliasname = sensorDevice.getAliasName();
        String address = sensorDevice.getAddress();
        int color = sensorDevice.getColor() | COLOR_MASK_ALPHA;

        ArrayList<SensorDevice> sensorDevices = getPairedSensorDevices();
        if (sensorDevices != null && sensorDevices.size() > 0) {
            for (SensorDevice sensorDeviceCheck: sensorDevices) {
                String address1 = sensorDeviceCheck.getAddress().toLowerCase(Locale.US);
                String address2 = address.toLowerCase(Locale.US);
                if (address1.contains(address2)) {
                    Log.i(TAG, "Devices already exists, not adding to paired device database");
                    save = false;
                }
            }
        }

        if (save) {
            ContentValues values = new ContentValues();

            values.put(GoSenseContract.DEVICE_RECORD.COLUMN.ADDRESS, address);
            values.put(GoSenseContract.DEVICE_RECORD.COLUMN.NAME, name);
            values.put(GoSenseContract.DEVICE_RECORD.COLUMN.ALIASNAME, aliasname);
            values.put(GoSenseContract.DEVICE_RECORD.COLUMN.COLOR, color);

            long rowid = db.insert(GoSenseContract.DEVICE_RECORD.TABLE.NAME, null, values);
            if (rowid < 0) {
                Log.e(TAG, "ERROR: unable to insert air pressure data into db");
            }
        }
    }

    public void saveSensorDevice(@NonNull SensorPairListData sensorDevice) {
        boolean save = true;

        String name = sensorDevice.getName();
        String aliasname = sensorDevice.getAliasName();
        String address = sensorDevice.getAddress();
        int color = sensorDevice.getColor() | COLOR_MASK_ALPHA;

        ArrayList<SensorDevice> sensorDevices = getPairedSensorDevices();
        if (sensorDevices != null && sensorDevices.size() > 0) {
            for (SensorDevice sensorDeviceCheck: sensorDevices) {
                String address1 = sensorDeviceCheck.getAddress().toLowerCase(Locale.US);
                String address2 = address.toLowerCase(Locale.US);
                if (address1.contains(address2)) {
                    Log.i(TAG, "Devices already exists, not adding to paired device database");
                    save = false;
                }
            }
        }

        if (save) {
            ContentValues values = new ContentValues();

            values.put(GoSenseContract.DEVICE_RECORD.COLUMN.ADDRESS, address);
            values.put(GoSenseContract.DEVICE_RECORD.COLUMN.NAME, name);
            values.put(GoSenseContract.DEVICE_RECORD.COLUMN.ALIASNAME, aliasname);
            values.put(GoSenseContract.DEVICE_RECORD.COLUMN.COLOR, color);

            long rowid = db.insert(GoSenseContract.DEVICE_RECORD.TABLE.NAME, null, values);
            if (rowid < 0) {
                Log.e(TAG, "ERROR: unable to insert air pressure data into db");
            }
        }
    }

    public void saveAirPressure(AirPressureData airPressureData) {
        ContentValues values = new ContentValues();

        float airpressure = airPressureData.getAirPressure();
        long time = airPressureData.getTimestamp();
        String name = airPressureData.getName();
        String address = airPressureData.getAddress();
        double lat = airPressureData.getLatitude();
        double lng = airPressureData.getLongitude();

        values.put(GoSenseContract.AIRPRESSURE_RECORD.COLUMN.AIRPRESSURE, airpressure);
        values.put(GoSenseContract.AIRPRESSURE_RECORD.COLUMN.TIME, time);
        values.put(GoSenseContract.AIRPRESSURE_RECORD.COLUMN.NAME, name);
        values.put(GoSenseContract.AIRPRESSURE_RECORD.COLUMN.ADDRESS, address);
        values.put(GoSenseContract.AIRPRESSURE_RECORD.COLUMN.LATITUDE, lat);
        values.put(GoSenseContract.AIRPRESSURE_RECORD.COLUMN.LONGITUDE, lng);

        long rowid = db.insert(GoSenseContract.AIRPRESSURE_RECORD.TABLE.NAME, null, values);
        if (rowid < 0) {
            Log.e(TAG, "ERROR: unable to insert air pressure data into db");
        }
    }

    public void saveTemperature(TemperatureData temperatureData) {
        ContentValues values = new ContentValues();

        float temperature = temperatureData.getTemperature();
        long time = temperatureData.getTimestamp();
        String name = temperatureData.getName();
        String address = temperatureData.getAddress();
        double lat = temperatureData.getLatitude();
        double lng = temperatureData.getLongitude();

        values.put(GoSenseContract.TEMPERATURE_RECORD.COLUMN.TEMPERATURE, temperature);
        values.put(GoSenseContract.TEMPERATURE_RECORD.COLUMN.TIME, time);
        values.put(GoSenseContract.TEMPERATURE_RECORD.COLUMN.NAME, name);
        values.put(GoSenseContract.TEMPERATURE_RECORD.COLUMN.ADDRESS, address);
        values.put(GoSenseContract.TEMPERATURE_RECORD.COLUMN.LATITUDE, lat);
        values.put(GoSenseContract.TEMPERATURE_RECORD.COLUMN.LONGITUDE, lng);

        long rowid = db.insert(GoSenseContract.TEMPERATURE_RECORD.TABLE.NAME, null, values);
        if (rowid < 0) {
            Log.e(TAG, "ERROR: unable to insert temperature data into db");
        }
    }

    public void saveRelativeHumidity(RelativeHumidityData relativeHumidityData) {
        ContentValues values = new ContentValues();

        float temperature = relativeHumidityData.getRelativeHumidity();
        long time = relativeHumidityData.getTimestamp();
        String name = relativeHumidityData.getName();
        String address = relativeHumidityData.getAddress();
        double lat = relativeHumidityData.getLatitude();
        double lng = relativeHumidityData.getLongitude();

        values.put(GoSenseContract.RELATIVEHUMIDITY_RECORD.COLUMN.RELATIVEHUMIDITY, temperature);
        values.put(GoSenseContract.RELATIVEHUMIDITY_RECORD.COLUMN.TIME, time);
        values.put(GoSenseContract.RELATIVEHUMIDITY_RECORD.COLUMN.NAME, name);
        values.put(GoSenseContract.RELATIVEHUMIDITY_RECORD.COLUMN.ADDRESS, address);
        values.put(GoSenseContract.RELATIVEHUMIDITY_RECORD.COLUMN.LATITUDE, lat);
        values.put(GoSenseContract.RELATIVEHUMIDITY_RECORD.COLUMN.LONGITUDE, lng);

        long rowid = db.insert(GoSenseContract.RELATIVEHUMIDITY_RECORD.TABLE.NAME, null, values);
        if (rowid < 0) {
            Log.e(TAG, "ERROR: unable to insert relative humidity data into db");
        }
    }

    public void deleteAll() {
        db.execSQL(DELETE_DB_DEVICE);
        deleteAllData();
    }

    public void deleteAllData() {
        db.execSQL(DELETE_DB_AIR_PRESSURE);
        db.execSQL(DELETE_DB_TEMPERATURE);
        db.execSQL(DELETE_DB_RELATIVE_HUMIDITY);
        createDataTables();
    }

    public void createDataTables() {
        db.execSQL(DB_AIR_PRESSURE);
        db.execSQL(DB_TEMPERATURE);
        db.execSQL(DB_RELATIVE_HUMIDITY);
    }

    public void createTables() {
        db.execSQL(DB_DEVICE);
        createDataTables();
    }

    public int getAirPressureCount() {
        int count = 0;
        String query = "SELECT COUNT(*) FROM " + GoSenseContract.AIRPRESSURE_RECORD.TABLE.NAME;
        Cursor cursor = db.rawQuery(query, null);
        if (cursor != null) {
            if (cursor.getCount() > 0) {
                count = cursor.getCount();
            }
            cursor.close();
        }
        return count;
    }

    public int getTemperatureCount() {
        int count = 0;
        String query = "SELECT COUNT(*) FROM " + GoSenseContract.TEMPERATURE_RECORD.TABLE.NAME;
        Cursor cursor = db.rawQuery(query, null);
        if (cursor != null) {
            if (cursor.getCount() > 0) {
                count = cursor.getCount();
            }
            cursor.close();
        }
        return count;
    }

    public int getHumidityCount() {
        int count = 0;
        String query = "SELECT COUNT(*) FROM " + GoSenseContract.RELATIVEHUMIDITY_RECORD.TABLE.NAME;
        Cursor cursor = db.rawQuery(query, null);
        if (cursor != null) {
            if (cursor.getCount() > 0) {
                count = cursor.getCount();
            }
            cursor.close();
        }
        return count;
    }


    public int getAirPressureDistinctAddressCount() {
        String address = GoSenseContract.AIRPRESSURE_RECORD.COLUMN.ADDRESS;
        String tablename = GoSenseContract.AIRPRESSURE_RECORD.TABLE.NAME;

        int count = 0;
        String query = "SELECT COUNT(DISTINCT " + address + ") FROM " + tablename;
        Cursor cursor = db.rawQuery(query, null);
        if (cursor != null) {
            if (cursor.getCount() > 0) {
                count = cursor.getCount();
            }
            cursor.close();
        }
        return count;
    }

    public int getTemperatureDistinctAddressCount() {
        String address = GoSenseContract.TEMPERATURE_RECORD.COLUMN.ADDRESS;
        String tablename = GoSenseContract.TEMPERATURE_RECORD.TABLE.NAME;
        int count = 0;
        String query = "SELECT COUNT(DISTINCT " + address + ") FROM " + tablename;
        Cursor cursor = db.rawQuery(query, null);
        if (cursor != null) {
            if (cursor.getCount() > 0) {
                count = cursor.getCount();
            }
            cursor.close();
        }
        return count;
    }

    public int getHumidityDistinctAddressCount() {
        String address = GoSenseContract.RELATIVEHUMIDITY_RECORD.COLUMN.ADDRESS;
        String tablename = GoSenseContract.RELATIVEHUMIDITY_RECORD.TABLE.NAME;
        int count = 0;
        String query = "SELECT COUNT(DISTINCT " + address + ") FROM " + tablename;
        Cursor cursor = db.rawQuery(query, null);
        if (cursor != null) {
            if (cursor.getCount() > 0) {
                count = cursor.getCount();
            }
            cursor.close();
        }
        return count;
    }

    public boolean isAirPressureEmpty() {
        boolean empty = true;
        String query = "SELECT COUNT(*) FROM  (select 0 from " +
                GoSenseContract.AIRPRESSURE_RECORD.TABLE.NAME + " limit 1)";
        Cursor cursor = db.rawQuery(query, null);
        if (cursor != null) {
            if (cursor.getCount() > 0) {
                empty = true;
            }
            cursor.close();
        }
        return empty;
    }

    public boolean isTemperatureEmpty() {
        boolean empty = true;
        String query = "SELECT COUNT(*) FROM  (select 0 from " +
                GoSenseContract.TEMPERATURE_RECORD.TABLE.NAME + " limit 1)";
        Cursor cursor = db.rawQuery(query, null);
        if (cursor != null) {
            if (cursor.getCount() > 0) {
                empty = true;
            }
            cursor.close();
        }
        return empty;
    }

    public boolean isHumidityEmpty() {
        boolean empty = true;
        String query = "SELECT COUNT(*) FROM  (select 0 from " +
                GoSenseContract.RELATIVEHUMIDITY_RECORD.TABLE.NAME + " limit 1)";
        Cursor cursor = db.rawQuery(query, null);
        if (cursor != null) {
            if (cursor.getCount() > 0) {
                empty = true;
            }
            cursor.close();
        }
        return empty;
    }

    public void cleanup(long time) {
        String rhWhereClause = GoSenseContract.RELATIVEHUMIDITY_RECORD.COLUMN.TIME+"<?";
        String rhWhereArgs[] = { String.valueOf(time) };
        db.delete(GoSenseContract.RELATIVEHUMIDITY_RECORD.TABLE.NAME, rhWhereClause, rhWhereArgs);

        String tWhereClause = GoSenseContract.TEMPERATURE_RECORD.COLUMN.TIME+"<?";
        String tWhereArgs[] = { String.valueOf(time) };
        db.delete(GoSenseContract.TEMPERATURE_RECORD.TABLE.NAME, tWhereClause, tWhereArgs);

        String apWhereClause = GoSenseContract.AIRPRESSURE_RECORD.COLUMN.TIME+"<?";
        String apWhereArgs[] = { String.valueOf(time) };
        db.delete(GoSenseContract.AIRPRESSURE_RECORD.TABLE.NAME, apWhereClause, apWhereArgs);
    }

    public void saveTemperatureIfNotExist(TemperatureData temperatureData) {
        String query = "SELECT " +
                GoSenseContract.TEMPERATURE_RECORD.COLUMN.TIME +
                " FROM " + GoSenseContract.TEMPERATURE_RECORD.TABLE.NAME + " WHERE " +
                GoSenseContract.TEMPERATURE_RECORD.COLUMN.TIME + "=" + temperatureData.getTimestamp();
        Cursor cursor = db.rawQuery(query, null);
        if(cursor.getCount() <= 0){
            saveTemperature(temperatureData);
        }
        cursor.close();
    }

    public void saveRelativeHumidityIfNotExist(RelativeHumidityData relativeHumidityData) {
        String query = "SELECT " +
                GoSenseContract.RELATIVEHUMIDITY_RECORD.COLUMN.TIME +
                " FROM " + GoSenseContract.RELATIVEHUMIDITY_RECORD.TABLE.NAME + " WHERE " +
                GoSenseContract.RELATIVEHUMIDITY_RECORD.COLUMN.TIME + "=" + relativeHumidityData.getTimestamp();
        Cursor cursor = db.rawQuery(query, null);
        if(cursor.getCount() <= 0){
            saveRelativeHumidity(relativeHumidityData);
        }
        cursor.close();
    }

    public void saveAirPressureIfNotExist(AirPressureData airPressureData) {
        String query = "SELECT " +
                GoSenseContract.AIRPRESSURE_RECORD.COLUMN.TIME +
                " FROM " + GoSenseContract.AIRPRESSURE_RECORD.TABLE.NAME + " WHERE " +
                GoSenseContract.AIRPRESSURE_RECORD.COLUMN.TIME + "=" + airPressureData.getTimestamp();
        Cursor cursor = db.rawQuery(query, null);
        if(cursor.getCount() <= 0){
            saveAirPressure(airPressureData);
        }
        cursor.close();
    }

    public void saveSensorDeviceIfNotExist(SensorDevice sensorDevice) {
        String query = "SELECT " +
                GoSenseContract.DEVICE_RECORD.COLUMN.ADDRESS +
                " FROM " + GoSenseContract.DEVICE_RECORD.TABLE.NAME + " WHERE " +
                GoSenseContract.DEVICE_RECORD.COLUMN.ADDRESS + "='" + sensorDevice.getAddress()+"'";
        Cursor cursor = db.rawQuery(query, null);
        if(cursor.getCount() <= 0){
            saveSensorDevice(sensorDevice);
        }
        cursor.close();
    }

    public boolean isOpen() {
        return db.isOpen();
    }
}

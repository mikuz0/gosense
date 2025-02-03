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

package com.draekko.gosense.database;

import static com.draekko.gosense.database.GoSenseContract.AIRPRESSURE_RECORD.*;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.draekko.gosense.database.GoSenseContract.*;

public class GoSenseDbHelper extends SQLiteOpenHelper {

    public static final String DB_NAME = "GoSense.db";

    public static final int DB_VERSION = 3;


    public static final String DELETE_DB_AIR_PRESSURE = "DROP TABLE IF EXISTS " +
            AIRPRESSURE_RECORD.TABLE.NAME;
    public static final String DB_AIR_PRESSURE =
            "CREATE TABLE " + AIRPRESSURE_RECORD.TABLE.NAME + " (" +
                    AIRPRESSURE_RECORD.COLUMN.ID + " INTEGER PRIMARY KEY, " +
                    AIRPRESSURE_RECORD.COLUMN.TIME + " INTEGER, " +
                    AIRPRESSURE_RECORD.COLUMN.AIRPRESSURE + " REAL, " +
                    AIRPRESSURE_RECORD.COLUMN.NAME + " TEXT, " +
                    AIRPRESSURE_RECORD.COLUMN.ADDRESS + " TEXT, " +
                    AIRPRESSURE_RECORD.COLUMN.LATITUDE + " REAL, " +
                    AIRPRESSURE_RECORD.COLUMN.LONGITUDE + " REAL)";

    public static final String DELETE_DB_TEMPERATURE = "DROP TABLE IF EXISTS " +
            TEMPERATURE_RECORD.TABLE.NAME;
    public static final String DB_TEMPERATURE =
            "CREATE TABLE " + TEMPERATURE_RECORD.TABLE.NAME + " (" +
                    TEMPERATURE_RECORD.COLUMN.ID + " INTEGER PRIMARY KEY, " +
                    TEMPERATURE_RECORD.COLUMN.TIME + " INTEGER, " +
                    TEMPERATURE_RECORD.COLUMN.TEMPERATURE + " REAL, " +
                    TEMPERATURE_RECORD.COLUMN.NAME + " TEXT, " +
                    TEMPERATURE_RECORD.COLUMN.ADDRESS + " TEXT, " +
                    TEMPERATURE_RECORD.COLUMN.LATITUDE + " REAL, " +
                    TEMPERATURE_RECORD.COLUMN.LONGITUDE + " REAL)";

    public static final String DELETE_DB_RELATIVE_HUMIDITY = "DROP TABLE IF EXISTS " +
            RELATIVEHUMIDITY_RECORD.TABLE.NAME;
    public static final String DB_RELATIVE_HUMIDITY =
            "CREATE TABLE " + RELATIVEHUMIDITY_RECORD.TABLE.NAME + " (" +
                    RELATIVEHUMIDITY_RECORD.COLUMN.ID + " INTEGER PRIMARY KEY, " +
                    RELATIVEHUMIDITY_RECORD.COLUMN.TIME + " INTEGER, " +
                    RELATIVEHUMIDITY_RECORD.COLUMN.RELATIVEHUMIDITY + " REAL, " +
                    RELATIVEHUMIDITY_RECORD.COLUMN.NAME + " TEXT, " +
                    RELATIVEHUMIDITY_RECORD.COLUMN.ADDRESS + " TEXT, " +
                    RELATIVEHUMIDITY_RECORD.COLUMN.LATITUDE + " REAL, " +
                    RELATIVEHUMIDITY_RECORD.COLUMN.LONGITUDE + " REAL)";

    public static final String DELETE_DB_DEVICE = "DROP TABLE IF EXISTS " +
            DEVICE_RECORD.TABLE.NAME;
    public static final String DB_DEVICE =
            "CREATE TABLE " + DEVICE_RECORD.TABLE.NAME + " (" +
                    DEVICE_RECORD.COLUMN.ID + " INTEGER PRIMARY KEY, " +
                    DEVICE_RECORD.COLUMN.ADDRESS + " TEXT, " +
                    DEVICE_RECORD.COLUMN.NAME + " TEXT, " +
                    DEVICE_RECORD.COLUMN.ALIASNAME + " TEXT, " +
                    DEVICE_RECORD.COLUMN.COLOR + " INTEGER)";

    public static final String DB_ALTER_AIR_PRESSURE_TABLE_1 =
            "ALTER TABLE " + RELATIVEHUMIDITY_RECORD.TABLE.NAME + " RENAME TO " +
                    RELATIVEHUMIDITY_RECORD.TABLE.NAME + "_old";
    public static final String DB_ALTER_AIR_PRESSURE_TABLE_2 = DB_RELATIVE_HUMIDITY;
    public static final String DB_ALTER_AIR_PRESSURE_TABLE_3 =
            "INSERT INTO " + RELATIVEHUMIDITY_RECORD.TABLE.NAME + " (" +
                    RELATIVEHUMIDITY_RECORD.COLUMN.ID + "," +
                    RELATIVEHUMIDITY_RECORD.COLUMN.TIME + "," +
                    RELATIVEHUMIDITY_RECORD.COLUMN.RELATIVEHUMIDITY + "," +
                    RELATIVEHUMIDITY_RECORD.COLUMN.NAME + "," +
                    RELATIVEHUMIDITY_RECORD.COLUMN.ADDRESS + "," +
                    RELATIVEHUMIDITY_RECORD.COLUMN.LATITUDE + "," +
                    RELATIVEHUMIDITY_RECORD.COLUMN.LONGITUDE  +
                    ") SELECT " +
                    RELATIVEHUMIDITY_RECORD.COLUMN.ID + "," +
                    RELATIVEHUMIDITY_RECORD.COLUMN.TIME + "," +
                    RELATIVEHUMIDITY_RECORD.COLUMN.RELATIVEHUMIDITY + "," +
                    RELATIVEHUMIDITY_RECORD.COLUMN.NAME + "," +
                    RELATIVEHUMIDITY_RECORD.COLUMN.ADDRESS + "," +
                    RELATIVEHUMIDITY_RECORD.COLUMN.LATITUDE + "," +
                    RELATIVEHUMIDITY_RECORD.COLUMN.LONGITUDE  +
                    " FROM " + RELATIVEHUMIDITY_RECORD.TABLE.NAME + "_old";
    public static final String DB_ALTER_AIR_PRESSURE_TABLE_4 =
            "DROP TABLE " + RELATIVEHUMIDITY_RECORD.TABLE.NAME + "_old";


    public GoSenseDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    public void deleteAll(SQLiteDatabase db) {
        db.execSQL(DELETE_DB_AIR_PRESSURE);
        db.execSQL(DELETE_DB_TEMPERATURE);
        db.execSQL(DELETE_DB_RELATIVE_HUMIDITY);
        db.execSQL(DELETE_DB_DEVICE);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DB_AIR_PRESSURE);
        db.execSQL(DB_TEMPERATURE);
        db.execSQL(DB_RELATIVE_HUMIDITY);
        db.execSQL(DB_DEVICE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion == 1 && newVersion == 2) {
            db.execSQL(DB_DEVICE);
        }
        if (oldVersion == 1 && newVersion == 3 ) {
            db.execSQL(DB_DEVICE);
            db.execSQL(DB_ALTER_AIR_PRESSURE_TABLE_1);
            db.execSQL(DB_ALTER_AIR_PRESSURE_TABLE_2);
            db.execSQL(DB_ALTER_AIR_PRESSURE_TABLE_3);
            db.execSQL(DB_ALTER_AIR_PRESSURE_TABLE_4);
        }
        if (oldVersion == 2 && newVersion == 3) {
            db.execSQL(DB_ALTER_AIR_PRESSURE_TABLE_1);
            db.execSQL(DB_ALTER_AIR_PRESSURE_TABLE_2);
            db.execSQL(DB_ALTER_AIR_PRESSURE_TABLE_3);
            db.execSQL(DB_ALTER_AIR_PRESSURE_TABLE_4);
        }
        if (false && oldVersion > newVersion) {
            deleteAll(db);
            onCreate(db);
        }
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        super.onDowngrade(db, oldVersion, newVersion);
    }
}

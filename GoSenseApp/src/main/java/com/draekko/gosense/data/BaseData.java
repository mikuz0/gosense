/*
 * GoSense Android App
 * Copyright (C) 2024, 2025 Draekko
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

package com.draekko.gosense.data;

public class BaseData {

    private long id;
    private long timestamp;
    private double latitude;
    private double longitude;
    private String address;
    private String name;

    public BaseData(long time, double lat, double lng, String nm, String addr) {
        this.id = -9999;
        this.timestamp = time;
        this.latitude = lat;
        this.longitude = lng;
        this.address = addr;
        this.name = nm;
    }

    public BaseData(long id, long time, double lat, double lng, String nm, String addr) {
        this.id = id;
        this.timestamp = time;
        this.latitude = lat;
        this.longitude = lng;
        this.address = addr;
        this.name = nm;
    }

    public long getId() {
        return id;
    }

    public String getAddress() {
        return address;
    }

    public String getName() {
        return name;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setTimestamp(long time) {
        timestamp = time;
    }

    public void setLatitude(double lat) {
        latitude = lat;
    }

    public void setLongitude(double lng) {
        longitude = lng;
    }

    public void setAddress(String addr) {
        address = addr;
    }

    public void setName(String nm) {
        name = nm;
    }

}

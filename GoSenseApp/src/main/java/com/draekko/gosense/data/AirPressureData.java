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

import com.draekko.gosense.misc.Utils;

public class AirPressureData extends BaseData {

    private float airpressure;

    public AirPressureData(long time, float airprsr, double lat, double lng, String nm, String addr) {
        super(time, lat, lng, nm, addr);
        airpressure = airprsr;
    }

    public AirPressureData(long id, long time, float airprsr, double lat, double lng, String nm, String addr) {
        super(id, time, lat, lng, nm, addr);
        airpressure = airprsr;
    }

    public AirPressureData() {
        super(-9999, -9999, -9999, "BT-DEVICE", Utils.DEFAULT_EMPTY_ADDRESS);
        airpressure = -9999;
    }

    public float getAirPressure() {
        return airpressure;
    }

    public void setAirPressure(float airprsr) {
        airpressure = airprsr;
    }

}

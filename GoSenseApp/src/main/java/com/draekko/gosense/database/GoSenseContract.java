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

package com.draekko.gosense.database;

public class GoSenseContract {

    private GoSenseContract() {}

    public static class TEMPERATURE_RECORD {

        public static class TABLE {
            public static final String NAME = "TEMPERATURE_RECORDS";
        }

        public static class COLUMN {
            public static final String ID = "_ID";
            public static final String TIME = "TIME";

            public static final String TEMPERATURE = "TEMPERATURE";
            public static final String LONGITUDE = "LONGITUDE";
            public static final String LATITUDE = "LATITUDE";

            public static final String NAME = "NAME";
            public static final String ADDRESS = "ADDRESS";
        }
    }

    public static class RELATIVEHUMIDITY_RECORD {

        public static class TABLE {
            public static final String NAME = "RELATIVEHUMIDITY_RECORDS";
        }

        public static class COLUMN {
            public static final String ID = "_ID";
            public static final String TIME = "TIME";

            public static final String RELATIVEHUMIDITY = "RELATIVEHUMIDITY";
            public static final String LONGITUDE = "LONGITUDE";
            public static final String LATITUDE = "LATITUDE";

            public static final String NAME = "NAME";
            public static final String ADDRESS = "ADDRESS";
        }
    }

    public static class AIRPRESSURE_RECORD {

        public static class TABLE {
            public static final String NAME = "AIRPRESSURE_RECORDS";
        }

        public static class COLUMN {
            public static final String ID = "_ID";
            public static final String TIME = "TIME";

            public static final String AIRPRESSURE = "AIRPRESSURE";
            public static final String LONGITUDE = "LONGITUDE";
            public static final String LATITUDE = "LATITUDE";

            public static final String NAME = "NAME";
            public static final String ADDRESS = "ADDRESS";
        }
    }


    public static class DEVICE_RECORD {

        public static class TABLE {
            public static final String NAME = "DEVICE_RECORDS";
        }

        public static class COLUMN {
            public static final String ID = "_ID";
            public static final String ADDRESS = "ADDRESS";
            public static final String NAME = "NAME";
            public static final String ALIASNAME = "ALIASNAME";
            public static final String COLOR = "COLOR";
        }
    }

}

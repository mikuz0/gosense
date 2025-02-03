/*
 * Copyright (c) 2017, Sensirion AG
 * Copyright (c) 2024, Draekko RAND
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * * Neither the name of Sensirion AG nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.draekko.gosense.misc;

import java.math.RoundingMode;
import java.text.DecimalFormat;

public class Converter {

    public static float convertToC(final float tempInFahrenheit) {
        return (tempInFahrenheit - 32f) * 5f / 9f;
    }

    public static float convertToF(final float tempInC) {
        return (tempInC * 9f / 5f + 32f);
    }

    public static float calcDewPoint(final float relativeHumidity, final float ambientTemperature) {
        float h = (float) (Math.log((relativeHumidity / 100.0)) + (17.62 * ambientTemperature) / (243.12 + ambientTemperature));
        return (float) (243.12 * h / (17.62 - h));
    }

    public static float calculateHeatIndexCelsius(final float relativeHumidity, final float ambientTemperatureCelsius) {
        return HeatIndexCalculator.calcHeatIndexInCelsius(relativeHumidity, ambientTemperatureCelsius);
    }

    public static float calculateHeatIndexFahrenheit(final float relativeHumidity, final float ambientTemperatureFahrenheit) {
        return HeatIndexCalculator.calcHeatIndexInFahrenheit(relativeHumidity, ambientTemperatureFahrenheit);
    }

    public static float calculateHeatIndexCelsiusOld(final float relativeHumidity, final float ambientTemperatureCelsius) {
        return HeatIndexCalculator.calcHeatIndexInCelsiusOld(relativeHumidity, ambientTemperatureCelsius);
    }

    public static float calculateHeatIndexFahrenheitOld(final float relativeHumidity, final float ambientTemperatureFahrenheit) {
        return HeatIndexCalculator.calcHeatIndexInFahrenheitOld(relativeHumidity, ambientTemperatureFahrenheit);
    }

    /**
     * @param rh relative humidity
     * @param t ambient temperature in Fahrenheit.
     * @return Humidex.
     */
    public static float calculateHumidexFahrenheit(final float rh, final float t) {
        float t_celsius = (t - 32)  / 1.8f;
        float t_kelvin = t_celsius + 273;
        float eTs = (float)Math.pow(10, ((-2937.4f / t_kelvin) - 4.9283f * Math.log(t_kelvin) / 2.302585092994046f + 23.5471f));
        float eTd = eTs * rh /100;
        float humidex = Math.round(t_celsius + ((eTd - 10) * 5f / 9f));
        float tfh = Math.round(((9.0f/5.0f) * humidex + 32.0f));
        if (tfh < t) {
            tfh = t;
        }
        return tfh;
    }

    /**
     * @param rh relative humidity
     * @param t ambient temperature in Celsius.
     * @return Humidex.
     */
    public static float calculateHumidexCelsius(final float rh, final float t) {
        float t_kelvin = t + 273;
        float eTs = (float)Math.pow(10, ((-2937.4f / t_kelvin) - 4.9283f * Math.log(t_kelvin) / 2.302585092994046f + 23.5471f));
        float eTd = eTs * rh /100;
        float tf = Math.round((1.8f * t + 32.0f)); // temp in F
        float humidex=Math.round(t + ((eTd - 10) * 5f / 9f));
        if (humidex < t) {
            humidex = t;
        }
        return humidex;
    }

    private static class HeatIndexCalculator {

        /**
         * Heat formula coefficients.
         */
        private final static float c1 = 16.923f;
        private final static float c2 = 0.185212f;
        private final static float c3 = 5.37941f;
        private final static float c4 = -0.100254f;
        private final static float c5 = 9.41695E-3f;
        private final static float c6 = 7.28898E-3f;
        private final static float c7 = 3.45372E-4f;
        private final static float c8 = -8.14971E-4f;
        private final static float c9 = 1.02102E-5f;
        private final static float c10 = -3.8646E-5f;
        private final static float c11 = 2.91583E-5f;
        private final static float c12 = 1.42721E-6f;
        private final static float c13 = 1.97483E-7f;
        private final static float c14 = -2.18429E-8f;
        private final static float c15 = 8.43296E-10f;
        private final static float c16 = -4.81975E-11f;

        /**
         * Heat formula boundaries.
         */
        private final static float LOW_BOUNDARY_FORMULA_FAHRENHEIT = 70f;
        private final static float UPPER_BOUNDARY_FORMULA_FAHRENHEIT = 115f;

        /**
         * This method obtains the heat index of a temperature and humidity
         * using the formula from: http://en.wikipedia.org/wiki/Heat_index that
         * comes from Stull, Richard (2000). Meteorology for Scientists and
         * Engineers, Second Edition. Brooks/Cole. p. 60. ISBN 9780534372149.
         *
         * @param relativeHumidity relative humidity
         * @param tempInCelsius    ambient temperature in Celsius.
         * @return Heat Index.
         */
        private static float calcHeatIndexInCelsius(final float relativeHumidity, final float tempInCelsius) {
            final float tempInFahrenheit = Converter.convertToF(tempInCelsius);
            final float heatIndexInFahrenheit = calcHeatIndexInFahrenheit(relativeHumidity, tempInFahrenheit);
            return Converter.convertToC(heatIndexInFahrenheit);
        }

        /**
         * This method obtains the heat index of a temperature and humidity
         * using the formula from: http://en.wikipedia.org/wiki/Heat_index that
         * comes from Stull, Richard (2000). Meteorology for Scientists and
         * Engineers, Second Edition. Brooks/Cole. p. 60. ISBN 9780534372149.
         *
         * @param relativeHumidity relative humidity
         * @param tempInCelsius    ambient temperature in Celsius.
         * @return Heat Index.
         */
        private static float calcHeatIndexInCelsiusOld(final float relativeHumidity, final float tempInCelsius) {
            final float tempInFahrenheit = Converter.convertToF(tempInCelsius);
            final float heatIndexInFahrenheit = calcHeatIndexInFahrenheitOld(relativeHumidity, tempInFahrenheit);
            return Converter.convertToC(heatIndexInFahrenheit);
        }

        /**
         * This method obtains the heat index of a temperature and humidity
         * using the formula from: http://en.wikipedia.org/wiki/Heat_index that
         * comes from Stull, Richard (2000). Meteorology for Scientists and
         * Engineers, Second Edition. Brooks/Cole. p. 60. ISBN 9780534372149.
         *
         * @param h relative humidity
         * @param t ambient temperature in Fahrenheit.
         * @return Heat Index.
         */
        private static float calcHeatIndexInFahrenheitOld(final float rh, final float t) {

            //Checks if the temperature and the humidity makes sense.
            if (t > UPPER_BOUNDARY_FORMULA_FAHRENHEIT || rh < 0 || rh > 100) {
                return Float.NaN;
            } else if (t < LOW_BOUNDARY_FORMULA_FAHRENHEIT) {
                // use actual temperature for heat index if below LOW_BOUNDARY_FORMULA_FAHRENHEIT
                return t;
            }

            //Prepares values for improving the readability of the method.
            final float t2 = t * t;
            final float t3 = t2 * t;
            final float h2 = rh * rh;
            final float h3 = h2 * rh;

            return c1
                    + c2 * t
                    + c3 * rh
                    + c4 * t * rh
                    + c5 * t2
                    + c6 * h2
                    + c7 * t2 * rh
                    + c8 * t * h2
                    + c9 * t2 * h2
                    + c10 * t3
                    + c11 * h3
                    + c12 * t3 * rh
                    + c13 * t * h3
                    + c14 * t3 * h2
                    + c15 * t2 * h3
                    + c16 * t3 * h3;
        }

        /**
         * Utility class to calculate the heat index, given a Fahrenheit temperature (F) and relative humidity (rh).
         *
         * See http://www.srh.noaa.gov/images/ffc/pdf/ta_htindx.PDF for calculation details, noting that there is a
         * degree of variance in result to be expected since the equation is an approximation derived from regression analysis.
         *
         * Formula from from
         * https://github.com/USDepartmentofLabor/Calculate-Heat-Index-Java/blob/master/src/main/java/gov/dol/CalculateHeatIndex.java
         */
        public static final String RESULT_FORMAT = "#.#";
        private static float calcHeatIndexInFahrenheit(final float rh, final float t) {
            double Hindex;

            Hindex = -42.379 + 2.04901523 * t + 10.14333127 * rh;
            Hindex = Hindex - 0.22475541 * t * rh - 6.83783 * Math.pow(10, -3) * t * t;
            Hindex = Hindex - 5.481717 * Math.pow(10, -2) * rh * rh;
            Hindex = Hindex + 1.22874 * Math.pow(10, -3) * t * t * rh;
            Hindex = Hindex + 8.5282 * Math.pow(10, -4) * t * rh * rh;
            Hindex = Hindex - 1.99 * Math.pow(10, -6) * t * t * rh * rh;

            DecimalFormat df = new DecimalFormat(RESULT_FORMAT);
            df.setRoundingMode(RoundingMode.HALF_UP);

            return Float.parseFloat(df.format(Hindex));
        }
    }
}

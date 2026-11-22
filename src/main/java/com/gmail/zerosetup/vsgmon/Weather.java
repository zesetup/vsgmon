package com.gmail.zerosetup.vsgmon;

/**
 * Weather readings from one source. Any field may be null if the source did not provide it.
 *
 * @param temperature   e.g. "+0.8 °C"
 * @param humidity      e.g. "96%"
 * @param windSpeed     e.g. "3.3 m/s"
 * @param windDirection e.g. "SW"
 */
public record Weather(String temperature, String humidity, String windSpeed, String windDirection) {

    public static final Weather EMPTY = new Weather(null, null, null, null);

    /** Fields missing in {@code fresh} keep their previous value from {@code this}. */
    public Weather mergedWith(Weather fresh) {
        if (fresh == null) {
            return this;
        }
        return new Weather(
                fresh.temperature != null ? fresh.temperature : temperature,
                fresh.humidity != null ? fresh.humidity : humidity,
                fresh.windSpeed != null ? fresh.windSpeed : windSpeed,
                fresh.windDirection != null ? fresh.windDirection : windDirection);
    }
}

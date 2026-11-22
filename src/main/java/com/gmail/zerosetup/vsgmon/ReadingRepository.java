package com.gmail.zerosetup.vsgmon;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Repository
public class ReadingRepository {

    private static final Pattern SIGNED_NUMBER = Pattern.compile("([+\\-−]?)(\\d+(?:[.,]\\d+)?)");

    private final JdbcTemplate jdbc;

    public ReadingRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Stores one fresh reading; values the source did not provide are stored as NULL. */
    public void save(String source, long timestamp, Weather weather) {
        Double temperature = number(weather.temperature());
        Double humidity = number(weather.humidity());
        Double windSpeed = number(weather.windSpeed());
        if (temperature == null && humidity == null && windSpeed == null) {
            return;
        }
        jdbc.update("INSERT INTO reading (ts, source, temperature, humidity, wind_speed) VALUES (?, ?, ?, ?, ?)",
                timestamp, source, temperature, humidity, windSpeed);
    }

    /**
     * Values averaged over all sources, grouped into buckets of {@code bucketMillis},
     * for readings newer than {@code since}.
     */
    public List<HistoryPoint> averages(long since, long bucketMillis) {
        return jdbc.query("""
                        SELECT (ts / ?) * ? AS bucket,
                               AVG(temperature) AS temperature,
                               AVG(humidity)    AS humidity,
                               AVG(wind_speed)  AS wind_speed
                        FROM reading
                        WHERE ts >= ?
                        GROUP BY bucket
                        ORDER BY bucket
                        """,
                (rs, i) -> new HistoryPoint(
                        rs.getLong("bucket") + bucketMillis / 2,
                        rs.getObject("temperature", Double.class),
                        rs.getObject("humidity", Double.class),
                        rs.getObject("wind_speed", Double.class)),
                bucketMillis, bucketMillis, since);
    }

    /** "+1.8 °C" -> 1.8, "-3 C" -> -3.0, "95%" -> 95.0, "3.3 m/s" -> 3.3; null if no number. */
    static Double number(String value) {
        if (value == null) {
            return null;
        }
        Matcher m = SIGNED_NUMBER.matcher(value);
        if (!m.find()) {
            return null;
        }
        double n = Double.parseDouble(m.group(2).replace(',', '.'));
        return m.group(1).isEmpty() || m.group(1).equals("+") ? n : -n;
    }

    /** @param time middle of the bucket, epoch millis */
    public record HistoryPoint(long time, Double temperature, Double humidity, Double windSpeed) {
    }
}

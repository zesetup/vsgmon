package com.gmail.zerosetup.vsgmon;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TemperatureService {

    private static final Logger log = LoggerFactory.getLogger(TemperatureService.class);
    private static final String SUGARDAS_URL = "http://web.sugardas.lt/";
    private static final String TTS_URL = "https://web.tts.lt/";
    private static final String IAE_URL = "https://altra.lt/aktualu/radiacinis-monitoringas/";
    private static final Pattern LEADING_NUMBER = Pattern.compile("^(\\d+(?:[.,]\\d+)?)");
    private static final Pattern NUMBER = Pattern.compile("(\\d+(?:[.,]\\d+)?)");
    // Sugardas wind: "V 1.2 m/s" — Lithuanian direction letters before the speed
    private static final Pattern SUGARDAS_WIND = Pattern.compile("^([ŠPRV]{1,2})?\\s*(\\d+(?:[.,]\\d+)?)");

    // Cached values, refreshed by the scheduler and served to clients as is.
    private volatile Weather sugardas = Weather.EMPTY;
    private volatile Weather tts = Weather.EMPTY;
    private volatile Weather iae = Weather.EMPTY;

    private final ReadingRepository readings;

    public TemperatureService(ReadingRepository readings) {
        this.readings = readings;
    }

    public Weather getSugardas() {
        return sugardas;
    }

    public Weather getTts() {
        return tts;
    }

    public Weather getIae() {
        return iae;
    }

    @Scheduled(initialDelay = 0, fixedRate = 60_000)
    public void refresh() {
        // A failed fetch (or a missing field) keeps the last good value.
        sugardas = sugardas.mergedWith(fetchAndStore("Sugardas", SUGARDAS_URL, TemperatureService::parseSugardas));
        tts = tts.mergedWith(fetchAndStore("TTS", TTS_URL, TemperatureService::parseTts));
        iae = iae.mergedWith(fetchAndStore("IAE", IAE_URL, TemperatureService::parseIae));
    }

    // Only fresh values go to the history, never the cached fallbacks.
    private Weather fetchAndStore(String name, String url, Parser parser) {
        Weather fresh = fetch(name, url, parser);
        if (fresh != null) {
            try {
                readings.save(name, System.currentTimeMillis(), fresh);
            } catch (RuntimeException e) {
                log.error("Error saving {} reading: {}", name, e.getMessage(), e);
            }
        }
        return fresh;
    }

    private interface Parser {
        Weather parse(Document doc);
    }

    private Weather fetch(String name, String url, Parser parser) {
        log.info("Fetching weather from {}: {}", name, url);
        try {
            Document doc = Jsoup.connect(url)
                    .timeout(10000)
                    .get();
            Weather weather = parser.parse(doc);
            if (weather.temperature() == null) {
                log.warn("{} temperature not found on page", name);
            }
            log.info("Fetched {} weather: {}", name, weather);
            return weather;
        } catch (IOException e) {
            log.error("Error fetching {} weather: {}", name, e.getMessage(), e);
            return null;
        }
    }

    static Weather parseSugardas(Document doc) {
        // Rows are <td> cells with background images bg_1 (temperature), bg_2 (humidity), bg_3 (wind)
        String temperature = text(doc.selectFirst("td[style*=client/weather/bg_1.jpg]"));
        String humidity = percent(text(doc.selectFirst("td[style*=client/weather/bg_2.jpg]")));

        String windSpeed = null;
        String windDirection = null;
        String wind = text(doc.selectFirst("td[style*=client/weather/bg_3.jpg]"));
        if (wind != null) {
            Matcher m = SUGARDAS_WIND.matcher(wind);
            if (m.find()) {
                windDirection = lithuanianDirection(m.group(1));
                windSpeed = metersPerSecond(m.group(2));
            }
        }
        return new Weather(temperature, humidity, windSpeed, windDirection);
    }

    static Weather parseTts(Document doc) {
        // Current layout: <strong data-weather-temperature>1.9 °C</strong>, <dd data-weather-humidity>96%</dd>, ...
        String temperature = text(doc.selectFirst("[data-weather-temperature]"));

        // Legacy layout: span "Температура:" followed by a span with the value
        if (temperature == null) {
            Element label = doc.selectFirst("span.blue:contains(Температура:)");
            temperature = label != null ? text(label.nextElementSibling()) : null;
        }

        return new Weather(
                withPlusSign(temperature),
                percent(text(doc.selectFirst("[data-weather-humidity]"))),
                metersPerSecond(text(doc.selectFirst("[data-weather-wind-speed]"))),
                text(doc.selectFirst("[data-weather-wind-direction]")));
    }

    static Weather parseIae(Document doc) {
        // Footer "Aplinkos duomenys": <div class="param temperature"><svg/> 0.7 C</div>, ...
        return new Weather(
                withPlusSign(text(doc.selectFirst(".env-params .param.temperature"))),
                percent(text(doc.selectFirst(".env-params .param.humidity"))),
                metersPerSecond(text(doc.selectFirst(".env-params .param.wind-speed"))),
                null);
    }

    /** Trimmed text, or null if missing, blank, "***" (sensor in maintenance) or "н/д"/"nėra" (no data). */
    private static String text(Element element) {
        if (element == null) {
            return null;
        }
        String text = element.text().trim();
        if (text.isEmpty() || text.contains("***") || text.equals("н/д") || text.equals("nėra")) {
            return null;
        }
        return text;
    }

    /** "  95 %" / "96.0 %" -> "95%" / "96%". */
    static String percent(String value) {
        String number = firstNumber(value);
        return number == null ? null : Math.round(Double.parseDouble(number)) + "%";
    }

    /** "3.3 м/с" / "3.0 m/s" -> "3.3 m/s" / "3 m/s". */
    static String metersPerSecond(String value) {
        String number = firstNumber(value);
        if (number == null) {
            return null;
        }
        String formatted = number.replaceFirst("\\.0+$", "");
        return formatted + " m/s";
    }

    private static String firstNumber(String value) {
        if (value == null) {
            return null;
        }
        Matcher m = NUMBER.matcher(value);
        return m.find() ? m.group(1).replace(',', '.') : null;
    }

    /** Lithuanian compass letters (Š, P, R, V) to English (N, S, E, W), e.g. "PV" -> "SW". */
    static String lithuanianDirection(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (char c : value.toCharArray()) {
            switch (c) {
                case 'Š' -> sb.append('N');
                case 'P' -> sb.append('S');
                case 'R' -> sb.append('E');
                case 'V' -> sb.append('W');
                default -> { return null; }
            }
        }
        return sb.toString();
    }

    // "1.9 °C" -> "+1.9 °C"; zero, negative and already signed values stay as is.
    static String withPlusSign(String value) {
        if (value == null) {
            return null;
        }
        Matcher m = LEADING_NUMBER.matcher(value);
        if (m.find() && Double.parseDouble(m.group(1).replace(',', '.')) > 0) {
            return "+" + value;
        }
        return value;
    }
}

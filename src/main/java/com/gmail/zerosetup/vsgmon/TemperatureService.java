package com.gmail.zerosetup.vsgmon;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
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
    private static final Pattern LEADING_NUMBER = Pattern.compile("^(\\d+(?:[.,]\\d+)?)");
    private static final String NOT_AVAILABLE = "Temperature not available";

    // Cached values, refreshed by the scheduler and served to clients as is.
    private volatile String temperature = NOT_AVAILABLE;
    private volatile String ttsTemperature = NOT_AVAILABLE;

    public String getTemperature() {
        return temperature;
    }

    public String getTtsTemperature() {
        return ttsTemperature;
    }

    @Scheduled(initialDelay = 0, fixedRate = 60_000)
    public void refresh() {
        temperature = update(temperature, fetchTemperature());
        ttsTemperature = update(ttsTemperature, fetchTtsTemperature());
    }

    // Keep the last good value if the new fetch failed.
    private static String update(String current, String fetched) {
        if (fetched != null) {
            return fetched;
        }
        return current;
    }

    private String fetchTemperature() {
        log.info("Fetching temperature from Sugardas: {}", SUGARDAS_URL);
        try {
            Document doc = Jsoup.connect(SUGARDAS_URL)
                    .timeout(10000)
                    .get();

            // Find the td element with the weather background image
            Elements elements = doc.select("td[style*=client/weather/bg_1.jpg]");

            if (!elements.isEmpty()) {
                Element tempElement = elements.first();
                String temperature = tempElement.text().trim();
                log.info("Successfully fetched Sugardas temperature: {}", temperature);
                return temperature;
            }

            log.warn("Sugardas temperature element not found on page");
            return null;
        } catch (IOException e) {
            log.error("Error fetching Sugardas temperature: {}", e.getMessage(), e);
            return null;
        }
    }

    private String fetchTtsTemperature() {
        log.info("Fetching temperature from TTS: {}", TTS_URL);
        try {
            Document doc = Jsoup.connect(TTS_URL)
                    .timeout(10000)
                    .get();

            // Current layout: <strong data-weather-temperature>1.9 °C</strong>
            Element current = doc.selectFirst("[data-weather-temperature]");
            if (current != null && !current.text().isBlank()) {
                String temperature = withPlusSign(current.text().trim());
                log.info("Successfully fetched TTS temperature: {}", temperature);
                return temperature;
            }

            // Legacy layout: span "Температура:" followed by a span with the value
            Elements elements = doc.select("span.blue:contains(Температура:)");

            if (!elements.isEmpty()) {
                Element tempLabel = elements.first();
                Element tempValue = tempLabel.nextElementSibling();
                if (tempValue != null) {
                    String temperature = withPlusSign(tempValue.text().trim());
                    log.info("Successfully fetched TTS temperature: {}", temperature);
                    return temperature;
                }
            }

            log.warn("TTS temperature element not found on page");
            return null;
        } catch (IOException e) {
            log.error("Error fetching TTS temperature: {}", e.getMessage(), e);
            return null;
        }
    }

    // "1.9 °C" -> "+1.9 °C"; zero, negative and already signed values stay as is.
    static String withPlusSign(String value) {
        Matcher m = LEADING_NUMBER.matcher(value);
        if (m.find() && Double.parseDouble(m.group(1).replace(',', '.')) > 0) {
            return "+" + value;
        }
        return value;
    }
}

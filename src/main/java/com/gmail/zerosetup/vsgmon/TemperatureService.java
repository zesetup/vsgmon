package com.gmail.zerosetup.vsgmon;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class TemperatureService {

    private static final Logger log = LoggerFactory.getLogger(TemperatureService.class);
    private static final String SUGARDAS_URL = "http://web.sugardas.lt/";
    private static final String TTS_URL = "https://web.tts.lt/";

    public String getTemperature() {
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
            return "Temperature not available";
        } catch (IOException e) {
            log.error("Error fetching Sugardas temperature: {}", e.getMessage(), e);
            return "Error fetching temperature: " + e.getMessage();
        }
    }

    public String getTtsTemperature() {
        log.info("Fetching temperature from TTS: {}", TTS_URL);
        try {
            Document doc = Jsoup.connect(TTS_URL)
                    .timeout(10000)
                    .get();

            // Find the span element with "Температура:" and get the next span with the temperature
            Elements elements = doc.select("span.blue:contains(Температура:)");

            if (!elements.isEmpty()) {
                Element tempLabel = elements.first();
                Element tempValue = tempLabel.nextElementSibling();
                if (tempValue != null) {
                    String temperature = tempValue.text().trim();
                    log.info("Successfully fetched TTS temperature: {}", temperature);
                    return temperature;
                }
            }

            log.warn("TTS temperature element not found on page");
            return "Temperature not available";
        } catch (IOException e) {
            log.error("Error fetching TTS temperature: {}", e.getMessage(), e);
            return "Error fetching temperature: " + e.getMessage();
        }
    }
}

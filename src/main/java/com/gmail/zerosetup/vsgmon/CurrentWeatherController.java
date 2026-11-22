package com.gmail.zerosetup.vsgmon;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class CurrentWeatherController {

    private final TemperatureService temperatureService;

    public CurrentWeatherController(TemperatureService temperatureService) {
        this.temperatureService = temperatureService;
    }

    /** Cached values per source, the same ones the page renders. */
    @GetMapping("/api/current")
    public Map<String, Weather> current() {
        Map<String, Weather> result = new LinkedHashMap<>();
        result.put("sugardas", temperatureService.getSugardas());
        result.put("tts", temperatureService.getTts());
        result.put("iae", temperatureService.getIae());
        return result;
    }
}

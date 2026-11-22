package com.gmail.zerosetup.vsgmon;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;

@RestController
public class HistoryController {

    private static final Duration PERIOD = Duration.ofHours(6);
    private static final Duration BUCKET = Duration.ofMinutes(5);

    private final ReadingRepository readings;

    public HistoryController(ReadingRepository readings) {
        this.readings = readings;
    }

    /** Last 6 hours, averaged over the three sources in 5-minute buckets. */
    @GetMapping("/api/history")
    public List<ReadingRepository.HistoryPoint> history() {
        long since = System.currentTimeMillis() - PERIOD.toMillis();
        return readings.averages(since, BUCKET.toMillis());
    }
}

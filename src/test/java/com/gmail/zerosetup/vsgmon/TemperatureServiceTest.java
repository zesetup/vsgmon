package com.gmail.zerosetup.vsgmon;

import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TemperatureServiceTest {

	@Test
	void addsPlusToPositiveTemperature() {
		assertEquals("+1.9 °C", TemperatureService.withPlusSign("1.9 °C"));
		assertEquals("+12 °C", TemperatureService.withPlusSign("12 °C"));
		assertEquals("+0,5 °C", TemperatureService.withPlusSign("0,5 °C"));
	}

	@Test
	void leavesZeroNegativeAndSignedAsIs() {
		assertEquals("0 °C", TemperatureService.withPlusSign("0 °C"));
		assertEquals("0.0 °C", TemperatureService.withPlusSign("0.0 °C"));
		assertEquals("-3.2 °C", TemperatureService.withPlusSign("-3.2 °C"));
		assertEquals("+2 °C", TemperatureService.withPlusSign("+2 °C"));
		assertEquals("−1 °C", TemperatureService.withPlusSign("−1 °C"));
	}

	@Test
	void normalizesHumidityAndWind() {
		assertEquals("95%", TemperatureService.percent("   95 %"));
		assertEquals("96%", TemperatureService.percent("96.0 %"));
		assertEquals("3.3 m/s", TemperatureService.metersPerSecond("3.3 м/с"));
		assertEquals("3 m/s", TemperatureService.metersPerSecond("3.0 m/s"));
		assertEquals(null, TemperatureService.percent(null));
	}

	@Test
	void translatesLithuanianWindDirection() {
		assertEquals("W", TemperatureService.lithuanianDirection("V"));
		assertEquals("NW", TemperatureService.lithuanianDirection("ŠV"));
		assertEquals("SE", TemperatureService.lithuanianDirection("PR"));
	}

	@Test
	void parsesPages() {
		Weather sug = TemperatureService.parseSugardas(Jsoup.parse(
				"<table><tr><td style=\"background-image:url(client/weather/bg_1.jpg)\">+1 C</td></tr>"
				+ "<tr><td style=\"background-image:url(client/weather/bg_2.jpg)\">   95 %</td></tr>"
				+ "<tr><td style=\"background-image:url(client/weather/bg_3.jpg)\">V 1.2 m/s </td></tr></table>"));
		assertEquals(new Weather("+1 C", "95%", "1.2 m/s", "W"), sug);

		Weather tts = TemperatureService.parseTts(Jsoup.parse(
				"<strong data-weather-temperature>0.8 °C</strong><dd data-weather-humidity>96%</dd>"
				+ "<dd data-weather-wind-speed>3.3 м/с</dd><dd data-weather-wind-direction>SW</dd>"));
		assertEquals(new Weather("+0.8 °C", "96%", "3.3 m/s", "SW"), tts);

		Weather iae = TemperatureService.parseIae(Jsoup.parse(
				"<div class=\"env-params\"><div class=\"param humidity\">96.0 %</div>"
				+ "<div class=\"param wind-speed\">3.0 m/s</div><div class=\"param temperature\">0.7 C</div></div>"));
		assertEquals(new Weather("+0.7 C", "96%", "3 m/s", null), iae);
	}

	@Test
	void keepsLastGoodValues() {
		Weather old = new Weather("+1 C", "95%", "1 m/s", "W");
		assertEquals(new Weather("+2 C", "95%", "1 m/s", "W"), old.mergedWith(new Weather("+2 C", null, null, null)));
		assertEquals(old, old.mergedWith(null));
	}

	@Test
	void extractsNumbersForHistory() {
		assertEquals(1.8, ReadingRepository.number("+1.8 °C"));
		assertEquals(-3.0, ReadingRepository.number("-3 C"));
		assertEquals(-1.5, ReadingRepository.number("−1,5 °C"));
		assertEquals(95.0, ReadingRepository.number("95%"));
		assertEquals(3.3, ReadingRepository.number("3.3 m/s"));
		assertEquals(null, ReadingRepository.number("Temperature not available"));
	}

	@Test
	void fetchNeverThrows() {
		// Response cut off mid-body: jsoup throws an unchecked exception, not IOException
		assertEquals(null, TemperatureService.fetch("X",
				() -> { throw new org.jsoup.UncheckedIOException(new java.io.IOException("Read timed out")); },
				TemperatureService::parseTts));
		assertEquals(null, TemperatureService.fetch("X",
				() -> { throw new java.io.IOException("Connection refused"); },
				TemperatureService::parseTts));
		assertEquals(null, TemperatureService.fetch("X",
				() -> Jsoup.parse("<p>ok</p>"),
				doc -> { throw new IllegalStateException("parser bug"); }));
		assertEquals(new Weather("+0.8 °C", null, null, null), TemperatureService.fetch("X",
				() -> Jsoup.parse("<strong data-weather-temperature>0.8 °C</strong>"),
				TemperatureService::parseTts));
	}
}

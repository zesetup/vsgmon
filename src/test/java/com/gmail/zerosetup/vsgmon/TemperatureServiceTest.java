package com.gmail.zerosetup.vsgmon;

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
}

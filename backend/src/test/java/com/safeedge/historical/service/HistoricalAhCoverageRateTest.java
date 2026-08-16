package com.safeedge.historical.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class HistoricalAhCoverageRateTest {

	@Test
	void zeroTotalMatchesYieldsZeroRate() {
		assertThat(HistoricalAhCoverageService.coverageRate(0, 0)).isEqualByComparingTo("0");
		assertThat(HistoricalAhCoverageService.coverageRate(5, 0)).isEqualByComparingTo("0");
		assertThat(HistoricalAhCoverageService.coverageRate(8, 10)).isEqualByComparingTo("0.8");
	}
}

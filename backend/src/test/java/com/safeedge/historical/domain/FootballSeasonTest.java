package com.safeedge.historical.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class FootballSeasonTest {

	@Test
	void requiresConsecutiveWinterCalendarYears() {
		FootballSeason season = new FootballSeason(2023, 2024);
		assertThat(season.displayValue()).isEqualTo("2023/24");
		assertThatThrownBy(() -> new FootballSeason(2023, 2025))
				.isInstanceOf(HistoricalDataException.class)
				.hasMessageContaining("startYear + 1");
	}

}

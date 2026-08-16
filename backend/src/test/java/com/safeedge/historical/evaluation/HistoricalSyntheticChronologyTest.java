package com.safeedge.historical.evaluation;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class HistoricalSyntheticChronologyTest {

	@Test
	void decisionIsStartOfMatchDateUtcAndSettlementIsNextDay() {
		LocalDate matchDate = LocalDate.of(2024, 1, 15);
		assertThat(HistoricalSyntheticChronology.decisionAt(matchDate))
				.isEqualTo(Instant.parse("2024-01-15T00:00:00Z"));
		assertThat(HistoricalSyntheticChronology.settlementAt(matchDate))
				.isEqualTo(Instant.parse("2024-01-16T00:00:00Z"));
		assertThat(HistoricalSyntheticChronology.decisionAt(matchDate))
				.isBefore(HistoricalSyntheticChronology.settlementAt(matchDate));
	}
}

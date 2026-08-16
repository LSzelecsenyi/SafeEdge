package com.safeedge.historical.diagnostics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.safeedge.historical.domain.CanonicalCompetition;
import org.junit.jupiter.api.Test;

class ProbabilityModelDevelopmentLeaguesTest {

	@Test
	void allowsDevelopmentLeagues() {
		ProbabilityModelDevelopmentLeagues.requireDevelopment(CanonicalCompetition.PREMIER_LEAGUE);
		ProbabilityModelDevelopmentLeagues.requireDevelopment(CanonicalCompetition.BUNDESLIGA);
		ProbabilityModelDevelopmentLeagues.requireDevelopment(CanonicalCompetition.SERIE_A);
	}

	@Test
	void refusesReservedValidationLeagues() {
		assertThatThrownBy(() -> ProbabilityModelDevelopmentLeagues.requireDevelopment(CanonicalCompetition.LA_LIGA))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("reserved");
		assertThatThrownBy(() -> ProbabilityModelDevelopmentLeagues.requireDevelopment(CanonicalCompetition.LIGUE_1))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("reserved");
	}

	@Test
	void developmentListDoesNotIncludeValidationLeagues() {
		assertThat(ProbabilityModelDevelopmentLeagues.DEVELOPMENT)
				.doesNotContain(CanonicalCompetition.LA_LIGA, CanonicalCompetition.LIGUE_1);
		assertThat(ProbabilityModelDevelopmentLeagues.RESERVED_VALIDATION)
				.containsExactlyInAnyOrder(CanonicalCompetition.LA_LIGA, CanonicalCompetition.LIGUE_1);
	}
}

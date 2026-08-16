package com.safeedge.probability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ProbabilityModelV3ConfigTest {

	@Test
	void defaultsAreDocumentedAssumptionsNotOptima() {
		ProbabilityModelV3Config config = ProbabilityModelV3Config.defaults();
		assertThat(config.decayHalfLifeDays()).isEqualTo(180);
		assertThat(config.maxGoalsPerTeam()).isEqualTo(10);
		assertThat(config.minimumTeamMatches()).isEqualTo(5);
		assertThat(config.minimumLeagueMatches()).isEqualTo(20);
		assertThat(config.attackRegularization()).isEqualTo(5.0d);
		assertThat(config.defenceRegularization()).isEqualTo(5.0d);
		assertThat(config.optimizerMaxIterations()).isEqualTo(80);
		assertThat(config.rhoScale()).isEqualTo(0.4d);
	}

	@Test
	void regularizationMustBeNonNegative() {
		assertThatThrownBy(() -> new ProbabilityModelV3Config(180, 10, 5, 20, -1.0d, 5.0d, 80, 1e-5d, 0.4d))
				.isInstanceOf(ProbabilityModelException.class);
	}
}

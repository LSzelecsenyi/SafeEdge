package com.safeedge.probability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ProbabilityModelConfigTest {

	@Test
	void defaultsAreTheDocumentedAssumptions() {
		ProbabilityModelConfig config = ProbabilityModelConfig.defaults();
		assertThat(config.decayHalfLifeDays()).isEqualTo(180);
		assertThat(config.maxGoalsPerTeam()).isEqualTo(10);
		assertThat(config.minimumTeamMatches()).isEqualTo(5);
	}

	@Test
	void rejectsNonPositiveParameters() {
		assertThatThrownBy(() -> new ProbabilityModelConfig(0, 10, 5))
				.isInstanceOf(ProbabilityModelException.class);
		assertThatThrownBy(() -> new ProbabilityModelConfig(180, 0, 5))
				.isInstanceOf(ProbabilityModelException.class);
		assertThatThrownBy(() -> new ProbabilityModelConfig(180, 10, 0))
				.isInstanceOf(ProbabilityModelException.class);
	}
}

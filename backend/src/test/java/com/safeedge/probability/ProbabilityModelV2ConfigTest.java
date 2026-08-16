package com.safeedge.probability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ProbabilityModelV2ConfigTest {

	@Test
	void defaultsAreDocumentedAssumptionsNotOptima() {
		ProbabilityModelV2Config config = ProbabilityModelV2Config.defaults();
		assertThat(config.decayHalfLifeDays()).isEqualTo(180);
		assertThat(config.maxGoalsPerTeam()).isEqualTo(10);
		assertThat(config.minimumTeamMatches()).isEqualTo(5);
		assertThat(config.attackDefenceShrinkageStrength()).isEqualByComparingTo("5");
		assertThat(config.dixonColesEnabled()).isTrue();
		assertThat(config.sharedPoissonConfig()).isEqualTo(ProbabilityModelConfig.defaults());
	}

	@Test
	void shrinkageMustBeNonNegative() {
		assertThatThrownBy(() -> new ProbabilityModelV2Config(180, 10, 5, new BigDecimal("-0.1"), true))
				.isInstanceOf(ProbabilityModelException.class);
	}
}

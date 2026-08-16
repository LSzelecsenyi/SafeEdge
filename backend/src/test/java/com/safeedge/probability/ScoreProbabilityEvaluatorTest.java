package com.safeedge.probability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.safeedge.candidate.ScoreProbability;
import com.safeedge.candidate.ScoreProbabilityDistribution;
import com.safeedge.settlement.MatchScore;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ScoreProbabilityEvaluatorTest {

	@Test
	void logLossIsNegativeLogOfActualCell() {
		ScoreProbabilityDistribution distribution = ScoreProbabilityDistribution.of(
				new ScoreProbability(new MatchScore(1, 0), new BigDecimal("0.40")),
				new ScoreProbability(new MatchScore(0, 1), new BigDecimal("0.60")));
		assertThat(ScoreProbabilityEvaluator.logLoss(distribution, new MatchScore(1, 0)))
				.hasValueSatisfying(value -> assertThat(value.doubleValue()).isCloseTo(-Math.log(0.4d), within(1e-12)));
		assertThat(ScoreProbabilityEvaluator.logLoss(distribution, new MatchScore(4, 4))).isEmpty();
	}
}

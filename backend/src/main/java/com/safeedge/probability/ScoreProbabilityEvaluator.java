package com.safeedge.probability;

import com.safeedge.candidate.ScoreProbability;
import com.safeedge.candidate.ScoreProbabilityDistribution;
import com.safeedge.settlement.MatchScore;
import java.math.BigDecimal;
import java.util.Optional;

/**
 * Single-prediction scoring. Missing truncated-grid outcomes are unavailable,
 * not treated as probability zero.
 */
public final class ScoreProbabilityEvaluator {

	private ScoreProbabilityEvaluator() {
	}

	public static Optional<BigDecimal> logLoss(ScoreProbabilityDistribution prediction, MatchScore actual) {
		if (prediction == null) {
			throw new ProbabilityModelException("prediction is required");
		}
		if (actual == null) {
			throw new ProbabilityModelException("actual score is required");
		}
		for (ScoreProbability entry : prediction.entries()) {
			if (entry.score().equals(actual)) {
				if (entry.probability().compareTo(BigDecimal.ZERO) <= 0) {
					return Optional.empty();
				}
				double p = entry.probability().doubleValue();
				return Optional.of(BigDecimal.valueOf(-Math.log(p)));
			}
		}
		return Optional.empty();
	}
}

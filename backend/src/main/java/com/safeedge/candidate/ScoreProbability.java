package com.safeedge.candidate;

import com.safeedge.settlement.MatchScore;
import java.math.BigDecimal;

public record ScoreProbability(MatchScore score, BigDecimal probability) {

	public ScoreProbability {
		if (score == null) {
			throw new CandidateException("score is required");
		}
		if (probability == null) {
			throw new CandidateException("probability is required");
		}
		if (probability.compareTo(BigDecimal.ZERO) < 0 || probability.compareTo(BigDecimal.ONE) > 0) {
			throw new CandidateException("probability must be >= 0 and <= 1");
		}
		probability = probability.stripTrailingZeros();
	}

}

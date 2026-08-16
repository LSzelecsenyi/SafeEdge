package com.safeedge.candidate;

import com.safeedge.settlement.MatchScore;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Provider-independent full-time scoreline probabilities. Values are not
 * silently normalized; they must already sum to 1. There is no maximum goal
 * count in this type.
 *
 * These probabilities are point-in-time model estimates. They must not be
 * derived from the event's final score or from {@code 1/odds}.
 */
public record ScoreProbabilityDistribution(List<ScoreProbability> entries) {

	public ScoreProbabilityDistribution {
		if (entries == null || entries.isEmpty()) {
			throw new CandidateException("score distribution must not be empty");
		}
		for (ScoreProbability entry : entries) {
			if (entry == null) {
				throw new CandidateException("score distribution must not contain null entries");
			}
		}
		entries = List.copyOf(entries);
		Set<MatchScore> seen = new HashSet<>();
		BigDecimal sum = BigDecimal.ZERO;
		for (ScoreProbability entry : entries) {
			if (!seen.add(entry.score())) {
				throw new CandidateException("Duplicate scoreline " + entry.score());
			}
			sum = sum.add(entry.probability());
		}
		if (sum.compareTo(BigDecimal.ONE) != 0) {
			throw new CandidateException("Score probabilities must sum to 1");
		}
	}

	public static ScoreProbabilityDistribution of(ScoreProbability... entries) {
		if (entries == null) {
			throw new CandidateException("score distribution must not be empty");
		}
		return new ScoreProbabilityDistribution(List.of(entries));
	}

}

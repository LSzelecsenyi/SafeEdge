package com.safeedge.probability;

import com.safeedge.candidate.ScoreProbability;
import com.safeedge.candidate.ScoreProbabilityDistribution;
import com.safeedge.settlement.MatchScore;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

final class ScoreGridNormalizer {

	private ScoreGridNormalizer() {
	}

	static ScoreProbabilityDistribution normalize(double[][] raw, double rawSum) {
		if (rawSum <= 0.0d || Double.isNaN(rawSum) || Double.isInfinite(rawSum)) {
			throw new ProbabilityModelException("raw probability mass must be positive and finite");
		}
		int homeMax = raw.length - 1;
		int awayMax = raw[0].length - 1;
		int lastIndex = (homeMax + 1) * (awayMax + 1);
		List<ScoreProbability> entries = new ArrayList<>(lastIndex);
		BigDecimal remaining = BigDecimal.ONE;
		int index = 0;
		for (int home = 0; home <= homeMax; home++) {
			for (int away = 0; away <= awayMax; away++) {
				index++;
				if (index == lastIndex) {
					entries.add(new ScoreProbability(new MatchScore(home, away), remaining));
				}
				else {
					BigDecimal probability = BigDecimal.valueOf(raw[home][away] / rawSum);
					remaining = remaining.subtract(probability);
					entries.add(new ScoreProbability(new MatchScore(home, away), probability));
				}
			}
		}
		return new ScoreProbabilityDistribution(entries);
	}
}

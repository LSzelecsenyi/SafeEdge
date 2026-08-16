package com.safeedge.probability;

/**
 * Exponential time decay. {@code double} is used only for {@code exp}/{@code ln};
 * domain outputs remain {@code BigDecimal}.
 *
 * {@code weight = exp(-ln(2) * ageDays / halfLifeDays)} so age = half-life → 0.5.
 */
final class TimeDecay {

	private TimeDecay() {
	}

	static double weight(long ageDays, int halfLifeDays) {
		if (ageDays < 0) {
			throw new ProbabilityModelException("ageDays must be >= 0");
		}
		if (ageDays == 0) {
			return 1.0d;
		}
		return Math.exp(-Math.log(2.0d) * ageDays / halfLifeDays);
	}
}

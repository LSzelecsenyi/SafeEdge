package com.safeedge.historical.diagnostics;

import java.math.BigDecimal;

/**
 * Rank-quality statistics between predicted edge and realized unit return.
 * Single-bet realized return is noisy; these are diagnostics, not proof.
 */
public record RankQualityStats(int n, BigDecimal spearman, BigDecimal pearson) {

	public RankQualityStats {
		if (n < 0) {
			throw new IllegalArgumentException("n must be >= 0");
		}
		spearman = spearman == null ? null : spearman.stripTrailingZeros();
		pearson = pearson == null ? null : pearson.stripTrailingZeros();
	}
}

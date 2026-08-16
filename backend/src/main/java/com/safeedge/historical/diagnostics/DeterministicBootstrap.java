package com.safeedge.historical.diagnostics;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Deterministic percentile bootstrap for a mean. Diagnostics only; not used by
 * StrategyEngine or production staking.
 *
 * Seed {@code 20260816}, {@code 2000} replicates, 95% interval from the 2.5th
 * and 97.5th percentiles of bootstrap means (nearest-rank, same as
 * {@link DiagnosticMath#quantile}).
 */
final class DeterministicBootstrap {

	static final long SEED = 20260816L;
	static final int REPLICATES = 2000;

	private DeterministicBootstrap() {
	}

	static MeanConfidenceInterval meanInterval(List<BigDecimal> values) {
		if (values == null || values.isEmpty()) {
			return new MeanConfidenceInterval(0, REPLICATES, SEED, null, null, null);
		}
		BigDecimal mean = DiagnosticMath.average(values);
		if (values.size() == 1) {
			return new MeanConfidenceInterval(1, REPLICATES, SEED, mean, mean, mean);
		}
		Random random = new Random(SEED);
		int n = values.size();
		List<BigDecimal> means = new ArrayList<>(REPLICATES);
		for (int r = 0; r < REPLICATES; r++) {
			BigDecimal sum = BigDecimal.ZERO;
			for (int i = 0; i < n; i++) {
				sum = sum.add(values.get(random.nextInt(n)), DiagnosticMath.MATH);
			}
			means.add(sum.divide(BigDecimal.valueOf(n), DiagnosticMath.MATH));
		}
		means.sort(null);
		return new MeanConfidenceInterval(
				n,
				REPLICATES,
				SEED,
				mean,
				DiagnosticMath.quantile(means, new BigDecimal("0.025")),
				DiagnosticMath.quantile(means, new BigDecimal("0.975")));
	}
}

package com.safeedge.historical.diagnostics;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class DiagnosticMath {

	static final MathContext MATH = MathContext.DECIMAL128;
	static final BigDecimal UNIT_STAKE = BigDecimal.ONE;

	private DiagnosticMath() {
	}

	static BigDecimal average(List<BigDecimal> values) {
		if (values == null || values.isEmpty()) {
			return null;
		}
		BigDecimal sum = BigDecimal.ZERO;
		for (BigDecimal value : values) {
			sum = sum.add(value, MATH);
		}
		return sum.divide(BigDecimal.valueOf(values.size()), MATH);
	}

	static BigDecimal median(List<BigDecimal> values) {
		if (values == null || values.isEmpty()) {
			return null;
		}
		List<BigDecimal> sorted = new ArrayList<>(values);
		sorted.sort(Comparator.naturalOrder());
		return quantile(sorted, new BigDecimal("0.50"));
	}

	static BigDecimal divide(BigDecimal numerator, int denominator) {
		if (denominator == 0) {
			return null;
		}
		return numerator.divide(BigDecimal.valueOf(denominator), MATH);
	}

	/**
	 * Nearest-rank quantile on an already-sorted list.
	 * {@code index = round_half_up(p * (n - 1))}.
	 */
	static BigDecimal quantile(List<BigDecimal> sortedAscending, BigDecimal percentile) {
		if (sortedAscending == null || sortedAscending.isEmpty()) {
			return null;
		}
		if (percentile.compareTo(BigDecimal.ZERO) < 0 || percentile.compareTo(BigDecimal.ONE) > 0) {
			throw new IllegalArgumentException("percentile must be in [0, 1]");
		}
		if (sortedAscending.size() == 1) {
			return sortedAscending.getFirst();
		}
		int index = percentile
				.multiply(BigDecimal.valueOf(sortedAscending.size() - 1), MATH)
				.setScale(0, RoundingMode.HALF_UP)
				.intValueExact();
		return sortedAscending.get(index);
	}

	static EdgeQuantiles quantiles(List<BigDecimal> values) {
		List<BigDecimal> sorted = new ArrayList<>(values == null ? List.of() : values);
		sorted.sort(Comparator.naturalOrder());
		return new EdgeQuantiles(
				quantile(sorted, BigDecimal.ZERO),
				quantile(sorted, new BigDecimal("0.10")),
				quantile(sorted, new BigDecimal("0.25")),
				quantile(sorted, new BigDecimal("0.50")),
				quantile(sorted, new BigDecimal("0.75")),
				quantile(sorted, new BigDecimal("0.90")),
				quantile(sorted, new BigDecimal("0.95")),
				quantile(sorted, new BigDecimal("0.99")),
				quantile(sorted, BigDecimal.ONE));
	}
}

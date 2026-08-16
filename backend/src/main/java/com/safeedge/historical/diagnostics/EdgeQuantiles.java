package com.safeedge.historical.diagnostics;

import java.math.BigDecimal;

/**
 * Deterministic nearest-rank quantiles on a sorted edge list.
 * {@code index = round_half_up(p * (n - 1))} for {@code p} in {@code [0, 1]}.
 * Null fields when the source list is empty.
 */
public record EdgeQuantiles(
		BigDecimal min,
		BigDecimal p10,
		BigDecimal p25,
		BigDecimal median,
		BigDecimal p75,
		BigDecimal p90,
		BigDecimal p95,
		BigDecimal p99,
		BigDecimal max) {

	public EdgeQuantiles {
		min = strip(min);
		p10 = strip(p10);
		p25 = strip(p25);
		median = strip(median);
		p75 = strip(p75);
		p90 = strip(p90);
		p95 = strip(p95);
		p99 = strip(p99);
		max = strip(max);
	}

	private static BigDecimal strip(BigDecimal value) {
		return value == null ? null : value.stripTrailingZeros();
	}
}

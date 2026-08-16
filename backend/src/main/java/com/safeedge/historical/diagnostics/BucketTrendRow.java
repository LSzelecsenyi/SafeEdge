package com.safeedge.historical.diagnostics;

import java.math.BigDecimal;

public record BucketTrendRow(String key, int n, BigDecimal averageEdge, BigDecimal unitStakeRoi) {

	public BucketTrendRow {
		if (key == null || key.isBlank()) {
			throw new IllegalArgumentException("key is required");
		}
		if (n < 0) {
			throw new IllegalArgumentException("n must be >= 0");
		}
		averageEdge = averageEdge == null ? null : averageEdge.stripTrailingZeros();
		unitStakeRoi = unitStakeRoi == null ? null : unitStakeRoi.stripTrailingZeros();
	}
}

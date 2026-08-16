package com.safeedge.historical.diagnostics;

import java.math.BigDecimal;

public record ConcentrationShare(String key, int count, BigDecimal shareOfPositiveEdge) {

	public ConcentrationShare {
		if (key == null || key.isBlank()) {
			throw new IllegalArgumentException("key is required");
		}
		if (count < 0) {
			throw new IllegalArgumentException("count must be >= 0");
		}
		shareOfPositiveEdge = shareOfPositiveEdge == null ? null : shareOfPositiveEdge.stripTrailingZeros();
	}
}

package com.safeedge.historical.diagnostics;

import java.math.BigDecimal;

public record SideSnapshot(String side, int n, BigDecimal averageEdge, BigDecimal unitStakeRoi) {

	public SideSnapshot {
		if (side == null || side.isBlank()) {
			throw new IllegalArgumentException("side is required");
		}
		if (n < 0) {
			throw new IllegalArgumentException("n must be >= 0");
		}
		averageEdge = averageEdge == null ? null : averageEdge.stripTrailingZeros();
		unitStakeRoi = unitStakeRoi == null ? null : unitStakeRoi.stripTrailingZeros();
	}
}

package com.safeedge.historical.diagnostics;

import java.math.BigDecimal;

public record AhFamilySnapshot(
		DiagnosticLineFamily family, int n, BigDecimal averageEdge, BigDecimal unitStakeRoi) {

	public AhFamilySnapshot {
		if (family == null) {
			throw new IllegalArgumentException("family is required");
		}
		if (n < 0) {
			throw new IllegalArgumentException("n must be >= 0");
		}
		averageEdge = averageEdge == null ? null : averageEdge.stripTrailingZeros();
		unitStakeRoi = unitStakeRoi == null ? null : unitStakeRoi.stripTrailingZeros();
	}
}

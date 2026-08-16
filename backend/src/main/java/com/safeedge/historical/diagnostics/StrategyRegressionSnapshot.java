package com.safeedge.historical.diagnostics;

import java.math.BigDecimal;

public record StrategyRegressionSnapshot(String name, int betsAccepted, BigDecimal roi, boolean pausedByDrawdown) {

	public StrategyRegressionSnapshot {
		if (name == null || name.isBlank()) {
			throw new IllegalArgumentException("name is required");
		}
		roi = roi == null ? null : roi.stripTrailingZeros();
	}
}

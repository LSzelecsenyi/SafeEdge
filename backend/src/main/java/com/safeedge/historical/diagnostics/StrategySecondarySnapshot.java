package com.safeedge.historical.diagnostics;

import java.math.BigDecimal;

public record StrategySecondarySnapshot(String name, int betsAccepted, BigDecimal roi) {

	public StrategySecondarySnapshot {
		if (name == null || name.isBlank()) {
			throw new IllegalArgumentException("name is required");
		}
		if (betsAccepted < 0) {
			throw new IllegalArgumentException("betsAccepted must be >= 0");
		}
		roi = roi == null ? null : roi.stripTrailingZeros();
	}
}

package com.safeedge.historical.diagnostics;

import java.math.BigDecimal;

public enum DiagnosticLineFamily {
	NEGATIVE_HANDICAP,
	ZERO,
	POSITIVE_HANDICAP;

	public static DiagnosticLineFamily of(BigDecimal selectedLine) {
		if (selectedLine == null) {
			throw new IllegalArgumentException("selectedLine is required");
		}
		int sign = selectedLine.compareTo(BigDecimal.ZERO);
		if (sign < 0) {
			return NEGATIVE_HANDICAP;
		}
		if (sign > 0) {
			return POSITIVE_HANDICAP;
		}
		return ZERO;
	}
}

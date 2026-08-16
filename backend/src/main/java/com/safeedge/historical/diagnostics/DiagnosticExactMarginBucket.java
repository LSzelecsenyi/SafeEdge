package com.safeedge.historical.diagnostics;

public enum DiagnosticExactMarginBucket {
	LTE_MINUS_3("<= -3"),
	MINUS_2("-2"),
	MINUS_1("-1"),
	ZERO("0"),
	PLUS_1("+1"),
	PLUS_2("+2"),
	GTE_PLUS_3(">= +3");

	private final String label;

	DiagnosticExactMarginBucket(String label) {
		this.label = label;
	}

	public String label() {
		return label;
	}

	public static DiagnosticExactMarginBucket ofHomeMargin(int homeMinusAway) {
		if (homeMinusAway <= -3) {
			return LTE_MINUS_3;
		}
		if (homeMinusAway == -2) {
			return MINUS_2;
		}
		if (homeMinusAway == -1) {
			return MINUS_1;
		}
		if (homeMinusAway == 0) {
			return ZERO;
		}
		if (homeMinusAway == 1) {
			return PLUS_1;
		}
		if (homeMinusAway == 2) {
			return PLUS_2;
		}
		return GTE_PLUS_3;
	}
}

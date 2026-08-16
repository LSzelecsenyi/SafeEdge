package com.safeedge.historical.diagnostics;

import java.math.BigDecimal;

public record AhLineDiagnostics(BigDecimal selectedLine, UnitStakeSummary summary) {

	public AhLineDiagnostics {
		if (selectedLine == null) {
			throw new IllegalArgumentException("selectedLine is required");
		}
		if (summary == null) {
			throw new IllegalArgumentException("summary is required");
		}
		selectedLine = selectedLine.stripTrailingZeros();
	}
}

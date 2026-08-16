package com.safeedge.historical.diagnostics;

public record EdgeSignDiagnostics(UnitStakeSummary negativeEdge, UnitStakeSummary positiveEdge) {

	public EdgeSignDiagnostics {
		if (negativeEdge == null) {
			throw new IllegalArgumentException("negativeEdge is required");
		}
		if (positiveEdge == null) {
			throw new IllegalArgumentException("positiveEdge is required");
		}
	}
}

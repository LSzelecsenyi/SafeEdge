package com.safeedge.historical.diagnostics;

import com.safeedge.event.domain.SelectionType;

public record SideDiagnostics(SelectionType side, UnitStakeSummary summary) {

	public SideDiagnostics {
		if (side == null) {
			throw new IllegalArgumentException("side is required");
		}
		if (summary == null) {
			throw new IllegalArgumentException("summary is required");
		}
	}
}

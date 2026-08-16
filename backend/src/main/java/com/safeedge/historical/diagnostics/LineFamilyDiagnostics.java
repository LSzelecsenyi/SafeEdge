package com.safeedge.historical.diagnostics;

public record LineFamilyDiagnostics(DiagnosticLineFamily family, UnitStakeSummary summary) {

	public LineFamilyDiagnostics {
		if (family == null) {
			throw new IllegalArgumentException("family is required");
		}
		if (summary == null) {
			throw new IllegalArgumentException("summary is required");
		}
	}
}

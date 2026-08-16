package com.safeedge.historical.diagnostics;

public record CandidateSubsetDiagnostics(String label, UnitStakeSummary summary) {

	public CandidateSubsetDiagnostics {
		if (label == null || label.isBlank()) {
			throw new IllegalArgumentException("label is required");
		}
		if (summary == null) {
			throw new IllegalArgumentException("summary is required");
		}
	}
}

package com.safeedge.historical.diagnostics;

public record OddsBucketDiagnostics(DiagnosticOddsBucket bucket, UnitStakeSummary summary) {

	public OddsBucketDiagnostics {
		if (bucket == null) {
			throw new IllegalArgumentException("bucket is required");
		}
		if (summary == null) {
			throw new IllegalArgumentException("summary is required");
		}
	}
}

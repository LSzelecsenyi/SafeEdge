package com.safeedge.historical.diagnostics;

public record EdgeBucketDiagnostics(DiagnosticEdgeBucket bucket, UnitStakeSummary summary) {

	public EdgeBucketDiagnostics {
		if (bucket == null) {
			throw new IllegalArgumentException("bucket is required");
		}
		if (summary == null) {
			throw new IllegalArgumentException("summary is required");
		}
	}
}

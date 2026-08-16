package com.safeedge.historical.diagnostics;

public record DisagreementGroup(DisagreementMagnitudeBucket bucket, EdgeQualityGroupSummary summary) {

	public DisagreementGroup {
		if (bucket == null) {
			throw new IllegalArgumentException("bucket is required");
		}
		if (summary == null) {
			throw new IllegalArgumentException("summary is required");
		}
	}
}

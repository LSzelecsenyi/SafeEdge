package com.safeedge.historical.diagnostics;

public record NamedMeanInterval(String label, MeanConfidenceInterval interval) {

	public NamedMeanInterval {
		if (label == null || label.isBlank()) {
			throw new IllegalArgumentException("label is required");
		}
		if (interval == null) {
			throw new IllegalArgumentException("interval is required");
		}
	}
}

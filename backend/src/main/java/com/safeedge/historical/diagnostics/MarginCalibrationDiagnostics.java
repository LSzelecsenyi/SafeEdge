package com.safeedge.historical.diagnostics;

import java.util.List;

public record MarginCalibrationDiagnostics(
		int predictionCount,
		List<MarginCategoryCalibration> categories,
		List<ExactMarginCalibration> exactMargins) {

	public MarginCalibrationDiagnostics {
		if (predictionCount < 0) {
			throw new IllegalArgumentException("predictionCount must be >= 0");
		}
		categories = List.copyOf(categories == null ? List.of() : categories);
		exactMargins = List.copyOf(exactMargins == null ? List.of() : exactMargins);
	}
}

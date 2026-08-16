package com.safeedge.historical.diagnostics;

import java.math.BigDecimal;
import java.util.List;

public record StrategyAcceptedBetDiagnostics(
		String strategyName,
		int acceptedCount,
		BigDecimal averageEdge,
		BigDecimal unitStakeRoi,
		List<SideDiagnostics> bySide,
		List<AhLineDiagnostics> byAhLine,
		List<OddsBucketDiagnostics> byOddsBucket,
		List<EdgeBucketDiagnostics> byEdgeBucket,
		DrawdownPauseDiagnostics pause) {

	public StrategyAcceptedBetDiagnostics {
		if (strategyName == null || strategyName.isBlank()) {
			throw new IllegalArgumentException("strategyName is required");
		}
		if (acceptedCount < 0) {
			throw new IllegalArgumentException("acceptedCount must be >= 0");
		}
		averageEdge = averageEdge == null ? null : averageEdge.stripTrailingZeros();
		unitStakeRoi = unitStakeRoi == null ? null : unitStakeRoi.stripTrailingZeros();
		bySide = List.copyOf(bySide == null ? List.of() : bySide);
		byAhLine = List.copyOf(byAhLine == null ? List.of() : byAhLine);
		byOddsBucket = List.copyOf(byOddsBucket == null ? List.of() : byOddsBucket);
		byEdgeBucket = List.copyOf(byEdgeBucket == null ? List.of() : byEdgeBucket);
	}
}

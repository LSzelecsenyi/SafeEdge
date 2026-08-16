package com.safeedge.historical.diagnostics;

import java.math.BigDecimal;

/**
 * Unit-stake group summary. {@code predictedExpectedReturn} is the average
 * CandidateEngine edge. {@code lowSample} is {@code n < 30}.
 */
public record EdgeQualityGroupSummary(
		String key,
		int n,
		boolean lowSample,
		BigDecimal averageEdge,
		BigDecimal medianEdge,
		BigDecimal averageOdds,
		BigDecimal unitStakeRoi,
		BigDecimal predictedExpectedProfit,
		BigDecimal realizedProfit,
		BigDecimal calibrationGap,
		SettlementCounts settlements,
		SettlementCalibration settlementCalibration) {

	public static final int LOW_SAMPLE_THRESHOLD = 30;

	public EdgeQualityGroupSummary {
		if (key == null || key.isBlank()) {
			throw new IllegalArgumentException("key is required");
		}
		if (n < 0) {
			throw new IllegalArgumentException("n must be >= 0");
		}
		if (settlements == null) {
			throw new IllegalArgumentException("settlements are required");
		}
		if (settlementCalibration == null) {
			throw new IllegalArgumentException("settlementCalibration is required");
		}
		averageEdge = strip(averageEdge);
		medianEdge = strip(medianEdge);
		averageOdds = strip(averageOdds);
		unitStakeRoi = strip(unitStakeRoi);
		predictedExpectedProfit = strip(predictedExpectedProfit);
		realizedProfit = strip(realizedProfit);
		calibrationGap = strip(calibrationGap);
	}

	private static BigDecimal strip(BigDecimal value) {
		return value == null ? null : value.stripTrailingZeros();
	}
}

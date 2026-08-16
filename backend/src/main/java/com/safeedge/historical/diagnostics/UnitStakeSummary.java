package com.safeedge.historical.diagnostics;

import java.math.BigDecimal;

/**
 * Flat unit-stake diagnostic for one candidate group. Stake is exactly 1 per
 * candidate. Not a StrategyEngine result: no bankroll, drawdown, Vault, or Kelly.
 *
 * {@code unitStakeRoi} is {@code unitStakeProfit / candidateCount}. Null when
 * {@code candidateCount == 0}.
 */
public record UnitStakeSummary(
		int candidateCount,
		int positiveEdgeCount,
		BigDecimal averagePredictedEdge,
		BigDecimal averageOdds,
		BigDecimal averageRealizedReturnRate,
		BigDecimal calibrationGap,
		BigDecimal unitStakeProfit,
		BigDecimal unitStakeRoi,
		SettlementCounts settlements) {

	public UnitStakeSummary {
		if (candidateCount < 0 || positiveEdgeCount < 0) {
			throw new IllegalArgumentException("counts must be >= 0");
		}
		if (settlements == null) {
			throw new IllegalArgumentException("settlements are required");
		}
		if (unitStakeProfit == null) {
			throw new IllegalArgumentException("unitStakeProfit is required");
		}
		unitStakeProfit = unitStakeProfit.stripTrailingZeros();
		averagePredictedEdge = strip(averagePredictedEdge);
		averageOdds = strip(averageOdds);
		averageRealizedReturnRate = strip(averageRealizedReturnRate);
		calibrationGap = strip(calibrationGap);
		unitStakeRoi = strip(unitStakeRoi);
	}

	private static BigDecimal strip(BigDecimal value) {
		return value == null ? null : value.stripTrailingZeros();
	}
}

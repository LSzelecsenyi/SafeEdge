package com.safeedge.historical.diagnostics;

/**
 * Qualitative pattern flags. Cutoffs are diagnostic, not production filters.
 */
public record StructuralPatternFlags(
		boolean aggregateGoalsAndMatchResultCalibrated,
		boolean aggregateEdgeNearRealizedReturn,
		boolean edgeRankingWeak,
		boolean highEdgeWinOverconfidentAndLossUnderconfident,
		boolean higherEdgeDoesNotMonotonicallyImproveRoi,
		boolean failureStableAcrossSeasons) {

	public int trueCount() {
		int count = 0;
		if (aggregateGoalsAndMatchResultCalibrated) {
			count++;
		}
		if (aggregateEdgeNearRealizedReturn) {
			count++;
		}
		if (edgeRankingWeak) {
			count++;
		}
		if (highEdgeWinOverconfidentAndLossUnderconfident) {
			count++;
		}
		if (higherEdgeDoesNotMonotonicallyImproveRoi) {
			count++;
		}
		if (failureStableAcrossSeasons) {
			count++;
		}
		return count;
	}

	public boolean coreEdgeFailure() {
		return edgeRankingWeak && highEdgeWinOverconfidentAndLossUnderconfident;
	}
}

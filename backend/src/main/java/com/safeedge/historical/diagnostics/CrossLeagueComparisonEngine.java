package com.safeedge.historical.diagnostics;

import com.safeedge.historical.domain.CanonicalCompetition;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Diagnostic pattern comparison. Does not retune Poisson or select a league to
 * bet. Cutoffs are documented diagnostic thresholds, not production filters.
 */
public final class CrossLeagueComparisonEngine {

	static final BigDecimal MAX_ABS_1X2_GAP = new BigDecimal("0.03");
	static final BigDecimal MAX_ABS_GOAL_GAP = new BigDecimal("0.15");
	static final BigDecimal MAX_ABS_EDGE_RETURN_GAP = new BigDecimal("0.01");
	static final BigDecimal WEAK_SPEARMAN_ABS = new BigDecimal("0.10");
	static final BigDecimal TEN_PERCENT = new BigDecimal("0.10");
	static final BigDecimal TWENTY_PERCENT = new BigDecimal("0.20");
	static final int MONOTONE_MIN_N = 30;

	private CrossLeagueComparisonEngine() {
	}

	public static CrossLeagueComparison compare(
			LeagueDiagnosticSnapshot premierLeague, LeagueDiagnosticSnapshot bundesliga) {
		if (premierLeague == null || bundesliga == null) {
			throw new IllegalArgumentException("both league snapshots are required");
		}
		if (premierLeague.competition() != CanonicalCompetition.PREMIER_LEAGUE) {
			throw new IllegalArgumentException(
					"first snapshot must be PREMIER_LEAGUE, got " + premierLeague.competition());
		}
		if (bundesliga.competition() != CanonicalCompetition.BUNDESLIGA) {
			throw new IllegalArgumentException("second snapshot must be BUNDESLIGA, got " + bundesliga.competition());
		}
		if (premierLeague.quoteSource() != bundesliga.quoteSource()) {
			throw new IllegalArgumentException("quote sources must match for a like-for-like comparison");
		}
		StructuralPatternFlags plFlags = flags(premierLeague);
		StructuralPatternFlags blFlags = flags(bundesliga);
		return new CrossLeagueComparison(premierLeague, bundesliga, plFlags, blFlags, classify(plFlags, blFlags));
	}

	static StructuralPatternFlags flags(LeagueDiagnosticSnapshot league) {
		return new StructuralPatternFlags(
				goalsAndMatchResultCalibrated(league.predictionQuality()),
				aggregateEdgeNearRealized(league.calibrationGap()),
				edgeRankingWeak(league.spearman()),
				highEdgeOverconfident(league.highEdgeSlices()),
				higherEdgeNotMonotone(league.edgeBuckets()),
				failureStableAcrossSeasons(league.seasons()));
	}

	public static ThreeLeagueComparison compareThree(
			LeagueDiagnosticSnapshot premierLeague,
			LeagueDiagnosticSnapshot bundesliga,
			LeagueDiagnosticSnapshot serieA) {
		if (premierLeague == null || bundesliga == null || serieA == null) {
			throw new IllegalArgumentException("all three league snapshots are required");
		}
		if (premierLeague.competition() != CanonicalCompetition.PREMIER_LEAGUE) {
			throw new IllegalArgumentException(
					"first snapshot must be PREMIER_LEAGUE, got " + premierLeague.competition());
		}
		if (bundesliga.competition() != CanonicalCompetition.BUNDESLIGA) {
			throw new IllegalArgumentException("second snapshot must be BUNDESLIGA, got " + bundesliga.competition());
		}
		if (serieA.competition() != CanonicalCompetition.SERIE_A) {
			throw new IllegalArgumentException("third snapshot must be SERIE_A, got " + serieA.competition());
		}
		if (premierLeague.quoteSource() != bundesliga.quoteSource()
				|| premierLeague.quoteSource() != serieA.quoteSource()) {
			throw new IllegalArgumentException("quote sources must match for a like-for-like comparison");
		}
		StructuralPatternFlags plFlags = flags(premierLeague);
		StructuralPatternFlags blFlags = flags(bundesliga);
		StructuralPatternFlags saFlags = flags(serieA);
		return new ThreeLeagueComparison(
				premierLeague, bundesliga, serieA, plFlags, blFlags, saFlags, classifyThree(plFlags, blFlags, saFlags));
	}

	static StructuralReplicationClassification classifyThree(
			StructuralPatternFlags premierLeague,
			StructuralPatternFlags bundesliga,
			StructuralPatternFlags serieA) {
		boolean priorStrong = premierLeague.coreEdgeFailure()
				&& bundesliga.coreEdgeFailure()
				&& premierLeague.higherEdgeDoesNotMonotonicallyImproveRoi()
				&& bundesliga.higherEdgeDoesNotMonotonicallyImproveRoi();
		boolean thirdCore = serieA.coreEdgeFailure() && serieA.higherEdgeDoesNotMonotonicallyImproveRoi();
		if (priorStrong && thirdCore && serieA.failureStableAcrossSeasons()) {
			return StructuralReplicationClassification.FAILURE_STRONGLY_REPLICATES_AGAIN;
		}
		if (serieA.coreEdgeFailure()
				|| serieA.edgeRankingWeak()
				|| serieA.highEdgeWinOverconfidentAndLossUnderconfident()) {
			return StructuralReplicationClassification.FAILURE_PARTIALLY_REPLICATES;
		}
		return StructuralReplicationClassification.FAILURE_DOES_NOT_REPLICATE;
	}

	static StructuralReplicationClassification classify(
			StructuralPatternFlags premierLeague, StructuralPatternFlags bundesliga) {
		boolean bothCore = premierLeague.coreEdgeFailure() && bundesliga.coreEdgeFailure();
		boolean bothMonotoneFailure =
				premierLeague.higherEdgeDoesNotMonotonicallyImproveRoi()
						&& bundesliga.higherEdgeDoesNotMonotonicallyImproveRoi();
		if (bothCore && bothMonotoneFailure) {
			return StructuralReplicationClassification.FAILURE_STRONGLY_REPLICATES;
		}
		if (bundesliga.coreEdgeFailure()
				|| bundesliga.edgeRankingWeak()
				|| bundesliga.highEdgeWinOverconfidentAndLossUnderconfident()) {
			return StructuralReplicationClassification.FAILURE_PARTIALLY_REPLICATES;
		}
		return StructuralReplicationClassification.FAILURE_DOES_NOT_REPLICATE;
	}

	private static boolean goalsAndMatchResultCalibrated(PredictionQualitySnapshot quality) {
		return absAtMost(quality.predictedHomeGoals(), quality.actualHomeGoals(), MAX_ABS_GOAL_GAP)
				&& absAtMost(quality.predictedAwayGoals(), quality.actualAwayGoals(), MAX_ABS_GOAL_GAP)
				&& absAtMost(quality.predictedHomeWin(), quality.actualHomeWin(), MAX_ABS_1X2_GAP)
				&& absAtMost(quality.predictedDraw(), quality.actualDraw(), MAX_ABS_1X2_GAP)
				&& absAtMost(quality.predictedAwayWin(), quality.actualAwayWin(), MAX_ABS_1X2_GAP);
	}

	private static boolean aggregateEdgeNearRealized(BigDecimal gap) {
		if (gap == null) {
			return false;
		}
		return gap.abs().compareTo(MAX_ABS_EDGE_RETURN_GAP) <= 0;
	}

	private static boolean edgeRankingWeak(BigDecimal spearman) {
		if (spearman == null) {
			return false;
		}
		return spearman.abs().compareTo(WEAK_SPEARMAN_ABS) < 0;
	}

	private static boolean highEdgeOverconfident(List<HighEdgeCalibrationSlice> slices) {
		HighEdgeCalibrationSlice ten = sliceAt(slices, TEN_PERCENT);
		HighEdgeCalibrationSlice twenty = sliceAt(slices, TWENTY_PERCENT);
		if (ten == null || twenty == null || ten.n() == 0 || twenty.n() == 0) {
			return false;
		}
		return ten.winOverconfident()
				&& ten.lossUnderconfident()
				&& twenty.winOverconfident()
				&& twenty.lossUnderconfident();
	}

	private static boolean higherEdgeNotMonotone(List<BucketTrendRow> buckets) {
		List<BucketTrendRow> populated = new ArrayList<>();
		for (BucketTrendRow row : buckets) {
			if (row.n() >= MONOTONE_MIN_N && row.averageEdge() != null && row.unitStakeRoi() != null) {
				populated.add(row);
			}
		}
		for (int i = 0; i < populated.size(); i++) {
			for (int j = i + 1; j < populated.size(); j++) {
				if (populated.get(i).averageEdge().compareTo(populated.get(j).averageEdge()) < 0
						&& populated.get(i).unitStakeRoi().compareTo(populated.get(j).unitStakeRoi()) > 0) {
					return true;
				}
			}
		}
		return false;
	}

	private static boolean failureStableAcrossSeasons(List<SeasonStabilityRow> seasons) {
		int populated = 0;
		int negativeAll = 0;
		int highEdgePopulated = 0;
		int negativeHighEdge = 0;
		for (SeasonStabilityRow row : seasons) {
			if (row.candidateCount() >= 100 && row.unitStakeRoi() != null) {
				populated++;
				if (row.unitStakeRoi().compareTo(BigDecimal.ZERO) < 0) {
					negativeAll++;
				}
			}
			if (row.edgeAtLeast10Count() >= 30 && row.edgeAtLeast10Roi() != null) {
				highEdgePopulated++;
				if (row.edgeAtLeast10Roi().compareTo(BigDecimal.ZERO) < 0) {
					negativeHighEdge++;
				}
			}
		}
		boolean allNegativeMajority = populated >= 3 && negativeAll >= 3;
		boolean highEdgeRepeated = highEdgePopulated >= 3 && negativeHighEdge >= 2;
		return allNegativeMajority && highEdgeRepeated;
	}

	private static HighEdgeCalibrationSlice sliceAt(List<HighEdgeCalibrationSlice> slices, BigDecimal threshold) {
		for (HighEdgeCalibrationSlice slice : slices) {
			if (slice.threshold().compareTo(threshold) == 0) {
				return slice;
			}
		}
		return null;
	}

	private static boolean absAtMost(BigDecimal left, BigDecimal right, BigDecimal max) {
		if (left == null || right == null) {
			return false;
		}
		return left.subtract(right).abs().compareTo(max) <= 0;
	}
}

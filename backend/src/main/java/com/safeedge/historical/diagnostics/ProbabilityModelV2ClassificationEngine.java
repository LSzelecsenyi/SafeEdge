package com.safeedge.historical.diagnostics;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Predeclared v2 vs v1 classification. Thresholds were chosen before historical
 * evaluation and are not ROI-fitted.
 *
 * CLEAR_STRUCTURAL_IMPROVEMENT requires all three development leagues to improve
 * Spearman by at least {@link #SPEARMAN_MATERIAL_DELTA}, shrink high-edge ≥10%
 * |P(WIN)−actual| and |P(LOSS)−actual| by at least {@link #HIGH_EDGE_GAP_SHRINK},
 * not worsen score log loss by more than {@link #LOG_LOSS_MATERIAL_WORSE}, and
 * reduce p99 predicted edge or p99 P(WIN) in at least two leagues.
 *
 * Positive ROI is ignored.
 */
public final class ProbabilityModelV2ClassificationEngine {

	static final BigDecimal SPEARMAN_MATERIAL_DELTA = new BigDecimal("0.05");
	static final BigDecimal SPEARMAN_SMALL_DELTA = new BigDecimal("0.02");
	static final BigDecimal HIGH_EDGE_GAP_SHRINK = new BigDecimal("0.03");
	static final BigDecimal LOG_LOSS_MATERIAL_WORSE = new BigDecimal("0.02");

	private ProbabilityModelV2ClassificationEngine() {
	}

	public static ClassificationDecision classify(List<ProbabilityModelComparison> leagues) {
		if (leagues == null || leagues.size() != ProbabilityModelDevelopmentLeagues.DEVELOPMENT.size()) {
			throw new IllegalArgumentException("classification requires the three development leagues");
		}
		List<String> reasons = new ArrayList<>();
		int spearmanMaterial = 0;
		int spearmanSmall = 0;
		int spearmanWorse = 0;
		int winGapShrink = 0;
		int winGapWorse = 0;
		int lossGapShrink = 0;
		int logLossWorse = 0;
		int confidenceCompressed = 0;
		int decileImproved = 0;
		for (ProbabilityModelComparison league : leagues) {
			String name = league.competition().name();
			BigDecimal spearmanDelta = league.spearmanDelta();
			if (atLeast(spearmanDelta, SPEARMAN_MATERIAL_DELTA)) {
				spearmanMaterial++;
				reasons.add(name + " Spearman improved by " + spearmanDelta.toPlainString());
			}
			else if (atLeast(spearmanDelta, SPEARMAN_SMALL_DELTA)) {
				spearmanSmall++;
				reasons.add(name + " Spearman improved slightly by " + spearmanDelta.toPlainString());
			}
			else if (atMost(spearmanDelta, SPEARMAN_SMALL_DELTA.negate())) {
				spearmanWorse++;
				reasons.add(name + " Spearman worsened by " + spearmanDelta.toPlainString());
			}
			else {
				reasons.add(name + " Spearman delta " + nullSafe(spearmanDelta) + " is below the small-improvement cutoff");
			}
			if (gapShrunk(league.v1().highEdge10().absWinGap(), league.v2().highEdge10().absWinGap())) {
				winGapShrink++;
			}
			else if (gapWorsened(league.v1().highEdge10().absWinGap(), league.v2().highEdge10().absWinGap())) {
				winGapWorse++;
			}
			if (gapShrunk(league.v1().highEdge10().absLossGap(), league.v2().highEdge10().absLossGap())) {
				lossGapShrink++;
			}
			if (worsenedBy(league.logLossDelta(), LOG_LOSS_MATERIAL_WORSE)) {
				logLossWorse++;
				reasons.add(name + " score log loss worsened by " + nullSafe(league.logLossDelta()));
			}
			if (compressed(league.v1().predictedEdge(), league.v2().predictedEdge())
					|| compressed(league.v1().predictedWin(), league.v2().predictedWin())) {
				confidenceCompressed++;
			}
			if (league.v2().decileRoiInversions() < league.v1().decileRoiInversions()) {
				decileImproved++;
			}
		}
		boolean clear = spearmanMaterial == 3
				&& winGapShrink == 3
				&& lossGapShrink == 3
				&& logLossWorse == 0
				&& confidenceCompressed >= 2;
		if (clear) {
			reasons.add("All three leagues met the predeclared CLEAR STRUCTURAL IMPROVEMENT gates.");
			return new ClassificationDecision(ProbabilityModelV2Classification.MODEL_V2_CLEAR_STRUCTURAL_IMPROVEMENT, reasons);
		}
		boolean worse = (spearmanWorse >= 2 || winGapWorse >= 2) && spearmanMaterial == 0 && winGapShrink <= 1;
		if (worse) {
			reasons.add("Ranking or high-edge WIN calibration worsened in at least two leagues without offsetting gains.");
			return new ClassificationDecision(ProbabilityModelV2Classification.MODEL_V2_WORSE, reasons);
		}
		boolean partial = logLossWorse == 0
				&& (spearmanMaterial + spearmanSmall >= 2 || winGapShrink >= 2 || (spearmanMaterial >= 1 && winGapShrink >= 1))
				&& spearmanWorse <= 1;
		if (partial) {
			reasons.add("Some ranking or calibration gates improved, but not the full CLEAR set across all three leagues.");
			return new ClassificationDecision(ProbabilityModelV2Classification.MODEL_V2_PARTIAL_IMPROVEMENT, reasons);
		}
		reasons.add("Changes were mixed or smaller than the predeclared material thresholds. ROI was not used.");
		if (decileImproved > 0) {
			reasons.add("Decile ROI inversions improved in " + decileImproved + " league(s); not sufficient alone.");
		}
		return new ClassificationDecision(ProbabilityModelV2Classification.MODEL_V2_NO_MEANINGFUL_IMPROVEMENT, reasons);
	}

	private static boolean gapShrunk(BigDecimal v1AbsGap, BigDecimal v2AbsGap) {
		if (v1AbsGap == null || v2AbsGap == null) {
			return false;
		}
		return v1AbsGap.subtract(v2AbsGap).compareTo(HIGH_EDGE_GAP_SHRINK) >= 0;
	}

	private static boolean gapWorsened(BigDecimal v1AbsGap, BigDecimal v2AbsGap) {
		if (v1AbsGap == null || v2AbsGap == null) {
			return false;
		}
		return v2AbsGap.subtract(v1AbsGap).compareTo(HIGH_EDGE_GAP_SHRINK) >= 0;
	}

	private static boolean compressed(EdgeQuantiles v1, EdgeQuantiles v2) {
		if (v1 == null || v2 == null || v1.p99() == null || v2.p99() == null) {
			return false;
		}
		return v2.p99().compareTo(v1.p99()) < 0;
	}

	private static boolean atLeast(BigDecimal value, BigDecimal threshold) {
		return value != null && value.compareTo(threshold) >= 0;
	}

	private static boolean atMost(BigDecimal value, BigDecimal threshold) {
		return value != null && value.compareTo(threshold) <= 0;
	}

	private static boolean worsenedBy(BigDecimal delta, BigDecimal threshold) {
		return delta != null && delta.compareTo(threshold) > 0;
	}

	private static String nullSafe(BigDecimal value) {
		return value == null ? "n/a" : value.stripTrailingZeros().toPlainString();
	}

	public record ClassificationDecision(ProbabilityModelV2Classification classification, List<String> reasons) {

		public ClassificationDecision {
			if (classification == null) {
				throw new IllegalArgumentException("classification is required");
			}
			reasons = List.copyOf(reasons == null ? List.of() : reasons);
		}
	}
}
